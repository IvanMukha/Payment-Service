package com.mukha.paymentservice.exception;

import org.springframework.http.HttpStatus;

public class InvalidDateRangeException extends GlobalServiceException {
    public InvalidDateRangeException() {
        super("Invalid date range: 'from' must be before ",HttpStatus.BAD_REQUEST);
    }
}
