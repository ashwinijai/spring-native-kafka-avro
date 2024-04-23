package com.sample.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ProducerScheduler {

    @Value("${topicName}")
    private String topicName;

    @Scheduled(cron="${scheduler.cron.value}")
    public void produceMessage(){

    }
}
