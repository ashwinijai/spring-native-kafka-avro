package com.sample.transaction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sample.kafka.StringProducer;
import com.sample.transaction.entity.*;
import com.sample.transaction.exception.TransactionException;
import com.sample.transaction.model.*;
import com.sample.transaction.repo.*;
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

    @Autowired
    PaymentTypesRepository paymentTypesRepository;

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

    public ObpmAckNackRespModel validateTransactions(String reqMsgId) throws TransactionException {
        List<StaticDetails> staticDetailsList = null;
        List<FileDetails> stagedFiles = null;
        List<FailedMsgsModel> validationFailures = new ArrayList<>();
        ObpmAckNackRespModel respModel = null;
        staticDetailsList = staticDetailsRepository.getStaticDetails("N");
        stagedFiles = fileDetailsRepository.getFileDetails("N");
        if (CollectionUtils.isEmpty(staticDetailsList) && CollectionUtils.isEmpty(stagedFiles)) {
            FailedMsgsModel failedMsgsModel = new FailedMsgsModel(reqMsgId, "No messages to process");
            validationFailures.add(failedMsgsModel);
            respModel = validationFailure(reqMsgId, validationFailures);
            return respModel;
        }
        staticDetailsValidation(staticDetailsList, reqMsgId, validationFailures);
        List<PaymentTypes> paymentTypesList = paymentTypesRepository.getPaymentTypes();
        for (PaymentTypes p : paymentTypesList) {
            validatePayments(stagedFiles, p.getPaymentType(), p.getEftDirection(), reqMsgId, validationFailures);
        }
        if (CollectionUtils.isEmpty(validationFailures)) {
            validationSuccessful(reqMsgId);
            // save into transaction table get distinct customer Numbers from transaction table
            saveTransactions(stagedFiles, paymentTypesList);
            List<String> customerNumberList = transactionDetailsRepository.getDistinctCustomerNos();
            log.info("List of Distinct customer Nos - " + customerNumberList.toString());
            log.info("Payments validated successfully for reqMsgId - " + reqMsgId);
            return respModel;
        } else {
            validationFailure(reqMsgId, validationFailures);
            return respModel;
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
        }
    }

    private void updateIndividualFileValidationStatus(FileDetails stagedFile, String status) {
        stagedFile.setProcessed(status);
        fileDetailsRepository.save(stagedFile);
    }


    private void staticDetailsValidation(List<StaticDetails> staticDetailsList, String kafkaMsgId, List<FailedMsgsModel> validationFailures) {
        boolean isFailed = false;
        if (CollectionUtils.isEmpty(staticDetailsList)) {
            FailedMsgsModel failedMsgsModel = new FailedMsgsModel("STATIC", "No static message received for KafkaMsgId - " + kafkaMsgId);
            validationFailures.add(failedMsgsModel);
            isFailed = true;
        } else if (staticDetailsList.size() > 1) {
            FailedMsgsModel failedMsgsModel = new FailedMsgsModel("STATIC", "More than 1 static message received for KafkaMsgId - " + kafkaMsgId);
            validationFailures.add(failedMsgsModel);
            isFailed = true;
        } else if (!staticDetailsList.get(0).getRequestMsgId().equals(kafkaMsgId)) {
            FailedMsgsModel failedMsgsModel = new FailedMsgsModel("STATIC", "Validation failed for STATIC message. KafkaMsgId - " + kafkaMsgId + " doesn't match with fileRefId of STATIC message");
            validationFailures.add(failedMsgsModel);
            isFailed = true;
        }
        if (isFailed)
            updateStaticValidationStatus(staticDetailsList, "F");
        else
            updateStaticValidationStatus(staticDetailsList, "P");
    }

    private ObpmAckNackRespModel validationFailure(String reqMsgId, List<FailedMsgsModel> errorMessage) {
        ObpmAckNackRespModel responseModel = new ObpmAckNackRespModel();
        responseModel.setRequestMsgType("StatusNotification");
        ObpmAckNackNotifModel notifModel = new ObpmAckNackNotifModel("NACK", reqMsgId, "call that date method here", errorMessage);
        responseModel.setRequestMsg(notifModel);
        try {
            stringProducer.sendMessage("NACK for ReqMsgId - " + reqMsgId + " with exception - " + errorMessage.toString(), "acknowledgmentTopic");
        } catch (IOException e) {
            throw new TransactionException(500, e.getMessage());
        }
        return responseModel;
    }

    private void validationSuccessful(String reqMsgId) {
        try {
            stringProducer.sendMessage("ACK - All payments validated for ReqMsgId - " + reqMsgId, "acknowledgmentTopic");
        } catch (IOException e) {
            throw new TransactionException(500, e.getMessage());
        }

    }

    private void validatePayments(List<FileDetails> stagedFiles, String paymentType, String eftDirection, String reqMsgId, List<FailedMsgsModel> validationFailures) {
        List<FileDetails> paymentList = stagedFiles.stream().filter(f -> f.getPaymentType().equals(paymentType) && f.getEftDir().equals(eftDirection)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(paymentList)) {
            FailedMsgsModel failedMsgsModel = new FailedMsgsModel("PAYMENT", "No payment found for the combination " + paymentType + " and " + eftDirection);
            validationFailures.add(failedMsgsModel);
        } else
            validateTransactions(paymentList, paymentType, eftDirection, reqMsgId, validationFailures);
    }

    private void validateTransactions(List<FileDetails> filesToBeStaged, String paymentType, String eftDirection, String reqMsgId, List<FailedMsgsModel> validationFailures) {
        filesToBeStaged.sort(Comparator.comparing(FileDetails::getFileRefNo));
        int noOfFiles = Integer.parseInt(filesToBeStaged.get(0).getPageBlock().split("/")[1]);
        log.info("Validation for payment type - " + paymentType + " and eftDirection  - " + eftDirection);
        log.info("Number of files expected - " + noOfFiles);
        log.info("Number of files received - " + filesToBeStaged.size());
        int totalTransactions = filesToBeStaged.get(0).getTotalTxCount().intValue();
        boolean isFailure = false;
        //validate if we have received all transactions within the timeframe
        if (filesToBeStaged.size() != noOfFiles) {
            log.error("All messages not received within the timeframe for payment type - " + paymentType + " and eftDirection" + eftDirection);
            FailedMsgsModel failedMsgsModel = new FailedMsgsModel(paymentType + "-" + eftDirection, "All messages not received within the timeframe for payment type - " + paymentType + " and eftDirection - " + eftDirection);
            validationFailures.add(failedMsgsModel);
            isFailure = true;
            updateFileValidationStatus(filesToBeStaged, "F");
        } else if (filesToBeStaged.size() != filesToBeStaged.stream().map(FileDetails::getFileRefNo).collect(Collectors.toSet()).size()) {
            log.error("FileRefNum is not unique for payment type - " + paymentType + " and eftDirection" + eftDirection);
            FailedMsgsModel failedMsgsModel = new FailedMsgsModel(paymentType + "-" + eftDirection, "FileRefNum is not unique for payment type - " + paymentType + " and eftDirection - " + eftDirection);
            validationFailures.add(failedMsgsModel);
            isFailure = true;
            updateFileValidationStatus(filesToBeStaged, "F");
        } else {
            //validate transaction count and total transaction count
            int totalTxs = 0;
            for (FileDetails f : filesToBeStaged) {
                //validate ReqMsgId
                if (!f.getRequestMsgId().equals(reqMsgId)) {
                    FailedMsgsModel failedMsgsModel = new FailedMsgsModel(f.getFileRefNo(), "ReqMsgId doesn't match for FileRefId - " + f.getFileRefNo() + "for payment type - " + paymentType + " and eftDirection - " + eftDirection);
                    validationFailures.add(failedMsgsModel);
                    isFailure = true;
                    updateIndividualFileValidationStatus(f, "F");
                } else {
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        FileRequest request = mapper.readValue(f.getPaymentMsg(), FileRequest.class);
                        //validate transaction count
                        if (request.getTransactions().size() != request.getTxCount().intValue()) {
                            log.error("Transaction counts doesn't match");
                            FailedMsgsModel failedMsgsModel = new FailedMsgsModel(f.getFileRefNo(), "Transaction counts doesn't match for FileRefId - " + f.getFileRefNo() + "for payment type - " + paymentType + " and eftDirection - " + eftDirection);
                            validationFailures.add(failedMsgsModel);
                            updateIndividualFileValidationStatus(f, "F");
                            isFailure = true;
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
                FailedMsgsModel failedMsgsModel = new FailedMsgsModel(paymentType + "-" + eftDirection, "Total Transaction counts doesn't match for payment type - " + paymentType + " and eftDirection - " + eftDirection);
                validationFailures.add(failedMsgsModel);
            }
        }
        if (!isFailure) {
            updateFileValidationStatus(filesToBeStaged, "P");
        }
    }

    private void saveTransactions(List<FileDetails> stagedFiles, List<PaymentTypes> paymentTypesList) {
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
                    transaction.setPaymentFormat("");
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

