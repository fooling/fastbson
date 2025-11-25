package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.reader.BsonReader;

/**
 * Parser for BSON Null (0x0A) and Undefined (0x06) types.
 *
 * <p>Both types have no value payload and are represented as Java null.
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 */
public enum NullParser implements BsonTypeParser {
    INSTANCE;

    @Override
    public Object parse(BsonReader reader) {
        return null;
    }
}
