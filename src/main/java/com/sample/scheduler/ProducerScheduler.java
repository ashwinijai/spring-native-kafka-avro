package com.sample.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class ProducerScheduler {

    @Value("${topicName}")
    private String topicName;

    @Autowired
    RestTemplate restTemplate;

   // @Scheduled(cron="${scheduler.cron.value}")
    public void produceMessage(){
        log.info("Producer scheduler - start");
        HttpEntity<String> entity = new HttpEntity<>("This is the message to topic",new HttpHeaders());
        ResponseEntity<String> responseEntity = restTemplate.exchange("http://localhost:8080/producer/", HttpMethod.POST, entity, String.class);
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            String response = responseEntity.getBody();
            log.info("Response from producer - {}", response);
        }
        log.info("Producer scheduler - end");
    }
}
