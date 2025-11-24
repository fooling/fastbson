package com.cloud.fastbson.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BsonUtils.
 */
public class BsonUtilsTest {

    // ==================== bytesToHex ====================

    @Test
    public void testBytesToHex_Normal() {
        byte[] bytes = {0x01, 0x02, (byte) 0xFF, 0x00, 0x10};
        String result = BsonUtils.bytesToHex(bytes);
        assertEquals("0102ff0010", result);
    }

    @Test
    public void testBytesToHex_Empty() {
        byte[] bytes = new byte[0];
        String result = BsonUtils.bytesToHex(bytes);
        assertEquals("", result);
    }

    @Test
    public void testBytesToHex_Null() {
        String result = BsonUtils.bytesToHex(null);
        assertNull(result);
    }

    @Test
    public void testBytesToHex_AllZeros() {
        byte[] bytes = {0x00, 0x00, 0x00};
        String result = BsonUtils.bytesToHex(bytes);
        assertEquals("000000", result);
    }

    // ==================== hexToBytes ====================

    @Test
    public void testHexToBytes_Normal() {
        String hex = "0102ff0010";
        byte[] result = BsonUtils.hexToBytes(hex);
        assertArrayEquals(new byte[]{0x01, 0x02, (byte) 0xFF, 0x00, 0x10}, result);
    }

    @Test
    public void testHexToBytes_UpperCase() {
        String hex = "0102FF0010";
        byte[] result = BsonUtils.hexToBytes(hex);
        assertArrayEquals(new byte[]{0x01, 0x02, (byte) 0xFF, 0x00, 0x10}, result);
    }

    @Test
    public void testHexToBytes_Empty() {
        String hex = "";
        byte[] result = BsonUtils.hexToBytes(hex);
        assertEquals(0, result.length);
    }

    @Test
    public void testHexToBytes_Null() {
        byte[] result = BsonUtils.hexToBytes(null);
        assertNull(result);
    }

    @Test
    public void testHexToBytes_OddLength() {
        String hex = "012"; // Odd length
        assertThrows(IllegalArgumentException.class, () -> {
            BsonUtils.hexToBytes(hex);
        });
    }

    @Test
    public void testHexToBytes_InvalidCharacter() {
        String hex = "01XZ"; // Invalid hex characters
        assertThrows(NumberFormatException.class, () -> {
            BsonUtils.hexToBytes(hex);
        });
    }

    @Test
    public void testBytesToHex_And_HexToBytes_RoundTrip() {
        byte[] original = {0x12, 0x34, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF};
        String hex = BsonUtils.bytesToHex(original);
        byte[] converted = BsonUtils.hexToBytes(hex);
        assertArrayEquals(original, converted);
    }

    // ==================== readCString ====================

    @Test
    public void testReadCString_Normal() {
        byte[] buffer = "Hello\0World\0".getBytes();
        BsonUtils.CStringResult result = BsonUtils.readCString(buffer, 0);

        assertEquals("Hello", result.value);
        assertEquals(6, result.bytesConsumed); // "Hello\0"
    }

    @Test
    public void testReadCString_WithOffset() {
        byte[] buffer = "Hello\0World\0".getBytes();
        BsonUtils.CStringResult result = BsonUtils.readCString(buffer, 6);

        assertEquals("World", result.value);
        assertEquals(6, result.bytesConsumed); // "World\0"
    }

    @Test
    public void testReadCString_Empty() {
        byte[] buffer = {0x00};
        BsonUtils.CStringResult result = BsonUtils.readCString(buffer, 0);

        assertEquals("", result.value);
        assertEquals(1, result.bytesConsumed);
    }

    @Test
    public void testReadCString_NoNullTerminator() {
        byte[] buffer = "Hello".getBytes(); // No null terminator
        assertThrows(IllegalArgumentException.class, () -> {
            BsonUtils.readCString(buffer, 0);
        });
    }

    // ==================== validateBufferSize ====================

    @Test
    public void testValidateBufferSize_Valid() {
        byte[] buffer = new byte[10];
        // Should not throw
        BsonUtils.validateBufferSize(buffer, 0, 10);
        BsonUtils.validateBufferSize(buffer, 5, 5);
        BsonUtils.validateBufferSize(buffer, 9, 1);
    }

    @Test
    public void testValidateBufferSize_NullBuffer() {
        assertThrows(IllegalArgumentException.class, () -> {
            BsonUtils.validateBufferSize(null, 0, 5);
        });
    }

    @Test
    public void testValidateBufferSize_NegativePosition() {
        byte[] buffer = new byte[10];
        assertThrows(IllegalArgumentException.class, () -> {
            BsonUtils.validateBufferSize(buffer, -1, 5);
        });
    }

    @Test
    public void testValidateBufferSize_BufferUnderflow() {
        byte[] buffer = new byte[10];
        assertThrows(IllegalArgumentException.class, () -> {
            BsonUtils.validateBufferSize(buffer, 5, 6); // 5 + 6 = 11 > 10
        });
    }

    @Test
    public void testValidateBufferSize_ExactFit() {
        byte[] buffer = new byte[10];
        // Should not throw
        BsonUtils.validateBufferSize(buffer, 0, 10);
    }

    // ==================== readInt32LittleEndian ====================

