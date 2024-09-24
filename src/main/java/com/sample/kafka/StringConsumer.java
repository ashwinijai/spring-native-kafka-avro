package com.sample.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sample.entity.Transaction;
import com.sample.model.RequestPojo;
import com.sample.repo.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StringConsumer {

    @Autowired
    TransactionRepository transactionRepository;

    @KafkaListener(topics = "mongoTopic", groupId = "group_id")
    public void consume(String message) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
                List<RequestPojo> jsonList = mapper.readValue(message, new TypeReference<>(){});
                List<Transaction> transactionList=jsonList.stream().map( json -> {
                    Transaction t = new Transaction();
                    t.setTransactionId(Long.valueOf(json.getId()));
                    t.setToAccNumber(Long.valueOf(json.getTo()));
                    t.setFromAccNumber(Long.valueOf(json.getFrom()));
                    t.setTransactionDate("24-Sep-2024");
                    t.setAmount(Long.valueOf(json.getAmt()));
                    return t;
                }).collect(Collectors.toList());
                transactionRepository.saveAll(transactionList);
                log.info("No of transactions inserted - {}",transactionRepository.count());
            }


    public List<Transaction> getAllValues(){
        return transactionRepository.findAll();
    }
}
