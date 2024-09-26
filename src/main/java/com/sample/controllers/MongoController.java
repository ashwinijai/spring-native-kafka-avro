package com.sample.controllers;

import com.sample.SampleApplication;
import com.sample.entity.Transaction;
import com.sample.kafka.StringConsumer;
import com.sample.kafka.StringProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
public class MongoController {

    @Autowired
    StringProducer mongoProducer;

    @Autowired
    StringConsumer mongoConsumer;

    @PostMapping("/mongoStringProducer")
    public String postToMongoTopic(@RequestBody String bigJson) throws IOException {
        mongoProducer.sendMessage(bigJson);
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
