package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.document.BsonArrayBuilder;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.document.BsonDocumentFactory;
import com.cloud.fastbson.handler.BsonTypeParser;
import com.cloud.fastbson.handler.TypeHandler;
import com.cloud.fastbson.reader.BsonReader;
import com.cloud.fastbson.util.BsonType;
import com.cloud.fastbson.util.BsonUtils;

/**
 * Parser for BSON Array type (0x04).
 *
 * <p>Parses BSON arrays using BsonArrayBuilder for zero-boxing. Arrays are stored as documents
 * with string indices ("0", "1", "2", etc.) as field names.
 *
 * <p>Uses enum singleton pattern for optimal performance and thread safety.
 *
 * <p>Performance optimization:
 * <ul>
 *   <li>Primitive types (int32, int64, double, boolean) are stored without boxing</li>
 *   <li>Uses BsonDocumentFactory to create appropriate implementation (Fast or Simple)</li>
 * </ul>
 */
public enum ArrayParser implements BsonTypeParser {
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
     * Sets the BsonDocumentFactory for creating arrays.
     * Called by TypeHandler during initialization.
     */
    public void setFactory(BsonDocumentFactory factory) {
        this.factory = factory;
    }

    @Override
    public Object parse(BsonReader reader) {
        int docLength = reader.readInt32();
        int endPosition = reader.position() + docLength - 4;

        // 使用工厂创建ArrayBuilder
        BsonArrayBuilder builder = factory.newArrayBuilder();

        // 估算大小
        int estimatedSize = Math.max(4, docLength / 15);
        builder.estimateSize(estimatedSize);

        while (reader.position() < endPosition) {
            byte type = reader.readByte();
            if (type == BsonType.END_OF_DOCUMENT) {
                break;
            }

            // 跳过字段名（数组索引 "0", "1", "2"...）
            reader.readCString();

            // ✅ 根据类型使用不同的add方法（无装箱）
            switch (type) {
                case BsonType.INT32:
                    int intValue = reader.readInt32();
                    builder.addInt32(intValue);  // ✅ 无装箱
                    break;

                case BsonType.INT64:
                    long longValue = reader.readInt64();
                    builder.addInt64(longValue);  // ✅ 无装箱
                    break;

                case BsonType.DOUBLE:
                    double doubleValue = reader.readDouble();
                    builder.addDouble(doubleValue);  // ✅ 无装箱
                    break;

                case BsonType.BOOLEAN:
                    boolean boolValue = reader.readByte() != 0;
                    builder.addBoolean(boolValue);  // ✅ 无装箱
                    break;

                case BsonType.STRING:
                case BsonType.JAVASCRIPT:
                case BsonType.SYMBOL:
                    String stringValue = reader.readString();
                    builder.addString(stringValue);
                    break;

                case BsonType.DOCUMENT:
                    BsonDocument nestedDoc = (BsonDocument) DocumentParser.INSTANCE.parse(reader);
                    builder.addDocument(nestedDoc);
                    break;

                case BsonType.ARRAY:
                    BsonArray nestedArray = (BsonArray) parse(reader);
                    builder.addArray(nestedArray);
                    break;

                case BsonType.OBJECT_ID:
                    String objectId = BsonUtils.bytesToHex(reader.readBytes(12));
                    builder.addObjectId(objectId);
                    break;

                case BsonType.DATE_TIME:
                    long timestamp = reader.readInt64();
                    builder.addDateTime(timestamp);
                    break;

                case BsonType.NULL:
                    builder.addNull();
                    break;

                case BsonType.BINARY:
                    int binLength = reader.readInt32();
                    byte subtype = reader.readByte();
                    byte[] data = reader.readBytes(binLength);
                    builder.addBinary(subtype, data);
                    break;

                default:
                    // 其他复杂类型通过TypeHandler处理
                    Object value = handler.parseValue(reader, type);
                    // TODO: 需要根据类型添加到builder
                    // 暂时跳过不支持的类型
                    break;
            }
        }

        return builder.build();
    }
}
