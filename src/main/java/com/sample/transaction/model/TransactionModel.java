package com.sample.transaction.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionModel {

    private String transactionReference;
    private String transactionDateTime;
    private String source;
    private String eftType;
    private String customerNo;
    private String accountNumber;
    private String accountOpenDate;
    private String accountCcy;
    private String requestorReference;
    private Float exchRate;
    private String udfBeneCtry;
    private String paymentMessage;
}
