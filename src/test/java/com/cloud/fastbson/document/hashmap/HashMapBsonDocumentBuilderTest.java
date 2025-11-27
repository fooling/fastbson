package com.cloud.fastbson.document.hashmap;

import com.cloud.fastbson.document.BsonDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HashMapBsonDocumentBuilder测试 - 目标100%分支覆盖率
 *
 * 主要测试build()方法的两个分支：
 * 1. 正常build (data != null)
 * 2. 重复build抛异常 (data == null)
 */
public class HashMapBsonDocumentBuilderTest {

    @Test
    public void testBuild_Success() {
        HashMapBsonDocumentBuilder builder = new HashMapBsonDocumentBuilder();
        builder.putInt32("age", 25);
        builder.putString("name", "Alice");

        BsonDocument doc = builder.build();
        assertNotNull(doc);
        assertEquals(25, doc.getInt32("age"));
        assertEquals("Alice", doc.getString("name"));
    }

    @Test
    public void testBuild_CalledTwice_ThrowsException() {
        HashMapBsonDocumentBuilder builder = new HashMapBsonDocumentBuilder();
        builder.putInt32("value", 42);

        // First build should succeed
        BsonDocument doc = builder.build();
        assertNotNull(doc);

        // Second build should throw exception
        assertThrows(IllegalStateException.class, () -> {
            builder.build();
        });
    }

    @Test
    public void testFluentAPI() {
        HashMapBsonDocumentBuilder builder = new HashMapBsonDocumentBuilder();
        BsonDocument doc = builder
                .putInt32("int", 42)
                .putInt64("long", 123L)
                .putDouble("double", 3.14)
                .putBoolean("bool", true)
                .putString("str", "value")
                .putNull("nullField")
                .build();

        assertNotNull(doc);
        assertEquals(42, doc.getInt32("int"));
        assertEquals(123L, doc.getInt64("long"));
        assertEquals(3.14, doc.getDouble("double"));
        assertTrue(doc.getBoolean("bool"));
        assertEquals("value", doc.getString("str"));
        assertTrue(doc.isNull("nullField"));
    }

    @Test
    public void testEstimateSize() {
        HashMapBsonDocumentBuilder builder = new HashMapBsonDocumentBuilder();
        builder.estimateSize(100);  // Pre-allocate for 100 fields
        builder.putInt32("value", 42);

        BsonDocument doc = builder.build();
        assertNotNull(doc);
        assertEquals(42, doc.getInt32("value"));
    }

    @Test
    public void testReset() {
        HashMapBsonDocumentBuilder builder = new HashMapBsonDocumentBuilder();
        builder.putInt32("value", 42);
        builder.reset();
        builder.putString("name", "Alice");

        BsonDocument doc = builder.build();
        assertNotNull(doc);
        assertEquals("Alice", doc.getString("name"));
        // Previous value should not exist
        assertThrows(Exception.class, () -> doc.getInt32("value"));
    }

    @Test
    public void testAllTypes() {
        HashMapBsonDocumentBuilder builder = new HashMapBsonDocumentBuilder();

        HashMapBsonDocument nestedDoc = new HashMapBsonDocument();
        HashMapBsonArray nestedArray = new HashMapBsonArray();
        byte[] binaryData = new byte[]{1, 2, 3};

        builder.putInt32("int32", 42);
        builder.putInt64("int64", 123L);
        builder.putDouble("double", 3.14);
        builder.putBoolean("boolean", true);
        builder.putString("string", "value");
        builder.putDocument("document", nestedDoc);
        builder.putArray("array", nestedArray);
        builder.putObjectId("objectId", "507f1f77bcf86cd799439011");
        builder.putDateTime("dateTime", 1638360000000L);
        builder.putNull("null");
        builder.putBinary("binary", (byte)0, binaryData);

        BsonDocument doc = builder.build();
        assertNotNull(doc);
        assertEquals(42, doc.getInt32("int32"));
        assertEquals(123L, doc.getInt64("int64"));
        assertEquals(3.14, doc.getDouble("double"));
        assertTrue(doc.getBoolean("boolean"));
        assertEquals("value", doc.getString("string"));
        assertNotNull(doc.getDocument("document"));
        assertNotNull(doc.getArray("array"));
        assertEquals("507f1f77bcf86cd799439011", doc.getObjectId("objectId"));
        assertEquals(1638360000000L, doc.getDateTime("dateTime"));
        assertTrue(doc.isNull("null"));
    }
}
