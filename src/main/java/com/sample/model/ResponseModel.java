package com.sample.model;

import lombok.Data;

import java.time.Instant;

@Data
public
class ResponseModel{
    private String id;
    private String evtCode;
    private String logType;
    private String logDescription;
    private String serviceData;
    private String logTime;
    private String publishedTime;

}
