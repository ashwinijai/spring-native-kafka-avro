package com.sample.transaction.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
