package com.sample.transaction.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sample.transaction.entity.FileDetails;
import com.sample.transaction.entity.StaticDetails;
import com.sample.transaction.model.FileRequest;
import com.sample.transaction.model.RequestModel;
import com.sample.transaction.model.StaticRequest;
import com.sample.transaction.repo.FileDetailsRepository;
import com.sample.transaction.repo.StaticDetailsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TransactionConsumer {
    @Autowired
    FileDetailsRepository fileDetailsRepository;

    @Autowired
    StaticDetailsRepository staticDetailsRepository;

    //@KafkaListener(id = "id1", topics = "transactionTopic", groupId = "group_id", containerFactory = "concurrentKafkaListenerContainerFactory", autoStartup = "false")
    @KafkaListener(topics = "transactionTopic", groupId = "group_id")
    public void consume(String message) throws JsonProcessingException {
        log.info("All messages consumed");
            ObjectMapper mapper = new ObjectMapper();
            RequestModel request = mapper.readValue(message, RequestModel.class);
            if (request.getResponseStatus().equals("PASS") && request.getResponseType().equals("STATIC")) {
                Long sNo = staticDetailsRepository.getMaxSno();
                StaticRequest staticRequest = mapper.readValue(request.getResponseMsg(), StaticRequest.class);
                StaticDetails staticDetails = new StaticDetails();
                staticDetails.setSNo(null!=sNo?sNo + 1:1);
                staticDetails.setDetailCount(staticRequest.getDetailCount());
                staticDetails.setProcessed("Y");
                staticDetails.setServiceName("ServiceName");
                staticDetails.setStaticDataMsg(request.getResponseMsg());
                staticDetails.setFileRefNo(staticRequest.getFileReferenceNo());
                staticDetails.setClientName("FINTRAC");
                staticDetails.setCreatedBy("SYSTEM");
                staticDetails.setRequestMsgId(request.getRequestMsgId());
                staticDetails.setResponseStatus(request.getResponseStatus());
                staticDetails.setResponseType(request.getResponseType());
                staticDetailsRepository.save(staticDetails);
            } else if (request.getResponseStatus().equals("PASS") && request.getResponseType().equals("PAYMENT")) {
                FileRequest fileRequest = mapper.readValue(request.getResponseMsg(), FileRequest.class);
                if ((fileRequest.getPaymentType().equals("MX") || fileRequest.getPaymentType().equals("MT")) && (fileRequest.getEftDirection().equals("I") || fileRequest.getEftDirection().equals("O"))) {
                   Long sNo = fileDetailsRepository.getMaxSno();
                    FileDetails fileDetails = new FileDetails();
                    fileDetails.setSNo(null!=sNo?sNo + 1:1);
                    fileDetails.setServiceName("ServiceName");
                    fileDetails.setFileRefNo(fileRequest.getFileReferenceNo());
                    fileDetails.setMsgDate(fileRequest.getExtractDateTime());
                    fileDetails.setPageBlock(fileRequest.getPageBlock());
                    fileDetails.setEftDir(fileRequest.getEftDirection());
                    fileDetails.setPaymentType(fileRequest.getPaymentType());
                    fileDetails.setTxCount(fileRequest.getTxCount());
                    fileDetails.setTotalTxCount(fileRequest.getTotalTxnCount());
                    fileDetails.setProcessed("N");
                    fileDetails.setPaymentMsg(request.getResponseMsg());
                    fileDetails.setRequestMsgId(request.getRequestMsgId());
                    fileDetails.setResponseStatus(request.getResponseStatus());
                    fileDetails.setResponseType(request.getResponseType());
                    fileDetailsRepository.save(fileDetails);
                }
            }
            else {
                    log.error("Not a valid message, cannot be staged");
                }
    }


}
