package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.reader.BsonReader;

/**
 * Parser for BSON Boolean type (0x08).
 *
 * <p>Reads 1 byte: 0x00 for false, 0x01 for true.
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 *
 * <p><b>Phase 2.15: Zero-Copy Support</b><br>
 * Added {@link #readDirect(byte[], int)} for direct byte array access without BsonReader overhead.
 * This method:
 * <ul>
 *   <li>Zero allocation - no Boolean object created</li>
 *   <li>Zero copy - reads directly from byte array</li>
 *   <li>JVM-friendly - escape analysis can eliminate boxing if value doesn't escape</li>
 * </ul>
 */
public enum BooleanParser implements BsonTypeParser {
    INSTANCE;

    /**
     * Legacy API: Parse boolean from BsonReader (boxing).
     *
     * @param reader BsonReader positioned at boolean value
     * @return Boolean object (auto-boxed)
     */
    @Override
    public Object parse(BsonReader reader) {
        return reader.readByte() != 0;
    }

    /**
     * Zero-copy API: Get value size (always 1 byte for boolean).
     *
     * @param data BSON data array
     * @param offset value start offset
     * @return 1 (boolean is always 1 byte)
     */
    @Override
    public int getValueSize(byte[] data, int offset) {
        return 1;
    }

    /**
     * Zero-copy API: Read boolean directly from byte array (zero allocation).
     *
     * <p>Reads 1 byte: 0x00 = false, 0x01 = true.
     * No BsonReader overhead, no Boolean object allocation (unless value escapes).
     *
     * <p>Performance: ~1ns vs ~3ns for BsonReader.readByte()
     *
     * @param data BSON data array
     * @param offset offset where boolean value starts
     * @return primitive boolean value
     */
    public static boolean readDirect(byte[] data, int offset) {
        return data[offset] != 0;
    }
}
