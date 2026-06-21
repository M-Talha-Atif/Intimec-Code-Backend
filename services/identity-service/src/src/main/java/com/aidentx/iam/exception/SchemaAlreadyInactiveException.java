package com.aidentx.iam.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an attempt is made to deactivate a schema that is already inactive.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class SchemaAlreadyInactiveException extends RuntimeException {

    public SchemaAlreadyInactiveException(String message) {
        super(message);
    }
}