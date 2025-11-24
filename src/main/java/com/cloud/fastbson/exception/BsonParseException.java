package com.cloud.fastbson.exception;

/**
 * Exception thrown when BSON data cannot be parsed.
 */
public class BsonParseException extends BsonException {

    /**
     * Creates a new BsonParseException with the specified message.
     *
     * @param message the error message
     */
    public BsonParseException(String message) {
        super(message);
    }

    /**
     * Creates a new BsonParseException with the specified message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public BsonParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
