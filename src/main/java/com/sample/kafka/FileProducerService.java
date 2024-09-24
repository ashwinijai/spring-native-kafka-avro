package com.sample.kafka;

import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class FileProducerService {

    public void sendMessage(byte[] bytes, String fileName, String fileExtn) throws Exception {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        //properties.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");

        Producer<String, byte[]> producer = new KafkaProducer<>(properties);

            //prepare the kafka record
            ProducerRecord<String, byte[]> record = new ProducerRecord<>("file-topic", bytes);
        //ProducerRecord<String, String> record = new ProducerRecord<>("file-topic", "Hello");
            record.headers().add("fileName", fileName.getBytes());
            record.headers().add("fileExtn", fileExtn.getBytes());
            producer.send(record);
        log.info("File name from header-"  + new String(record.headers().headers("fileName").iterator().next().value()));

        //ensures record is sent before closing the producer
            producer.flush();

            producer.close();
        }

    }
