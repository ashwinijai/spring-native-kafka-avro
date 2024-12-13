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
import java.util.ArrayList;
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
        List<String> validationFailures = new ArrayList<>();
            staticDetailsList = staticDetailsRepository.getStaticDetails("N");
            stagedFiles = fileDetailsRepository.getFileDetails("N");
            if(CollectionUtils.isEmpty(staticDetailsList) && CollectionUtils.isEmpty(stagedFiles))
                return "No Messages pending for validation";
            staticDetailsValidation(staticDetailsList, reqMsgId, validationFailures);
            validatePayments(stagedFiles, "MX", "I", reqMsgId, validationFailures);
            validatePayments(stagedFiles, "MX", "O", reqMsgId, validationFailures);
            validatePayments(stagedFiles, "MT", "I", reqMsgId, validationFailures);
            validatePayments(stagedFiles, "MT", "O", reqMsgId, validationFailures);
            if(CollectionUtils.isEmpty(validationFailures)) {
                validationSuccessful(reqMsgId);
                // save into transaction table get distinct customer Numbers from transaction table
                saveTransactions(stagedFiles);
                List<String> customerNumberList = transactionDetailsRepository.getDistinctCustomerNos();
                log.info("List of Distinct customer Nos - " + customerNumberList.toString());
                return "Payments validated successfully for reqMsgId - "+reqMsgId;
            }
            else {
                validationFailure(reqMsgId, validationFailures);
                return  "Payments validation failed for reqMsgId - "+reqMsgId+" with errors - "+validationFailures.toString();
            }

    }

    private void updateStaticValidationStatus(List<StaticDetails> staticDetailsList, String status) {
        if (!CollectionUtils.isEmpty(staticDetailsList)) {
            staticDetailsList.forEach(s -> s.setProcessed(status));
            staticDetailsRepository.saveAll(staticDetailsList);
        }

    }
    private void updateFileValidationStatus(List<FileDetails> stagedFiles, String status) {
        if (!CollectionUtils.isEmpty(stagedFiles)) {
        stagedFiles.forEach(s -> s.setProcessed(status));
        fileDetailsRepository.saveAll(stagedFiles);
    }}
    private void updateIndividualFileValidationStatus(FileDetails stagedFile, String status) {
            stagedFile.setProcessed(status);
            fileDetailsRepository.save(stagedFile);
        }


    private void staticDetailsValidation(List<StaticDetails> staticDetailsList, String kafkaMsgId, List<String> validationFailures) {
        boolean isFailed = false;
        if (CollectionUtils.isEmpty(staticDetailsList)) {
            validationFailures.add("No static message received for KafkaMsgId - " + kafkaMsgId);
            isFailed = true;
        }
        else if (staticDetailsList.size() > 1) {
            validationFailures.add("More than 1 static message received for KafkaMsgId - " + kafkaMsgId);
            isFailed = true;
        }
        else if (!staticDetailsList.get(0).getRequestMsgId().equals(kafkaMsgId)) {
            validationFailures.add("Validation failed for STATIC message. KafkaMsgId - " + kafkaMsgId + " doesn't match with fileRefId of STATIC message");
            isFailed = true;
        }
        if(isFailed)
            updateStaticValidationStatus(staticDetailsList, "F");
        else
            updateStaticValidationStatus(staticDetailsList, "P");
    }

    private void validationFailure(String reqMsgId, List<String> errorMessage) {
        try {
            stringProducer.sendMessage("NACK for ReqMsgId - " + reqMsgId + " with exception - " + errorMessage.toString(), "acknowledgmentTopic");
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

    private void validatePayments(List<FileDetails> stagedFiles, String paymentType, String eftDirection, String reqMsgId, List<String> validationFailures) {
        List<FileDetails> paymentList = stagedFiles.stream().filter(f -> f.getPaymentType().equals(paymentType) && f.getEftDir().equals(eftDirection)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(paymentList))
            validationFailures.add("No payment found for the combination " + paymentType + " and " + eftDirection);
        else
            validateTransactions(stagedFiles, paymentType, eftDirection, reqMsgId, validationFailures);
    }

    private void validateTransactions(List<FileDetails> filesToBeStaged, String paymentType, String eftDirection, String reqMsgId, List<String> validationFailures) {
        filesToBeStaged.sort(Comparator.comparing(FileDetails::getFileRefNo));
        int noOfFiles = Integer.parseInt(filesToBeStaged.get(0).getPageBlock().split("/")[1]);
        log.info("Validation for payment type - "+paymentType +" and eftDirection  - "+eftDirection);
        log.info("Number of files expected - "+ noOfFiles);
        log.info("Number of files received - "+ filesToBeStaged.size());
        int totalTransactions = filesToBeStaged.get(0).getTotalTxCount().intValue();
        boolean isFailure = false;
        //validate if we have received all transactions within the timeframe
        if (filesToBeStaged.size() != noOfFiles) {
            log.error("All messages not received within the timeframe for payment type - " + paymentType + " and eftDirection" + eftDirection );
            validationFailures.add("All messages not received within the timeframe for payment type - " + paymentType + " and eftDirection - " + eftDirection );
            isFailure=true;
            updateFileValidationStatus(filesToBeStaged, "F");
        } else {
            //validate transaction count and total transaction count
            int totalTxs = 0;
            for (FileDetails f : filesToBeStaged) {
                //validate ReqMsgId
                if (!f.getRequestMsgId().equals(reqMsgId)) {
                    validationFailures.add("ReqMsgId doesn't match for FileRefId - " + f.getFileRefNo()+ "for payment type - " + paymentType + " and eftDirection - " + eftDirection );
                    isFailure=true;
                    updateIndividualFileValidationStatus(f,"F");
                }
                else {
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        FileRequest request = mapper.readValue(f.getPaymentMsg(), FileRequest.class);
                        //validate transaction count
                        if (request.getTransactions().size() != request.getTxCount().intValue()) {
                            log.error("Transaction counts doesn't match");
                            validationFailures.add("Transaction counts doesn't match for payment type - " + paymentType + " and eftDirection - " + eftDirection + " in FileRefNo -" + request.getFileReferenceNo());
                            updateIndividualFileValidationStatus(f,"F");
                            isFailure=true;
                        } else {
                            totalTxs = totalTxs + request.getTransactions().size();
                        }
                    } catch (IOException e) {
                        updateFileValidationStatus(filesToBeStaged, "F");
                        throw new TransactionException(500, e.getMessage());
                    }
                }
            }
            //validate total transaction count
            if (!isFailure && totalTxs != totalTransactions) {
                log.error("Total Transaction counts doesn't match");
                updateFileValidationStatus(filesToBeStaged, "F");
                validationFailures.add("Total Transaction counts doesn't match for payment type - " + paymentType + " and eftDirection - " + eftDirection);
            }
        }
        if(!isFailure){
            updateFileValidationStatus(filesToBeStaged, "P");
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

