package com.sample.model;


import lombok.Data;

@Data
public class GenericAvroBean {

    private String avroMessage;
    private String avroSchema;
    private String errorMessage;

}
