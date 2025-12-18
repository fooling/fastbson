package com.cloud.fastbson.reader;

import com.cloud.fastbson.util.BsonUtils;
import com.cloud.fastbson.util.StringPool;

import java.nio.charset.StandardCharsets;

/**
 * Low-level byte stream reader for BSON data.
 *
 * <p>Provides methods to read BSON primitive types from a byte array
 * in little-endian byte order as per BSON specification.
 *
 * <p>This class is NOT thread-safe and should be used within a single thread
 * or protected by ThreadLocal for multi-threaded scenarios.
 */
public class BsonReader {

    private byte[] buffer;
    private int position;

    /**
     * Creates a new BsonReader with the given byte array.
     *
     * @param buffer the BSON data buffer
     * @throws IllegalArgumentException if buffer is null
     */
    public BsonReader(byte[] buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("Buffer cannot be null");
        }
        this.buffer = buffer;
        this.position = 0;
    }

    /**
     * Resets this reader with new data.
     * Used for object pooling to avoid creating new instances.
     *
     * @param buffer the new BSON data buffer
     * @throws IllegalArgumentException if buffer is null
     */
    public void reset(byte[] buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("Buffer cannot be null");
        }
        this.buffer = buffer;
        this.position = 0;
    }

    /**
     * Returns the current reading position.
     *
     * @return the current position
     */
    public int position() {
        return position;
    }

    /**
     * Returns the underlying buffer for zero-copy operations.
     *
     * <p><b>Phase 2 Zero-Copy Support</b>
     * <p>Exposes the raw byte buffer to enable zero-copy parsing strategies
     * like IndexedBsonDocument which builds a field index without copying data.
     *
     * @return the underlying byte buffer
     */
    public byte[] getBuffer() {
        return buffer;
    }

    /**
     * Sets the reading position.
     *
     * @param position the new position
     * @throws IllegalArgumentException if position is negative or beyond buffer length
     */
    public void position(int position) {
        if (position < 0) {
            throw new IllegalArgumentException("Position cannot be negative: " + position);
        }
        if (position > buffer.length) {
            throw new IllegalArgumentException(
                String.format("Position %d exceeds buffer length %d", position, buffer.length)
            );
        }
        this.position = position;
    }

    /**
     * Returns the total buffer length.
     *
     * @return the buffer length
     */
    public int length() {
        return buffer.length;
    }

    /**
     * Returns the number of bytes remaining from current position.
     *
     * @return the remaining bytes
     */
    public int remaining() {
        return buffer.length - position;
    }

    /**
     * Checks if there are at least the specified number of bytes remaining.
     *
     * @param bytes the number of bytes to check
     * @return true if enough bytes are available
     */
    public boolean hasRemaining(int bytes) {
        return position + bytes <= buffer.length;
    }

    /**
     * Reads a single byte and advances the position.
     *
     * @return the byte value
     * @throws IllegalArgumentException if buffer underflow
     */
    public byte readByte() {
        BsonUtils.validateBufferSize(buffer, position, 1);
        return buffer[position++];
    }

    /**
     * Reads a 32-bit integer in little-endian byte order.
     *
     * @return the int32 value
     * @throws IllegalArgumentException if buffer underflow
     */
    public int readInt32() {
        BsonUtils.validateBufferSize(buffer, position, 4);
        int value = BsonUtils.readInt32LittleEndian(buffer, position);
        position += 4;
        return value;
    }

    /**
     * Reads a 64-bit integer in little-endian byte order.
     *
     * @return the int64 value
     * @throws IllegalArgumentException if buffer underflow
     */
    public long readInt64() {
        BsonUtils.validateBufferSize(buffer, position, 8);
        long value = BsonUtils.readInt64LittleEndian(buffer, position);
        position += 8;
        return value;
    }

    /**
     * Reads a 64-bit IEEE 754 double in little-endian byte order.
     *
     * @return the double value
     * @throws IllegalArgumentException if buffer underflow
     */
    public double readDouble() {
        BsonUtils.validateBufferSize(buffer, position, 8);
        double value = BsonUtils.readDoubleLittleEndian(buffer, position);
        position += 8;
        return value;
    }

    /**
     * Reads a C-style null-terminated UTF-8 string.
     *
     * @return the string value
     * @throws IllegalArgumentException if no null terminator found or buffer underflow
     */
    public String readCString() {
        int start = position;
        while (position < buffer.length && buffer[position] != 0) {
            position++;
        }
        if (position >= buffer.length) {
            throw new IllegalArgumentException(
                String.format("No null terminator found for C-string starting at position %d", start)
            );
        }
        String str = new String(buffer, start, position - start, StandardCharsets.UTF_8);
        position++; // skip null terminator
        return StringPool.intern(str);
    }

    /**
     * Skips a C-style null-terminated string without creating a String object.
     *
     * <p><b>Phase 3.5: Array Optimization</b><br>
     * This method is used to skip array index field names ("0", "1", "2", ...)
     * without the overhead of UTF-8 decoding and String object creation.
     *
     * <p>Performance improvement: ~30-40% faster than readCString() for array parsing,
     * as it avoids UTF-8 decoding, String object creation, and StringPool interning.
     *
     * @throws IllegalArgumentException if no null terminator found or buffer underflow
     */
    public void skipCString() {
        int start = position;
        while (position < buffer.length && buffer[position] != 0) {
            position++;
        }
        if (position >= buffer.length) {
            throw new IllegalArgumentException(
                String.format("No null terminator found for C-string starting at position %d", start)
            );
        }
        position++; // skip null terminator
    }

    /**
     * Reads a BSON string (int32 length prefix + UTF-8 string + null terminator).
     *
     * @return the string value
     * @throws IllegalArgumentException if buffer underflow
     */
    public String readString() {
        int length = readInt32();
        if (length < 1) {
            throw new IllegalArgumentException(
                String.format("Invalid string length: %d (must be at least 1)", length)
            );
        }
        BsonUtils.validateBufferSize(buffer, position, length);
        // length includes null terminator
        String str = new String(buffer, position, length - 1, StandardCharsets.UTF_8);
        position += length;
        return str;
    }

    /**
     * Reads the specified number of bytes into a new array.
     *
     * @param length the number of bytes to read
     * @return the byte array
     * @throws IllegalArgumentException if length is negative or buffer underflow
     */
    public byte[] readBytes(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative: " + length);
        }
        if (length == 0) {
            return new byte[0];
        }
        BsonUtils.validateBufferSize(buffer, position, length);
        byte[] bytes = new byte[length];
        System.arraycopy(buffer, position, bytes, 0, length);
        position += length;
        return bytes;
    }

    /**
     * Skips the specified number of bytes.
     *
     * @param bytes the number of bytes to skip
     * @throws IllegalArgumentException if bytes is negative or would exceed buffer
     */
    public void skip(int bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("Cannot skip negative bytes: " + bytes);
        }
        BsonUtils.validateBufferSize(buffer, position, bytes);
        position += bytes;
    }

    /**
     * Peeks at the byte at the current position without advancing.
     *
     * @return the byte value
     * @throws IllegalArgumentException if buffer underflow
     */
    public byte peekByte() {
        BsonUtils.validateBufferSize(buffer, position, 1);
        return buffer[position];
    }

    /**
     * Checks if we've reached the end of the buffer.
     *
     * @return true if at end of buffer
     */
    public boolean isAtEnd() {
        return position >= buffer.length;
    }
}
