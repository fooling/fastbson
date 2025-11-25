package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.reader.BsonReader;

/**
 * Parser for BSON Null (0x0A) and Undefined (0x06) types.
 *
 * <p>Both types have no value payload and are represented as Java null.
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 *
 * <p><b>Phase 2.15: Zero-Copy Support</b><br>
 * Added {@link #getValueSize(byte[], int)} for index building.
 */
public enum NullParser implements BsonTypeParser {
    INSTANCE;

    @Override
    public Object parse(BsonReader reader) {
        return null;
    }

    /**
     * Zero-copy API: Get value size (always 0 bytes for null/undefined).
     *
     * @param data BSON data array
     * @param offset value start offset (unused)
     * @return 0 (null has no value payload)
     */
    @Override
    public int getValueSize(byte[] data, int offset) {
        return 0;
    }
}
