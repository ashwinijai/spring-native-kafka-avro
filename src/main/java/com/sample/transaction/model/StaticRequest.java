package com.sample.transaction.model;

import lombok.Data;

import java.util.List;

@Data
public class StaticRequest {
    private String fileReferenceNo;
    private String msgDateTimestamp;
    private Long detailCount;
    private List<FintracStaticFields> staticDataDetails;
}
