package com.sample.transaction.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sample.kafka.StringProducer;
import com.sample.transaction.entity.FileDetails;
import com.sample.transaction.entity.StaticDetails;
import com.sample.transaction.entity.TransactionDetails;
import com.sample.transaction.model.FileRequest;
import com.sample.transaction.model.RequestModel;
import com.sample.transaction.model.StaticRequest;
import com.sample.transaction.model.TransactionModel;
import com.sample.transaction.repo.FileDetailsRepository;
import com.sample.transaction.repo.StaticDetailsRepository;
import com.sample.transaction.repo.TransactionDetailsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TransactionConsumer {
    @Autowired
    FileDetailsRepository fileDetailsRepository;

    @Autowired
    TransactionDetailsRepository transactionDetailsRepository;

    @Autowired
    StaticDetailsRepository staticDetailsRepository;

    @Autowired
    StringProducer stringProducer;
    Timer timer;

    @KafkaListener(topics = "transactionTopic", groupId = "group_id_1")
    public void consume(String message) throws JsonProcessingException {
        log.info("Meesage consumed");
        ObjectMapper mapper = new ObjectMapper();
        RequestModel request = mapper.readValue(message, RequestModel.class);
        if (request.getResponseStatus().equals("PASS") && request.getResponseType().equals("STATIC")) {
            StaticRequest staticRequest = mapper.readValue(request.getResponseMsg(), StaticRequest.class);
            StaticDetails staticDetails = new StaticDetails();
            staticDetails.setSNo(staticDetailsRepository.getMaxSno() + 1);
            staticDetails.setDetailCount(staticRequest.getDetailCount());
            staticDetails.setProcessed("Y");
            staticDetails.setServiceName("");
            staticDetails.setStaticDataMsg(message);
            staticDetails.setFileRefNo(staticRequest.getFileReferenceNo());
            staticDetails.setClientName("FINTRAC");
            staticDetails.setCreatedBy("SYSTEM");
            staticDetailsRepository.save(staticDetails);
        } else if (request.getResponseStatus().equals("PASS") && request.getResponseType().equals("PAYMENT")) {
            FileRequest fileRequest = mapper.readValue(request.getResponseMsg(), FileRequest.class);
            String extractDate = fileRequest.getExtractDateTime().substring(0, fileRequest.getExtractDateTime().indexOf("T"));
            log.info("Message extract Date - " + extractDate);
            //find if this is the first message and start timer
            if (CollectionUtils.isEmpty(fileDetailsRepository.findFilesByDate(extractDate, "N"))) {
                timer = new Timer();
                timer.schedule(new MessageProcessingTimer(), 60000);
            }
            if ((fileRequest.getPaymentType().equals("MX") || fileRequest.getPaymentType().equals("MT")) && (fileRequest.getEftDirection().equals("I") || fileRequest.getEftDirection().equals("O"))) {
                FileDetails fileDetails = new FileDetails();
                fileDetails.setSNo(fileDetailsRepository.getMaxSno() + 1);
                fileDetails.setServiceName("");
                fileDetails.setFileRefNo(fileRequest.getFileReferenceNo());
                fileDetails.setExtractDate(extractDate);
                fileDetails.setPageBlock(fileRequest.getPageBlock());
                fileDetails.setEftDir(fileRequest.getEftDirection());
                fileDetails.setPaymentType(fileRequest.getPaymentType());
                fileDetails.setTxCount(fileRequest.getTotalTxnCount());
                fileDetails.setTotalTxCount(fileRequest.getTxCount());
                fileDetails.setProcessed("N");
                fileDetails.setPaymentMsg(message);
                fileDetailsRepository.save(fileDetails);
            } else {
                log.error("Not a valid message");
            }
        }
    }

    class MessageProcessingTimer extends TimerTask {
        @Override
        public void run() {
            log.info("Inside timer task");
            List<FileDetails> filesToBeStaged = fileDetailsRepository.findUnProcessedFiles("N");
            List<FileDetails> mxIncomingList = filesToBeStaged.stream().filter(f -> f.getPaymentType().equals("MX") && f.getEftDir().equals("I")).collect(Collectors.toList());
            List<FileDetails> mxOutgoingList = filesToBeStaged.stream().filter(f -> f.getPaymentType().equals("MX") && f.getEftDir().equals("O")).collect(Collectors.toList());
            List<FileDetails> mtIncomingList = filesToBeStaged.stream().filter(f -> f.getPaymentType().equals("MT") && f.getEftDir().equals("I")).collect(Collectors.toList());
            List<FileDetails> mtOutgoingList = filesToBeStaged.stream().filter(f -> f.getPaymentType().equals("MT") && f.getEftDir().equals("O")).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(mxIncomingList) && validateTransactions(mxIncomingList)) {
                validationFailure(mxIncomingList, "MX", "I");
            } else {
                validationSuccessful(mxIncomingList, "MX", "I");
                saveTransactions(mxIncomingList);
            }
            if (!CollectionUtils.isEmpty(mxOutgoingList) && validateTransactions(mxOutgoingList)) {
                validationFailure(mxOutgoingList, "MX", "O");
            } else {
                validationSuccessful(mxOutgoingList, "MX", "O");
                saveTransactions(mxOutgoingList);
            }
            if (!CollectionUtils.isEmpty(mtIncomingList) && validateTransactions(mtIncomingList)) {
                validationFailure(mtIncomingList, "MT", "I");
            } else {
                validationSuccessful(mtIncomingList, "MT", "I");
                saveTransactions(mtIncomingList);
            }
            if (!CollectionUtils.isEmpty(mtOutgoingList) && validateTransactions(mtOutgoingList)) {
                validationFailure(mtOutgoingList, "MT", "O");
            } else {
                validationSuccessful(mtOutgoingList, "MT", "O");
                saveTransactions(mtOutgoingList);
            }

            // get distinct customer Numbers from transaction table
            List<String> customerNumberList = transactionDetailsRepository.getDistinctCustomerNos();
            log.info("List of Distinct customer Nos - " + customerNumberList.toString());

            timer.cancel();

        }

        private void validationFailure(List<FileDetails> fileDetailsList, String paymentType, String eftDirection) {
            fileDetailsRepository.deleteInBatch(fileDetailsList);
            sendMessageToKafka("NACK for Payment Type - " + paymentType + " and Eft Direction - " + eftDirection, "acknowledgmentTopic");

        }

        private void validationSuccessful(List<FileDetails> fileDetailsList, String paymentType, String eftDirection) {
            fileDetailsList.forEach(f -> f.setProcessed("Y"));
            fileDetailsRepository.saveAll(fileDetailsList);
            sendMessageToKafka("ACK for Payment Type - " + paymentType + " and Eft Direction - " + eftDirection, "acknowledgmentTopic");

        }

        private void sendMessageToKafka(String message, String topicName) {
            try {
                stringProducer.sendMessage(message, topicName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void saveTransactions(List<FileDetails> filesToBeStaged) {
            filesToBeStaged.forEach(f -> {
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
                    throw new RuntimeException(e);
                }
            });
        }

        private boolean validateTransactions(List<FileDetails> filesToBeStaged) {
            boolean isException = false;
            filesToBeStaged.sort(Comparator.comparing(FileDetails::getFileRefNo));
            int noOfFiles = Integer.parseInt(filesToBeStaged.get(0).getPageBlock().split("/")[1]);
            int totalTransactions = filesToBeStaged.get(0).getTotalTxCount().intValue();
            //validate if we have received all transactions within the timeframe
            if (filesToBeStaged.size() != noOfFiles) {
                log.error("All messages not received within timeout");
                isException = true;
            } else {
                //validate transaction count and total transaction count
                int totalTxs = 0;
                for (FileDetails f : filesToBeStaged) {
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        FileRequest request = mapper.readValue(f.getPaymentMsg(), FileRequest.class);
                        //validate transaction count
                        if (request.getTransactions().size() != request.getTxCount().intValue()) {
                            log.error("Transaction counts doesn't match");
                            isException = true;
                            break;
                        } else {
                            totalTxs = totalTxs + request.getTransactions().size();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                //validate total transaction count
                if (!isException && totalTxs != totalTransactions) {
                    log.error("Total Transaction counts doesn't match");
                    isException = true;
                }
            }
            return isException;
        }

    }
}
