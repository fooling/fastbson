package com.cloud.fastbson;

import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.document.BsonDocumentFactory;
import com.cloud.fastbson.document.IndexedBsonDocument;
import com.cloud.fastbson.document.fast.FastBsonDocumentFactory;
import com.cloud.fastbson.document.simple.SimpleBsonDocumentFactory;
import com.cloud.fastbson.handler.parsers.DocumentParser;
import com.cloud.fastbson.reader.BsonReader;

/**
 * FastBson - Main entry point for BSON parsing with zero-copy architecture.
 *
 * <p><b>Phase 2.16: True Zero-Copy Implementation</b>
 *
 * <p>This class uses IndexedBsonDocument for true zero-copy parsing:
 * <ul>
 *   <li><b>Index-based</b>: Builds field index during parse, no value parsing</li>
 *   <li><b>Lazy parsing</b>: Only parses fields when accessed</li>
 *   <li><b>Zero copy</b>: Works directly on byte array, no data duplication</li>
 *   <li><b>JVM-friendly</b>: Leverages escape analysis, boxing cache, inline caching</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * // Parse BSON byte array to IndexedBsonDocument (zero-copy, high performance)
 * byte[] bsonData = ...;
 * BsonDocument doc = FastBson.parse(bsonData);
 *
 * // Access fields with lazy parsing and caching
 * int value = doc.getInt32("fieldName");      // Parses on first access, cached after
 * String str = doc.getString("fieldName");    // Zero-copy until toString()
 * BsonDocument nested = doc.getDocument("nested");  // Zero-copy child view
 * }</pre>
 *
 * <p>Performance (Phase 2.16 target):
 * <ul>
 *   <li>Parse: ~100ms for 50-field document (on par with Phase 1: 99ms)</li>
 *   <li>Access: ~20-50ns per field (vs ~10ns for HashMap)</li>
 *   <li>Memory: ~30 bytes per field (vs ~50 for HashMap, ~200 for FastBsonDocument)</li>
 * </ul>
 *
 * @since 1.0.0 (Phase 2.16+)
 */
public class FastBson {

    /**
     * Default factory (FastBsonDocumentFactory - for backward compatibility).
     * Note: Phase 2.16+ uses IndexedBsonDocument directly, bypassing factory.
     */
    private static BsonDocumentFactory defaultFactory = FastBsonDocumentFactory.INSTANCE;

    static {
        // Ensure TypeHandler is loaded and parsers are initialized
        // This will set the factory in DocumentParser
        try {
            Class.forName("com.cloud.fastbson.handler.TypeHandler");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load TypeHandler", e);
        }
    }

    /**
     * Parses BSON byte array to IndexedBsonDocument (zero-copy, Phase 2.16+).
     *
     * <p>This method uses the new zero-copy architecture:
     * <ul>
     *   <li>Builds field index only (~30ms for 50 fields)</li>
     *   <li>No value parsing during parse phase</li>
     *   <li>Values parsed lazily on access</li>
     * </ul>
     *
     * @param bsonData BSON byte array
     * @return IndexedBsonDocument (zero-copy)
     */
    public static BsonDocument parse(byte[] bsonData) {
        // Phase 2.16: Use IndexedBsonDocument directly (zero-copy)
        return IndexedBsonDocument.parse(bsonData);
    }

    /**
     * Parses BSON from BsonReader to BsonDocument.
     *
     * <p>Note: This method still uses DocumentParser for backward compatibility.
     * To use zero-copy IndexedBsonDocument, use {@link #parse(byte[])} instead.
     *
     * @param reader BsonReader
     * @return BsonDocument (uses DocumentParser, not zero-copy)
     */
    public static BsonDocument parse(BsonReader reader) {
        return (BsonDocument) DocumentParser.INSTANCE.parse(reader);
    }

    /**
     * Sets the document factory to use for creating BsonDocument instances.
     *
     * <p>By default, FastBsonDocumentFactory is used (fastutil-based, zero-boxing).
     * You can switch to SimpleBsonDocumentFactory (zero-dependency) if needed.
     *
     * @param factory the BsonDocumentFactory to use
     */
    public static void setDocumentFactory(BsonDocumentFactory factory) {
        defaultFactory = factory;
        // Update factory in DocumentParser and ArrayParser
        DocumentParser.INSTANCE.setFactory(factory);
        // Note: ArrayParser will be updated by TypeHandler.setDocumentFactory()
    }

    /**
     * Gets the current document factory.
     *
     * @return current factory
     */
    public static BsonDocumentFactory getDocumentFactory() {
        return defaultFactory;
    }

    /**
     * Uses FastBsonDocumentFactory (fastutil-based, zero-boxing).
     * This is the default and recommended factory.
     */
    public static void useFastFactory() {
        setDocumentFactory(FastBsonDocumentFactory.INSTANCE);
    }

    /**
     * Uses SimpleBsonDocumentFactory (zero-dependency, boxing-based).
     * Use this if you don't want to depend on fastutil.
     */
    public static void useSimpleFactory() {
        setDocumentFactory(SimpleBsonDocumentFactory.INSTANCE);
    }

    // Private constructor to prevent instantiation
    private FastBson() {
        throw new AssertionError("FastBson is a utility class and should not be instantiated");
    }
}
