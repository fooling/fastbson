package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.document.BsonDocumentBuilder;
import com.cloud.fastbson.document.BsonDocumentFactory;
import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.handler.TypeHandler;
import com.cloud.fastbson.reader.BsonReader;
import com.cloud.fastbson.util.BsonType;
import com.cloud.fastbson.util.BsonUtils;
import com.cloud.fastbson.util.ObjectPool;

/**
 * Parser for BSON Document type (0x03).
 *
 * <p>Parses embedded BSON documents recursively using BsonDocumentBuilder for zero-boxing.
 *
 * <p>Document structure: int32 length + elements + 0x00 terminator
 * Each element: type byte + cstring field name + value
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 *
 * <p>Performance optimization:
 * <ul>
 *   <li>Primitive types (int32, int64, double, boolean) are stored without boxing</li>
 *   <li>Uses BsonDocumentFactory to create appropriate implementation (Fast or Simple)</li>
 * </ul>
 *
 * <p><b>Phase 2.15: Zero-Copy Support</b><br>
 * Added {@link #getValueSize(byte[], int)} for index building without parsing.
 * Future Phase 2.16+ will add {@code readView()} method returning BsonView for true zero-copy
 * nested document access.
 *
 * <p><b>Document Format:</b>
 * <ul>
 *   <li>4 bytes: int32 document length (includes the length field itself and terminator)</li>
 *   <li>variable: element list</li>
 *   <li>1 byte: 0x00 terminator</li>
 * </ul>
 */
public enum DocumentParser implements BsonTypeParser {
    INSTANCE;

    private TypeHandler handler;
    private BsonDocumentFactory factory;  // ✅ 工厂注入

    /**
     * Sets the TypeHandler for recursive parsing.
     * Called by TypeHandler during initialization.
     */
    public void setHandler(TypeHandler handler) {
        this.handler = handler;
    }

    /**
     * Sets the BsonDocumentFactory for creating documents.
     * Called by TypeHandler during initialization.
     */
    public void setFactory(BsonDocumentFactory factory) {
        this.factory = factory;
    }

    /**
     * Zero-copy API: Get document value size (variable length).
     *
     * <p>Reads the int32 length prefix which includes the entire document size
     * (length field + elements + terminator).
     *
     * <p>This enables fast skipping of unwanted nested documents during index building.
     *
     * @param data BSON data array
     * @param offset offset where document value starts (at the length field)
     * @return total document size in bytes (as specified in the length field)
     */
    @Override
    public int getValueSize(byte[] data, int offset) {
        // Read int32 document length (little-endian)
        return (data[offset] & 0xFF)
            | ((data[offset + 1] & 0xFF) << 8)
            | ((data[offset + 2] & 0xFF) << 16)
            | ((data[offset + 3] & 0xFF) << 24);
    }

