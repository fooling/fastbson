package com.cloud.fastbson.document.fast;

import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.util.BsonType;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Fast实现的BsonArray，使用fastutil的primitive lists实现零装箱
 *
 * <p>存储策略：
 * <ul>
 *   <li>Int32 → IntArrayList ✅ 零装箱</li>
 *   <li>Int64 → LongArrayList ✅ 零装箱</li>
 *   <li>Double → DoubleArrayList ✅ 零装箱</li>
 *   <li>Boolean → BitSet ✅ 零装箱</li>
 *   <li>String → ObjectArrayList&lt;String&gt;</li>
 *   <li>复杂类型 → ObjectArrayList&lt;Object&gt;</li>
 * </ul>
 */
public final class FastBsonArray implements BsonArray {

    private final ByteArrayList types;  // 记录每个元素的类型
    private final IntArrayList localIndices;  // 记录每个全局索引在其类型列表中的局部索引
    private final IntArrayList intElements;
    private final LongArrayList longElements;
    private final DoubleArrayList doubleElements;
    private final BitSet booleanElements;
    private final ObjectArrayList<String> stringElements;
    private final ObjectArrayList<Object> complexElements;

    // 空数组单例
    static final FastBsonArray EMPTY = new FastBsonArray(
        new ByteArrayList(),
        new IntArrayList(),
        new IntArrayList(),
        new LongArrayList(),
        new DoubleArrayList(),
        new BitSet(),
        new ObjectArrayList<String>(),
        new ObjectArrayList<Object>()
    );

    // 包内可见构造函数
    FastBsonArray(
        ByteArrayList types,
        IntArrayList localIndices,
        IntArrayList intElements,
        LongArrayList longElements,
        DoubleArrayList doubleElements,
        BitSet booleanElements,
        ObjectArrayList<String> stringElements,
        ObjectArrayList<Object> complexElements
    ) {
        this.types = types;
        this.localIndices = localIndices;
        this.intElements = intElements;
        this.longElements = longElements;
        this.doubleElements = doubleElements;
        this.booleanElements = booleanElements;
        this.stringElements = stringElements;
        this.complexElements = complexElements;
    }

    // ==================== 基本信息 ====================

    @Override
    public int size() {
        return types.size();
    }

    @Override
    public boolean isEmpty() {
        return types.isEmpty();
    }

    @Override
    public byte getType(int index) {
        if (index < 0 || index >= types.size()) {
            return 0;
        }
        return types.getByte(index);
    }

    // ==================== Primitive访问（零装箱）====================

