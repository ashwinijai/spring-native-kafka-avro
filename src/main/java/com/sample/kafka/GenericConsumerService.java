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
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

@Service
@Slf4j
public class GenericConsumerService {

    public GenericAvroBean readMessages(GenericAvroBean genericAvroBean) throws Exception {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "generic-record-consumer-group");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

        properties.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");

        KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(properties);

        consumer.subscribe(Collections.singleton("avro-topic"));

        while (true) {
            ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, byte[]> record : records) {
                String avroSchema = null;
                if (null == genericAvroBean) {
                    genericAvroBean = new GenericAvroBean();
                }
                if (null != record.headers() && null != record.headers().headers("schema")) {
                    avroSchema = new String(record.headers().headers("schema").iterator().next().value());
                    genericAvroBean.setAvroSchema(avroSchema);
                } else if (null != genericAvroBean.getAvroSchema()) {
                    avroSchema = genericAvroBean.getAvroSchema();
                }
                if (null == avroSchema) {
                    throw new Exception("No schema found to deserialize the avro message");
                }
                log.info("schema- {}",genericAvroBean.getAvroSchema());
                Schema.Parser parser = new Schema.Parser();
                Schema schema = parser.parse(avroSchema);
                SpecificDatumReader<GenericRecord>
                        datumReader =
                        new SpecificDatumReader<>(schema);
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(record.value());
                BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(byteArrayInputStream, null);

                GenericRecord genRecord = datumReader.read(null, binaryDecoder);
                genericAvroBean.setAvroMessage(genRecord.toString());
                return genericAvroBean;
            }
        }
    }
}
