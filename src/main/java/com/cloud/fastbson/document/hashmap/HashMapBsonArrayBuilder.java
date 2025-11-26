package com.cloud.fastbson.document.hashmap;

import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.document.BsonArrayBuilder;
import com.cloud.fastbson.document.BsonDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for HashMapBsonArray (Phase 1 style).
 *
 * <p>Creates ArrayList-based arrays with eager parsing and primitive boxing.
 */
public class HashMapBsonArrayBuilder implements BsonArrayBuilder {

    private List<Object> data;

    public HashMapBsonArrayBuilder() {
        this.data = new ArrayList<Object>();
    }

    @Override
    public BsonArrayBuilder estimateSize(int estimatedElements) {
        this.data = new ArrayList<Object>(estimatedElements);
        return this;
    }

    @Override
    public void reset() {
        this.data = new ArrayList<Object>();
    }

    // ==================== Primitive Types (with boxing) ====================

    @Override
    public BsonArrayBuilder addInt32(int value) {
        data.add(Integer.valueOf(value));  // Boxing
        return this;
    }

    @Override
    public BsonArrayBuilder addInt64(long value) {
        data.add(Long.valueOf(value));  // Boxing
        return this;
    }

    @Override
    public BsonArrayBuilder addDouble(double value) {
        data.add(Double.valueOf(value));  // Boxing
        return this;
    }

    @Override
    public BsonArrayBuilder addBoolean(boolean value) {
        data.add(Boolean.valueOf(value));  // Boxing
        return this;
    }

    // ==================== Reference Types ====================

    @Override
    public BsonArrayBuilder addString(String value) {
        data.add(value);
        return this;
    }

    @Override
    public BsonArrayBuilder addDocument(BsonDocument value) {
        data.add(value);
        return this;
    }

    @Override
    public BsonArrayBuilder addArray(BsonArray value) {
        data.add(value);
        return this;
    }

    @Override
    public BsonArrayBuilder addNull() {
        data.add(null);
        return this;
    }

    @Override
    public BsonArrayBuilder addObjectId(String hexString) {
        // Store as hex string for simplicity
        data.add(hexString);
        return this;
    }

    @Override
    public BsonArrayBuilder addDateTime(long timestamp) {
        data.add(Long.valueOf(timestamp));  // Boxing
        return this;
    }

    @Override
    public BsonArrayBuilder addBinary(byte subtype, byte[] data) {
        // Store binary data as byte[] for simplicity (ignore subtype for Phase 1)
        this.data.add(data);
        return this;
    }

    @Override
    public BsonArray build() {
        if (data == null) {
            throw new IllegalStateException("Builder has already been used");
        }

        HashMapBsonArray array = new HashMapBsonArray(data);

        // Mark builder as used
        data = null;

        return array;
    }
}
