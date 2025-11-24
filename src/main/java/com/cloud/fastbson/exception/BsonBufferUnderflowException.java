package com.cloud.fastbson.exception;

/**
 * Exception thrown when attempting to read beyond the end of a BSON buffer.
 */
public class BsonBufferUnderflowException extends BsonParseException {

    /**
     * Creates a new BsonBufferUnderflowException with the specified message.
     *
     * @param message the error message
     */
    public BsonBufferUnderflowException(String message) {
        super(message);
    }

    /**
     * Creates a new BsonBufferUnderflowException with position information.
     *
     * @param position the current position
     * @param required the number of bytes required
     * @param available the number of bytes available
     */
    public BsonBufferUnderflowException(int position, int required, int available) {
        super(String.format(
            "Buffer underflow: position=%d, required=%d, available=%d",
            position, required, available
        ));
    }
}
