package com.cloud.fastbson.reader;

import com.cloud.fastbson.util.StringPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Unit tests for BsonReader.
 *
 * Tests all methods with normal, boundary, and exceptional cases
 * to achieve 100% branch coverage.
 */
public class BsonReaderTest {

    @AfterEach
    public void tearDown() {
        // Clear StringPool after each test to ensure isolation
        StringPool.clear();
    }

    // Helper method to create little-endian byte array for int32
    private byte[] createInt32Bytes(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    // Helper method to create little-endian byte array for int64
    private byte[] createInt64Bytes(long value) {
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array();
    }

    // Helper method to create little-endian byte array for double
    private byte[] createDoubleBytes(double value) {
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(value).array();
    }

    // Helper method to create C-string bytes
    private byte[] createCStringBytes(String value) {
        byte[] stringBytes = value.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[stringBytes.length + 1];
        System.arraycopy(stringBytes, 0, result, 0, stringBytes.length);
        result[stringBytes.length] = 0; // null terminator
        return result;
    }

    @Test
    public void testConstructor_ValidBuffer() {
        // Arrange & Act
        byte[] buffer = new byte[10];
        BsonReader reader = new BsonReader(buffer);

        // Assert
        assertNotNull(reader);
        assertEquals(0, reader.position());
        assertEquals(10, reader.length());
    }

    @Test
    public void testConstructor_NullBuffer() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> new BsonReader(null));
    }

    @Test
    public void testReset_ValidBuffer() {
        // Arrange
        BsonReader reader = new BsonReader(new byte[5]);
        reader.skip(3);

        // Act
        reader.reset(new byte[10]);

        // Assert
        assertEquals(0, reader.position());
        assertEquals(10, reader.length());
    }

    @Test
    public void testReset_NullBuffer() {
        // Arrange
        BsonReader reader = new BsonReader(new byte[5]);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> reader.reset(null));
    }

    @Test
    public void testPosition_GetAndSet() {
        // Arrange
        BsonReader reader = new BsonReader(new byte[10]);

        // Act & Assert
        assertEquals(0, reader.position());

        reader.position(5);
        assertEquals(5, reader.position());

        reader.position(0);
        assertEquals(0, reader.position());

        reader.position(10);
        assertEquals(10, reader.position());
    }

    @Test
    public void testPosition_Negative() {
        // Arrange
        BsonReader reader = new BsonReader(new byte[10]);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> reader.position(-1));
    }

    @Test
    public void testPosition_ExceedsLength() {
        // Arrange
        BsonReader reader = new BsonReader(new byte[10]);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> reader.position(11));
    }

    @Test
    public void testLength() {
        // Arrange & Act
        BsonReader reader = new BsonReader(new byte[100]);

        // Assert
        assertEquals(100, reader.length());
    }

    @Test
    public void testRemaining() {
        // Arrange
        BsonReader reader = new BsonReader(new byte[10]);

        // Assert
        assertEquals(10, reader.remaining());

        reader.skip(3);
        assertEquals(7, reader.remaining());

        reader.skip(7);
        assertEquals(0, reader.remaining());
    }

    @Test
    public void testHasRemaining_True() {
        // Arrange
        BsonReader reader = new BsonReader(new byte[10]);

        // Assert
        assertTrue(reader.hasRemaining(5));
        assertTrue(reader.hasRemaining(10));
    }

    @Test
    public void testHasRemaining_False() {
        // Arrange
        BsonReader reader = new BsonReader(new byte[10]);

        // Assert
        assertFalse(reader.hasRemaining(11));
    }

    @Test
    public void testReadByte_Success() {
        // Arrange
        byte[] buffer = new byte[]{0x12, 0x34, (byte) 0xFF};
        BsonReader reader = new BsonReader(buffer);

        // Act & Assert
        assertEquals(0x12, reader.readByte());
        assertEquals(0x34, reader.readByte());
        assertEquals((byte) 0xFF, reader.readByte());
        assertEquals(3, reader.position());
    }

    @Test
    public void testReadByte_Underflow() {
        // Arrange
        BsonReader reader = new BsonReader(new byte[0]);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> reader.readByte());
    }

    @Test
    public void testReadInt32_Success() {
        // Arrange
        byte[] buffer = createInt32Bytes(123456789);
        BsonReader reader = new BsonReader(buffer);

        // Act
        int value = reader.readInt32();

        // Assert
        assertEquals(123456789, value);
        assertEquals(4, reader.position());
    }

    @Test
    public void testReadInt32_NegativeValue() {
        // Arrange
        byte[] buffer = createInt32Bytes(-123456789);
        BsonReader reader = new BsonReader(buffer);

        // Act
        int value = reader.readInt32();

        // Assert
        assertEquals(-123456789, value);
    }

    @Test
    public void testReadInt32_Underflow() {
        // Arrange
        BsonReader reader = new BsonReader(new byte[3]); // Not enough bytes

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> reader.readInt32());
    }

    @Test
    public void testReadInt64_Success() {
        // Arrange
        byte[] buffer = createInt64Bytes(9876543210L);
        BsonReader reader = new BsonReader(buffer);

        // Act
        long value = reader.readInt64();

        // Assert
        assertEquals(9876543210L, value);
        assertEquals(8, reader.position());
    }

    @Test
    public void testReadInt64_NegativeValue() {
        // Arrange
        byte[] buffer = createInt64Bytes(-9876543210L);
        BsonReader reader = new BsonReader(buffer);

        // Act
        long value = reader.readInt64();

        // Assert
        assertEquals(-9876543210L, value);
    }

    @Test
    public void testReadInt64_Underflow() {
        // Arrange
        BsonReader reader = new BsonReader(new byte[7]); // Not enough bytes

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> reader.readInt64());
    }

    @Test
    public void testReadDouble_Success() {
        // Arrange
        byte[] buffer = createDoubleBytes(3.14159265359);
        BsonReader reader = new BsonReader(buffer);

        // Act
        double value = reader.readDouble();

        // Assert
        assertEquals(3.14159265359, value, 0.00000000001);
        assertEquals(8, reader.position());
    }

    @Test
    public void testReadDouble_NegativeValue() {
        // Arrange
        byte[] buffer = createDoubleBytes(-123.456);
        BsonReader reader = new BsonReader(buffer);

        // Act
        double value = reader.readDouble();

        // Assert
        assertEquals(-123.456, value, 0.0001);
    }

    @Test
    public void testReadDouble_Underflow() {
        // Arrange
        BsonReader reader = new BsonReader(new byte[7]); // Not enough bytes

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> reader.readDouble());
    }

    @Test
    public void testReadCString_Success() {
        // Arrange
        byte[] buffer = createCStringBytes("Hello, BSON!");
        BsonReader reader = new BsonReader(buffer);

        // Act
        String value = reader.readCString();

        // Assert
        assertEquals("Hello, BSON!", value);
        assertEquals(buffer.length, reader.position());
    }

    @Test
    public void testReadCString_EmptyString() {
        // Arrange
        byte[] buffer = new byte[]{0};
        BsonReader reader = new BsonReader(buffer);

        // Act
        String value = reader.readCString();

        // Assert
        assertEquals("", value);
        assertEquals(1, reader.position());
    }

    @Test
    public void testReadCString_UTF8Characters() {
        // Arrange
        byte[] buffer = createCStringBytes("你好世界");
        BsonReader reader = new BsonReader(buffer);

        // Act
        String value = reader.readCString();

        // Assert
        assertEquals("你好世界", value);
    }

    @Test
    public void testReadCString_NoNullTerminator() {
        // Arrange
        byte[] buffer = "NoNullTerminator".getBytes(StandardCharsets.UTF_8);
        BsonReader reader = new BsonReader(buffer);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> reader.readCString());
    }

    @Test
    public void testReadCString_Interning() {
        // Arrange - Create two separate buffers with same field name
        byte[] buffer1 = createCStringBytes("fieldName");
        byte[] buffer2 = createCStringBytes("fieldName");
        BsonReader reader1 = new BsonReader(buffer1);
        BsonReader reader2 = new BsonReader(buffer2);

        // Act
        String str1 = reader1.readCString();
        String str2 = reader2.readCString();

        // Assert
        assertEquals("fieldName", str1);
        assertEquals("fieldName", str2);
        assertSame(str1, str2); // Key assertion: same reference (interned)
        assertEquals(1, StringPool.getPoolSize()); // Only one entry in pool
    }

    @Test
    public void testReadCString_DifferentStrings() {
        // Arrange
        byte[] buffer1 = createCStringBytes("field1");
        byte[] buffer2 = createCStringBytes("field2");
        BsonReader reader1 = new BsonReader(buffer1);
        BsonReader reader2 = new BsonReader(buffer2);

        // Act
        String str1 = reader1.readCString();
        String str2 = reader2.readCString();

        // Assert
        assertEquals("field1", str1);
        assertEquals("field2", str2);
        assertNotSame(str1, str2); // Different strings, different references
        assertEquals(2, StringPool.getPoolSize()); // Two entries in pool
    }

    @Test
    public void testReadString_Success() {
        // Arrange
        String testString = "Hello, BSON!";
        byte[] stringBytes = testString.getBytes(StandardCharsets.UTF_8);
        byte[] buffer = new byte[4 + stringBytes.length + 1];

        // Write length (including null terminator)
        ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(stringBytes.length + 1);
        System.arraycopy(stringBytes, 0, buffer, 4, stringBytes.length);
        buffer[buffer.length - 1] = 0; // null terminator

        BsonReader reader = new BsonReader(buffer);

        // Act
        String value = reader.readString();

        // Assert
        assertEquals(testString, value);
    }

    @Test
    public void testReadString_InvalidLength() {
        // Arrange
        byte[] buffer = createInt32Bytes(0); // Invalid length
        BsonReader reader = new BsonReader(buffer);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> reader.readString());
    }

    @Test
    public void testReadString_Underflow() {
        // Arrange
        byte[] buffer = createInt32Bytes(100); // Claims 100 bytes but buffer is smaller
        BsonReader reader = new BsonReader(buffer);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> reader.readString());
    }

    @Test
    public void testReadBytes_Success() {
        // Arrange
        byte[] buffer = new byte[]{1, 2, 3, 4, 5};
        BsonReader reader = new BsonReader(buffer);

        // Act
        byte[] result = reader.readBytes(3);

        // Assert
        assertArrayEquals(new byte[]{1, 2, 3}, result);
        assertEquals(3, reader.position());
    }

    @Test
    public void testReadBytes_ZeroLength() {
        // Arrange
        BsonReader reader = new BsonReader(new byte[5]);

        // Act
        byte[] result = reader.readBytes(0);

        // Assert
        assertEquals(0, result.length);
        assertEquals(0, reader.position());
    }

    @Test
    public void testReadBytes_NegativeLength() {
        // Arrange
        BsonReader reader = new BsonReader(new byte[5]);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> reader.readBytes(-1));
    }

    @Test
    public void testReadBytes_Underflow() {
        // Arrange
        BsonReader reader = new BsonReader(new byte[5]);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> reader.readBytes(10));
    }

    @Test
    public void testSkip_Success() {
        // Arrange
        BsonReader reader = new BsonReader(new byte[10]);

        // Act
        reader.skip(5);

        // Assert
        assertEquals(5, reader.position());
    }

    @Test
    public void testSkip_ZeroBytes() {
        // Arrange
        BsonReader reader = new BsonReader(new byte[10]);

        // Act
        reader.skip(0);

        // Assert
        assertEquals(0, reader.position());
    }

    @Test
    public void testSkip_NegativeBytes() {
        // Arrange
        BsonReader reader = new BsonReader(new byte[10]);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> reader.skip(-1));
    }

    @Test
    public void testSkip_ExceedsBuffer() {
        // Arrange
        BsonReader reader = new BsonReader(new byte[10]);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> reader.skip(11));
    }

    @Test
    public void testPeekByte_Success() {
        // Arrange
        byte[] buffer = new byte[]{0x12, 0x34};
        BsonReader reader = new BsonReader(buffer);

        // Act
        byte value = reader.peekByte();

        // Assert
        assertEquals(0x12, value);
        assertEquals(0, reader.position()); // Position unchanged

        // Peek again
        value = reader.peekByte();
        assertEquals(0x12, value);
        assertEquals(0, reader.position());
    }

    @Test
    public void testPeekByte_Underflow() {
        // Arrange
        BsonReader reader = new BsonReader(new byte[0]);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> reader.peekByte());
    }

    @Test
    public void testIsAtEnd_True() {
        // Arrange
        BsonReader reader = new BsonReader(new byte[5]);
        reader.skip(5);

        // Assert
        assertTrue(reader.isAtEnd());
    }

    @Test
    public void testIsAtEnd_False() {
        // Arrange
        BsonReader reader = new BsonReader(new byte[5]);

        // Assert
        assertFalse(reader.isAtEnd());

        reader.skip(4);
        assertFalse(reader.isAtEnd());
    }

    @Test
    public void testSequentialReading() {
        // Arrange
        ByteBuffer buffer = ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(12345);           // int32
        buffer.putLong(9876543210L);    // int64
        buffer.putDouble(3.14159);      // double
        buffer.put((byte) 0x42);        // byte
        buffer.put("test\u0000".getBytes(StandardCharsets.UTF_8)); // C-string

        BsonReader reader = new BsonReader(buffer.array());

        // Act & Assert
        assertEquals(12345, reader.readInt32());
        assertEquals(9876543210L, reader.readInt64());
        assertEquals(3.14159, reader.readDouble(), 0.00001);
        assertEquals(0x42, reader.readByte());
        assertEquals("test", reader.readCString());
    }
}
