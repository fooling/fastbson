package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.handler.TypeHandler;
import com.cloud.fastbson.reader.BsonReader;
import com.cloud.fastbson.util.BsonType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for BSON Array type (0x04).
 *
 * <p>Parses BSON arrays. Arrays are stored as documents with string indices
 * ("0", "1", "2", etc.) as field names.
 *
 * <p>Converts the document representation to a Java List.
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 */
public enum ArrayParser implements BsonTypeParser {
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
        // Read array as document first
        int docLength = reader.readInt32();
        int endPosition = reader.position() + docLength - 4;

        Map<String, Object> arrayDoc = new HashMap<String, Object>();

        while (reader.position() < endPosition) {
            byte type = reader.readByte();
            if (type == BsonType.END_OF_DOCUMENT) {
                break;
            }

            String fieldName = reader.readCString();
            Object value = handler.parseValue(reader, type);
            arrayDoc.put(fieldName, value);
        }

        // Convert to list (arrays use string indices "0", "1", "2"...)
        List<Object> array = new ArrayList<Object>();
        for (int i = 0; i < arrayDoc.size(); i++) {
            String key = String.valueOf(i);
            if (arrayDoc.containsKey(key)) {
                array.add(arrayDoc.get(key));
            } else {
                break;
            }
        }

        return array;
    }
}
