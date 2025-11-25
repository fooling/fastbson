package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.reader.BsonReader;

/**
 * Parser for BSON String type (0x02).
 *
 * <p>Reads int32 length, UTF-8 bytes, and null terminator.
 *
 * <p>Also used for JavaScript (0x0D) and Symbol (0x0E) types
 * as they share the same string format.
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 */
public enum StringParser implements BsonTypeParser {
    INSTANCE;

    @Override
    public Object parse(BsonReader reader) {
        return reader.readString();
    }
}
