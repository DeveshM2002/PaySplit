package com.splitwise.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Thrown when the configured LLM provider returns a non-success HTTP status. Mapped to REST status in {@link GlobalExceptionHandler}. */
@Getter
public class UpstreamAiException extends RuntimeException {

    private final HttpStatus httpStatus;

    public UpstreamAiException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus == null ? HttpStatus.BAD_GATEWAY : httpStatus;
    }

    public UpstreamAiException(HttpStatus httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus == null ? HttpStatus.BAD_GATEWAY : httpStatus;
    }
}
