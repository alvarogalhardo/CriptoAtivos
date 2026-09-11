package com.criptoativos.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String detail) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Business rule violated", detail);
    }
}
