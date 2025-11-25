package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.reader.BsonReader;

/**
 * Parser for BSON Int32 type (0x10).
 *
 * <p>Reads 4 bytes as little-endian signed 32-bit integer.
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 */
public enum Int32Parser implements BsonTypeParser {
    INSTANCE;

    @Override
    public Object parse(BsonReader reader) {
        return reader.readInt32();
    }
}
