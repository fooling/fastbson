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
            switch (type) {
                case BsonType.INT32:
                    int intValue = reader.readInt32();
                    builder.putInt32(fieldName, intValue);  // ✅ 无装箱
                    break;

                case BsonType.INT64:
                    long longValue = reader.readInt64();
                    builder.putInt64(fieldName, longValue);  // ✅ 无装箱
                    break;

                case BsonType.DOUBLE:
                    double doubleValue = reader.readDouble();
                    builder.putDouble(fieldName, doubleValue);  // ✅ 无装箱
                    break;

                case BsonType.BOOLEAN:
                    boolean boolValue = reader.readByte() != 0;
                    builder.putBoolean(fieldName, boolValue);  // ✅ 无装箱
                    break;

                case BsonType.STRING:
                case BsonType.JAVASCRIPT:
                case BsonType.SYMBOL:
                    String stringValue = reader.readString();
                    builder.putString(fieldName, stringValue);
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
                    Object value = handler.parseValue(reader, type);
                    builder.putComplex(fieldName, type, value);
                    break;
            }
        }

        return builder.build();
    }
}
