package com.sample.transaction.entity;

import lombok.Data;

import javax.persistence.*;
import java.sql.Date;

@Entity
@Table(name ="FILE_DETAILS")
@Data
public class FileDetails {
    @Id
    @Column(name="FILE_REFNO")
    private String fileRefNo;
    @Column(name="PAYMENT_TYPE")
    private String paymentType;
    @Column(name="EFF_DIRECTION")
    private String effDirection;
    @Column(name="TOTAl_TX_COUNT")
    private Long totalTxCount;
    @Column(name="TX_COUNT")
    private Long txCount;
    @Column(name="CONTROL_SUM")
    private String controlSum;
    @Column(name="PAGE_BLOCK")
    private String pageBlock;
    @Column(name="CREATED_DATE")
    private String createdDate;
    @Column(name="IS_STAGING_COMPLETED")
    private String isStagingCompleted;
    @Lob
    @Column(name="RAW_JSON")
    private String rawJson;

}
