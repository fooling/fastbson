package com.cloud.fastbson;

import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.document.BsonDocumentFactory;
import com.cloud.fastbson.document.fast.FastBsonDocumentFactory;
import com.cloud.fastbson.document.simple.SimpleBsonDocumentFactory;
import com.cloud.fastbson.handler.parsers.DocumentParser;
import com.cloud.fastbson.reader.BsonReader;

/**
 * FastBson - Main entry point for BSON parsing with zero-boxing architecture.
 *
 * <p>This class provides a clean API for parsing BSON documents using the
 * zero-boxing architecture (Phase 2.13+). It uses the factory pattern to
 * create either Fast (fastutil-based) or Simple (zero-dependency) implementations.
 *
 * <p>Usage:
 * <pre>{@code
 * // Parse BSON byte array to BsonDocument (zero-boxing, high performance)
 * byte[] bsonData = ...;
 * BsonDocument doc = FastBson.parse(bsonData);
 *
 * // Access fields with zero-boxing
 * int value = doc.getInt32("fieldName");
 * String str = doc.getString("fieldName");
 *
 * // Or convert to legacy Map if needed (requires boxing)
 * Map<String, Object> map = doc.toLegacyMap();
 * }</pre>
 *
 * <p>Performance:
 * <ul>
 *   <li>Zero-boxing for primitive types (int32, int64, double, boolean)</li>
 *   <li>20-50% faster than boxing-based parsers for numeric-heavy documents</li>
 *   <li>Uses fastutil for optimal memory layout and cache performance</li>
 * </ul>
 *
 * @since 1.0.0 (Phase 2.13+)
 */
public class FastBson {

    /**
     * Default factory (FastBsonDocumentFactory - fastutil-based, zero-boxing).
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
     * Parses BSON byte array to BsonDocument using zero-boxing architecture.
     *
     * @param bsonData BSON byte array
     * @return BsonDocument (zero-boxing)
     */
    public static BsonDocument parse(byte[] bsonData) {
        BsonReader reader = new BsonReader(bsonData);
        return (BsonDocument) DocumentParser.INSTANCE.parse(reader);
    }

    /**
     * Parses BSON from BsonReader to BsonDocument using zero-boxing architecture.
     *
     * @param reader BsonReader
     * @return BsonDocument (zero-boxing)
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
