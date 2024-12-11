package com.sample.transaction.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@AllArgsConstructor
public class TransactionException extends RuntimeException{
    int errorCode;
    String errorMessage;
}
