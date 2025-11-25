package com.cloud.fastbson.document.simple;

import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.document.BsonDocumentBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple实现的文档构建器
 *
 * <p>使用HashMap存储SimpleBsonValue，在build()时创建不可变的SimpleBsonDocument
 */
public final class SimpleBsonDocumentBuilder implements BsonDocumentBuilder {

    private Map<String, SimpleBsonValue> fields;
    private int estimatedSize = 16;

    public SimpleBsonDocumentBuilder() {
        fields = new HashMap<String, SimpleBsonValue>(estimatedSize);
    }

    @Override
    public BsonDocumentBuilder estimateSize(int fieldCount) {
        this.estimatedSize = (int) (fieldCount / 0.75) + 1;
        if (fields.isEmpty()) {
            fields = new HashMap<String, SimpleBsonValue>(estimatedSize);
        }
        return this;
    }

    // ==================== Primitive添加（无装箱）====================

    @Override
    public BsonDocumentBuilder putInt32(String fieldName, int value) {
        fields.put(fieldName, SimpleBsonValue.ofInt32(value));  // ✅ 缓存优化
        return this;
    }

    @Override
    public BsonDocumentBuilder putInt64(String fieldName, long value) {
        fields.put(fieldName, SimpleBsonValue.ofInt64(value));
        return this;
    }

    @Override
    public BsonDocumentBuilder putDouble(String fieldName, double value) {
        fields.put(fieldName, SimpleBsonValue.ofDouble(value));
        return this;
    }

    @Override
    public BsonDocumentBuilder putBoolean(String fieldName, boolean value) {
        fields.put(fieldName, SimpleBsonValue.ofBoolean(value));  // ✅ 单例
        return this;
    }

    // ==================== 引用类型添加 ====================

    @Override
    public BsonDocumentBuilder putString(String fieldName, String value) {
        fields.put(fieldName, SimpleBsonValue.ofString(value));
        return this;
    }

    @Override
    public BsonDocumentBuilder putDocument(String fieldName, BsonDocument value) {
        fields.put(fieldName, SimpleBsonValue.ofDocument(value));
        return this;
    }

    @Override
    public BsonDocumentBuilder putArray(String fieldName, BsonArray value) {
        fields.put(fieldName, SimpleBsonValue.ofArray(value));
        return this;
    }

    @Override
    public BsonDocumentBuilder putObjectId(String fieldName, String hexString) {
        fields.put(fieldName, SimpleBsonValue.ofObjectId(hexString));
        return this;
    }

    @Override
    public BsonDocumentBuilder putDateTime(String fieldName, long timestamp) {
        fields.put(fieldName, SimpleBsonValue.ofDateTime(timestamp));
        return this;
    }

    @Override
    public BsonDocumentBuilder putNull(String fieldName) {
        fields.put(fieldName, SimpleBsonValue.ofNull());
        return this;
    }

    @Override
    public BsonDocumentBuilder putBinary(String fieldName, byte subtype, byte[] data) {
        fields.put(fieldName, SimpleBsonValue.ofBinary(subtype, data));
        return this;
    }

    @Override
    public BsonDocumentBuilder putComplex(String fieldName, byte type, Object value) {
        fields.put(fieldName, SimpleBsonValue.ofComplex(type, value));
        return this;
    }

    // ==================== 构建 ====================

    @Override
    public BsonDocument build() {
        if (fields == null) {
            throw new IllegalStateException("Builder has already been used");
        }
        BsonDocument doc = new SimpleBsonDocument(fields);
        fields = null;  // 释放引用，Builder失效
        return doc;
    }

    @Override
    public void reset() {
        if (fields == null) {
            fields = new HashMap<String, SimpleBsonValue>(estimatedSize);
        } else {
            fields.clear();
        }
    }
}
