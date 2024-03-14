package com.sample.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sample.model.GenericAvroBean;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.*;

@Service
@Slf4j
public class GenericProducerService {

    public void sendMessage(GenericAvroBean avroBean) throws Exception {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        properties.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");

        Producer<String, byte[]> producer = new KafkaProducer<>(properties);

        Schema.Parser parser = new Schema.Parser();
        Schema schema = parser.parse(avroBean.getAvroSchema());
        GenericRecord avroRecord = new GenericData.Record(schema);

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> schemaMap = mapper.readValue(avroBean.getAvroSchema(), new TypeReference<>() {
        });
        Map<String, Object> messageMap = mapper.readValue(avroBean.getAvroMessage(), new TypeReference<>() {
        });
        log.info("Keys: {}", schemaMap.keySet());
        if(schemaMap.get("fields") instanceof List){
            List<Map<String, String>> fieldsList = (List<Map<String, String>>)schemaMap.get("fields");
            List<String> schemaFields= new ArrayList<>();
            log.info("Keys inside fields - {}", fieldsList.size());
            log.info(fieldsList.toString());
            for (Map<String, String> fieldsMap : fieldsList) {
                schemaFields.add(fieldsMap.get("name"));
                log.info(fieldsMap.get("name"));
            }
            for (String field : schemaFields) {
                if (messageMap.containsKey(field)) {
                    avroRecord.put(field, messageMap.get(field));
                }

            }
            if(null==avroRecord.get(1)){
                throw new Exception("Schema and message doesn't match");
            }
            log.info(avroRecord.toString());
            SpecificDatumWriter<GenericRecord>
                    datumWriter =
                    new SpecificDatumWriter<>(schema);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byteArrayOutputStream.reset();
            BinaryEncoder binaryEncoder = new EncoderFactory().binaryEncoder(byteArrayOutputStream, null);
            datumWriter.write(avroRecord, binaryEncoder);
            System.out.println("Schema" + avroRecord.getSchema().toString());
            binaryEncoder.flush();
            byte[] bytes = byteArrayOutputStream.toByteArray();

            //prepare the kafka record
            ProducerRecord<String, byte[]> record = new ProducerRecord<>("avro-topic", null, bytes);
            record.headers().add("schema", avroBean.getAvroSchema().getBytes());
            log.info("Schema from header" + new String(record.headers().headers("schema").iterator().next().value()));
            producer.send(record);
            //ensures record is sent before closing the producer
            producer.flush();



            producer.close();
        }

    }
}
