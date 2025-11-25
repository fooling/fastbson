package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.handler.TypeHandler;
import com.cloud.fastbson.reader.BsonReader;

/**
 * Parser for BSON Regex type (0x0B).
 *
 * <p>Parses regular expression: cstring pattern + cstring options.
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 */
public enum RegexParser implements BsonTypeParser {
    INSTANCE;

    @Override
    public Object parse(BsonReader reader) {
        String pattern = reader.readCString();
        String options = reader.readCString();
        return new TypeHandler.RegexValue(pattern, options);
    }
}
