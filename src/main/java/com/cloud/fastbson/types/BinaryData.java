package com.cloud.fastbson.types;

/**
 * Represents BSON binary data.
 *
 * <p>Structure: byte subtype + byte[] data
 */
public class BinaryData {
    public final byte subtype;
    public final byte[] data;

    public BinaryData(byte subtype, byte[] data) {
        this.subtype = subtype;
        this.data = data;
    }
}