    @Override
    public Object parse(BsonReader reader) {
        int docLength = reader.readInt32();
        int endPosition = reader.position() + docLength - 4;

        // ✅ Phase 1 优化：HashMap 模式使用直接解析（绕过Builder，性能提升50%）
        if (factory instanceof com.cloud.fastbson.document.hashmap.HashMapBsonDocumentFactory) {
            return parseDirectHashMap(reader, endPosition, docLength);
        }

        // ✅ Phase 2 优化：IndexedBsonDocument 零复制惰性解析（性能提升10-20x）
        if (factory instanceof com.cloud.fastbson.document.IndexedBsonDocumentFactory) {
            return parseZeroCopyIndexed(reader, docLength);
        }

        // 使用工厂创建Builder
        BsonDocumentBuilder builder = factory.newDocumentBuilder();

        // 估算容量（粗略估计：每个字段平均20字节）
        int estimatedFields = Math.max(4, docLength / 20);
        builder.estimateSize(estimatedFields);

        while (reader.position() < endPosition) {
            byte type = reader.readByte();
            if (type == BsonType.END_OF_DOCUMENT) {
                break;
            }

            String fieldName = reader.readCString();

            // ✅ 根据类型使用不同的put方法（无装箱）
            // Phase 3.3: 按类型频率排序，优化CPU分支预测（INT32 35%, STRING 30%, DOUBLE 15%, INT64 10%）
            switch (type) {
                case BsonType.INT32:
                    int intValue = reader.readInt32();
                    builder.putInt32(fieldName, intValue);  // ✅ 无装箱
                    break;

                case BsonType.STRING:
                case BsonType.JAVASCRIPT:
                case BsonType.SYMBOL:
                    String stringValue = reader.readString();
                    builder.putString(fieldName, stringValue);
                    break;

                case BsonType.DOUBLE:
                    double doubleValue = reader.readDouble();
                    builder.putDouble(fieldName, doubleValue);  // ✅ 无装箱
                    break;

                case BsonType.INT64:
                    long longValue = reader.readInt64();
                    builder.putInt64(fieldName, longValue);  // ✅ 无装箱
                    break;

                case BsonType.BOOLEAN:
                    boolean boolValue = reader.readByte() != 0;
                    builder.putBoolean(fieldName, boolValue);  // ✅ 无装箱
                    break;

                case BsonType.DOCUMENT:
                    // 递归解析嵌套文档
                    BsonDocument nestedDoc = (BsonDocument) parse(reader);
                    builder.putDocument(fieldName, nestedDoc);
                    break;

                case BsonType.ARRAY:
                    BsonArray array = (BsonArray) ArrayParser.INSTANCE.parse(reader);
                    builder.putArray(fieldName, array);
                    break;

                case BsonType.OBJECT_ID:
                    String objectId = BsonUtils.bytesToHex(reader.readBytes(12));
                    builder.putObjectId(fieldName, objectId);
                    break;

                case BsonType.DATE_TIME:
                    long timestamp = reader.readInt64();
                    builder.putDateTime(fieldName, timestamp);
                    break;

                case BsonType.NULL:
                    builder.putNull(fieldName);
                    break;

                case BsonType.BINARY:
                    int binLength = reader.readInt32();
                    byte subtype = reader.readByte();
                    byte[] data = reader.readBytes(binLength);
                    builder.putBinary(fieldName, subtype, data);
                    break;

                default:
                    // 其他复杂类型通过TypeHandler处理
                    Object value = handler.getParsedValue(reader, type);
                    builder.putComplex(fieldName, type, value);
                    break;
            }
        }

        return builder.build();
    }

    /**
     * Phase 1 优化：直接解析到 HashMap（模仿 0.0.1 实现）
     *
     * <p>性能优势：
     * <ul>
     *   <li>绕过 Builder 模式开销</li>
     *   <li>单次遍历直接构建 HashMap</li>
     *   <li>减少对象分配和方法调用</li>
     *   <li>性能提升 50-100%</li>
     * </ul>
     *
     * <p>Phase 3 优化：启发式容量估算，避免 rehash
     *
     * @param reader BSON reader
     * @param endPosition 文档结束位置
     * @param docLength 文档总长度（用于容量估算）
     * @return HashMap-based BsonDocument
     */
    private Object parseDirectHashMap(BsonReader reader, int endPosition, int docLength) {
        // Phase 1 优化：直接返回 HashMap，避免防御性复制
        // Phase 3 优化：根据文档长度估算容量，避免 rehash
        // 启发式：平均每字段 ~20 字节，负载因子 0.75
        int estimatedFields = Math.max(4, docLength / 20);
        int initialCapacity = (int)(estimatedFields / 0.75) + 1;

        java.util.Map<String, Object> data = new java.util.HashMap<String, Object>(initialCapacity);
        java.util.Map<String, Byte> types = new java.util.HashMap<String, Byte>(initialCapacity);

        // Parse fields until we hit the END_OF_DOCUMENT marker (0x00)
        // Well-formed BSON always has this terminator
        while (true) {
            // Safety check: prevent reading beyond document boundary
            if (reader.position() >= endPosition) {
                break;  // Malformed BSON without terminator
            }

            byte type = reader.readByte();
            if (type == BsonType.END_OF_DOCUMENT) {
                break;  // Normal termination
            }

            String fieldName = reader.readCString();
            Object value = parseValueDirect(reader, type);
            data.put(fieldName, value);
            types.put(fieldName, Byte.valueOf(type));  // Track type for each field
        }

        // 创建轻量级 HashMapBsonDocument（避免防御性复制）
        return com.cloud.fastbson.document.hashmap.HashMapBsonDocument.createDirect(data, types);
    }

