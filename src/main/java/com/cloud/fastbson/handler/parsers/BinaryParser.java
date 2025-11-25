package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.reader.BsonReader;
import com.cloud.fastbson.types.BinaryData;

/**
 * Parser for BSON Binary type (0x05).
 *
 * <p>Parses binary data: int32 length + subtype byte + data bytes.
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 */
public enum BinaryParser implements BsonTypeParser {
    INSTANCE;

    @Override
    public Object parse(BsonReader reader) {
        int length = reader.readInt32();
        byte subtype = reader.readByte();
        byte[] data = reader.readBytes(length);
        return new BinaryData(subtype, data);
    }
}