    @Test
    public void testReadInt32LittleEndian_Positive() {
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(12345);
        byte[] bytes = buffer.array();

        int result = BsonUtils.readInt32LittleEndian(bytes, 0);
        assertEquals(12345, result);
    }

    @Test
    public void testReadInt32LittleEndian_Negative() {
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(-12345);
        byte[] bytes = buffer.array();

        int result = BsonUtils.readInt32LittleEndian(bytes, 0);
        assertEquals(-12345, result);
    }

    @Test
    public void testReadInt32LittleEndian_Zero() {
        byte[] bytes = {0x00, 0x00, 0x00, 0x00};
        int result = BsonUtils.readInt32LittleEndian(bytes, 0);
        assertEquals(0, result);
    }

    @Test
    public void testReadInt32LittleEndian_MaxValue() {
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(Integer.MAX_VALUE);
        byte[] bytes = buffer.array();

        int result = BsonUtils.readInt32LittleEndian(bytes, 0);
        assertEquals(Integer.MAX_VALUE, result);
    }

    @Test
    public void testReadInt32LittleEndian_MinValue() {
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(Integer.MIN_VALUE);
        byte[] bytes = buffer.array();

        int result = BsonUtils.readInt32LittleEndian(bytes, 0);
        assertEquals(Integer.MIN_VALUE, result);
    }

    @Test
    public void testReadInt32LittleEndian_WithOffset() {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(0); // Padding
        buffer.putInt(12345);
        byte[] bytes = buffer.array();

        int result = BsonUtils.readInt32LittleEndian(bytes, 4);
        assertEquals(12345, result);
    }

    // ==================== readInt64LittleEndian ====================

    @Test
    public void testReadInt64LittleEndian_Positive() {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(9876543210L);
        byte[] bytes = buffer.array();

        long result = BsonUtils.readInt64LittleEndian(bytes, 0);
        assertEquals(9876543210L, result);
    }

    @Test
    public void testReadInt64LittleEndian_Negative() {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(-9876543210L);
        byte[] bytes = buffer.array();

        long result = BsonUtils.readInt64LittleEndian(bytes, 0);
        assertEquals(-9876543210L, result);
    }

    @Test
    public void testReadInt64LittleEndian_Zero() {
        byte[] bytes = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        long result = BsonUtils.readInt64LittleEndian(bytes, 0);
        assertEquals(0L, result);
    }

    @Test
    public void testReadInt64LittleEndian_MaxValue() {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(Long.MAX_VALUE);
        byte[] bytes = buffer.array();

        long result = BsonUtils.readInt64LittleEndian(bytes, 0);
        assertEquals(Long.MAX_VALUE, result);
    }

    @Test
    public void testReadInt64LittleEndian_MinValue() {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(Long.MIN_VALUE);
        byte[] bytes = buffer.array();

        long result = BsonUtils.readInt64LittleEndian(bytes, 0);
        assertEquals(Long.MIN_VALUE, result);
    }

    // ==================== readDoubleLittleEndian ====================

    @Test
    public void testReadDoubleLittleEndian_Normal() {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(3.14159);
        byte[] bytes = buffer.array();

        double result = BsonUtils.readDoubleLittleEndian(bytes, 0);
        assertEquals(3.14159, result, 0.00001);
    }

    @Test
    public void testReadDoubleLittleEndian_Negative() {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(-123.456);
        byte[] bytes = buffer.array();

        double result = BsonUtils.readDoubleLittleEndian(bytes, 0);
        assertEquals(-123.456, result, 0.001);
    }

    @Test
    public void testReadDoubleLittleEndian_Zero() {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(0.0);
        byte[] bytes = buffer.array();

        double result = BsonUtils.readDoubleLittleEndian(bytes, 0);
        assertEquals(0.0, result, 0.0);
    }

    @Test
    public void testReadDoubleLittleEndian_NaN() {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(Double.NaN);
        byte[] bytes = buffer.array();

        double result = BsonUtils.readDoubleLittleEndian(bytes, 0);
        assertTrue(Double.isNaN(result));
    }

    @Test
    public void testReadDoubleLittleEndian_PositiveInfinity() {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(Double.POSITIVE_INFINITY);
        byte[] bytes = buffer.array();

        double result = BsonUtils.readDoubleLittleEndian(bytes, 0);
        assertEquals(Double.POSITIVE_INFINITY, result, 0.0);
    }

    @Test
    public void testReadDoubleLittleEndian_NegativeInfinity() {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(Double.NEGATIVE_INFINITY);
        byte[] bytes = buffer.array();

        double result = BsonUtils.readDoubleLittleEndian(bytes, 0);
        assertEquals(Double.NEGATIVE_INFINITY, result, 0.0);
    }

    // ==================== Constructor Test ====================

    @Test
    public void testConstructor_CannotInstantiate() throws Exception {
        Constructor<BsonUtils> constructor = BsonUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        try {
            constructor.newInstance();
            fail("Expected AssertionError");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof AssertionError);
            assertEquals("Cannot instantiate BsonUtils", e.getCause().getMessage());
        }
    }

    // ==================== CStringResult Test ====================

    @Test
    public void testCStringResult() {
        BsonUtils.CStringResult result = new BsonUtils.CStringResult("test", 5);
        assertEquals("test", result.value);
        assertEquals(5, result.bytesConsumed);
    }
}
