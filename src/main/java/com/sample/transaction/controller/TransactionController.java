package com.sample.transaction.controller;

import com.sample.transaction.exception.TransactionException;
import com.sample.transaction.kafka.KafkaConsumerHandler;
import com.sample.transaction.model.ProducerResponse;
import com.sample.transaction.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class TransactionController {

    @Autowired
    TransactionService transactionService;

    @Autowired
    KafkaConsumerHandler kafkaConsumerHandler;

    @GetMapping("/sendHandOffNotification")
    public ResponseEntity<ProducerResponse> sendHandOffNotification(){
        try {
            return new ResponseEntity<>(transactionService.sendHandOffNotification(), HttpStatus.OK);
        } catch (TransactionException e) {
            return new ResponseEntity<>(new ProducerResponse(null,"HandOff Notification failed with exception - "+e.getErrorMessage()),HttpStatus.valueOf(e.getErrorCode()));
        }
    }

    @GetMapping("/startTransactionConsumer")
    public ResponseEntity<String> startTransactionConsumer(){
        transactionService.deleteStaticEntries();
        transactionService.deleteFileEntries();
        transactionService.deleteCustomerEntries();
        if(kafkaConsumerHandler.startListener("id1")){
            return new ResponseEntity<>("Transaction Consumer started successfully", HttpStatus.OK);
        }
        else{
            return new ResponseEntity<>("Consumer cannot be started as the listener id is invalid", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/stopTransactionConsumer")
    public ResponseEntity<String> stopTransactionConsumer(){
        if(kafkaConsumerHandler.stopListener("id1")){
            return new ResponseEntity<>("Transaction Consumer started successfully", HttpStatus.OK);
        }
        else{
            return new ResponseEntity<>("Consumer cannot be started as the listener id is invalid", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/validateTransactions")
    public ResponseEntity<String> validateTransactions(@RequestParam("reqMsgId") String reqMsgId){
       try{
           String response = transactionService.validateTransactions(reqMsgId);
           return new ResponseEntity<>(response, HttpStatus.OK);
       }
       catch(TransactionException e){
           return new ResponseEntity<>(e.getErrorMessage(),HttpStatus.valueOf(e.getErrorCode()));
       }
    }

}