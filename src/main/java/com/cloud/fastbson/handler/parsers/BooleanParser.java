package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.reader.BsonReader;

/**
 * Parser for BSON Boolean type (0x08).
 *
 * <p>Reads 1 byte: 0x00 for false, 0x01 for true.
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 */
public enum BooleanParser implements BsonTypeParser {
    INSTANCE;

    @Override
    public Object parse(BsonReader reader) {
        return reader.readByte() != 0;
    }
}
