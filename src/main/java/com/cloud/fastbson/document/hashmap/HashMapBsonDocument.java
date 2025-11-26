package com.cloud.fastbson.document.hashmap;

import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.exception.BsonParseException;
import com.cloud.fastbson.util.BsonType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * HashMap-based BsonDocument implementation (Phase 1 style).
 *
 * <p><b>Phase 1 Compatible - Eager Parsing with Boxing</b>
 *
 * <p>Characteristics:
 * <ul>
 *   <li>Uses standard HashMap&lt;String, Object&gt; for storage</li>
 *   <li>Eager parsing: all values parsed immediately during document creation</li>
 *   <li>Boxing primitives: int/long/double stored as Integer/Long/Double</li>
 *   <li>Simple and fast for full document parsing scenarios</li>
 * </ul>
 *
 * <p>Performance:
 * <ul>
 *   <li>Full parsing: ~3.88x faster than MongoDB BSON (Phase 1 verified)</li>
 *   <li>Memory: Uses standard Java boxing (~40-80 bytes per primitive field)</li>
 *   <li>Access: O(1) HashMap lookup, very fast</li>
 * </ul>
 *
 * <p>Use Cases:
 * <ul>
 *   <li>When you need to access most/all fields in the document</li>
 *   <li>When simplicity and compatibility are more important than memory</li>
 *   <li>Phase 1 scenarios: complete document parsing</li>
 * </ul>
 *
 * <p><b>NOT recommended for:</b>
 * <ul>
 *   <li>Partial field parsing (use FastBsonDocument instead)</li>
 *   <li>Memory-sensitive applications (40% more memory than FastBson)</li>
 *   <li>High GC pressure scenarios (creates boxed objects)</li>
 * </ul>
 */
public class HashMapBsonDocument implements BsonDocument {

    private final Map<String, Object> data;
    private final Map<String, Byte> types;  // Track BSON type for each field

    /**
     * Creates an empty HashMap-based document.
     */
    public HashMapBsonDocument() {
        this.data = new HashMap<String, Object>();
        this.types = new HashMap<String, Byte>();
    }

    /**
     * Creates a HashMap-based document from existing data.
     * Public for use by optimized parsers.
     */
    public HashMapBsonDocument(Map<String, Object> data, Map<String, Byte> types) {
        this.data = Collections.unmodifiableMap(new HashMap<String, Object>(data));
        this.types = Collections.unmodifiableMap(new HashMap<String, Byte>(types));
    }

    /**
     * Phase 1 优化：直接使用传入的 Map，避免防御性复制（性能优先）
     *
     * <p>⚠️ 警告：调用者必须保证不会修改传入的 data 和 types
     *
     * @param data 字段数据（直接存储，零复制）
     * @param types 类型映射（直接存储，零复制）
     * @return HashMapBsonDocument（零复制，最高性能）
     */
    public static HashMapBsonDocument createDirect(Map<String, Object> data, Map<String, Byte> types) {
        return new HashMapBsonDocument(data, types, true);  // 使用特殊构造函数
    }

    /**
     * Phase 1 优化专用构造函数：直接存储，零复制
     */
    private HashMapBsonDocument(Map<String, Object> data, Map<String, Byte> types, boolean direct) {
        this.data = data;   // 直接引用，零复制
        this.types = types; // 直接引用，零复制
    }

    // ==================== Type判断 ====================

    @Override
    public boolean contains(String fieldName) {
        return data.containsKey(fieldName);
    }

    @Override
    public byte getType(String fieldName) {
        Byte type = types.get(fieldName);
        if (type == null) {
            return 0;
        }
        return type.byteValue();
    }

    @Override
    public boolean isNull(String fieldName) {
        if (!data.containsKey(fieldName)) {
            return false;
        }
        return data.get(fieldName) == null;
    }

    @Override
    public Set<String> fieldNames() {
        return data.keySet();
    }

    // ==================== Primitive类型访问 (无装箱) ====================

    @Override
    public int getInt32(String fieldName) {
        Object value = data.get(fieldName);
        if (value == null) {
            throw new BsonParseException("Field '" + fieldName + "' not found");
        }
        if (!(value instanceof Integer)) {
            throw new BsonParseException("Field '" + fieldName + "' is not Int32");
        }
        return ((Integer) value).intValue();
    }

    @Override
    public int getInt32(String fieldName, int defaultValue) {
        if (!data.containsKey(fieldName)) {
            return defaultValue;
        }
        Object value = data.get(fieldName);
        if (value == null || !(value instanceof Integer)) {
            return defaultValue;
        }
        return ((Integer) value).intValue();
    }

    @Override
    public long getInt64(String fieldName) {
        Object value = data.get(fieldName);
        if (value == null) {
            throw new BsonParseException("Field '" + fieldName + "' not found");
        }
        if (!(value instanceof Long)) {
            throw new BsonParseException("Field '" + fieldName + "' is not Int64");
        }
        return ((Long) value).longValue();
    }

