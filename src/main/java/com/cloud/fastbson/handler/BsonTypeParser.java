package com.cloud.fastbson.handler;

import com.cloud.fastbson.reader.BsonReader;

/**
 * Strategy interface for BSON type parsing.
 *
 * <p>Each BSON type has its own implementation of this interface,
 * allowing for modular and maintainable type-specific parsing logic.
 *
 * <p>Implementations should use singleton pattern (enum or static instance)
 * to reduce GC pressure and improve performance.
 *
 * @see com.cloud.fastbson.handler.parsers
 */
@FunctionalInterface
public interface BsonTypeParser {
    /**
     * Parses a BSON value from the reader.
     *
     * @param reader the BsonReader positioned at the value
     * @return the parsed value (may be null for NULL/UNDEFINED types)
     */
    Object parse(BsonReader reader);
}
