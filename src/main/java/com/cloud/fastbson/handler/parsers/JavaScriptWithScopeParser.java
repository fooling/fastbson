package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.handler.TypeHandler;
import com.cloud.fastbson.reader.BsonReader;
import com.cloud.fastbson.types.JavaScriptWithScope;

import java.util.Map;

/**
 * Parser for BSON JavaScriptWithScope type (0x0F).
 *
 * <p>Parses JavaScript code with an associated scope document.
 *
 * <p>Structure: int32 total_length + string code + document scope
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 */
public enum JavaScriptWithScopeParser implements BsonTypeParser {
    INSTANCE;

    private TypeHandler handler;

    /**
     * Sets the TypeHandler for parsing scope document.
     * Called by TypeHandler during initialization.
     */
    public void setHandler(TypeHandler handler) {
        this.handler = handler;
    }

    @Override
    public Object parse(BsonReader reader) {
        int totalLength = reader.readInt32();
        String code = reader.readString();
        Map<String, Object> scope = handler.parseDocument(reader);
        return new JavaScriptWithScope(code, scope);
    }
}

