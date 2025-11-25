package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.handler.TypeHandler;
import com.cloud.fastbson.reader.BsonReader;

/**
 * Parser for BSON MaxKey type (0x7F).
 *
 * <p>MaxKey is a special BSON type that always compares greater than any other BSON value.
 * It has no value payload.
 *
 * <p>Returns singleton instance to avoid allocations.
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 */
public enum MaxKeyParser implements BsonTypeParser {
    INSTANCE;

    @Override
    public Object parse(BsonReader reader) {
        return TypeHandler.MaxKey.INSTANCE;
    }
}
