package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.handler.TypeHandler;
import com.cloud.fastbson.reader.BsonReader;
import com.cloud.fastbson.util.BsonType;

import java.util.HashMap;
import java.util.Map;

/**
 * Parser for BSON Document type (0x03).
 *
 * <p>Parses embedded BSON documents recursively by delegating to TypeHandler.
 *
 * <p>Document structure: int32 length + elements + 0x00 terminator
 * Each element: type byte + cstring field name + value
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 */
public enum DocumentParser implements BsonTypeParser {
    INSTANCE;

    private TypeHandler handler;

    /**
     * Sets the TypeHandler for recursive parsing.
     * Called by TypeHandler during initialization.
     */
    public void setHandler(TypeHandler handler) {
        this.handler = handler;
    }

    @Override
    public Object parse(BsonReader reader) {
        int docLength = reader.readInt32();
        int endPosition = reader.position() + docLength - 4;

        Map<String, Object> document = new HashMap<String, Object>();

        while (reader.position() < endPosition) {
            byte type = reader.readByte();
            if (type == BsonType.END_OF_DOCUMENT) {
                break;
            }

            String fieldName = reader.readCString();
            // Delegate to TypeHandler for recursive parsing
            Object value = handler.parseValue(reader, type);
            document.put(fieldName, value);
        }

        return document;
    }
}
