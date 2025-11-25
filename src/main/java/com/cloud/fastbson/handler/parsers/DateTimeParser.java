package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.reader.BsonReader;

/**
 * Parser for BSON DateTime type (0x09).
 *
 * <p>Reads 8 bytes as int64 representing UTC milliseconds since Unix epoch.
 *
 * <p>Returns Long for optimal performance. Users can convert to Date if needed:
 * {@code new Date(longValue)}.
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 *
 * <p><b>Phase 2.15: Zero-Copy Support</b><br>
 * Added {@link #getValueSize(byte[], int)} and {@link #readDirect(byte[], int)} for zero-copy parsing.
 */
public enum DateTimeParser implements BsonTypeParser {
    INSTANCE;

    @Override
    public Object parse(BsonReader reader) {
        return reader.readInt64();
    }

    /**
     * Zero-copy API: Get value size (always 8 bytes for datetime).
     *
     * @param data BSON data array
     * @param offset value start offset
     * @return 8 (datetime is always 8 bytes)
     */
    @Override
    public int getValueSize(byte[] data, int offset) {
        return 8;
    }

    /**
     * Zero-copy API: Read datetime directly from byte array (zero allocation).
     *
     * <p>Reads 8 bytes in little-endian order as milliseconds since epoch.
     *
     * @param data BSON data array
     * @param offset offset where datetime value starts
     * @return primitive long value (milliseconds since Unix epoch)
     */
    public static long readDirect(byte[] data, int offset) {
        return (data[offset] & 0xFFL)
            | ((data[offset + 1] & 0xFFL) << 8)
            | ((data[offset + 2] & 0xFFL) << 16)
            | ((data[offset + 3] & 0xFFL) << 24)
            | ((data[offset + 4] & 0xFFL) << 32)
            | ((data[offset + 5] & 0xFFL) << 40)
            | ((data[offset + 6] & 0xFFL) << 48)
            | ((data[offset + 7] & 0xFFL) << 56);
    }
}
