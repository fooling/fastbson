package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.handler.TypeHandler;
import com.cloud.fastbson.reader.BsonReader;

/**
 * Parser for BSON Timestamp type (0x11).
 *
 * <p>Parses MongoDB internal timestamp: int64 with increment in low 32 bits
 * and seconds in high 32 bits.
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 */
public enum TimestampParser implements BsonTypeParser {
    INSTANCE;

    @Override
    public Object parse(BsonReader reader) {
        long value = reader.readInt64();
        int increment = (int) (value & 0xFFFFFFFFL);
        int seconds = (int) ((value >> 32) & 0xFFFFFFFFL);
        return new TypeHandler.Timestamp(seconds, increment);
    }
}
