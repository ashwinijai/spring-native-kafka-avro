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
        Properties consumerProperties = new Properties();
        consumerProperties.put("bootstrap.servers", "localhost:9092");
        consumerProperties.put("group.id", "zookeeperGroupId");
        consumerProperties.put("auto.offset.reset","latest");
        consumerProperties.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        consumerProperties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(consumerProperties);

        consumer.subscribe(Collections.singleton("file-topic"));

            ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(100));
            while(true) {
                for (ConsumerRecord<String, byte[]> record : records.records("file-topic")) {
                   ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(record.value());
                    byte[] docContent = byteArrayInputStream.readAllBytes();
                    //String docContent = record.value();
                    String fileName = "messageFile";
                    String fileExtn = null;
                    if (null != record.headers()) {
                        Iterable<Header> fileNameHeader = record.headers().headers("fileName");
                        Iterable<Header> fileExtnHeader = record.headers().headers("fileExtn");
                        if (null == fileExtnHeader)
                            throw new Exception("File cant be consumed as Extension is empty");
                        outputMap.put("docContent", docContent);
                        fileName = new String(fileNameHeader.iterator().next().value());
                        fileExtn = new String(fileExtnHeader.iterator().next().value());
                        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                        headers.setContentLength(docContent.length);
                        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; fileName=" + fileName + "." + fileExtn);
                        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                        outputMap.put("headers", headers);
                    }


                }
                return outputMap;
            }

    }
}
