package com.cloud.fastbson.exception;

/**
 * Base exception for all BSON-related errors.
 */
public class BsonException extends RuntimeException {

    /**
     * Creates a new BsonException with the specified message.
     *
     * @param message the error message
     */
    public BsonException(String message) {
        super(message);
    }

    /**
     * Creates a new BsonException with the specified message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public BsonException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new BsonException with the specified cause.
     *
     * @param cause the underlying cause
     */
    public BsonException(Throwable cause) {
        super(cause);
    }
}
