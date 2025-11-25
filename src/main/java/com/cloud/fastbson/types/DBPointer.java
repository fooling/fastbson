package com.cloud.fastbson.types;

/**
 * Represents a BSON DBPointer (deprecated).
 *
 * <p>Structure: string namespace + 12-byte ObjectId
 */
public class DBPointer {
    public final String namespace;
    public final String id;

    public DBPointer(String namespace, String id) {
        this.namespace = namespace;
        this.id = id;
    }
}
