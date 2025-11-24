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

    /**
     * 生成 String 密集型文档（80% String 字段）
     *
     * @param fieldCount 字段数量
     * @return BSON 字节数组
     */
    public static byte[] generateStringHeavyDocument(int fieldCount) {
        BsonDocument doc = new BsonDocument();

        for (int i = 0; i < fieldCount; i++) {
            String fieldName = "field" + i;

            // 80% String, 20% 其他类型
            if (i % 5 == 0) {
                // 20% 其他类型
                doc.put(fieldName, new BsonInt32(i));
            } else {
                // 80% String 类型
                doc.put(fieldName, new BsonString("string_value_" + i + "_abcdefghijklmnopqrstuvwxyz"));
            }
        }

        return serializeDocument(doc);
    }

    /**
     * 生成纯 String 文档（100% String 字段）
     *
     * @param fieldCount 字段数量
     * @return BSON 字节数组
     */
    public static byte[] generatePureStringDocument(int fieldCount) {
        BsonDocument doc = new BsonDocument();

        for (int i = 0; i < fieldCount; i++) {
            String fieldName = "field" + i;
            doc.put(fieldName, new BsonString("string_value_" + i + "_0123456789abcdefghijklmnopqrstuvwxyz"));
        }

        return serializeDocument(doc);
    }

    /**
     * 生成 Int32/Int64 密集型文档（100% 数值类型）
     *
     * @param fieldCount 字段数量
     * @return BSON 字节数组
     */
    public static byte[] generateNumericHeavyDocument(int fieldCount) {
        BsonDocument doc = new BsonDocument();

        for (int i = 0; i < fieldCount; i++) {
            String fieldName = "field" + i;

            // 50% Int32, 50% Int64
            if (i % 2 == 0) {
                doc.put(fieldName, new BsonInt32(i * 1000));
            } else {
                doc.put(fieldName, new BsonInt64((long) i * 1000000000L));
            }
        }

        return serializeDocument(doc);
    }

    /**
     * 生成数组密集型文档（大量数组字段）
     *
     * @param arrayCount 数组数量
     * @param arraySize 每个数组的大小
     * @return BSON 字节数组
     */
    public static byte[] generateArrayHeavyDocument(int arrayCount, int arraySize) {
        BsonDocument doc = new BsonDocument();

        for (int i = 0; i < arrayCount; i++) {
            org.bson.BsonArray array = new org.bson.BsonArray();
            for (int j = 0; j < arraySize; j++) {
                // 数组中混合不同类型
                switch (j % 3) {
                    case 0:
                        array.add(new BsonInt32(j));
                        break;
                    case 1:
                        array.add(new BsonString("item_" + j));
                        break;
                    case 2:
                        array.add(new BsonDouble(j * 1.5));
                        break;
                }
            }
            doc.put("array" + i, array);
        }

        return serializeDocument(doc);
    }

    /**
     * 生成指定大小的文档（通过调整字段数量和字符串长度）
     *
     * @param targetSizeKB 目标大小（KB）
     * @return BSON 字节数组
     */
    public static byte[] generateDocumentBySize(int targetSizeKB) {
        BsonDocument doc = new BsonDocument();
        int targetSizeBytes = targetSizeKB * 1024;
        int currentSize = 0;
        int fieldIndex = 0;

        // 使用较长的字符串填充到目标大小
        String longString = repeatString("abcdefghijklmnopqrstuvwxyz0123456789", 50); // ~1800 字符

        while (currentSize < targetSizeBytes) {
            String fieldName = "field" + fieldIndex;

            // 每 10 个字段，添加一个长字符串字段
            if (fieldIndex % 10 == 0) {
                doc.put(fieldName, new BsonString(longString));
                currentSize += longString.length() + fieldName.length() + 10; // 粗略估计
            } else {
                // 其他字段使用短字符串或数值
                if (fieldIndex % 2 == 0) {
                    doc.put(fieldName, new BsonString("value_" + fieldIndex));
                    currentSize += 20;
                } else {
                    doc.put(fieldName, new BsonInt32(fieldIndex));
                    currentSize += fieldName.length() + 10;
                }
            }

            fieldIndex++;

            // 定期检查实际大小，避免死循环
            if (fieldIndex % 100 == 0) {
                byte[] testData = serializeDocument(doc);
                currentSize = testData.length;
                if (currentSize >= targetSizeBytes) {
                    break;
                }
            }
        }

        return serializeDocument(doc);
    }

    /**
     * 生成 100KB 文档
     */
    public static byte[] generate100KBDocument() {
        return generateDocumentBySize(100);
    }

    /**
     * 生成 1MB 文档
     */
    public static byte[] generate1MBDocument() {
        return generateDocumentBySize(1024);
    }

    /**
     * 重复字符串
     */
    private static String repeatString(String str, int times) {
        StringBuilder sb = new StringBuilder(str.length() * times);
        for (int i = 0; i < times; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * 序列化 BsonDocument 为字节数组
     */
    private static byte[] serializeDocument(BsonDocument doc) {
        BasicOutputBuffer buffer = new BasicOutputBuffer();
        BsonBinaryWriter writer = new BsonBinaryWriter(buffer);
        new org.bson.codecs.BsonDocumentCodec().encode(writer, doc,
            org.bson.codecs.EncoderContext.builder().build());
        return buffer.toByteArray();
    }
}
