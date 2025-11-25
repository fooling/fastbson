package com.cloud.fastbson.types;

/**
 * Represents a BSON regular expression.
 *
 * <p>Structure: cstring pattern + cstring options
 */
public class RegexValue {
    public final String pattern;
    public final String options;

    public RegexValue(String pattern, String options) {
        this.pattern = pattern;
        this.options = options;
    }
}
