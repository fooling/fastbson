package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.reader.BsonReader;

/**
 * Parser for BSON Int64 type (0x12).
 *
 * <p>Reads 8 bytes as little-endian signed 64-bit integer.
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 */
public enum Int64Parser implements BsonTypeParser {
    INSTANCE;

    @Override
    public Object parse(BsonReader reader) {
        return reader.readInt64();
    }
}
