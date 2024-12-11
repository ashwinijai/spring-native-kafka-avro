package com.sample.transaction.entity;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name ="STATIC_DETAILS")
@Data
public class StaticDetails {
    @Id
    @Column(name= "SNO")
    private Long sNo;
    @Column(name="SERVICE_NAME")
    private String serviceName;
    @Column(name="FILEREF_NUM")
    private String fileRefNo;
    @Lob
    @Column(name="STATICDATA_MSG")
    private String staticDataMsg;
    @Column(name="DETAIL_COUNT")
    private Long detailCount;
    @Column(name="PROCESSED")
    private String processed;
    @Column(name="CLIENT_NAME")
    private String clientName;
    @Column(name="CREATED_BY")
    private String createdBy;
    @Column(name="REQUEST_MSG_ID")
    private String requestMsgId;
    @Column(name="RESPONSE_STATUS")
    private String responseStatus;
    @Column(name="RESPONSE_TYPE")
    private String responseType;
}
