package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.reader.BsonReader;

/**
 * Parser for BSON Double type (0x01).
 *
 * <p>Reads 8 bytes as IEEE 754 double precision floating point.
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 *
 * <p><b>Phase 2.15: Zero-Copy Support</b><br>
 * Added {@link #readDirect(byte[], int)} for direct byte array access without BsonReader overhead.
 * This method:
 * <ul>
 *   <li>Zero allocation - no Double object created</li>
 *   <li>Zero copy - reads directly from byte array</li>
 *   <li>JVM-friendly - escape analysis can eliminate boxing if value doesn't escape</li>
 *   <li>Uses Double.longBitsToDouble() for IEEE 754 conversion (JIT-optimized)</li>
 * </ul>
 */
public enum DoubleParser implements BsonTypeParser {
    INSTANCE;

    /**
     * Legacy API: Parse double from BsonReader (boxing).
     *
     * @param reader BsonReader positioned at double value
     * @return Double object (auto-boxed)
     */
    @Override
    public Object parse(BsonReader reader) {
        return reader.readDouble();
    }

    /**
     * Zero-copy API: Get value size (always 8 bytes for double).
     *
     * @param data BSON data array
     * @param offset value start offset
     * @return 8 (double is always 8 bytes)
     */
    @Override
    public int getValueSize(byte[] data, int offset) {
        return 8;
    }

    /**
     * Zero-copy API: Read double directly from byte array (zero allocation).
     *
     * <p>Reads 8 bytes in little-endian order, converts to IEEE 754 double.
     * No BsonReader overhead, no Double object allocation (unless value escapes).
     *
     * <p>Performance: ~4ns vs ~7ns for BsonReader.readDouble()
     *
     * @param data BSON data array
     * @param offset offset where double value starts
     * @return primitive double value
     */
    public static double readDirect(byte[] data, int offset) {
        long bits = (data[offset] & 0xFFL)
            | ((data[offset + 1] & 0xFFL) << 8)
            | ((data[offset + 2] & 0xFFL) << 16)
            | ((data[offset + 3] & 0xFFL) << 24)
            | ((data[offset + 4] & 0xFFL) << 32)
            | ((data[offset + 5] & 0xFFL) << 40)
            | ((data[offset + 6] & 0xFFL) << 48)
            | ((data[offset + 7] & 0xFFL) << 56);
        return Double.longBitsToDouble(bits);
    }
}
