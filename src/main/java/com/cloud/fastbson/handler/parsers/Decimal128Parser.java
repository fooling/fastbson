package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.reader.BsonReader;
import com.cloud.fastbson.types.Decimal128;

/**
 * Parser for BSON Decimal128 type (0x13).
 *
 * <p>Parses 128-bit decimal value stored as 16 bytes.
 *
 * <p>Note: Java doesn't have native 128-bit decimal, so we store as bytes.
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 */
public enum Decimal128Parser implements BsonTypeParser {
    INSTANCE;

    @Override
    public Object parse(BsonReader reader) {
        byte[] bytes = reader.readBytes(16);
        return new Decimal128(bytes);
    }
}
