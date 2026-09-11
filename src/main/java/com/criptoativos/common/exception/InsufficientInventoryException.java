package com.criptoativos.common.exception;

import org.springframework.http.HttpStatus;

public class InsufficientInventoryException extends DomainException {

    public InsufficientInventoryException(String detail) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient inventory", detail);
    }
}
