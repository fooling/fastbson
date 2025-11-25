package com.cloud.fastbson.document.fast;

import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.document.BsonDocumentBuilder;
import com.cloud.fastbson.util.BsonType;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.*;

import java.util.BitSet;

/**
 * Fast实现的文档构建器
 *
 * <p>使用fastutil的primitive maps构建文档，实现零装箱
 */
public final class FastBsonDocumentBuilder implements BsonDocumentBuilder {

    private Object2IntMap<String> fieldNameToId;
    private Int2ObjectMap<String> fieldIdToName;
    private Int2ByteMap fieldTypes;
    private Int2IntMap intFields;
    private Int2LongMap longFields;
    private Int2DoubleMap doubleFields;
    private BitSet booleanFields;
    private BitSet booleanExists;
    private Int2ObjectMap<String> stringFields;
    private Int2ObjectMap<Object> complexFields;

    private int nextFieldId = 0;
    private int estimatedSize = 16;

    public FastBsonDocumentBuilder() {
        initialize(estimatedSize);
    }

    private void initialize(int capacity) {
        fieldNameToId = new Object2IntOpenHashMap<String>(capacity);
        fieldNameToId.defaultReturnValue(-1);  // 不存在返回-1

        fieldIdToName = new Int2ObjectOpenHashMap<String>(capacity);
        fieldTypes = new Int2ByteOpenHashMap(capacity);
        intFields = new Int2IntOpenHashMap(capacity);
        longFields = new Int2LongOpenHashMap(capacity);
        doubleFields = new Int2DoubleOpenHashMap(capacity);
        booleanFields = new BitSet(capacity);
        booleanExists = new BitSet(capacity);
        stringFields = new Int2ObjectOpenHashMap<String>(capacity);
        complexFields = new Int2ObjectOpenHashMap<Object>(capacity);
    }

    @Override
    public BsonDocumentBuilder estimateSize(int fieldCount) {
        this.estimatedSize = fieldCount;
        // 如果已经初始化，不需要重新分配
        return this;
    }

    // ==================== Primitive添加（零装箱）====================

    @Override
    public BsonDocumentBuilder putInt32(String fieldName, int value) {
        int fieldId = getOrCreateFieldId(fieldName);
        fieldTypes.put(fieldId, BsonType.INT32);
        intFields.put(fieldId, value);  // ✅ 零装箱
        return this;
    }

    @Override
    public BsonDocumentBuilder putInt64(String fieldName, long value) {
        int fieldId = getOrCreateFieldId(fieldName);
        fieldTypes.put(fieldId, BsonType.INT64);
        longFields.put(fieldId, value);  // ✅ 零装箱
        return this;
    }

    @Override
    public BsonDocumentBuilder putDouble(String fieldName, double value) {
        int fieldId = getOrCreateFieldId(fieldName);
        fieldTypes.put(fieldId, BsonType.DOUBLE);
        doubleFields.put(fieldId, value);  // ✅ 零装箱
        return this;
    }

    @Override
    public BsonDocumentBuilder putBoolean(String fieldName, boolean value) {
        int fieldId = getOrCreateFieldId(fieldName);
        fieldTypes.put(fieldId, BsonType.BOOLEAN);
        booleanExists.set(fieldId);  // 标记存在
        if (value) {
            booleanFields.set(fieldId);  // true
        } else {
            booleanFields.clear(fieldId);  // false
        }
        return this;
    }

    // ==================== 引用类型添加 ====================

    @Override
    public BsonDocumentBuilder putString(String fieldName, String value) {
        int fieldId = getOrCreateFieldId(fieldName);
        fieldTypes.put(fieldId, BsonType.STRING);
        stringFields.put(fieldId, value);
        return this;
    }

    @Override
    public BsonDocumentBuilder putDocument(String fieldName, BsonDocument value) {
        int fieldId = getOrCreateFieldId(fieldName);
        fieldTypes.put(fieldId, BsonType.DOCUMENT);
        complexFields.put(fieldId, value);
        return this;
    }

    @Override
    public BsonDocumentBuilder putArray(String fieldName, BsonArray value) {
        int fieldId = getOrCreateFieldId(fieldName);
        fieldTypes.put(fieldId, BsonType.ARRAY);
        complexFields.put(fieldId, value);
        return this;
    }

    @Override
    public BsonDocumentBuilder putObjectId(String fieldName, String hexString) {
        int fieldId = getOrCreateFieldId(fieldName);
        fieldTypes.put(fieldId, BsonType.OBJECT_ID);
        complexFields.put(fieldId, hexString);
        return this;
    }

    @Override
    public BsonDocumentBuilder putDateTime(String fieldName, long timestamp) {
        int fieldId = getOrCreateFieldId(fieldName);
        fieldTypes.put(fieldId, BsonType.DATE_TIME);
        longFields.put(fieldId, timestamp);
        return this;
    }

    @Override
    public BsonDocumentBuilder putNull(String fieldName) {
        int fieldId = getOrCreateFieldId(fieldName);
        fieldTypes.put(fieldId, BsonType.NULL);
        return this;
    }

    @Override
    public BsonDocumentBuilder putBinary(String fieldName, byte subtype, byte[] data) {
        int fieldId = getOrCreateFieldId(fieldName);
        fieldTypes.put(fieldId, BsonType.BINARY);
        // TODO: Store binary with subtype
        complexFields.put(fieldId, data);
        return this;
    }

    // ==================== 辅助方法 ====================

    private int getOrCreateFieldId(String fieldName) {
        int fieldId = fieldNameToId.getInt(fieldName);
        if (fieldId < 0) {
            fieldId = nextFieldId++;
            fieldNameToId.put(fieldName, fieldId);
            fieldIdToName.put(fieldId, fieldName);
        }
        return fieldId;
    }

    // ==================== 构建 ====================

    @Override
    public BsonDocument build() {
        if (fieldNameToId == null) {
            throw new IllegalStateException("Builder has already been used");
        }

        // 构建不可变文档
        FastBsonDocument doc = new FastBsonDocument(
            fieldNameToId,
            fieldIdToName,
            fieldTypes,
            intFields,
            longFields,
            doubleFields,
            (BitSet) booleanFields.clone(),
            (BitSet) booleanExists.clone(),
            stringFields,
            complexFields
        );

        // 释放引用，Builder失效
        fieldNameToId = null;
        fieldIdToName = null;
        fieldTypes = null;
        intFields = null;
        longFields = null;
        doubleFields = null;
        booleanFields = null;
        booleanExists = null;
        stringFields = null;
        complexFields = null;

        return doc;
    }

    @Override
    public void reset() {
        if (fieldNameToId == null) {
            initialize(estimatedSize);
        } else {
            fieldNameToId.clear();
            fieldIdToName.clear();
            fieldTypes.clear();
            intFields.clear();
            longFields.clear();
            doubleFields.clear();
            booleanFields.clear();
            booleanExists.clear();
            stringFields.clear();
            complexFields.clear();
            nextFieldId = 0;
        }
    }
}
