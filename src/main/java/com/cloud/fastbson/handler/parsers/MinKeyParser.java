package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.reader.BsonReader;
import com.cloud.fastbson.types.MinKey;

/**
 * Parser for BSON MinKey type (0xFF).
 *
 * <p>MinKey is a special BSON type that always compares less than any other BSON value.
 * It has no value payload.
 *
 * <p>Returns singleton instance to avoid allocations.
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 */
public enum MinKeyParser implements BsonTypeParser {
    INSTANCE;

    @Override
    public Object parse(BsonReader reader) {
        return MinKey.INSTANCE;
    }
}
