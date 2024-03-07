package com.sample.controllers;

import com.sample.kafka.ConsumerService;
import com.sample.kafka.GenericConsumerService;
import com.sample.kafka.GenericProducerService;
import com.sample.kafka.ProducerService;
import com.sample.model.GenericAvroBean;
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

    @Autowired
    GenericProducerService genericProducer;

    @Autowired
    GenericConsumerService genericConsumer;

    @PostMapping("/producer")
    public String sendMessageToKafkaTopic(@RequestBody ResponseModel responseModel) throws IOException {
        producer.sendMessage(responseModel);
        return "Message published successfully";
    }

    @GetMapping("/consumer")
    public ResponseModel getMessageFromKafkaTopic() throws IOException{
        return consumer.readMessages();
    }

    @PostMapping("/producer/generic")
    public String sendMessageToKafkaTopic(@RequestBody GenericAvroBean avroBean)  {
        try {
            genericProducer.sendMessage(avroBean);
            return "Message published successfully";
        }
        catch(Exception e){
            return e.getMessage();
        }
    }
    @PostMapping("/consumer/generic")
    public GenericAvroBean getMessageFromKafkaTopic(@RequestBody GenericAvroBean avroBean)  {
        try {
            avroBean = genericConsumer.readMessages(avroBean);
        }catch(Exception e){
            avroBean.setErrorMessage(e.getMessage());
        }
        return avroBean;
    }

}
