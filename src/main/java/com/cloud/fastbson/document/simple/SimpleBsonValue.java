package com.cloud.fastbson.document.simple;

import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.types.BinaryData;
import com.cloud.fastbson.util.BsonType;

/**
 * 轻量级Union类型，存储所有BSON值
 *
 * <p>内存布局（64位JVM）：
 * <ul>
 *   <li>对象头：16字节</li>
 *   <li>type: 1字节 + padding: 3字节 = 4字节</li>
 *   <li>intValue: 4字节</li>
 *   <li>longValue: 8字节</li>
 *   <li>refValue: 8字节（对象引用）</li>
 *   <li>总计：40字节</li>
 * </ul>
 *
 * <p>对比：
 * <ul>
 *   <li>装箱Integer：16字节 + Map Entry开销 ~48字节</li>
 *   <li>SimpleBsonValue：40字节，支持所有类型，可缓存</li>
 * </ul>
 *
 * <p>优化：
 * <ul>
 *   <li>Int32缓存：-128~127 的值使用缓存，零GC</li>
 *   <li>Boolean单例：true/false 共享单例，零GC</li>
 *   <li>Double复用longValue：通过Double.doubleToRawLongBits存储</li>
 * </ul>
 */
final class SimpleBsonValue {
    final byte type;

    // Primitive存储（union）
    int intValue;
    long longValue;      // 也用于存储DateTime, Double

    // 引用类型存储
    Object refValue;     // String, BsonDocument, BsonArray, byte[]等

    // ==================== 私有构造 ====================

    private SimpleBsonValue(byte type) {
        this.type = type;
    }

    // ==================== Int32缓存 (-128~127) ====================

    private static final SimpleBsonValue[] INT32_CACHE = new SimpleBsonValue[256];

    static {
        for (int i = 0; i < 256; i++) {
            SimpleBsonValue v = new SimpleBsonValue(BsonType.INT32);
            v.intValue = i - 128;
            INT32_CACHE[i] = v;
        }
    }

    /**
     * 创建Int32值
     * <p>缓存范围：-128~127（零GC）
     */
    static SimpleBsonValue ofInt32(int value) {
        if (value >= -128 && value <= 127) {
            return INT32_CACHE[value + 128];  // ✅ 缓存命中，零GC
        }
        SimpleBsonValue v = new SimpleBsonValue(BsonType.INT32);
        v.intValue = value;
        return v;
    }

    // ==================== Int64 ====================

    static SimpleBsonValue ofInt64(long value) {
        SimpleBsonValue v = new SimpleBsonValue(BsonType.INT64);
        v.longValue = value;
        return v;
    }

    // ==================== Double（复用longValue） ====================

    static SimpleBsonValue ofDouble(double value) {
        SimpleBsonValue v = new SimpleBsonValue(BsonType.DOUBLE);
        v.longValue = Double.doubleToRawLongBits(value);  // 复用longValue
        return v;
    }

    // ==================== Boolean单例 ====================

    static final SimpleBsonValue TRUE;
    static final SimpleBsonValue FALSE;

    static {
        TRUE = new SimpleBsonValue(BsonType.BOOLEAN);
        TRUE.intValue = 1;

        FALSE = new SimpleBsonValue(BsonType.BOOLEAN);
        FALSE.intValue = 0;
    }

    static SimpleBsonValue ofBoolean(boolean value) {
        return value ? TRUE : FALSE;  // ✅ 单例，零GC
    }

    // ==================== String ====================

    static SimpleBsonValue ofString(String value) {
        SimpleBsonValue v = new SimpleBsonValue(BsonType.STRING);
        v.refValue = value;
        return v;
    }

    // ==================== Document ====================

    static SimpleBsonValue ofDocument(BsonDocument value) {
        SimpleBsonValue v = new SimpleBsonValue(BsonType.DOCUMENT);
        v.refValue = value;
        return v;
    }

    // ==================== Array ====================

    static SimpleBsonValue ofArray(BsonArray value) {
        SimpleBsonValue v = new SimpleBsonValue(BsonType.ARRAY);
        v.refValue = value;
        return v;
    }

    // ==================== ObjectId ====================

    static SimpleBsonValue ofObjectId(String hexString) {
        SimpleBsonValue v = new SimpleBsonValue(BsonType.OBJECT_ID);
        v.refValue = hexString;
        return v;
    }

    // ==================== DateTime（使用longValue） ====================

    static SimpleBsonValue ofDateTime(long timestamp) {
        SimpleBsonValue v = new SimpleBsonValue(BsonType.DATE_TIME);
        v.longValue = timestamp;
        return v;
    }

    // ==================== Null ====================

    static final SimpleBsonValue NULL = new SimpleBsonValue(BsonType.NULL);

