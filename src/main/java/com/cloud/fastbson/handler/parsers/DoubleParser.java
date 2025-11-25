package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.reader.BsonReader;

/**
 * Parser for BSON Double type (0x01).
 *
 * <p>Reads 8 bytes as IEEE 754 double precision floating point.
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 */
public enum DoubleParser implements BsonTypeParser {
    INSTANCE;

    @Override
    public Object parse(BsonReader reader) {
        return reader.readDouble();
    }
}
