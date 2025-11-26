package com.cloud.fastbson.document.hashmap;

import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.exception.BsonParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * ArrayList-based BsonArray implementation (Phase 1 style).
 *
 * <p>Uses standard ArrayList&lt;Object&gt; for storage with boxing primitives.
 * Simple and fast for scenarios where you access most array elements.
 */
public class HashMapBsonArray implements BsonArray {

    private final List<Object> data;

    /**
     * Creates an empty ArrayList-based array.
     */
    public HashMapBsonArray() {
        this.data = new ArrayList<Object>();
    }

    /**
     * Creates an ArrayList-based array from existing data.
     * Package-private for use by builder.
     */
    HashMapBsonArray(List<Object> data) {
        this.data = Collections.unmodifiableList(new ArrayList<Object>(data));
    }

    @Override
    public byte getType(int index) {
        if (index < 0 || index >= data.size()) {
            return 0;
        }
        // Infer type from value
        Object value = data.get(index);
        if (value == null) {
            return com.cloud.fastbson.util.BsonType.NULL;
        }
        if (value instanceof Integer) {
            return com.cloud.fastbson.util.BsonType.INT32;
        }
        if (value instanceof Long) {
            return com.cloud.fastbson.util.BsonType.INT64;
        }
        if (value instanceof Double) {
            return com.cloud.fastbson.util.BsonType.DOUBLE;
        }
        if (value instanceof Boolean) {
            return com.cloud.fastbson.util.BsonType.BOOLEAN;
        }
        if (value instanceof String) {
            return com.cloud.fastbson.util.BsonType.STRING;
        }
        if (value instanceof com.cloud.fastbson.document.BsonDocument) {
            return com.cloud.fastbson.util.BsonType.DOCUMENT;
        }
        if (value instanceof BsonArray) {
            return com.cloud.fastbson.util.BsonType.ARRAY;
        }
        if (value instanceof byte[]) {
            return com.cloud.fastbson.util.BsonType.BINARY;
        }
        return 0;
    }

    // ==================== Primitive类型访问 (无装箱) ====================

    @Override
    public int getInt32(int index) {
        if (index < 0 || index >= data.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + data.size());
        }
        Object value = data.get(index);
        if (!(value instanceof Integer)) {
            throw new BsonParseException("Element at index " + index + " is not Int32");
        }
        return ((Integer) value).intValue();
    }

    @Override
    public int getInt32(int index, int defaultValue) {
        if (index < 0 || index >= data.size()) {
            return defaultValue;
        }
        Object value = data.get(index);
        if (value == null || !(value instanceof Integer)) {
            return defaultValue;
        }
        return ((Integer) value).intValue();
    }

    @Override
    public long getInt64(int index) {
        if (index < 0 || index >= data.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + data.size());
        }
        Object value = data.get(index);
        if (!(value instanceof Long)) {
            throw new BsonParseException("Element at index " + index + " is not Int64");
        }
        return ((Long) value).longValue();
    }

    @Override
    public long getInt64(int index, long defaultValue) {
        if (index < 0 || index >= data.size()) {
            return defaultValue;
        }
        Object value = data.get(index);
        if (value == null || !(value instanceof Long)) {
            return defaultValue;
        }
        return ((Long) value).longValue();
    }

    @Override
    public double getDouble(int index) {
        if (index < 0 || index >= data.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + data.size());
        }
        Object value = data.get(index);
        if (!(value instanceof Double)) {
            throw new BsonParseException("Element at index " + index + " is not Double");
        }
        return ((Double) value).doubleValue();
    }

    @Override
    public double getDouble(int index, double defaultValue) {
        if (index < 0 || index >= data.size()) {
            return defaultValue;
        }
        Object value = data.get(index);
        if (value == null || !(value instanceof Double)) {
            return defaultValue;
        }
        return ((Double) value).doubleValue();
    }

    @Override
    public boolean getBoolean(int index) {
        if (index < 0 || index >= data.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + data.size());
        }
        Object value = data.get(index);
        if (!(value instanceof Boolean)) {
            throw new BsonParseException("Element at index " + index + " is not Boolean");
        }
        return ((Boolean) value).booleanValue();
    }

    @Override
    public boolean getBoolean(int index, boolean defaultValue) {
        if (index < 0 || index >= data.size()) {
            return defaultValue;
        }
        Object value = data.get(index);
        if (value == null || !(value instanceof Boolean)) {
            return defaultValue;
        }
        return ((Boolean) value).booleanValue();
    }

    // ==================== 引用类型访问 ====================

    @Override
    public String getString(int index) {
        if (index < 0 || index >= data.size()) {
            return null;
        }
        Object value = data.get(index);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String)) {
            throw new BsonParseException("Element at index " + index + " is not String");
        }
        return (String) value;
    }

    @Override
    public String getString(int index, String defaultValue) {
        if (index < 0 || index >= data.size()) {
            return defaultValue;
        }
        Object value = data.get(index);
        if (value == null || !(value instanceof String)) {
            return defaultValue;
        }
        return (String) value;
    }

    @Override
    public com.cloud.fastbson.document.BsonDocument getDocument(int index) {
        if (index < 0 || index >= data.size()) {
            return null;
        }
        Object value = data.get(index);
        if (value == null) {
            return null;
        }
        if (!(value instanceof com.cloud.fastbson.document.BsonDocument)) {
            throw new BsonParseException("Element at index " + index + " is not Document");
        }
        return (com.cloud.fastbson.document.BsonDocument) value;
    }

    @Override
    public BsonArray getArray(int index) {
        if (index < 0 || index >= data.size()) {
            return null;
        }
        Object value = data.get(index);
        if (value == null) {
            return null;
        }
        if (!(value instanceof BsonArray)) {
            throw new BsonParseException("Element at index " + index + " is not Array");
        }
        return (BsonArray) value;
    }

    // ==================== 通用访问 ====================

    @Override
    @Deprecated
    public Object get(int index) {
        if (index < 0 || index >= data.size()) {
            return null;
        }
        return data.get(index);
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public Iterator<Object> iterator() {
        return data.iterator();
    }

    @Override
    public String toJson() {
        // Simple JSON representation for debugging
        throw new UnsupportedOperationException("toJson() not implemented yet for Phase 1");
    }

    @Override
    public String toString() {
        return "HashMapBsonArray" + data.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HashMapBsonArray)) {
            return false;
        }
        HashMapBsonArray other = (HashMapBsonArray) obj;
        return data.equals(other.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }
}
