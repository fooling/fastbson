package com.cloud.fastbson.document;

/**
 * Factory for creating IndexedBsonDocument (Phase 2 zero-copy lazy parsing).
 *
 * <p><b>Phase 2 - Zero-Copy Lazy Parsing</b>
 *
 * <p>Characteristics:
 * <ul>
 *   <li>Zero-copy: operates directly on原始 byte[]，不复制数据</li>
 *   <li>Lazy parsing: parse阶段只构建field index，不解析值</li>
 *   <li>On-demand: 只有访问字段时才解析该字段的值</li>
 *   <li>Early exit: 访问部分字段时，只解析需要的字段</li>
 * </ul>
 *
 * <p>Performance (Phase 2 目标):
 * <ul>
 *   <li><b>Parse phase</b>: ~30ms for 50-field document (only build index)</li>
 *   <li><b>Partial access (5/100 fields)</b>: 10-20x faster than full parsing</li>
 *   <li><b>Memory</b>: ~20-30 bytes per field (vs ~200 for eager parsing)</li>
 * </ul>
 *
 * <p>Best for:
 * <ul>
 *   <li>Partial field access scenarios (extracting 5-10 fields from 100+ field documents)</li>
 *   <li>Memory-sensitive applications</li>
 *   <li>Streaming/pipeline processing</li>
 * </ul>
 *
 * <p>NOT for:
 * <ul>
 *   <li>Full document access (use {@code HashMapBsonDocumentFactory} instead)</li>
 *   <li>Documents accessed multiple times (lazy parsing overhead on each get)</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * // Enable Phase 2 mode (lazy parsing + zero-copy)
 * FastBson.useIndexedFactory();
 *
 * // Parse BSON - only builds field index, no value parsing
 * BsonDocument doc = DocumentParser.INSTANCE.parse(reader);
 *
 * // Access specific fields - only these 2 fields are parsed
 * String name = doc.getString("name");
 * int age = doc.getInt32("age");
 * // The other 98 fields in a 100-field document are never parsed!
 * }</pre>
 */
public final class IndexedBsonDocumentFactory implements BsonDocumentFactory {

    /**
     * Singleton instance.
     */
    public static final IndexedBsonDocumentFactory INSTANCE = new IndexedBsonDocumentFactory();

    private static final byte[] EMPTY_BSON_BYTES = new byte[]{5, 0, 0, 0, 0};
    private static final IndexedBsonDocument EMPTY_DOCUMENT = IndexedBsonDocument.parse(EMPTY_BSON_BYTES, 0, 5);
    private static final IndexedBsonArray EMPTY_ARRAY = IndexedBsonArray.parse(EMPTY_BSON_BYTES, 0, 5);

    private IndexedBsonDocumentFactory() {
        // Private constructor - use singleton
    }

    @Override
    public BsonDocumentBuilder newDocumentBuilder() {
        throw new UnsupportedOperationException(
            "IndexedBsonDocument uses zero-copy parsing, not builder pattern. " +
            "Use IndexedBsonDocument.parse(byte[]) directly."
        );
    }

    @Override
    public BsonArrayBuilder newArrayBuilder() {
        throw new UnsupportedOperationException(
            "IndexedBsonArray uses zero-copy parsing, not builder pattern. " +
            "Use IndexedBsonArray.parse(byte[]) directly."
        );
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
        return "Indexed (Zero-copy lazy parsing)";
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
