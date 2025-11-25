package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.reader.BsonReader;
import com.cloud.fastbson.util.BsonUtils;

/**
 * Parser for BSON ObjectId type (0x07).
 *
 * <p>Reads 12 bytes representing MongoDB ObjectId and converts to hex string.
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 *
 * <p><b>Phase 2.15: Zero-Copy Support</b><br>
 * Added {@link #getValueSize(byte[], int)} for index building.
 */
public enum ObjectIdParser implements BsonTypeParser {
    INSTANCE;

    @Override
    public Object parse(BsonReader reader) {
        return BsonUtils.bytesToHex(reader.readBytes(12));
    }

    /**
     * Zero-copy API: Get value size (always 12 bytes for ObjectId).
     *
     * @param data BSON data array
     * @param offset value start offset
     * @return 12 (ObjectId is always 12 bytes)
     */
    @Override
    public int getValueSize(byte[] data, int offset) {
        return 12;
    }
}
