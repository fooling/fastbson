package com.cloud.fastbson.util;

import java.nio.charset.StandardCharsets;

/**
 * Utility methods for BSON operations.
 */
public final class BsonUtils {

    // Prevent instantiation
    private BsonUtils() {
        throw new AssertionError("Cannot instantiate BsonUtils");
    }

    /**
     * Converts a byte array to a hexadecimal string.
     *
     * @param bytes the byte array
     * @return hexadecimal string representation
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }

    /**
     * Converts a hexadecimal string to a byte array.
     *
     * @param hex the hexadecimal string
     * @return byte array
     * @throws IllegalArgumentException if hex string is invalid
     */
    public static byte[] hexToBytes(String hex) {
        if (hex == null) {
            return null;
        }
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;
            bytes[i] = (byte) Integer.parseInt(hex.substring(index, index + 2), 16);
        }
        return bytes;
    }

    /**
     * Reads a C-style null-terminated string from a byte array.
     *
     * @param buffer the byte array
     * @param offset the starting offset
     * @return the string and the number of bytes consumed (including null terminator)
     */
    public static CStringResult readCString(byte[] buffer, int offset) {
        int start = offset;
        int end = offset;
        while (end < buffer.length && buffer[end] != 0) {
            end++;
        }
        if (end >= buffer.length) {
            throw new IllegalArgumentException("No null terminator found for C-string");
        }
        String str = new String(buffer, start, end - start, StandardCharsets.UTF_8);
        return new CStringResult(str, end - start + 1); // +1 for null terminator
    }

    /**
     * Result of reading a C-string.
     */
    public static class CStringResult {
        public final String value;
        public final int bytesConsumed;

        public CStringResult(String value, int bytesConsumed) {
            this.value = value;
            this.bytesConsumed = bytesConsumed;
        }
    }

    /**
     * Validates that a buffer has at least the required number of bytes remaining.
     *
     * @param buffer the buffer
     * @param position the current position
     * @param required the required number of bytes
     * @throws IllegalArgumentException if insufficient bytes
     */
    public static void validateBufferSize(byte[] buffer, int position, int required) {
        if (buffer == null) {
            throw new IllegalArgumentException("Buffer cannot be null");
        }
        if (position < 0) {
            throw new IllegalArgumentException("Position cannot be negative: " + position);
        }
        if (position + required > buffer.length) {
            throw new IllegalArgumentException(
                String.format("Buffer underflow: position=%d, required=%d, available=%d",
                    position, required, buffer.length - position)
            );
        }
    }

    /**
     * Reads an int32 value in little-endian byte order.
     *
     * @param buffer the byte array
     * @param offset the starting offset
     * @return the int32 value
     */
    public static int readInt32LittleEndian(byte[] buffer, int offset) {
        return (buffer[offset] & 0xFF) |
               ((buffer[offset + 1] & 0xFF) << 8) |
               ((buffer[offset + 2] & 0xFF) << 16) |
               ((buffer[offset + 3] & 0xFF) << 24);
    }

    /**
     * Reads an int64 value in little-endian byte order.
     *
     * @param buffer the byte array
     * @param offset the starting offset
     * @return the int64 value
     */
    public static long readInt64LittleEndian(byte[] buffer, int offset) {
        return (buffer[offset] & 0xFFL) |
               ((buffer[offset + 1] & 0xFFL) << 8) |
               ((buffer[offset + 2] & 0xFFL) << 16) |
               ((buffer[offset + 3] & 0xFFL) << 24) |
               ((buffer[offset + 4] & 0xFFL) << 32) |
               ((buffer[offset + 5] & 0xFFL) << 40) |
               ((buffer[offset + 6] & 0xFFL) << 48) |
               ((buffer[offset + 7] & 0xFFL) << 56);
    }

    /**
     * Reads a double value in little-endian byte order.
     *
     * @param buffer the byte array
     * @param offset the starting offset
     * @return the double value
     */
    public static double readDoubleLittleEndian(byte[] buffer, int offset) {
        long bits = readInt64LittleEndian(buffer, offset);
        return Double.longBitsToDouble(bits);
    }
}
