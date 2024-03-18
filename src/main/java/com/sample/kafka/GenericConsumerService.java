package com.sample.kafka;

import com.sample.model.GenericAvroBean;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GenericConsumerService {

    public List<GenericAvroBean> readMessages()  {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "generic-record-consumer-group");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        properties.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");

        KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(properties);
        List<TopicPartition> partitionList = consumer.partitionsFor("avro-topic").stream().map(it -> new TopicPartition(it.topic(), it.partition())).collect(Collectors.toList());
        consumer.assign(partitionList);
        List<GenericAvroBean> avroBeanList = new ArrayList<>();
            ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(100));
            int messageNo = 0;
            if(records.isEmpty()){
                GenericAvroBean genericAvroBean= GenericAvroBean.builder().errorMessage("No messages to consume from topic").build();
                avroBeanList.add(genericAvroBean);
                return avroBeanList;
            }
            for (ConsumerRecord<String, byte[]> record : records) {
                log.info("offset"+record.offset());
                GenericAvroBean genericAvroBean = new GenericAvroBean();
                try {
                    String avroSchema = null;
                    if (null != record.headers() && null != record.headers().headers("schema") && record.headers().headers("schema").iterator().hasNext()) {
                        log.info("Schema is - {}", record.headers().headers("schema"));
                        avroSchema = new String(record.headers().headers("schema").iterator().next().value());
                        genericAvroBean.setAvroSchema(avroSchema);
                    }
                    if (null == avroSchema) {
                        throw new Exception("No schema found to deserialize the avro message");
                    }
                    log.info("schema- {}", genericAvroBean.getAvroSchema());
                    Schema.Parser parser = new Schema.Parser();
                    Schema schema = parser.parse(avroSchema);
                    SpecificDatumReader<GenericRecord>
                            datumReader =
                            new SpecificDatumReader<>(schema);
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(record.value());
                    BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(byteArrayInputStream, null);

                    GenericRecord genRecord = datumReader.read(null, binaryDecoder);
                    genericAvroBean.setAvroMessage(genRecord.toString());
                    genericAvroBean.setMessageNo(++messageNo);
                }catch(Exception e){
                    genericAvroBean= GenericAvroBean.builder().errorMessage(e.getMessage()).build();
                    genericAvroBean.setMessageNo(++messageNo);
                }
                avroBeanList.add(genericAvroBean);
            }

            consumer.commitSync();
        return avroBeanList;
    }
}
