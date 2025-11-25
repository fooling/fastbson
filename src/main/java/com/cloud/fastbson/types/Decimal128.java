package com.cloud.fastbson.types;

/**
 * Represents a BSON Decimal128 value.
 *
 * <p>Java doesn't have native 128-bit decimal, so we store as 16 bytes.
 */
public class Decimal128 {
    public final byte[] bytes;

    public Decimal128(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            throw new IllegalArgumentException("Decimal128 must be exactly 16 bytes");
        }
        this.bytes = bytes;
    }
}
