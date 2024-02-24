package com.sample.controllers;

import com.sample.kafka.ConsumerService;
import com.sample.kafka.ProducerService;
import com.sample.model.ResponseModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping(value = "/avro")
public class KafkaController {


    @Autowired
    ProducerService producer;

    @Autowired
    ConsumerService consumer;

    @PostMapping("/producer")
    public String sendMessageToKafkaTopic(@RequestBody ResponseModel responseModel) throws IOException {
        producer.sendMessage(responseModel);
        return "Message published successfully";
    }

    @GetMapping("/consumer")
    public ResponseModel getMessageFromKafkaTopic() throws IOException{
        return consumer.readMessages();
    }
}
