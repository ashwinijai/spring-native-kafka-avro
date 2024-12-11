package com.sample.transaction.kafka;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaConsumerHandler {

    @Autowired
    KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    public boolean startListener(String listenerId) {
        MessageListenerContainer listenerContainer =
                kafkaListenerEndpointRegistry.getListenerContainer(listenerId);
        if (listenerContainer == null) return false;
        listenerContainer.start();
        log.info("{} Kafka Listener Started", listenerId);
        return true;
    }

    public boolean stopListener(String listenerId) {
        MessageListenerContainer listenerContainer =
                kafkaListenerEndpointRegistry.getListenerContainer(listenerId);
        if (listenerContainer == null) return false;
        listenerContainer.stop();
        log.info("{} Kafka Listener Stopped.", listenerId);
        return true;
    }
}
