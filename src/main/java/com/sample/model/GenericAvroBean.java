package com.sample.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenericAvroBean {

    private String avroMessage;
    private String avroSchema;
    private String errorMessage;
    private Integer messageNo;

}
