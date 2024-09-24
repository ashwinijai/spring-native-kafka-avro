package com.sample.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "transaction")
@Data
public class Transaction {

    @Id
    private Long transactionId;
    private Long fromAccNumber;
    private Long amount;
    private Long toAccNumber;
    private String transactionDate;


}
