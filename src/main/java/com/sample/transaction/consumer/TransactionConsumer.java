package com.sample.transaction.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sample.kafka.StringProducer;
import com.sample.transaction.entity.FileDetails;
import com.sample.transaction.entity.TransactionDetails;
import com.sample.transaction.model.FileRequest;
import com.sample.transaction.model.TransactionModel;
import com.sample.transaction.repo.FileDetailsRepository;
import com.sample.transaction.repo.TransactionDetailsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Service
@Slf4j
public class TransactionConsumer {
    @Autowired
    FileDetailsRepository fileDetailsRepository;

    @Autowired
    TransactionDetailsRepository transactionDetailsRepository;

    @Autowired
    StringProducer stringProducer;
    Timer timer;

    @KafkaListener(topics = "transactionTopic", groupId = "group_id_1")
    public void consume(String message) throws JsonProcessingException {
        log.info("Meesage consumed");
        ObjectMapper mapper = new ObjectMapper();
        FileRequest request =  mapper.readValue(message,FileRequest.class);
        String extractDate = request.getExtractDateTime().substring(0,request.getExtractDateTime().indexOf("T"));
        log.info("Message extract Date - "+extractDate);

        //find if this is the first message and start timer
        if(CollectionUtils.isEmpty(fileDetailsRepository.findFilesByDate(extractDate, "N"))){
            timer =new Timer();
            timer.schedule(new MessageProcessingTimer(), 60000);
        }

        //save json to DB
        FileDetails fileDetails = new FileDetails();
        fileDetails.setFileRefNo(request.getFileReferenceNo());
        fileDetails.setCreatedDate(extractDate);
        fileDetails.setControlSum(request.getControlSum());
        fileDetails.setPageBlock(request.getPageBlock());
        fileDetails.setEffDirection(request.getEftDirection());
        fileDetails.setPaymentType(request.getPaymentType());
        fileDetails.setTxCount(request.getTotalTxnCount());
        fileDetails.setTotalTxCount(request.getTxCount());
        fileDetails.setIsStagingCompleted("N");
        fileDetails.setRawJson(message);
        fileDetailsRepository.save(fileDetails);
    }
    class MessageProcessingTimer extends TimerTask {
        @Override
        public void run() {
            log.info("Inside timer task");
            List<FileDetails> filesToBeStaged = fileDetailsRepository.findUnProcessedFiles("N");
            //validate
            if(!CollectionUtils.isEmpty(filesToBeStaged)){
                int noOfFiles = Integer.parseInt(filesToBeStaged.get(0).getPageBlock().split("/")[1]);
                //validation failure (need to add few more conditions)
                if(filesToBeStaged.size()!=noOfFiles){
                    log.info("All messages not received within timeout");
                    try {
                        //delete all entries and send NACK
                        fileDetailsRepository.deleteInBatch(filesToBeStaged);
                        stringProducer.sendMessage("NACK","acknowledgmentTopic");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                // validation successful
                else{
                    try {
                        //send ACK and mark entries as staged successfully
                        filesToBeStaged.forEach(f -> f.setIsStagingCompleted("Y"));
                        fileDetailsRepository.saveAll(filesToBeStaged);
                        stringProducer.sendMessage("ACK","acknowledgmentTopic");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    // save transactions
                    filesToBeStaged.forEach( f -> {
                        ObjectMapper mapper = new ObjectMapper();
                        try {
                            FileRequest request = mapper.readValue(f.getRawJson(), FileRequest.class);
                            List<TransactionModel> transactionModels = request.getTransactions();
                            transactionModels.forEach( t -> {
                                TransactionDetails transaction = new TransactionDetails();
                                transaction.setTransactionRef(t.getTransactionReference());
                                transaction.setAccountNo(t.getAccountNumber());
                                transaction.setSourceOfTransaction(t.getSource());
                                transaction.setAccountCcy(t.getAccountCcy());
                                transaction.setPaymentMsg(t.getPaymentMessage());
                                transaction.setCustomerNo(t.getCustomerNo());
                                transactionDetailsRepository.save(transaction);
                            });
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    } );
                    //get distinct customer Numbers from transaction table
                    List<String> customerNumberList = transactionDetailsRepository.getDistinctCustomerNos();
                    log.info("List of Distinct customer Nos - "+ customerNumberList.toString());
                }
            }
            timer.cancel();
        }
    }
}
