package com.sample.transaction.model;

import lombok.Data;

@Data
public class RequestModel {

    private String requestMsgId;
    private String responseStatus;

    private String responseType;

    private String responseMsg;
}
