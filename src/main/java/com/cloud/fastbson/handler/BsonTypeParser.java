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
 * <p><b>Phase 2.15 Enhancement: Zero-Copy Support</b><br>
 * Added new methods for zero-copy parsing:
 * <ul>
 *   <li>{@link #getValueSize(byte[], int)}: Get value size without parsing (for index building)</li>
 *   <li>Static readDirect() methods in implementations: Direct byte array access</li>
 * </ul>
 *
 * @see com.cloud.fastbson.handler.parsers
 */
@FunctionalInterface
public interface BsonTypeParser {
    /**
     * Parses a BSON value from the reader (legacy API).
     *
     * <p>This method creates boxed objects and may allocate memory.
     * For zero-copy parsing, use the static readDirect() methods
     * in specific parser implementations.
     *
     * @param reader the BsonReader positioned at the value
     * @return the parsed value (may be null for NULL/UNDEFINED types)
     */
    Object parse(BsonReader reader);

    /**
     * Gets the size of the value in bytes without parsing it (zero-copy).
     *
     * <p>This method is used for:
     * <ul>
     *   <li>Building field indexes during document parsing</li>
     *   <li>Skipping unwanted fields efficiently</li>
     *   <li>Calculating document offsets</li>
     * </ul>
     *
     * <p>Default implementation throws UnsupportedOperationException.
     * Parsers should override this for zero-copy support.
     *
     * @param data BSON data byte array
     * @param offset offset where the value starts
     * @return size of the value in bytes
     * @throws UnsupportedOperationException if not implemented
     */
    default int getValueSize(byte[] data, int offset) {
        throw new UnsupportedOperationException(
            getClass().getSimpleName() + " does not support getValueSize() yet");
    }
}
