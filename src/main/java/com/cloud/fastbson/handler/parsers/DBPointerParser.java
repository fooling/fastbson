package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.handler.TypeHandler;
import com.cloud.fastbson.reader.BsonReader;
import com.cloud.fastbson.util.BsonUtils;

/**
 * Parser for BSON DBPointer type (0x0C) - deprecated.
 *
 * <p>Parses DBPointer: string namespace + 12-byte ObjectId.
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 */
public enum DBPointerParser implements BsonTypeParser {
    INSTANCE;

    @Override
    public Object parse(BsonReader reader) {
        String namespace = reader.readString();
        byte[] id = reader.readBytes(12);
        return new TypeHandler.DBPointer(namespace, BsonUtils.bytesToHex(id));
    }
}
