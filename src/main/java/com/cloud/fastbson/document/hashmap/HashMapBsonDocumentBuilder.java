package com.cloud.fastbson.document.hashmap;

import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.document.BsonDocumentBuilder;
import com.cloud.fastbson.util.BsonType;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder for HashMapBsonDocument (Phase 1 style).
 *
 * <p>Creates HashMap-based documents with eager parsing and primitive boxing.
 */
public class HashMapBsonDocumentBuilder implements BsonDocumentBuilder {

    private Map<String, Object> data;
    private Map<String, Byte> types;

    public HashMapBsonDocumentBuilder() {
        this.data = new HashMap<String, Object>();
        this.types = new HashMap<String, Byte>();
    }

    @Override
    public BsonDocumentBuilder estimateSize(int estimatedFields) {
        // Create HashMap with appropriate initial capacity
        int capacity = (int) (estimatedFields / 0.75f) + 1;
        this.data = new HashMap<String, Object>(capacity);
        this.types = new HashMap<String, Byte>(capacity);
        return this;
    }

    @Override
    public void reset() {
        this.data = new HashMap<String, Object>();
        this.types = new HashMap<String, Byte>();
    }

    // ==================== Primitive Types (with boxing) ====================

    @Override
    public BsonDocumentBuilder putInt32(String fieldName, int value) {
        data.put(fieldName, Integer.valueOf(value));  // Boxing
        types.put(fieldName, Byte.valueOf(BsonType.INT32));
        return this;
    }

    @Override
    public BsonDocumentBuilder putInt64(String fieldName, long value) {
        data.put(fieldName, Long.valueOf(value));  // Boxing
        types.put(fieldName, Byte.valueOf(BsonType.INT64));
        return this;
    }

    @Override
    public BsonDocumentBuilder putDouble(String fieldName, double value) {
        data.put(fieldName, Double.valueOf(value));  // Boxing
        types.put(fieldName, Byte.valueOf(BsonType.DOUBLE));
        return this;
    }

    @Override
    public BsonDocumentBuilder putBoolean(String fieldName, boolean value) {
        data.put(fieldName, Boolean.valueOf(value));  // Boxing
        types.put(fieldName, Byte.valueOf(BsonType.BOOLEAN));
        return this;
    }

    // ==================== Reference Types ====================

    @Override
    public BsonDocumentBuilder putString(String fieldName, String value) {
        data.put(fieldName, value);
        types.put(fieldName, Byte.valueOf(BsonType.STRING));
        return this;
    }

    @Override
    public BsonDocumentBuilder putDocument(String fieldName, BsonDocument value) {
        data.put(fieldName, value);
        types.put(fieldName, Byte.valueOf(BsonType.DOCUMENT));
        return this;
    }

    @Override
    public BsonDocumentBuilder putArray(String fieldName, BsonArray value) {
        data.put(fieldName, value);
        types.put(fieldName, Byte.valueOf(BsonType.ARRAY));
        return this;
    }

    @Override
    public BsonDocumentBuilder putObjectId(String fieldName, String hexString) {
        // Store as hex string for simplicity
        data.put(fieldName, hexString);
        types.put(fieldName, Byte.valueOf(BsonType.OBJECT_ID));
        return this;
    }

    @Override
    public BsonDocumentBuilder putDateTime(String fieldName, long timestamp) {
        data.put(fieldName, Long.valueOf(timestamp));  // Boxing
        types.put(fieldName, Byte.valueOf(BsonType.DATE_TIME));
        return this;
    }

    @Override
    public BsonDocumentBuilder putNull(String fieldName) {
        data.put(fieldName, null);
        types.put(fieldName, Byte.valueOf(BsonType.NULL));
        return this;
    }

    @Override
    public BsonDocumentBuilder putBinary(String fieldName, byte subtype, byte[] data) {
        // Store binary data as byte[] for simplicity (ignore subtype for Phase 1)
        this.data.put(fieldName, data);
        types.put(fieldName, Byte.valueOf(BsonType.BINARY));
        return this;
    }

    @Override
    public BsonDocumentBuilder putComplex(String fieldName, byte type, Object value) {
        // Store complex types as-is for Phase 1
        data.put(fieldName, value);
        types.put(fieldName, Byte.valueOf(type));
        return this;
    }

    @Override
    public BsonDocument build() {
        if (data == null) {
            throw new IllegalStateException("Builder has already been used");
        }

        HashMapBsonDocument doc = new HashMapBsonDocument(data, types);

        // Mark builder as used
        data = null;
        types = null;

        return doc;
    }
}
