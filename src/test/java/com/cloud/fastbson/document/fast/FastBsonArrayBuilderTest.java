package com.cloud.fastbson.document.fast;

import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.util.BsonType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FastBsonArrayBuilder测试 - 目标100%分支覆盖率
 *
 * 主要测试分支：
 * 1. build()方法的types == null检查（重复build抛异常）
 * 2. reset()方法的types == null检查（build后reset重新初始化）
 * 3. addBoolean的true/false分支
 */
public class FastBsonArrayBuilderTest {

    @Test
    public void testBuild_Success() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
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
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt32(42);

        // First build should succeed
        BsonArray array = builder.build();
        assertNotNull(array);

        // Second build should throw exception (types == null branch)
        assertThrows(IllegalStateException.class, () -> {
            builder.build();
        });
    }

    @Test
    public void testReset_BeforeBuild() {
        // Test reset when types != null (clear path)
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt32(42);
        builder.reset();  // This should clear the collections
        builder.addString("Alice");

        BsonArray array = builder.build();
        assertNotNull(array);
        assertEquals(1, array.size());
        assertEquals("Alice", array.getString(0));
    }

    @Test
    public void testReset_AfterBuild() {
        // Test reset when types == null (re-initialize path)
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt32(42);
        builder.build();  // types becomes null

        builder.reset();  // This should re-initialize
        builder.addString("Bob");

        BsonArray array = builder.build();
        assertNotNull(array);
        assertEquals(1, array.size());
        assertEquals("Bob", array.getString(0));
    }

    @Test
    public void testAddBoolean_True() {
        // Test if (value) branch
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addBoolean(true);

        BsonArray array = builder.build();
        assertNotNull(array);
        assertEquals(1, array.size());
        assertTrue(array.getBoolean(0));
    }

    @Test
    public void testAddBoolean_False() {
        // Test else branch (value == false)
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addBoolean(false);

        BsonArray array = builder.build();
        assertNotNull(array);
        assertEquals(1, array.size());
        assertFalse(array.getBoolean(0));
    }

    @Test
    public void testFluentAPI() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
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
    }

    @Test
    public void testEstimateSize() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.estimateSize(100);  // Pre-allocate for 100 elements
        builder.addInt32(42);

        BsonArray array = builder.build();
        assertNotNull(array);
        assertEquals(1, array.size());
        assertEquals(42, array.getInt32(0));
    }

    @Test
    public void testAllTypes() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();

        FastBsonDocument nestedDoc = (FastBsonDocument) new FastBsonDocumentBuilder().build();
        FastBsonArray nestedArray = (FastBsonArray) new FastBsonArrayBuilder().build();
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
        // Index 8 is DateTime, use get() or check via type
        assertEquals(BsonType.DATE_TIME, array.getType(8));
        // Index 9 is null
        // Index 10 is Binary data
        assertNotNull(array.get(10));
    }

    @Test
    public void testMixedBooleans() {
        // Test multiple boolean values to ensure BitSet works correctly
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addBoolean(true);
        builder.addBoolean(false);
        builder.addBoolean(true);
        builder.addBoolean(false);

        BsonArray array = builder.build();
        assertEquals(4, array.size());
        assertTrue(array.getBoolean(0));
        assertFalse(array.getBoolean(1));
        assertTrue(array.getBoolean(2));
        assertFalse(array.getBoolean(3));
    }

    @Test
    public void testResetAfterBuildThenAddMultipleElements() {
        // Test that reset after build properly re-initializes all collections
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt32(1).addString("old");
        builder.build();

        builder.reset();
        builder.addInt32(100);
        builder.addInt64(200L);
        builder.addDouble(3.14);
        builder.addBoolean(true);
        builder.addString("new");

        BsonArray array = builder.build();
        assertEquals(5, array.size());
        assertEquals(100, array.getInt32(0));
        assertEquals(200L, array.getInt64(1));
        assertEquals(3.14, array.getDouble(2));
        assertTrue(array.getBoolean(3));
        assertEquals("new", array.getString(4));
    }
}
