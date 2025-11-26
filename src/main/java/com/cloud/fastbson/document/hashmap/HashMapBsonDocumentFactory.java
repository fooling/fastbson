package com.cloud.fastbson.document.hashmap;

import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.document.BsonArrayBuilder;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.document.BsonDocumentBuilder;
import com.cloud.fastbson.document.BsonDocumentFactory;

/**
 * HashMap-based document factory (Phase 1 style).
 *
 * <p><b>Phase 1 Compatible - Eager Parsing with Boxing</b>
 *
 * <p>Characteristics:
 * <ul>
 *   <li>Uses HashMap&lt;String, Object&gt; for documents</li>
 *   <li>Uses ArrayList&lt;Object&gt; for arrays</li>
 *   <li>Boxing primitives (int → Integer, long → Long, etc.)</li>
 *   <li>Eager parsing: all values parsed immediately</li>
 *   <li>Zero external dependencies</li>
 * </ul>
 *
 * <p>Performance Characteristics:
 * <ul>
 *   <li><b>Full parsing</b>: ~3.88x faster than MongoDB BSON (Phase 1 verified)</li>
 *   <li><b>Access speed</b>: O(1) HashMap lookup, very fast</li>
 *   <li><b>Memory overhead</b>: ~40% more than FastBson due to boxing</li>
 *   <li><b>GC pressure</b>: Higher due to boxed objects</li>
 * </ul>
 *
 * <p>When to Use:
 * <ul>
 *   <li>Full document parsing scenarios (accessing most fields)</li>
 *   <li>When you need Phase 1 performance baseline (3.88x)</li>
 *   <li>When simplicity is more important than memory efficiency</li>
 *   <li>Legacy compatibility or migration scenarios</li>
 * </ul>
 *
 * <p>When NOT to Use:
 * <ul>
 *   <li>Partial field parsing (use {@code FastBsonDocumentFactory} instead)</li>
 *   <li>Memory-sensitive applications (40% more memory than FastBson)</li>
 *   <li>High-throughput scenarios with GC concerns</li>
 * </ul>
 *
 * <p>Usage Example:
 * <pre>{@code
 * // Enable HashMap mode for Phase 1 scenarios
 * TypeHandler.setDocumentFactory(HashMapBsonDocumentFactory.INSTANCE);
 *
 * // Parse BSON - uses HashMap internally
 * BsonDocument doc = DocumentParser.INSTANCE.parse(reader);
 * int age = doc.getInt32("age");  // Fast O(1) access, but value was boxed
 * }</pre>
 */
public final class HashMapBsonDocumentFactory implements BsonDocumentFactory {

    /**
     * Singleton instance.
     */
    public static final HashMapBsonDocumentFactory INSTANCE = new HashMapBsonDocumentFactory();

    private static final HashMapBsonDocument EMPTY_DOCUMENT = new HashMapBsonDocument();
    private static final HashMapBsonArray EMPTY_ARRAY = new HashMapBsonArray();

    private HashMapBsonDocumentFactory() {
        // Private constructor - use singleton
    }

    @Override
    public BsonDocumentBuilder newDocumentBuilder() {
        return new HashMapBsonDocumentBuilder();
    }

    @Override
    public BsonArrayBuilder newArrayBuilder() {
        return new HashMapBsonArrayBuilder();
    }

    @Override
    public BsonDocument emptyDocument() {
        return EMPTY_DOCUMENT;
    }

    @Override
    public BsonArray emptyArray() {
        return EMPTY_ARRAY;
    }

    @Override
    public String getName() {
        return "HashMap (Phase 1 compatible)";
    }

    @Override
    public boolean requiresExternalDependencies() {
        return false;  // No external dependencies
    }

    @Override
    public String toString() {
        return getName();
    }
}
