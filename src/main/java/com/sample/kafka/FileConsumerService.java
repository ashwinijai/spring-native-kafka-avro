package com.sample.kafka;

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Service
@Slf4j
public class FileConsumerService {

    public Map<String, Object> readMessages() throws Exception {
        Map<String, Object> outputMap = new HashMap<>();
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "generic-file-group");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        //properties.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");

        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

        KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(properties);

        consumer.subscribe(Collections.singleton("file-topic"));

            ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, byte[]> record : records) {
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(record.value());
                byte [] docContent = byteArrayInputStream.readAllBytes();
                Headers headers1 = record.headers();

                for (Header header : headers1) {
                    String key = header.key();
                    String value = new String(header.value());
                    System.out.println("Key: " + key + ", Value: " + value);
                }
                String fileName = "messageFile";
                String fileExtn = null;
                if(null!= record.headers() ){
                    Iterable<Header> fileNameHeader = record.headers().headers("fileName");
                    Iterable<Header>  fileExtnHeader = record.headers().headers("fileExtn");
                    if(null == fileExtnHeader)
                        throw new Exception("File cant be consumed as Extension is empty");
                    outputMap.put("docContent", docContent);
                    fileName= new String(record.headers().headers("fileName").iterator().next().value());
                    fileExtn=new String(fileExtnHeader.iterator().next().value());
                    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                    headers.setContentLength(docContent.length);
                    headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; fileName=" + fileName+"."+fileExtn);
                    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                    outputMap.put("headers", headers);
                }


            }
            return outputMap;


    }
}
