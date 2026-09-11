package com.criptoativos.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends DomainException {

    public ConflictException(String detail) {
        super(HttpStatus.CONFLICT, "Conflict", detail);
    }
}
