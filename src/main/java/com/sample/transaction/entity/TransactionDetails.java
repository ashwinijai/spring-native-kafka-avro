package com.sample.transaction.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Table(name="TRANSACTION_DETAILS")
@Entity
@Data
public class TransactionDetails {
    @Id
    @Column(name="TRANSACTION_REF")
    private String transactionRef;
    @Column(name="SOURCE")
    private String sourceOfTransaction;
    @Column(name="CUSTOMER_NO")
    private String customerNo;
    @Column(name="ACCOUNT_NO")
    private String accountNo;
   @Column(name="ACCOUNT_CCY")
   private String accountCcy;
   @Column(name="PAYMENT_MSG")
   private String paymentMsg;


}
