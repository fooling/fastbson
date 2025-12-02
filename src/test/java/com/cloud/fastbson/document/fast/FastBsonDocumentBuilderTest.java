package com.cloud.fastbson.document.fast;

import com.cloud.fastbson.document.BsonDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FastBsonDocumentBuilder测试 - 目标100%分支覆盖率
 *
 * 主要测试分支：
 * 1. build()方法的fieldNameToId == null检查（重复build抛异常）
 * 2. reset()方法的fieldNameToId == null检查（build后reset重新初始化）
 * 3. estimateSize设置
 */
public class FastBsonDocumentBuilderTest {

    @Test
    public void testBuild_Success() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putInt32("a", 1);
        builder.putInt32("b", 2);
        builder.putInt32("c", 3);

        BsonDocument doc = builder.build();
        assertNotNull(doc);
        assertEquals(3, doc.size());
        assertEquals(1, doc.getInt32("a"));
        assertEquals(2, doc.getInt32("b"));
        assertEquals(3, doc.getInt32("c"));
    }

    @Test
    public void testBuild_CalledTwice_ThrowsException() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putInt32("value", 42);

        // First build should succeed
        BsonDocument doc = builder.build();
        assertNotNull(doc);

        // Second build should throw exception (fieldNameToId == null branch)
        assertThrows(IllegalStateException.class, () -> {
            builder.build();
        });
    }

    @Test
    public void testReset_BeforeBuild() {
        // Test reset when fieldNameToId != null (clear path)
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putInt32("old", 42);
        builder.reset();  // This should clear the collections
        builder.putString("new", "Alice");

        BsonDocument doc = builder.build();
        assertNotNull(doc);
        assertEquals(1, doc.size());
        assertFalse(doc.contains("old"));
        assertEquals("Alice", doc.getString("new"));
    }

    @Test
    public void testReset_AfterBuild() {
        // Test reset when fieldNameToId == null (re-initialize path)
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putInt32("old", 42);
        builder.build();  // fieldNameToId becomes null

        builder.reset();  // This should re-initialize
        builder.putString("new", "Bob");

        BsonDocument doc = builder.build();
        assertNotNull(doc);
        assertEquals(1, doc.size());
        assertFalse(doc.contains("old"));
        assertEquals("Bob", doc.getString("new"));
    }

    @Test
    public void testFluentAPI() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        BsonDocument doc = builder
                .putInt32("intVal", 42)
                .putInt64("longVal", 123L)
                .putDouble("doubleVal", 3.14)
                .putBoolean("boolVal", true)
                .putString("strVal", "value")
                .putNull("nullVal")
                .build();

        assertNotNull(doc);
        assertEquals(6, doc.size());
        assertEquals(42, doc.getInt32("intVal"));
        assertEquals(123L, doc.getInt64("longVal"));
        assertEquals(3.14, doc.getDouble("doubleVal"));
        assertTrue(doc.getBoolean("boolVal"));
        assertEquals("value", doc.getString("strVal"));
        assertTrue(doc.isNull("nullVal"));
    }

    @Test
    public void testEstimateSize() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.estimateSize(100);  // Pre-allocate for 100 fields
        builder.putInt32("value", 42);

        BsonDocument doc = builder.build();
        assertNotNull(doc);
        assertEquals(1, doc.size());
        assertEquals(42, doc.getInt32("value"));
    }

    @Test
    public void testAllTypes() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();

        FastBsonDocument nestedDoc = (FastBsonDocument) new FastBsonDocumentBuilder().build();
        FastBsonArray nestedArray = (FastBsonArray) new FastBsonArrayBuilder().build();
        byte[] binaryData = new byte[]{1, 2, 3};

        builder.putInt32("int32", 42);
        builder.putInt64("int64", 123L);
        builder.putDouble("double", 3.14);
        builder.putBoolean("bool", true);
        builder.putString("string", "value");
        builder.putDocument("doc", nestedDoc);
        builder.putArray("array", nestedArray);
        builder.putObjectId("oid", "507f1f77bcf86cd799439011");
        builder.putDateTime("datetime", 1638360000000L);
        builder.putNull("null");
        builder.putBinary("binary", (byte)0, binaryData);

        BsonDocument doc = builder.build();
        assertNotNull(doc);
        assertEquals(11, doc.size());
        assertEquals(42, doc.getInt32("int32"));
        assertEquals(123L, doc.getInt64("int64"));
        assertEquals(3.14, doc.getDouble("double"));
        assertTrue(doc.getBoolean("bool"));
        assertEquals("value", doc.getString("string"));
        assertNotNull(doc.getDocument("doc"));
        assertNotNull(doc.getArray("array"));
        assertEquals("507f1f77bcf86cd799439011", doc.getObjectId("oid"));
        assertEquals(1638360000000L, doc.getDateTime("datetime"));
        assertTrue(doc.isNull("null"));
        // Binary data is stored
        assertTrue(doc.contains("binary"));
    }

    @Test
    public void testOverwriteField() {
        // Test that putting the same field name twice overwrites the value
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putInt32("value", 42);
        builder.putInt32("value", 100);  // Overwrite

        BsonDocument doc = builder.build();
        assertEquals(1, doc.size());
        assertEquals(100, doc.getInt32("value"));
    }

    @Test
    public void testResetAfterBuildThenAddMultipleFields() {
        // Test that reset after build properly re-initializes all collections
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putInt32("old1", 1).putString("old2", "old");
        builder.build();

        builder.reset();
        builder.putInt32("new1", 100);
        builder.putInt64("new2", 200L);
        builder.putDouble("new3", 3.14);
        builder.putBoolean("new4", true);
        builder.putString("new5", "new");

        BsonDocument doc = builder.build();
        assertEquals(5, doc.size());
        assertFalse(doc.contains("old1"));
        assertFalse(doc.contains("old2"));
        assertEquals(100, doc.getInt32("new1"));
        assertEquals(200L, doc.getInt64("new2"));
        assertEquals(3.14, doc.getDouble("new3"));
        assertTrue(doc.getBoolean("new4"));
        assertEquals("new", doc.getString("new5"));
    }

    @Test
    public void testBooleanFalse() {
        // Ensure false boolean value is properly stored
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putBoolean("falseVal", false);
        builder.putBoolean("trueVal", true);

        BsonDocument doc = builder.build();
        assertEquals(2, doc.size());
        assertFalse(doc.getBoolean("falseVal"));
        assertTrue(doc.getBoolean("trueVal"));
    }
}
