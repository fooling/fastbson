package com.cloud.fastbson.document.fast;

import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.document.BsonArrayBuilder;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.types.BinaryData;
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
    private IntArrayList localIndices;  // 记录局部索引
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
        localIndices = new IntArrayList(capacity);
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
        localIndices.add(intElements.size());  // 记录局部索引
        intElements.add(value);  // ✅ 零装箱
        return this;
    }

    @Override
    public BsonArrayBuilder addInt64(long value) {
        types.add(BsonType.INT64);
        localIndices.add(longElements.size());  // 记录局部索引
        longElements.add(value);  // ✅ 零装箱
        return this;
    }

    @Override
    public BsonArrayBuilder addDouble(double value) {
        types.add(BsonType.DOUBLE);
        localIndices.add(doubleElements.size());  // 记录局部索引
        doubleElements.add(value);  // ✅ 零装箱
        return this;
    }

    @Override
    public BsonArrayBuilder addBoolean(boolean value) {
        int index = types.size();
        types.add(BsonType.BOOLEAN);
        localIndices.add(index);  // Boolean使用全局索引（BitSet）
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
        localIndices.add(stringElements.size());  // 记录局部索引
        stringElements.add(value);
        return this;
    }

    @Override
    public BsonArrayBuilder addDocument(BsonDocument value) {
        types.add(BsonType.DOCUMENT);
        localIndices.add(complexElements.size());  // 记录局部索引
        complexElements.add(value);
        return this;
    }

    @Override
    public BsonArrayBuilder addArray(BsonArray value) {
        types.add(BsonType.ARRAY);
        localIndices.add(complexElements.size());  // 记录局部索引
        complexElements.add(value);
        return this;
    }

    @Override
    public BsonArrayBuilder addNull() {
        types.add(BsonType.NULL);
        localIndices.add(-1);  // Null不需要索引
        return this;
    }

    @Override
    public BsonArrayBuilder addObjectId(String hexString) {
        types.add(BsonType.OBJECT_ID);
        localIndices.add(complexElements.size());  // 记录局部索引
        complexElements.add(hexString);
        return this;
    }

    @Override
    public BsonArrayBuilder addDateTime(long timestamp) {
        types.add(BsonType.DATE_TIME);
        localIndices.add(longElements.size());  // 记录局部索引
        longElements.add(timestamp);
        return this;
    }

    @Override
    public BsonArrayBuilder addBinary(byte subtype, byte[] data) {
        types.add(BsonType.BINARY);
        localIndices.add(complexElements.size());  // 记录局部索引
        // Store BinaryData object with subtype
        complexElements.add(new BinaryData(subtype, data));
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
            localIndices,
            intElements,
            longElements,
            doubleElements,
            (BitSet) booleanElements.clone(),
            stringElements,
            complexElements
        );

        // 释放引用，Builder失效
        types = null;
        localIndices = null;
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
            localIndices.clear();
            intElements.clear();
            longElements.clear();
            doubleElements.clear();
            booleanElements.clear();
            stringElements.clear();
            complexElements.clear();
        }
    }
}
