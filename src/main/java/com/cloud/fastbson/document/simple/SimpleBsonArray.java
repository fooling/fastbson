package com.cloud.fastbson.document.simple;

import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.util.BsonType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Simple实现的BsonArray，使用ArrayList存储SimpleBsonValue
 *
 * <p>特点：
 * <ul>
 *   <li>零外部依赖</li>
 *   <li>Primitive值使用SimpleBsonValue，减少装箱</li>
 *   <li>小整数和Boolean使用缓存/单例</li>
 * </ul>
 */
public final class SimpleBsonArray implements BsonArray {

    private final List<SimpleBsonValue> elements;

    // 空数组单例
    static final SimpleBsonArray EMPTY = new SimpleBsonArray(Collections.<SimpleBsonValue>emptyList());

    // 包内可见构造函数
    SimpleBsonArray(List<SimpleBsonValue> elements) {
        this.elements = Collections.unmodifiableList(elements);
    }

    // ==================== 基本信息 ====================

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    public byte getType(int index) {
        if (index < 0 || index >= elements.size()) {
            return 0;
        }
        return elements.get(index).type;
    }

    // ==================== Primitive访问（无装箱）====================

    @Override
    public int getInt32(int index) {
        if (index < 0 || index >= elements.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + elements.size());
        }
        return elements.get(index).asInt32();  // ✅ 无装箱
    }

    @Override
    public int getInt32(int index, int defaultValue) {
        if (index < 0 || index >= elements.size()) {
            return defaultValue;
        }
        return elements.get(index).asInt32();
    }

    @Override
    public long getInt64(int index) {
        if (index < 0 || index >= elements.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + elements.size());
        }
        return elements.get(index).asInt64();  // ✅ 无装箱
    }

    @Override
    public long getInt64(int index, long defaultValue) {
        if (index < 0 || index >= elements.size()) {
            return defaultValue;
        }
        return elements.get(index).asInt64();
    }

    @Override
    public double getDouble(int index) {
        if (index < 0 || index >= elements.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + elements.size());
        }
        return elements.get(index).asDouble();  // ✅ 无装箱
    }

    @Override
    public double getDouble(int index, double defaultValue) {
        if (index < 0 || index >= elements.size()) {
            return defaultValue;
        }
        return elements.get(index).asDouble();
    }

    @Override
    public boolean getBoolean(int index) {
        if (index < 0 || index >= elements.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + elements.size());
        }
        return elements.get(index).asBoolean();  // ✅ 无装箱
    }

    @Override
    public boolean getBoolean(int index, boolean defaultValue) {
        if (index < 0 || index >= elements.size()) {
            return defaultValue;
        }
        return elements.get(index).asBoolean();
    }

    // ==================== 引用类型访问 ====================

    @Override
    public String getString(int index) {
        if (index < 0 || index >= elements.size()) {
            return null;
        }
        return elements.get(index).asString();
    }

    @Override
    public String getString(int index, String defaultValue) {
        if (index < 0 || index >= elements.size()) {
            return defaultValue;
        }
        return elements.get(index).asString();
    }

    @Override
    public BsonDocument getDocument(int index) {
        if (index < 0 || index >= elements.size()) {
            return null;
        }
        return elements.get(index).asDocument();
    }

    @Override
    public BsonArray getArray(int index) {
        if (index < 0 || index >= elements.size()) {
            return null;
        }
        return elements.get(index).asArray();
    }

    // ==================== 通用访问 ====================

    @Override
    @Deprecated
    public Object get(int index) {
        if (index < 0 || index >= elements.size()) {
            return null;
        }
        return elements.get(index).toBoxedObject();  // 装箱
    }

    @Override
    public Iterator<Object> iterator() {
        return new Iterator<Object>() {
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < elements.size();
            }

            @Override
            public Object next() {
                return elements.get(cursor++).toBoxedObject();  // 装箱
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Cannot remove from immutable array");
            }
        };
    }

    @Override
    @Deprecated
    public List<Object> toLegacyList() {
        List<Object> result = new ArrayList<Object>(elements.size());
        for (SimpleBsonValue element : elements) {
            result.add(element.toBoxedObject());
        }
        return result;
    }

    // ==================== 调试 ====================

    @Override
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            appendValueAsJson(sb, elements.get(i));
        }
        sb.append(']');
        return sb.toString();
    }

    private void appendValueAsJson(StringBuilder sb, SimpleBsonValue value) {
        switch (value.type) {
            case BsonType.INT32:
                sb.append(value.asInt32());
                break;
            case BsonType.INT64:
                sb.append(value.asInt64());
                break;
            case BsonType.DOUBLE:
                sb.append(value.asDouble());
                break;
            case BsonType.BOOLEAN:
                sb.append(value.asBoolean());
                break;
            case BsonType.STRING:
                sb.append('\"').append(escapeJson(value.asString())).append('\"');
                break;
            case BsonType.DOCUMENT:
                sb.append(value.asDocument().toJson());
                break;
            case BsonType.ARRAY:
                sb.append(value.asArray().toJson());
                break;
            case BsonType.NULL:
                sb.append("null");
                break;
            case BsonType.OBJECT_ID:
                sb.append('\"').append(value.asObjectId()).append('\"');
                break;
            case BsonType.DATE_TIME:
                sb.append(value.asDateTime());
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
        SimpleBsonArray that = (SimpleBsonArray) o;
        return elements.equals(that.elements);
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }
}
