package com.sample.transaction.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name ="PAYMENT_TYPES")
@Data
public class PaymentTypes {
    @Id
    @Column(name= "SNO")
    private Long sNo;
    @Column(name="PAYMENT_TYPE")
    private String paymentType;
    @Column(name="EFT_DIRECTION")
    private String eftDirection;
    @Column(name ="PAYMENT_FORMAT")
    private String paymentFormat;

}
