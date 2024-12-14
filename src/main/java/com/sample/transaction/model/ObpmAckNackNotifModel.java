package com.sample.transaction.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.A;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObpmAckNackNotifModel {
    private String status;
    private String requestMsgId;
    private String requestDateTime;
    private List<FailedMsgsModel> failedFiles;
}
