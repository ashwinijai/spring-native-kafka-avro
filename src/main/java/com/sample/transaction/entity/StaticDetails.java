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
    @Column
    private String clientName;
    @Column
    private String createdBy;
}
