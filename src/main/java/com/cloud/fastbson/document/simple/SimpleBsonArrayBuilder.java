package com.cloud.fastbson.document.simple;

import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.document.BsonArrayBuilder;
import com.cloud.fastbson.document.BsonDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple实现的数组构建器
 *
 * <p>使用ArrayList存储SimpleBsonValue，在build()时创建不可变的SimpleBsonArray
 */
public final class SimpleBsonArrayBuilder implements BsonArrayBuilder {

    private List<SimpleBsonValue> elements;
    private int estimatedSize = 16;

    public SimpleBsonArrayBuilder() {
        elements = new ArrayList<SimpleBsonValue>(estimatedSize);
    }

    @Override
    public BsonArrayBuilder estimateSize(int size) {
        this.estimatedSize = size;
        if (elements.isEmpty()) {
            elements = new ArrayList<SimpleBsonValue>(estimatedSize);
        }
        return this;
    }

    // ==================== Primitive添加（无装箱）====================

    @Override
    public BsonArrayBuilder addInt32(int value) {
        elements.add(SimpleBsonValue.ofInt32(value));  // ✅ 缓存优化
        return this;
    }

    @Override
    public BsonArrayBuilder addInt64(long value) {
        elements.add(SimpleBsonValue.ofInt64(value));
        return this;
    }

    @Override
    public BsonArrayBuilder addDouble(double value) {
        elements.add(SimpleBsonValue.ofDouble(value));
        return this;
    }

    @Override
    public BsonArrayBuilder addBoolean(boolean value) {
        elements.add(SimpleBsonValue.ofBoolean(value));  // ✅ 单例
        return this;
    }

    // ==================== 引用类型添加 ====================

    @Override
    public BsonArrayBuilder addString(String value) {
        elements.add(SimpleBsonValue.ofString(value));
        return this;
    }

    @Override
    public BsonArrayBuilder addDocument(BsonDocument value) {
        elements.add(SimpleBsonValue.ofDocument(value));
        return this;
    }

    @Override
    public BsonArrayBuilder addArray(BsonArray value) {
        elements.add(SimpleBsonValue.ofArray(value));
        return this;
    }

    @Override
    public BsonArrayBuilder addNull() {
        elements.add(SimpleBsonValue.ofNull());
        return this;
    }

    @Override
    public BsonArrayBuilder addObjectId(String hexString) {
        elements.add(SimpleBsonValue.ofObjectId(hexString));
        return this;
    }

    @Override
    public BsonArrayBuilder addDateTime(long timestamp) {
        elements.add(SimpleBsonValue.ofDateTime(timestamp));
        return this;
    }

    @Override
    public BsonArrayBuilder addBinary(byte subtype, byte[] data) {
        elements.add(SimpleBsonValue.ofBinary(subtype, data));
        return this;
    }

    // ==================== 构建 ====================

    @Override
    public BsonArray build() {
        if (elements == null) {
            throw new IllegalStateException("Builder has already been used");
        }
        BsonArray array = new SimpleBsonArray(elements);
        elements = null;  // 释放引用，Builder失效
        return array;
    }

    @Override
    public void reset() {
        if (elements == null) {
            elements = new ArrayList<SimpleBsonValue>(estimatedSize);
        } else {
            elements.clear();
        }
    }
}
