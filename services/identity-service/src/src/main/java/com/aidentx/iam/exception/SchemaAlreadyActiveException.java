package com.aidentx.iam.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an attempt is made to activate a schema that is already active.
 */
@ResponseStatus(HttpStatus.CONFLICT) // 409 Conflict is an appropriate HTTP status for this state conflict
public class SchemaAlreadyActiveException extends RuntimeException {

    public SchemaAlreadyActiveException(String message) {
        super(message);
    }
}