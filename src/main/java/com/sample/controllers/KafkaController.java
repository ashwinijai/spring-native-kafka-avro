package com.sample.controllers;

import com.sample.kafka.*;
import com.sample.model.GenericAvroBean;
import com.sample.model.ResponseModel;
import com.sample.transaction.repo.CustomRepository;
import org.apache.commons.compress.utils.FileNameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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

    @Autowired
    FileProducerService fileProducer;

    @Autowired
    FileConsumerService fileConsumer;

    @Autowired
    CustomRepository customRepository;

    @PostMapping("/producer")
    public String sendMessageToKafkaTopic(@RequestBody ResponseModel responseModel) throws IOException {
        producer.sendMessage(responseModel);
        return "Message published successfully";
    }

    @GetMapping("/consumer")
    public ResponseModel getMessageFromKafkaTopic() throws IOException {
        consumer.readMessages();
        return null;
    }

    @PostMapping("/producer/generic")
    public String sendMessageToKafkaTopic(@RequestPart("schemaFile") MultipartFile schemaFile, @RequestPart("messageFile") MultipartFile messageFile) {
        try {
            String avroSchema = new String(schemaFile.getBytes());
            String avroMessage = new String(messageFile.getBytes());
            GenericAvroBean avroBean = GenericAvroBean.builder().avroSchema(avroSchema).avroMessage(avroMessage).build();
            genericProducer.sendMessage(avroBean);
            return "Message published successfully";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    @GetMapping("/consumer/generic")
    public List<GenericAvroBean> getGenMessageFromKafkaTopic() {
        List<GenericAvroBean> avroBeanList = null;
        avroBeanList = genericConsumer.readMessages();
        return avroBeanList;
    }

    @PostMapping("/producer/file")
    public String sendFileToKafka(@RequestPart("messageFile") MultipartFile messageFile) {
        try {
            byte[] messageBytes = messageFile.getBytes();
            String fileExtn = FileNameUtils.getExtension(messageFile.getOriginalFilename());
            String fileName = FileNameUtils.getBaseName(messageFile.getOriginalFilename());
            fileProducer.sendMessage(messageBytes, fileName, fileExtn);
            return "Message published successfully";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    @GetMapping("/consumer/file")
    public ResponseEntity<byte[]> getFileFromKafka() throws Exception {
        Map<String, Object> outputMap = fileConsumer.readMessages();
        if (!outputMap.isEmpty())
            return new ResponseEntity<>((byte[]) outputMap.get("docContent"), (HttpHeaders) outputMap.get("headers"), HttpStatus.OK);
        else
            return new ResponseEntity<>("No messages to consume".getBytes(), null, HttpStatus.INTERNAL_SERVER_ERROR);

    }

    @GetMapping("/executeCustomQuery")
    public ResponseEntity<List<Object[]>> executeCustomQuery(){
        return new ResponseEntity<>(customRepository.executeQueryFromCache("select * from FILE_DETAILS"),HttpStatus.OK);
    }

}
