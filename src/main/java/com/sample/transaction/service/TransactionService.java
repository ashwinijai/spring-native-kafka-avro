package com.sample.transaction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sample.kafka.StringProducer;
import com.sample.transaction.entity.FileDetails;
import com.sample.transaction.entity.KafkaAudit;
import com.sample.transaction.entity.StaticDetails;
import com.sample.transaction.entity.TransactionDetails;
import com.sample.transaction.exception.TransactionException;
import com.sample.transaction.model.FileRequest;
import com.sample.transaction.model.ProducerResponse;
import com.sample.transaction.model.TransactionModel;
import com.sample.transaction.repo.FileDetailsRepository;
import com.sample.transaction.repo.KafkaAuditRepository;
import com.sample.transaction.repo.StaticDetailsRepository;
import com.sample.transaction.repo.TransactionDetailsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TransactionService {

    @Autowired
    FileDetailsRepository fileDetailsRepository;

    @Autowired
    TransactionDetailsRepository transactionDetailsRepository;

    @Autowired
    StaticDetailsRepository staticDetailsRepository;
    @Autowired
    StringProducer stringProducer;

    @Autowired
    KafkaAuditRepository kafkaAuditRepository;

    public ProducerResponse sendHandOffNotification() throws TransactionException {
        ProducerResponse response = new ProducerResponse();
        response.setUuid(UUID.randomUUID().toString());
        try {
            stringProducer.sendMessage(response.toString(), "handOffTopic");
            KafkaAudit kafkaAudit = new KafkaAudit();
            kafkaAudit.setServiceName("FINTRAC");
            kafkaAudit.setRequestMsg(response.toString());
            kafkaAuditRepository.save(kafkaAudit);
        } catch (IOException e) {
            throw new TransactionException(500, e.getMessage());
        }
        response.setMessage("HandOff notification sent successfully");
        return response;

    }

    public void deleteStaticEntries() {
        staticDetailsRepository.deleteAll();
    }

    public void deleteFileEntries() {
        fileDetailsRepository.deleteAll();
    }

    public void deleteCustomerEntries() {
        transactionDetailsRepository.deleteAll();
    }

    public String validateTransactions(String reqMsgId) throws TransactionException {
        List<StaticDetails> staticDetailsList = null;
        List<FileDetails> stagedFiles = null;
        try {
            staticDetailsList = staticDetailsRepository.getStaticDetails();
            staticDetailsValidation(staticDetailsList, reqMsgId);
            stagedFiles = fileDetailsRepository.getFileDetails();
            validatePayments(stagedFiles, "MX", "I", reqMsgId);
            validatePayments(stagedFiles, "MX", "O", reqMsgId);
            validatePayments(stagedFiles, "MT", "I", reqMsgId);
            validatePayments(stagedFiles, "MT", "O", reqMsgId);
        } catch (TransactionException e) {
            updateValidationStatus(staticDetailsList, stagedFiles, "F");
            validationFailure(reqMsgId, e.getErrorMessage());
            throw e;
        }
        updateValidationStatus(staticDetailsList, stagedFiles, "P");
        validationSuccessful(reqMsgId);

        // save into transaction table get distinct customer Numbers from transaction table
        saveTransactions(stagedFiles);
        List<String> customerNumberList = transactionDetailsRepository.getDistinctCustomerNos();
        log.info("List of Distinct customer Nos - " + customerNumberList.toString());
        return "Payments validated successfully for reqMsgId - "+reqMsgId;
    }

    private void updateValidationStatus(List<StaticDetails> staticDetailsList, List<FileDetails> stagedFiles, String status) {
        if (!CollectionUtils.isEmpty(staticDetailsList)) {
            staticDetailsList.forEach(s -> s.setProcessed(status));
            staticDetailsRepository.saveAll(staticDetailsList);
        }
        if (!CollectionUtils.isEmpty(stagedFiles)) {
            stagedFiles.forEach(s -> s.setProcessed(status));
            fileDetailsRepository.saveAll(stagedFiles);
        }
    }

    private void staticDetailsValidation(List<StaticDetails> staticDetailsList, String kafkaMsgId) {
        if (CollectionUtils.isEmpty(staticDetailsList))
            throw new TransactionException(500, "No static message received for KafkaMsgId - " + kafkaMsgId);
        if (staticDetailsList.size() > 1)
            throw new TransactionException(500, "More than 1 static message received for KafkaMsgId - " + kafkaMsgId);
        if (staticDetailsList.get(0).getFileRefNo().equals(kafkaMsgId))
            throw new TransactionException(500, "Validation failed for STATIC message. KafkaMsgId - " + kafkaMsgId + " doesn't match with fileRefId of STATIC message");
    }

    private void validationFailure(String reqMsgId, String errorMessage) {
        try {
            stringProducer.sendMessage("NACK for ReqMsgId - " + reqMsgId + " with exception - " + errorMessage, "acknowledgmentTopic");
        } catch (IOException e) {
            throw new TransactionException(500, e.getMessage());
        }

    }

    private void validationSuccessful(String reqMsgId) {
        try {
            stringProducer.sendMessage("ACK - All payments validated for ReqMsgId - " + reqMsgId, "acknowledgmentTopic");
        } catch (IOException e) {
            throw new TransactionException(500, e.getMessage());
        }

    }

    private void validatePayments(List<FileDetails> stagedFiles, String paymentType, String eftDirection, String reqMsgId) {
        List<FileDetails> paymentList = stagedFiles.stream().filter(f -> f.getPaymentType().equals(paymentType) && f.getEftDir().equals(eftDirection)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(paymentList))
            throw new TransactionException(500, "No payment found for the combination " + paymentType + " and " + eftDirection);
        validateTransactions(stagedFiles, paymentType, eftDirection, reqMsgId);

    }

    private void validateTransactions(List<FileDetails> filesToBeStaged, String paymentType, String eftDirection, String reqMsgId) {
        filesToBeStaged.sort(Comparator.comparing(FileDetails::getFileRefNo));
        int noOfFiles = Integer.parseInt(filesToBeStaged.get(0).getPageBlock().split("/")[1]);
        int totalTransactions = filesToBeStaged.get(0).getTotalTxCount().intValue();
        //validate if we have received all transactions within the timeframe
        if (filesToBeStaged.size() != noOfFiles) {
            log.error("All messages not received within the timeframe");
            throw new TransactionException(500, "All messages not received within the timeframe");
        } else {
            //validate transaction count and total transaction count
            int totalTxs = 0;
            for (FileDetails f : filesToBeStaged) {
                //validate ReqMsgId
                if (!f.getRequestMsgId().equals(reqMsgId)) {
                    throw new TransactionException(500, "ReqMsgId doesn't match for FileRefId - " + f.getFileRefNo());
                }
                ObjectMapper mapper = new ObjectMapper();
                try {
                    FileRequest request = mapper.readValue(f.getPaymentMsg(), FileRequest.class);
                    //validate transaction count
                    if (request.getTransactions().size() != request.getTxCount().intValue()) {
                        log.error("Transaction counts doesn't match");
                        throw new TransactionException(500, "Transaction counts doesn't match for payment type - " + paymentType + " and eftDirection" + eftDirection + " in FileRefNo -" + request.getFileReferenceNo());
                    } else {
                        totalTxs = totalTxs + request.getTransactions().size();
                    }
                } catch (IOException e) {
                    throw new TransactionException(500, e.getMessage());
                }
            }
            //validate total transaction count
            if (totalTxs != totalTransactions) {
                log.error("Total Transaction counts doesn't match");
                throw new TransactionException(500, "Total Transaction counts doesn't match for payment type - " + paymentType + " and eftDirection" + eftDirection);
            }
        }
    }

    private void saveTransactions(List<FileDetails> stagedFiles) {
        stagedFiles.forEach(f -> {
            ObjectMapper mapper = new ObjectMapper();
            try {
                FileRequest request = mapper.readValue(f.getPaymentMsg(), FileRequest.class);
                List<TransactionModel> transactionModels = request.getTransactions();
                transactionModels.forEach(t -> {
                    TransactionDetails transaction = new TransactionDetails();
                    transaction.setSNo(transactionDetailsRepository.getMaxSno());
                    transaction.setPaymentType(request.getPaymentType());
                    transaction.setEftDir(request.getEftDirection());
                    transaction.setPageBlock(request.getPageBlock());
                    transaction.setFileRefNo(request.getFileReferenceNo());
                    transaction.setServiceName("");
                    transaction.setTransactionRef(t.getTransactionReference());
                    transaction.setAccountNo(t.getAccountNumber());
                    transaction.setTxnDt(t.getTransactionDateTime());
                    transaction.setPaymentMsg(t.getPaymentMessage());
                    transaction.setCustomerNo(t.getCustomerNo());
                    transaction.setProcessed("Y");
                    transaction.setClientName("FINTRAC");
                    transaction.setCreatedBy("SYSTEM");
                    transactionDetailsRepository.save(transaction);
                });
            } catch (JsonProcessingException e) {
                throw new TransactionException(500, e.getMessage());
            }
        });
    }
}

