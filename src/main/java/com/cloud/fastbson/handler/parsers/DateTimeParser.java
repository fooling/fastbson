package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.reader.BsonReader;

/**
 * Parser for BSON DateTime type (0x09).
 *
 * <p>Reads 8 bytes as int64 representing UTC milliseconds since Unix epoch.
 *
 * <p>Returns Long for optimal performance. Users can convert to Date if needed:
 * {@code new Date(longValue)}.
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 */
public enum DateTimeParser implements BsonTypeParser {
    INSTANCE;

    @Override
    public Object parse(BsonReader reader) {
        return reader.readInt64();
    }
}
