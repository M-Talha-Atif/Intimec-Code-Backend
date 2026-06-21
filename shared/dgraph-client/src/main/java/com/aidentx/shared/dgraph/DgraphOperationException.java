package com.aidentx.shared.dgraph;

public class DgraphOperationException extends RuntimeException {
    public DgraphOperationException(String message) {
        super(message);
    }
    public DgraphOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}