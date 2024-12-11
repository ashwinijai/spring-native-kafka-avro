package com.sample.transaction.entity;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name ="KAFKA_AUDIT")
@Data
public class KafkaAudit {
    @Id
    @GeneratedValue(generator = "sNo_seq", strategy = GenerationType. SEQUENCE)
    @SequenceGenerator(name = "sNo_seq", sequenceName = "sNo_seq", allocationSize = 1)
    @Column(name="SNO")
    Long sNo;
    @Column(name="SERVICE_NAME")
    String serviceName;
    @Column(name="EVENT")
    String event;
    @Lob
    @Column(name="REQUEST_MSG")
    String requestMsg;
    @Lob
    @Column(name="RESPONSE_MSG")
    String responseMsg;
    @Column(name="PROCESSED")
    String processed;
    @Column(name="CLIENT_NAME")
    String clientName;
    @Column(name="CREATED_BY")
    String createdBy;
}
