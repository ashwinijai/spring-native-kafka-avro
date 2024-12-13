package com.sample.transaction.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaticRequest {
    private String fileReferenceNo;
    private String msgDateTimestamp;
    private Long detailCount;
    private List<FintracStaticFields> staticDataDetails;
}