    @Override
    public long getInt64(String fieldName, long defaultValue) {
        if (!data.containsKey(fieldName)) {
            return defaultValue;
        }
        Object value = data.get(fieldName);
        if (value == null || !(value instanceof Long)) {
            return defaultValue;
        }
        return ((Long) value).longValue();
    }

    @Override
    public double getDouble(String fieldName) {
        Object value = data.get(fieldName);
        if (value == null) {
            throw new BsonParseException("Field '" + fieldName + "' not found");
        }
        if (!(value instanceof Double)) {
            throw new BsonParseException("Field '" + fieldName + "' is not Double");
        }
        return ((Double) value).doubleValue();
    }

    @Override
    public double getDouble(String fieldName, double defaultValue) {
        if (!data.containsKey(fieldName)) {
            return defaultValue;
        }
        Object value = data.get(fieldName);
        if (value == null || !(value instanceof Double)) {
            return defaultValue;
        }
        return ((Double) value).doubleValue();
    }

    @Override
    public boolean getBoolean(String fieldName) {
        Object value = data.get(fieldName);
        if (value == null) {
            throw new BsonParseException("Field '" + fieldName + "' not found");
        }
        if (!(value instanceof Boolean)) {
            throw new BsonParseException("Field '" + fieldName + "' is not Boolean");
        }
        return ((Boolean) value).booleanValue();
    }

    @Override
    public boolean getBoolean(String fieldName, boolean defaultValue) {
        if (!data.containsKey(fieldName)) {
            return defaultValue;
        }
        Object value = data.get(fieldName);
        if (value == null || !(value instanceof Boolean)) {
            return defaultValue;
        }
        return ((Boolean) value).booleanValue();
    }

    // ==================== 引用类型访问 ====================

    @Override
    public String getString(String fieldName) {
        Object value = data.get(fieldName);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String)) {
            throw new BsonParseException("Field '" + fieldName + "' is not String");
        }
        return (String) value;
    }

    @Override
    public String getString(String fieldName, String defaultValue) {
        if (!data.containsKey(fieldName)) {
            return defaultValue;
        }
        Object value = data.get(fieldName);
        if (value == null || !(value instanceof String)) {
            return defaultValue;
        }
        return (String) value;
    }

    @Override
    public BsonDocument getDocument(String fieldName) {
        Object value = data.get(fieldName);
        if (value == null) {
            return null;
        }
        if (!(value instanceof BsonDocument)) {
            throw new BsonParseException("Field '" + fieldName + "' is not Document");
        }
        return (BsonDocument) value;
    }

    @Override
    public BsonArray getArray(String fieldName) {
        Object value = data.get(fieldName);
        if (value == null) {
            return null;
        }
        if (!(value instanceof BsonArray)) {
            throw new BsonParseException("Field '" + fieldName + "' is not Array");
        }
        return (BsonArray) value;
    }

    @Override
    public String getObjectId(String fieldName) {
        Object value = data.get(fieldName);
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof byte[]) {
            // Convert byte[] to hex string
            byte[] bytes = (byte[]) value;
            if (bytes.length != 12) {
                throw new BsonParseException("ObjectId must be 12 bytes");
            }
            StringBuilder hex = new StringBuilder(24);
            for (int i = 0; i < bytes.length; i++) {
                hex.append(String.format("%02x", bytes[i]));
            }
            return hex.toString();
        }
        throw new BsonParseException("Field '" + fieldName + "' is not ObjectId");
    }

    @Override
    public long getDateTime(String fieldName) {
        Object value = data.get(fieldName);
        if (value == null) {
            throw new BsonParseException("Field '" + fieldName + "' not found");
        }
        if (!(value instanceof Long)) {
            throw new BsonParseException("Field '" + fieldName + "' is not DateTime");
        }
        return ((Long) value).longValue();
    }

    @Override
    public long getDateTime(String fieldName, long defaultValue) {
        if (!data.containsKey(fieldName)) {
            return defaultValue;
        }
        Object value = data.get(fieldName);
        if (value == null || !(value instanceof Long)) {
            return defaultValue;
        }
        return ((Long) value).longValue();
    }

    // ==================== 通用访问 (兼容旧API) ====================

    @Override
    @Deprecated
    public Object get(String fieldName) {
        return data.get(fieldName);
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    // ==================== 调试和序列化 ====================

    @Override
    public String toJson() {
        // Simple JSON representation for debugging
        // Note: Not fully compliant JSON, just for debugging
        throw new UnsupportedOperationException("toJson() not implemented yet for Phase 1");
    }

    @Override
    public byte[] toBson() {
        // BSON serialization not needed for Phase 1 benchmarking
        throw new UnsupportedOperationException("toBson() not implemented yet for Phase 1");
    }

    @Override
    public String toString() {
        return "HashMapBsonDocument" + data.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HashMapBsonDocument)) {
            return false;
        }
        HashMapBsonDocument other = (HashMapBsonDocument) obj;
        return data.equals(other.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }
}
