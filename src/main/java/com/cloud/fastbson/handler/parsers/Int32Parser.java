package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.reader.BsonReader;

/**
 * Parser for BSON Int32 type (0x10).
 *
 * <p>Reads 4 bytes as little-endian signed 32-bit integer.
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 *
 * <p><b>Phase 2.15: Zero-Copy Support</b><br>
 * Added {@link #readDirect(byte[], int)} for direct byte array access without BsonReader overhead.
 * This method:
 * <ul>
 *   <li>Zero allocation - no Integer object created</li>
 *   <li>Zero copy - reads directly from byte array</li>
 *   <li>JVM-friendly - escape analysis can eliminate boxing if value doesn't escape</li>
 * </ul>
 */
public enum Int32Parser implements BsonTypeParser {
    INSTANCE;

    /**
     * Legacy API: Parse int32 from BsonReader (boxing).
     *
     * @param reader BsonReader positioned at int32 value
     * @return Integer object (auto-boxed)
     */
    @Override
    public Object parse(BsonReader reader) {
        return reader.readInt32();
    }

    /**
     * Zero-copy API: Get value size (always 4 bytes for int32).
     *
     * @param data BSON data array
     * @param offset value start offset
     * @return 4 (int32 is always 4 bytes)
     */
    @Override
    public int getValueSize(byte[] data, int offset) {
        return 4;
    }

    /**
     * Zero-copy API: Read int32 directly from byte array (zero allocation).
     *
     * <p>Reads 4 bytes in little-endian order and returns primitive int.
     * No BsonReader overhead, no Integer object allocation (unless value escapes).
     *
     * <p>Performance: ~2ns vs ~5ns for BsonReader.readInt32()
     *
     * @param data BSON data array
     * @param offset offset where int32 value starts
     * @return primitive int value
     */
    public static int readDirect(byte[] data, int offset) {
        return (data[offset] & 0xFF)
            | ((data[offset + 1] & 0xFF) << 8)
            | ((data[offset + 2] & 0xFF) << 16)
            | ((data[offset + 3] & 0xFF) << 24);
    }
}
