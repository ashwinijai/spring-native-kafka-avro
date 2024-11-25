package com.sample.transaction.model;

import lombok.Data;

import java.util.List;

@Data
public class FileRequest {
    private String extractDateTime;
    private String fileReferenceNo;
    private String paymentType;
    private String eftDirection;
    private String reportingEntityBic;
    private Long totalTxnCount;
    private Long txCount;
    private String controlSum;
    private String pageBlock;
    private List<TransactionModel> transactions;
}
