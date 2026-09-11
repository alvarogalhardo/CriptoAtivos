package com.criptoativos.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base for every expected business failure.
 *
 * <p>Carrying the status and title on the exception lets {@code ApiExceptionHandler} render any
 * subclass as an RFC 7807 {@code ProblemDetail} without a growing switch statement.
 */
public abstract class DomainException extends RuntimeException {

    private final HttpStatus status;
    private final String title;

    protected DomainException(HttpStatus status, String title, String detail) {
        super(detail);
        this.status = status;
        this.title = title;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }
}
