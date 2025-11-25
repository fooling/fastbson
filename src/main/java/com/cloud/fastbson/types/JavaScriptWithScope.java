package com.cloud.fastbson.types;

import java.util.Map;

/**
 * Represents BSON JavaScript code with scope.
 *
 * <p>Structure: int32 total_length + string code + document scope
 */
public class JavaScriptWithScope {
    public final String code;
    public final Map<String, Object> scope;

    public JavaScriptWithScope(String code, Map<String, Object> scope) {
        this.code = code;
        this.scope = scope;
    }
}
