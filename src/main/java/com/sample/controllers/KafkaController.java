package com.sample.controllers;

import com.sample.kafka.ConsumerService;
import com.sample.kafka.GenericConsumerService;
import com.sample.kafka.GenericProducerService;
import com.sample.kafka.ProducerService;
import com.sample.model.GenericAvroBean;
import com.sample.model.ResponseModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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
         consumer.readMessages();
         return null;
    }

    @PostMapping("/producer/generic")
    public String sendMessageToKafkaTopic(@RequestPart("schemaFile") MultipartFile schemaFile, @RequestPart("messageFile") MultipartFile messageFile)  {
        try {
            String avroSchema = new String(schemaFile.getBytes());
            String avroMessage = new String(messageFile.getBytes());
            GenericAvroBean avroBean = GenericAvroBean.builder().avroSchema(avroSchema).avroMessage(avroMessage).build();
            genericProducer.sendMessage(avroBean);
            return "Message published successfully";
        }
        catch(Exception e){
            return e.getMessage();
        }
    }
    @GetMapping("/consumer/generic")
    public List<GenericAvroBean> getGenMessageFromKafkaTopic()  {
        List<GenericAvroBean> avroBeanList = null;
        avroBeanList = genericConsumer.readMessages();
        return avroBeanList;
    }

}
