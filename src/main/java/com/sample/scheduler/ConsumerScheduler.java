package com.sample.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.event.ListenerContainerIdleEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ConsumerScheduler {
    @Autowired
    KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;
    @Value("${topicName}")
    private String topicName;
   // @Scheduled(cron="${scheduler.cron.value}")
    public void consumeFromTopic(){
        kafkaListenerEndpointRegistry.getListenerContainer(topicName).start();
    }
    @EventListener
    void listen(ListenerContainerIdleEvent event){
        kafkaListenerEndpointRegistry.getListenerContainer(topicName).stop();
    }
}