    static SimpleBsonValue ofNull() {
        return NULL;  // ✅ 单例
    }

    // ==================== Binary ====================

    static SimpleBsonValue ofBinary(byte subtype, byte[] data) {
        SimpleBsonValue v = new SimpleBsonValue(BsonType.BINARY);
        v.intValue = subtype & 0xFF;  // subtype存储在intValue
        v.refValue = data;
        return v;
    }

    // ==================== 通用复杂类型 ====================

    /**
     * 创建通用复杂类型（Regex, DBPointer, Timestamp, Decimal128等）
     */
    static SimpleBsonValue ofComplex(byte type, Object value) {
        SimpleBsonValue v = new SimpleBsonValue(type);
        v.refValue = value;
        return v;
    }

    // ==================== 访问方法 ====================

    int asInt32() {
        if (type != BsonType.INT32) {
            throw new ClassCastException("Not an Int32, actual type: 0x" +
                Integer.toHexString(type & 0xFF));
        }
        return intValue;
    }

    long asInt64() {
        if (type != BsonType.INT64) {
            throw new ClassCastException("Not an Int64, actual type: 0x" +
                Integer.toHexString(type & 0xFF));
        }
        return longValue;
    }

    double asDouble() {
        if (type != BsonType.DOUBLE) {
            throw new ClassCastException("Not a Double, actual type: 0x" +
                Integer.toHexString(type & 0xFF));
        }
        return Double.longBitsToDouble(longValue);  // 从longValue还原
    }

    boolean asBoolean() {
        if (type != BsonType.BOOLEAN) {
            throw new ClassCastException("Not a Boolean, actual type: 0x" +
                Integer.toHexString(type & 0xFF));
        }
        return intValue != 0;
    }

    String asString() {
        if (type != BsonType.STRING) {
            throw new ClassCastException("Not a String, actual type: 0x" +
                Integer.toHexString(type & 0xFF));
        }
        return (String) refValue;
    }

    BsonDocument asDocument() {
        if (type != BsonType.DOCUMENT) {
            throw new ClassCastException("Not a Document, actual type: 0x" +
                Integer.toHexString(type & 0xFF));
        }
        return (BsonDocument) refValue;
    }

    BsonArray asArray() {
        if (type != BsonType.ARRAY) {
            throw new ClassCastException("Not an Array, actual type: 0x" +
                Integer.toHexString(type & 0xFF));
        }
        return (BsonArray) refValue;
    }

    String asObjectId() {
        if (type != BsonType.OBJECT_ID) {
            throw new ClassCastException("Not an ObjectId, actual type: 0x" +
                Integer.toHexString(type & 0xFF));
        }
        return (String) refValue;
    }

    long asDateTime() {
        if (type != BsonType.DATE_TIME) {
            throw new ClassCastException("Not a DateTime, actual type: 0x" +
                Integer.toHexString(type & 0xFF));
        }
        return longValue;
    }

    byte[] asBinary() {
        if (type != BsonType.BINARY) {
            throw new ClassCastException("Not Binary, actual type: 0x" +
                Integer.toHexString(type & 0xFF));
        }
        return (byte[]) refValue;
    }

    byte getBinarySubtype() {
        if (type != BsonType.BINARY) {
            throw new ClassCastException("Not Binary, actual type: 0x" +
                Integer.toHexString(type & 0xFF));
        }
        return (byte) intValue;
    }

    // ==================== 装箱访问（Legacy） ====================

    /**
     * 装箱为Object（用于Legacy API）
     */
    Object toBoxedObject() {
        switch (type) {
            case BsonType.INT32:
                return intValue;  // 自动装箱 int → Integer
            case BsonType.INT64:
                return longValue;  // 自动装箱 long → Long
            case BsonType.DOUBLE:
                return Double.longBitsToDouble(longValue);  // 自动装箱
            case BsonType.BOOLEAN:
                return intValue != 0;  // 自动装箱 boolean → Boolean
            case BsonType.STRING:
            case BsonType.OBJECT_ID:
                return refValue;
            case BsonType.BINARY:
                // Return BinaryData object for legacy compatibility
                return new BinaryData((byte) intValue, (byte[]) refValue);
            case BsonType.DOCUMENT:
                // Recursively convert BsonDocument to Map
                if (refValue instanceof BsonDocument) {
                    return ((BsonDocument) refValue).toLegacyMap();
                }
                return refValue;
            case BsonType.ARRAY:
                // Recursively convert BsonArray to List
                if (refValue instanceof BsonArray) {
                    return ((BsonArray) refValue).toLegacyList();
                }
                return refValue;
            case BsonType.DATE_TIME:
                return longValue;  // 返回timestamp
            case BsonType.NULL:
                return null;
            default:
                return refValue;
        }
    }
}
