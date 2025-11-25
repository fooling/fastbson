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
 */
public enum ObjectIdParser implements BsonTypeParser {
    INSTANCE;

    @Override
    public Object parse(BsonReader reader) {
        return BsonUtils.bytesToHex(reader.readBytes(12));
    }
}