    @Override
    public int getInt32(int index) {
        if (index < 0 || index >= types.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + types.size());
        }
        byte type = types.getByte(index);
        if (type != BsonType.INT32) {
            throw new ClassCastException("Element is not Int32 at index " + index +
                ", actual type: 0x" + Integer.toHexString(type & 0xFF));
        }
        return intElements.getInt(index);  // ✅ 零装箱
    }

    @Override
    public int getInt32(int index, int defaultValue) {
        if (index < 0 || index >= types.size()) {
            return defaultValue;
        }
        return intElements.getInt(index);
    }

    @Override
    public long getInt64(int index) {
        if (index < 0 || index >= types.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + types.size());
        }
        byte type = types.getByte(index);
        if (type != BsonType.INT64) {
            throw new ClassCastException("Element is not Int64 at index " + index +
                ", actual type: 0x" + Integer.toHexString(type & 0xFF));
        }
        return longElements.getLong(index);  // ✅ 零装箱
    }

    @Override
    public long getInt64(int index, long defaultValue) {
        if (index < 0 || index >= types.size()) {
            return defaultValue;
        }
        return longElements.getLong(index);
    }

    @Override
    public double getDouble(int index) {
        if (index < 0 || index >= types.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + types.size());
        }
        byte type = types.getByte(index);
        if (type != BsonType.DOUBLE) {
            throw new ClassCastException("Element is not Double at index " + index +
                ", actual type: 0x" + Integer.toHexString(type & 0xFF));
        }
        return doubleElements.getDouble(index);  // ✅ 零装箱
    }

    @Override
    public double getDouble(int index, double defaultValue) {
        if (index < 0 || index >= types.size()) {
            return defaultValue;
        }
        return doubleElements.getDouble(index);
    }

    @Override
    public boolean getBoolean(int index) {
        if (index < 0 || index >= types.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + types.size());
        }
        byte type = types.getByte(index);
        if (type != BsonType.BOOLEAN) {
            throw new ClassCastException("Element is not Boolean at index " + index +
                ", actual type: 0x" + Integer.toHexString(type & 0xFF));
        }
        return booleanElements.get(index);  // ✅ 零装箱
    }

    @Override
    public boolean getBoolean(int index, boolean defaultValue) {
        if (index < 0 || index >= types.size()) {
            return defaultValue;
        }
        return booleanElements.get(index);
    }

    // ==================== 引用类型访问 ====================

    @Override
    public String getString(int index) {
        if (index < 0 || index >= types.size()) {
            return null;
        }
        return stringElements.get(index);
    }

    @Override
    public String getString(int index, String defaultValue) {
        if (index < 0 || index >= types.size()) {
            return defaultValue;
        }
        String value = stringElements.get(index);
        return value != null ? value : defaultValue;
    }

    @Override
    public BsonDocument getDocument(int index) {
        if (index < 0 || index >= types.size()) {
            return null;
        }
        return (BsonDocument) complexElements.get(index);
    }

    @Override
    public BsonArray getArray(int index) {
        if (index < 0 || index >= types.size()) {
            return null;
        }
        return (BsonArray) complexElements.get(index);
    }

    // ==================== 通用访问 ====================

    @Override
    @Deprecated
    public Object get(int index) {
        if (index < 0 || index >= types.size()) {
            return null;
        }

        byte type = types.getByte(index);
        int localIndex = localIndices.getInt(index);  // ✅ 获取局部索引

        switch (type) {
            case BsonType.INT32:
                return intElements.getInt(localIndex);  // 自动装箱，使用局部索引
            case BsonType.INT64:
                return longElements.getLong(localIndex);  // 自动装箱，使用局部索引
            case BsonType.DOUBLE:
                return doubleElements.getDouble(localIndex);  // 自动装箱，使用局部索引
            case BsonType.BOOLEAN:
                return booleanElements.get(index);  // BitSet使用全局索引
            case BsonType.STRING:
                return stringElements.get(localIndex);  // 使用局部索引
            case BsonType.DOCUMENT:
            case BsonType.ARRAY:
            case BsonType.OBJECT_ID:
            case BsonType.BINARY:
                return complexElements.get(localIndex);  // 使用局部索引
            case BsonType.DATE_TIME:
                return longElements.getLong(localIndex);  // 使用局部索引
            case BsonType.NULL:
                return null;
            default:
                return complexElements.get(localIndex);  // 使用局部索引
        }
    }

    @Override
    public Iterator<Object> iterator() {
        return new Iterator<Object>() {
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < types.size();
            }

            @Override
            public Object next() {
                return get(cursor++);  // 使用get()装箱
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Cannot remove from immutable array");
            }
        };
    }

    // ==================== 调试 ====================

    @Override
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < types.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            appendValueAsJson(sb, i, types.getByte(i));
        }
        sb.append(']');
        return sb.toString();
    }

    private void appendValueAsJson(StringBuilder sb, int index, byte type) {
        switch (type) {
            case BsonType.INT32:
                sb.append(intElements.getInt(index));
                break;
            case BsonType.INT64:
                sb.append(longElements.getLong(index));
                break;
            case BsonType.DOUBLE:
                sb.append(doubleElements.getDouble(index));
                break;
            case BsonType.BOOLEAN:
                sb.append(booleanElements.get(index));
                break;
            case BsonType.STRING:
                sb.append('\"').append(escapeJson(stringElements.get(index))).append('\"');
                break;
            case BsonType.DOCUMENT:
                sb.append(((BsonDocument) complexElements.get(index)).toJson());
                break;
            case BsonType.ARRAY:
                sb.append(((BsonArray) complexElements.get(index)).toJson());
                break;
            case BsonType.NULL:
                sb.append("null");
                break;
            case BsonType.OBJECT_ID:
                sb.append('\"').append(complexElements.get(index)).append('\"');
                break;
            case BsonType.DATE_TIME:
                sb.append(longElements.getLong(index));
                break;
            default:
                sb.append("\"<unsupported>\"");
        }
    }

    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    @Override
    public String toString() {
        return toJson();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FastBsonArray that = (FastBsonArray) o;
        return types.equals(that.types) &&
               intElements.equals(that.intElements) &&
               longElements.equals(that.longElements) &&
               doubleElements.equals(that.doubleElements) &&
               booleanElements.equals(that.booleanElements) &&
               stringElements.equals(that.stringElements) &&
               complexElements.equals(that.complexElements);
    }

    @Override
    public int hashCode() {
        int result = types.hashCode();
        result = 31 * result + intElements.hashCode();
        result = 31 * result + longElements.hashCode();
        result = 31 * result + doubleElements.hashCode();
        result = 31 * result + booleanElements.hashCode();
        result = 31 * result + stringElements.hashCode();
        result = 31 * result + complexElements.hashCode();
        return result;
    }
}
