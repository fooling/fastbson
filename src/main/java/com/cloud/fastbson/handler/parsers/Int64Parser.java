package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.reader.BsonReader;

/**
 * Parser for BSON Int64 type (0x12).
 *
 * <p>Reads 8 bytes as little-endian signed 64-bit integer.
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 *
 * <p><b>Phase 2.15: Zero-Copy Support</b><br>
 * Added {@link #readDirect(byte[], int)} for direct byte array access without BsonReader overhead.
 * This method:
 * <ul>
 *   <li>Zero allocation - no Long object created</li>
 *   <li>Zero copy - reads directly from byte array</li>
 *   <li>JVM-friendly - escape analysis can eliminate boxing if value doesn't escape</li>
 * </ul>
 */
public enum Int64Parser implements BsonTypeParser {
    INSTANCE;

    /**
     * Legacy API: Parse int64 from BsonReader (boxing).
     *
     * @param reader BsonReader positioned at int64 value
     * @return Long object (auto-boxed)
     */
    @Override
    public Object parse(BsonReader reader) {
        return reader.readInt64();
    }

    /**
     * Zero-copy API: Get value size (always 8 bytes for int64).
     *
     * @param data BSON data array
     * @param offset value start offset
     * @return 8 (int64 is always 8 bytes)
     */
    @Override
    public int getValueSize(byte[] data, int offset) {
        return 8;
    }

    /**
     * Zero-copy API: Read int64 directly from byte array (zero allocation).
     *
     * <p>Reads 8 bytes in little-endian order and returns primitive long.
     * No BsonReader overhead, no Long object allocation (unless value escapes).
     *
     * <p>Performance: ~3ns vs ~6ns for BsonReader.readInt64()
     *
     * @param data BSON data array
     * @param offset offset where int64 value starts
     * @return primitive long value
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
