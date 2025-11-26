package com.cloud.fastbson.types;

import com.cloud.fastbson.document.BsonDocument;

/**
 * Represents BSON JavaScript code with scope.
 *
 * <p>Structure: int32 total_length + string code + document scope
 */
public class JavaScriptWithScope {
    public final String code;
    public final BsonDocument scope;

    public JavaScriptWithScope(String code, BsonDocument scope) {
        this.code = code;
        this.scope = scope;
    }
}
