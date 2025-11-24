package com.cloud.fastbson.benchmark;

import org.bson.BsonBinaryWriter;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonString;
import org.bson.BsonDouble;
import org.bson.BsonBoolean;
import org.bson.io.BasicOutputBuffer;

/**
 * BSON 测试数据生成器
 *
 * 使用 org.mongodb.bson 生成标准的 BSON 测试数据
 */
public class BsonTestDataGenerator {

    /**
     * 生成包含指定字段数量的 BSON 文档
     *
     * @param fieldCount 字段数量
     * @return BSON 字节数组
     */
    public static byte[] generateDocument(int fieldCount) {
        BsonDocument doc = new BsonDocument();

        for (int i = 0; i < fieldCount; i++) {
            String fieldName = "field" + i;

            // 混合不同类型的字段
            switch (i % 5) {
                case 0:
                    doc.put(fieldName, new BsonInt32(i * 100));
                    break;
                case 1:
                    doc.put(fieldName, new BsonString("value_" + i));
                    break;
                case 2:
                    doc.put(fieldName, new BsonDouble(i * 3.14));
                    break;
                case 3:
                    doc.put(fieldName, new BsonBoolean(i % 2 == 0));
                    break;
                case 4:
                    doc.put(fieldName, new BsonInt64((long) i * 1000000));
                    break;
            }
        }

        // 序列化为字节数组
        BasicOutputBuffer buffer = new BasicOutputBuffer();
        BsonBinaryWriter writer = new BsonBinaryWriter(buffer);
        new org.bson.codecs.BsonDocumentCodec().encode(writer, doc,
            org.bson.codecs.EncoderContext.builder().build());

        return buffer.toByteArray();
    }

    /**
     * 生成嵌套文档
     *
     * @param fieldCount 字段数量
     * @param nestedLevel 嵌套层级
     * @return BSON 字节数组
     */
    public static byte[] generateNestedDocument(int fieldCount, int nestedLevel) {
        BsonDocument doc = new BsonDocument();

        for (int i = 0; i < fieldCount; i++) {
            if (i % 10 == 0 && nestedLevel > 0) {
                // 创建嵌套文档
                BsonDocument nested = new BsonDocument();
                nested.put("nested_field1", new BsonString("nested_value"));
                nested.put("nested_field2", new BsonInt32(i));
                doc.put("nested" + i, nested);
            } else {
                doc.put("field" + i, new BsonString("value_" + i));
            }
        }

        BasicOutputBuffer buffer = new BasicOutputBuffer();
        BsonBinaryWriter writer = new BsonBinaryWriter(buffer);
        new org.bson.codecs.BsonDocumentCodec().encode(writer, doc,
            org.bson.codecs.EncoderContext.builder().build());

        return buffer.toByteArray();
    }

    /**
     * 生成包含数组的文档
     */
    public static byte[] generateDocumentWithArray(int fieldCount, int arraySize) {
        BsonDocument doc = new BsonDocument();

        for (int i = 0; i < fieldCount; i++) {
            if (i % 5 == 0) {
                // 创建数组
                org.bson.BsonArray array = new org.bson.BsonArray();
                for (int j = 0; j < arraySize; j++) {
                    array.add(new BsonInt32(j));
                }
                doc.put("array" + i, array);
            } else {
                doc.put("field" + i, new BsonString("value_" + i));
            }
        }

        BasicOutputBuffer buffer = new BasicOutputBuffer();
        BsonBinaryWriter writer = new BsonBinaryWriter(buffer);
        new org.bson.codecs.BsonDocumentCodec().encode(writer, doc,
            org.bson.codecs.EncoderContext.builder().build());

        return buffer.toByteArray();
    }
}
