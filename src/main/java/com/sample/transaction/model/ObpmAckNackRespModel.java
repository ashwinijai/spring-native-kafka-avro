package com.sample.transaction.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.N;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObpmAckNackRespModel {
    private String requestMsgType;
    private ObpmAckNackNotifModel requestMsg;
}