    /**
     * 直接解析值（Phase 1 优化路径）
     * Phase 3.3: 按类型频率排序，优化CPU分支预测（INT32 35%, STRING 30%, DOUBLE 15%, INT64 10%）
     */
    private Object parseValueDirect(BsonReader reader, byte type) {
        switch (type) {
            case BsonType.INT32:
                return Integer.valueOf(reader.readInt32());

            case BsonType.STRING:
            case BsonType.JAVASCRIPT:
            case BsonType.SYMBOL:
                return reader.readString();

            case BsonType.DOUBLE:
                return Double.valueOf(reader.readDouble());

            case BsonType.INT64:
                return Long.valueOf(reader.readInt64());

            case BsonType.BOOLEAN:
                return Boolean.valueOf(reader.readByte() != 0);

            case BsonType.DOCUMENT:
                return parse(reader);  // 递归使用相同优化路径

            case BsonType.ARRAY:
                return ArrayParser.INSTANCE.parse(reader);

            case BsonType.OBJECT_ID:
                return BsonUtils.bytesToHex(reader.readBytes(12));

            case BsonType.DATE_TIME:
                return Long.valueOf(reader.readInt64());

            case BsonType.NULL:
                return null;

            case BsonType.BINARY:
                int binLength = reader.readInt32();
                reader.readByte();  // Skip subtype
                return reader.readBytes(binLength);

            case BsonType.REGEX:
                String pattern = reader.readCString();
                String options = reader.readCString();
                return pattern + "/" + options;

            case BsonType.DB_POINTER:
                String namespace = reader.readString();
                byte[] objectId = reader.readBytes(12);
                return new Object[]{namespace, BsonUtils.bytesToHex(objectId)};

            case BsonType.JAVASCRIPT_WITH_SCOPE:
                int codeWithScopeLength = reader.readInt32();
                String code = reader.readString();
                Object scope = parse(reader);
                return new Object[]{code, scope};

            case BsonType.TIMESTAMP:
                return Long.valueOf(reader.readInt64());

            case BsonType.DECIMAL128:
                return reader.readBytes(16);

            case BsonType.MIN_KEY:
                return "MinKey";

            case BsonType.MAX_KEY:
                return "MaxKey";

            case BsonType.UNDEFINED:
                return null;  // Undefined type (deprecated) - treat as null

            default:
                throw new com.cloud.fastbson.exception.InvalidBsonTypeException(type);
        }
    }

    /**
     * Phase 2 优化：零复制惰性解析（IndexedBsonDocument）
     *
     * <p>性能优势：
     * <ul>
     *   <li>零复制：直接操作原始 byte[]，不复制数据</li>
     *   <li>惰性解析：只构建字段索引，不解析值</li>
     *   <li>按需访问：只有访问字段时才解析该字段</li>
     *   <li>早退机制：访问部分字段时只解析需要的字段</li>
     *   <li>性能提升 10-20x（部分字段访问场景）</li>
     * </ul>
     *
     * @param reader BSON reader
     * @param docLength 文档总长度（包含长度字段本身）
     * @return IndexedBsonDocument（零复制惰性解析）
     */
    private Object parseZeroCopyIndexed(BsonReader reader, int docLength) {
        // 获取底层 byte[] 和当前偏移（零复制关键）
        byte[] buffer = reader.getBuffer();
        int offset = reader.position() - 4;  // -4 because we already read the length

        // 直接调用 IndexedBsonDocument.parse（零复制惰性解析）
        com.cloud.fastbson.document.IndexedBsonDocument doc =
            com.cloud.fastbson.document.IndexedBsonDocument.parse(buffer, offset, docLength);

        // 跳过文档剩余部分（reader 位置需要更新）
        reader.position(offset + docLength);

        return doc;
    }
}
