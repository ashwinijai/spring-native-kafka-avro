package com.sample.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.KafkaListeners;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AllMessagesConsumer {

    @Value("${topicName}")
    private String topicName;

//    @KafkaListener (topics = "${topicName}", groupId = "generic-record-consumer-group", containerFactory = "kafkaListenerContinerFactory", autoStartup = "false")
//    public void consume(String message){
//        log.info("Message consumed from topic - {}, Message - {}",topicName, message);
//    }

}
