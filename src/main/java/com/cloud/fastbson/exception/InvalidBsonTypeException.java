package com.cloud.fastbson.exception;

/**
 * Exception thrown when an invalid or unsupported BSON type is encountered.
 */
public class InvalidBsonTypeException extends BsonParseException {

    private final byte typeValue;

    /**
     * Creates a new InvalidBsonTypeException for the given type byte.
     *
     * @param typeValue the invalid type byte value
     */
    public InvalidBsonTypeException(byte typeValue) {
        super("Invalid or unsupported BSON type: 0x" + Integer.toHexString(typeValue & 0xFF));
        this.typeValue = typeValue;
    }

    /**
     * Creates a new InvalidBsonTypeException with a custom message.
     *
     * @param typeValue the invalid type byte value
     * @param message the error message
     */
    public InvalidBsonTypeException(byte typeValue, String message) {
        super(message);
        this.typeValue = typeValue;
    }

    /**
     * Returns the invalid type byte value.
     *
     * @return the type byte
     */
    public byte getTypeValue() {
        return typeValue;
    }
}
