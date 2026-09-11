package com.criptoativos.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends DomainException {

    public NotFoundException(String detail) {
        super(HttpStatus.NOT_FOUND, "Resource not found", detail);
    }
}
