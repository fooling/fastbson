package com.cloud.fastbson.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for all BSON exception classes.
 */
public class BsonExceptionsTest {

    // ==================== BsonException ====================

    @Test
    public void testBsonException_WithMessage() {
        String message = "Test error message";
        BsonException exception = new BsonException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testBsonException_WithMessageAndCause() {
        String message = "Test error message";
        Throwable cause = new RuntimeException("Underlying cause");
        BsonException exception = new BsonException(message, cause);

        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    public void testBsonException_WithCause() {
        Throwable cause = new RuntimeException("Underlying cause");
        BsonException exception = new BsonException(cause);

        assertSame(cause, exception.getCause());
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("Underlying cause"));
    }

    // ==================== BsonParseException ====================

    @Test
    public void testBsonParseException_WithMessage() {
        String message = "Parse error";
        BsonParseException exception = new BsonParseException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertTrue(exception instanceof BsonException);
    }

    @Test
    public void testBsonParseException_WithMessageAndCause() {
        String message = "Parse error";
        Throwable cause = new IllegalArgumentException("Invalid argument");
        BsonParseException exception = new BsonParseException(message, cause);

        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    // ==================== BsonBufferUnderflowException ====================

    @Test
    public void testBsonBufferUnderflowException_WithMessage() {
        String message = "Buffer underflow";
        BsonBufferUnderflowException exception = new BsonBufferUnderflowException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertTrue(exception instanceof BsonParseException);
    }

    @Test
    public void testBsonBufferUnderflowException_WithPositionInfo() {
        int position = 100;
        int required = 8;
        int available = 3;
        BsonBufferUnderflowException exception =
            new BsonBufferUnderflowException(position, required, available);

        String message = exception.getMessage();
        assertNotNull(message);
        assertTrue(message.contains("position=100"));
        assertTrue(message.contains("required=8"));
        assertTrue(message.contains("available=3"));
        assertTrue(message.contains("Buffer underflow"));
    }

    @Test
    public void testBsonBufferUnderflowException_MessageFormat() {
        BsonBufferUnderflowException exception =
            new BsonBufferUnderflowException(10, 5, 2);

        String expected = "Buffer underflow: position=10, required=5, available=2";
        assertEquals(expected, exception.getMessage());
    }

    // ==================== InvalidBsonTypeException ====================

    @Test
    public void testInvalidBsonTypeException_WithTypeValue() {
        byte typeValue = (byte) 0x99;
        InvalidBsonTypeException exception = new InvalidBsonTypeException(typeValue);

        assertEquals(typeValue, exception.getTypeValue());
        String message = exception.getMessage();
        assertNotNull(message);
        assertTrue(message.contains("0x99"));
        assertTrue(message.contains("Invalid") || message.contains("unsupported"));
        assertTrue(exception instanceof BsonParseException);
    }

    @Test
    public void testInvalidBsonTypeException_WithTypeValueAndMessage() {
        byte typeValue = (byte) 0xAB;
        String customMessage = "Custom error message";
        InvalidBsonTypeException exception = new InvalidBsonTypeException(typeValue, customMessage);

        assertEquals(typeValue, exception.getTypeValue());
        assertEquals(customMessage, exception.getMessage());
    }

    @Test
    public void testInvalidBsonTypeException_NegativeTypeValue() {
        byte typeValue = (byte) 0xFF; // -1 in signed byte
        InvalidBsonTypeException exception = new InvalidBsonTypeException(typeValue);

        assertEquals(typeValue, exception.getTypeValue());
        String message = exception.getMessage();
        // Should show as 0xFF not as negative
        assertTrue(message.contains("0xff") || message.contains("0xFF"));
    }

    @Test
    public void testInvalidBsonTypeException_ZeroTypeValue() {
        byte typeValue = 0x00;
        InvalidBsonTypeException exception = new InvalidBsonTypeException(typeValue);

        assertEquals(typeValue, exception.getTypeValue());
        String message = exception.getMessage();
        assertTrue(message.contains("0x0") || message.contains("0x00"));
    }

    // ==================== Exception Hierarchy ====================

    @Test
    public void testExceptionHierarchy() {
        // BsonException extends RuntimeException
        assertTrue(new BsonException("test") instanceof RuntimeException);

        // BsonParseException extends BsonException
        BsonParseException parseEx = new BsonParseException("test");
        assertTrue(parseEx instanceof BsonException);
        assertTrue(parseEx instanceof RuntimeException);

        // BsonBufferUnderflowException extends BsonParseException
        BsonBufferUnderflowException underflowEx = new BsonBufferUnderflowException("test");
        assertTrue(underflowEx instanceof BsonParseException);
        assertTrue(underflowEx instanceof BsonException);
        assertTrue(underflowEx instanceof RuntimeException);

        // InvalidBsonTypeException extends BsonParseException
        InvalidBsonTypeException typeEx = new InvalidBsonTypeException((byte) 0x99);
        assertTrue(typeEx instanceof BsonParseException);
        assertTrue(typeEx instanceof BsonException);
        assertTrue(typeEx instanceof RuntimeException);
    }

    // ==================== Exception Throwing ====================

    @Test
    public void testExceptionsCanBeThrown() {
        assertThrows(BsonException.class, () -> {
            throw new BsonException("test");
        });

        assertThrows(BsonParseException.class, () -> {
            throw new BsonParseException("test");
        });

        assertThrows(BsonBufferUnderflowException.class, () -> {
            throw new BsonBufferUnderflowException("test");
        });

        assertThrows(InvalidBsonTypeException.class, () -> {
            throw new InvalidBsonTypeException((byte) 0x99);
        });
    }

    // ==================== Exception Catching ====================

    @Test
    public void testExceptionCatching() {
        // Can catch specific exception
        try {
            throw new InvalidBsonTypeException((byte) 0x99);
        } catch (InvalidBsonTypeException e) {
            assertEquals((byte) 0x99, e.getTypeValue());
        }

        // Can catch as BsonParseException
        try {
            throw new InvalidBsonTypeException((byte) 0x99);
        } catch (BsonParseException e) {
            assertTrue(e instanceof InvalidBsonTypeException);
        }

        // Can catch as BsonException
        try {
            throw new BsonBufferUnderflowException("test");
        } catch (BsonException e) {
            assertTrue(e instanceof BsonBufferUnderflowException);
        }

        // Can catch as RuntimeException
        try {
            throw new BsonParseException("test");
        } catch (RuntimeException e) {
            assertTrue(e instanceof BsonParseException);
        }
    }
}
