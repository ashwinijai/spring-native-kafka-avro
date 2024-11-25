package com.sample.controllers;

import com.sample.SampleApplication;
import com.sample.entity.Transaction;
import com.sample.kafka.StringConsumer;
import com.sample.kafka.StringProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
public class MongoController {

    @Value("${refreshed.value}")
    String actuatorTestValue;

    @Autowired
    StringProducer mongoProducer;

    @Autowired
    StringConsumer mongoConsumer;

    @PostMapping("/stringProducer")
    public String postToMongoTopic(@RequestBody String jsonRequest, @RequestParam("topicName") String topicName) throws IOException {
        mongoProducer.sendMessage(jsonRequest,topicName);
        return "Message posted successfully";
    }

    @GetMapping("/getAllTransactions")
    public List<Transaction> getAllTransactions(){
        return mongoConsumer.getAllValues();
    }

    @GetMapping("/restart")
    public String restart(){
        SampleApplication.restart();
        return "Application restarted successfully";
    }

}
