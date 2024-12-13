package com.sample.transaction.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestModel {

    private String requestMsgId;
    private String responseStatus;

    private String responseType;

    private String responseMsg;
}
