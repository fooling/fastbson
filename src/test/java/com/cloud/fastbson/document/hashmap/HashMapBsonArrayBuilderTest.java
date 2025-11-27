package com.cloud.fastbson.document.hashmap;

import com.cloud.fastbson.document.BsonArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HashMapBsonArrayBuilder测试 - 目标100%分支覆盖率
 *
 * 主要测试build()方法的两个分支：
 * 1. 正常build (data != null)
 * 2. 重复build抛异常 (data == null)
 */
public class HashMapBsonArrayBuilderTest {

    @Test
    public void testBuild_Success() {
        HashMapBsonArrayBuilder builder = new HashMapBsonArrayBuilder();
        builder.addInt32(1);
        builder.addInt32(2);
        builder.addInt32(3);

        BsonArray array = builder.build();
        assertNotNull(array);
        assertEquals(3, array.size());
        assertEquals(1, array.getInt32(0));
        assertEquals(2, array.getInt32(1));
        assertEquals(3, array.getInt32(2));
    }

    @Test
    public void testBuild_CalledTwice_ThrowsException() {
        HashMapBsonArrayBuilder builder = new HashMapBsonArrayBuilder();
        builder.addInt32(42);

        // First build should succeed
        BsonArray array = builder.build();
        assertNotNull(array);

        // Second build should throw exception
        assertThrows(IllegalStateException.class, () -> {
            builder.build();
        });
    }

    @Test
    public void testFluentAPI() {
        HashMapBsonArrayBuilder builder = new HashMapBsonArrayBuilder();
        BsonArray array = builder
                .addInt32(42)
                .addInt64(123L)
                .addDouble(3.14)
                .addBoolean(true)
                .addString("value")
                .addNull()
                .build();

        assertNotNull(array);
        assertEquals(6, array.size());
        assertEquals(42, array.getInt32(0));
        assertEquals(123L, array.getInt64(1));
        assertEquals(3.14, array.getDouble(2));
        assertTrue(array.getBoolean(3));
        assertEquals("value", array.getString(4));
        assertNull(array.get(5));
    }

    @Test
    public void testEstimateSize() {
        HashMapBsonArrayBuilder builder = new HashMapBsonArrayBuilder();
        builder.estimateSize(100);  // Pre-allocate for 100 elements
        builder.addInt32(42);

        BsonArray array = builder.build();
        assertNotNull(array);
        assertEquals(1, array.size());
        assertEquals(42, array.getInt32(0));
    }

    @Test
    public void testReset() {
        HashMapBsonArrayBuilder builder = new HashMapBsonArrayBuilder();
        builder.addInt32(42);
        builder.reset();
        builder.addString("Alice");

        BsonArray array = builder.build();
        assertNotNull(array);
        assertEquals(1, array.size());
        assertEquals("Alice", array.getString(0));
    }

    @Test
    public void testAllTypes() {
        HashMapBsonArrayBuilder builder = new HashMapBsonArrayBuilder();

        HashMapBsonDocument nestedDoc = new HashMapBsonDocument();
        HashMapBsonArray nestedArray = new HashMapBsonArray();
        byte[] binaryData = new byte[]{1, 2, 3};

        builder.addInt32(42);
        builder.addInt64(123L);
        builder.addDouble(3.14);
        builder.addBoolean(true);
        builder.addString("value");
        builder.addDocument(nestedDoc);
        builder.addArray(nestedArray);
        builder.addObjectId("507f1f77bcf86cd799439011");
        builder.addDateTime(1638360000000L);
        builder.addNull();
        builder.addBinary((byte)0, binaryData);

        BsonArray array = builder.build();
        assertNotNull(array);
        assertEquals(11, array.size());
        assertEquals(42, array.getInt32(0));
        assertEquals(123L, array.getInt64(1));
        assertEquals(3.14, array.getDouble(2));
        assertTrue(array.getBoolean(3));
        assertEquals("value", array.getString(4));
        assertNotNull(array.getDocument(5));
        assertNotNull(array.getArray(6));
        assertEquals("507f1f77bcf86cd799439011", array.get(7));
        assertEquals(1638360000000L, array.getInt64(8));
        assertNull(array.get(9));
        assertNotNull(array.get(10));
    }
}
