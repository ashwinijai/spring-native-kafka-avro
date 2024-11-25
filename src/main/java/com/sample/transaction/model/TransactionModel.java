package com.sample.transaction.model;


import lombok.Data;

@Data
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
