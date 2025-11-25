package com.cloud.fastbson.document.fast;

import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.document.BsonArrayBuilder;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.util.BsonType;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.BitSet;

/**
 * Fast实现的数组构建器
 *
 * <p>使用fastutil的primitive lists构建数组，实现零装箱
 */
public final class FastBsonArrayBuilder implements BsonArrayBuilder {

    private ByteArrayList types;
    private IntArrayList intElements;
    private LongArrayList longElements;
    private DoubleArrayList doubleElements;
    private BitSet booleanElements;
    private ObjectArrayList<String> stringElements;
    private ObjectArrayList<Object> complexElements;

    private int estimatedSize = 16;

    public FastBsonArrayBuilder() {
        initialize(estimatedSize);
    }

    private void initialize(int capacity) {
        types = new ByteArrayList(capacity);
        intElements = new IntArrayList(capacity);
        longElements = new LongArrayList(capacity);
        doubleElements = new DoubleArrayList(capacity);
        booleanElements = new BitSet(capacity);
        stringElements = new ObjectArrayList<String>(capacity);
        complexElements = new ObjectArrayList<Object>(capacity);
    }

    @Override
    public BsonArrayBuilder estimateSize(int size) {
        this.estimatedSize = size;
        return this;
    }

    // ==================== Primitive添加（零装箱）====================

    @Override
    public BsonArrayBuilder addInt32(int value) {
        types.add(BsonType.INT32);
        intElements.add(value);  // ✅ 零装箱
        return this;
    }

    @Override
    public BsonArrayBuilder addInt64(long value) {
        types.add(BsonType.INT64);
        longElements.add(value);  // ✅ 零装箱
        return this;
    }

    @Override
    public BsonArrayBuilder addDouble(double value) {
        types.add(BsonType.DOUBLE);
        doubleElements.add(value);  // ✅ 零装箱
        return this;
    }

    @Override
    public BsonArrayBuilder addBoolean(boolean value) {
        int index = types.size();
        types.add(BsonType.BOOLEAN);
        if (value) {
            booleanElements.set(index);
        } else {
            booleanElements.clear(index);
        }
        return this;
    }

    // ==================== 引用类型添加 ====================

    @Override
    public BsonArrayBuilder addString(String value) {
        types.add(BsonType.STRING);
        stringElements.add(value);
        return this;
    }

    @Override
    public BsonArrayBuilder addDocument(BsonDocument value) {
        types.add(BsonType.DOCUMENT);
        complexElements.add(value);
        return this;
    }

    @Override
    public BsonArrayBuilder addArray(BsonArray value) {
        types.add(BsonType.ARRAY);
        complexElements.add(value);
        return this;
    }

    @Override
    public BsonArrayBuilder addNull() {
        types.add(BsonType.NULL);
        return this;
    }

    @Override
    public BsonArrayBuilder addObjectId(String hexString) {
        types.add(BsonType.OBJECT_ID);
        complexElements.add(hexString);
        return this;
    }

    @Override
    public BsonArrayBuilder addDateTime(long timestamp) {
        types.add(BsonType.DATE_TIME);
        longElements.add(timestamp);
        return this;
    }

    @Override
    public BsonArrayBuilder addBinary(byte subtype, byte[] data) {
        types.add(BsonType.BINARY);
        // TODO: Store binary with subtype
        complexElements.add(data);
        return this;
    }

    // ==================== 构建 ====================

    @Override
    public BsonArray build() {
        if (types == null) {
            throw new IllegalStateException("Builder has already been used");
        }

        // 构建不可变数组
        FastBsonArray array = new FastBsonArray(
            types,
            intElements,
            longElements,
            doubleElements,
            (BitSet) booleanElements.clone(),
            stringElements,
            complexElements
        );

        // 释放引用，Builder失效
        types = null;
        intElements = null;
        longElements = null;
        doubleElements = null;
        booleanElements = null;
        stringElements = null;
        complexElements = null;

        return array;
    }

    @Override
    public void reset() {
        if (types == null) {
            initialize(estimatedSize);
        } else {
            types.clear();
            intElements.clear();
            longElements.clear();
            doubleElements.clear();
            booleanElements.clear();
            stringElements.clear();
            complexElements.clear();
        }
    }
}
