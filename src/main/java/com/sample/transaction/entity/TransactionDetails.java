package com.sample.transaction.entity;

import lombok.Data;

import javax.persistence.*;

@Table(name="TRANSACTION_DETAILS")
@Entity
@Data
public class TransactionDetails {
    @Id
    @Column(name= "SNO")
    private Long sNo;
    @Column(name="SERVICE_NAME")
    private String serviceName;
    @Column(name="PAYMENT_TYPE")
    private String paymentType;
    @Column(name="EFT_DIR")
    private String eftDir;
    @Column(name="FILEREF_NUM")
    private String fileRefNo;
    @Column(name="PAGE_BLOCK")
    private String pageBlock;
    @Column(name="TXN_REFNO")
    private String transactionRef;
    @Column(name="CUSTOMER_NUM")
    private String customerNo;
    @Column(name="ACCT_NUM")
    private String accountNo;
   @Column(name="TXN_DT")
   private String txnDt;
    @Column(name="PROCESSED")
    private String processed;
    @Lob
    @Column(name="PAYMENT_MSG")
    private String paymentMsg;
    @Column
    private String clientName;
    @Column
    private String createdBy;
    @Column(name="PAYMENT_FORMAT")
    private String paymentFormat;


}
