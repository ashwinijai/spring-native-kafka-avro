package com.sample.transaction.entity;

import lombok.Data;

import javax.persistence.*;
import java.sql.Date;

@Entity
@Table(name ="FILE_DETAILS")
@Data
public class FileDetails {
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
    @Column(name = "PAYMT_FORMAT")
    private String paymentFormat;
    @Column(name="TOTAlTXNS_COUNT")
    private Long totalTxCount;
    @Column(name="TXNS_COUNT")
    private Long txCount;
    @Column(name="PROCESSED")
    private String processed;
    @Lob
    @Column(name="PAYMENT_MSG")
    private String paymentMsg;
    @Column
    private String clientName;
    @Column
    private String createdBy;
    @Column(name="MSG_DT")
    private String msgDate;
    @Column(name="REQUEST_MSG_ID")
    private String requestMsgId;
    @Column(name="RESPONSE_STATUS")
    private String responseStatus;
    @Column(name="RESPONSE_TYPE")
    private String responseType;

}
