package com.cloud.fastbson.document;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ultra-aggressive test to cover the most stubborn remaining branches.
 *
 * Target branches:
 * - IndexedBsonDocument line 291: return -1 in linearSearch
 * - IndexedBsonDocument line 286-287: backward iteration with no match
 * - IndexedBsonDocument line 262-263: name length mismatch in matchesFieldName
 * - IndexedBsonDocument line 304: ensureCache race condition
 * - IndexedBsonDocument line 484, 594, 607, 722: various cache/branch conditions
 * - IndexedBsonArray line 66, 141, 337, 369, 500: similar conditions
 */
public class IndexedBsonUltraAggressiveCoverageTest {

    /**
     * Cover line 291: linearSearch returns -1 when no match found in collision group.
     *
     * Strategy: Create a document with hash collisions, then search for a field
     * that has the same hash but doesn't exist in the document.
     */
    @Test
    public void testLinearSearch_ReturnsMinusOne_HashCollisionNoMatch() throws Exception {
        // Use known hash collisions: "FB" and "Ea" both have hash 2236
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Add only "FB" field
        buffer.put((byte) 0x10);
        buffer.put("FB\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(100);

        // Add another field with different hash
        buffer.put((byte) 0x10);
        buffer.put("other\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(200);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // Now search for "Ea" which has same hash as "FB" but doesn't exist
        // This should trigger linearSearch and ultimately return -1 at line 291
        assertEquals(2236, "FB".hashCode());
        assertEquals(2236, "Ea".hashCode());

        assertNull(doc.get("Ea"));
        assertFalse(doc.contains("Ea"));

        // Also test with another collision pair: "AaAa" and "BBBB" (hash 2031744)
        buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);
        startPos = buffer.position();
        buffer.putInt(0);

        buffer.put((byte) 0x10);
        buffer.put("AaAa\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(300);

        buffer.put((byte) 0x00);

        endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        bson = Arrays.copyOf(buffer.array(), endPos);

        doc = IndexedBsonDocument.parse(bson);

        // Search for "BBBB" (same hash, doesn't exist)
        assertEquals("AaAa".hashCode(), "BBBB".hashCode());
        assertNull(doc.get("BBBB"));
        assertFalse(doc.contains("BBBB"));
    }

    /**
     * Cover line 286-287: backward iteration in linearSearch with no match.
     *
     * Strategy: Create document where binary search lands on a collision field,
     * but the target field (with same hash) doesn't exist, forcing backward search.
     */
    @Test
    public void testLinearSearch_BackwardIteration_NoMatchFound() throws Exception {
        // Use collision set: FB, Ea (both hash 2236)
        // Add multiple fields to force specific binary search outcome
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Add fields in alphabetical order (will be sorted by hash)
        buffer.put((byte) 0x10);
        buffer.put("FB\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(100);

        buffer.put((byte) 0x10);
        buffer.put("zzzz\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(200);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // Search for "Ea" - same hash as "FB" but doesn't exist
        // This forces backward linearSearch from wherever binary search lands
        assertNull(doc.get("Ea"));

        // Also test searching for third collision string
        // "FD" and "Ec" also collide (hash 2268)
        buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);
        startPos = buffer.position();
        buffer.putInt(0);

        buffer.put((byte) 0x10);
        buffer.put("FD\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(111);

        buffer.put((byte) 0x00);

        endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        bson = Arrays.copyOf(buffer.array(), endPos);

        doc = IndexedBsonDocument.parse(bson);

        // Search for "Ec" (same hash, doesn't exist)
        assertEquals("FD".hashCode(), "Ec".hashCode());
        assertNull(doc.get("Ec"));
    }

    /**
     * Cover line 262-263: matchesFieldName returns false on length mismatch.
     */
    @Test
    public void testMatchesFieldName_LengthMismatch_ReturnsFalse() {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        buffer.put((byte) 0x10);
        buffer.put("abc\0".getBytes(StandardCharsets.UTF_8)); // 3 chars
        buffer.putInt(100);

        buffer.put((byte) 0x10);
        buffer.put("abcde\0".getBytes(StandardCharsets.UTF_8)); // 5 chars
        buffer.putInt(200);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // Search for fields with different lengths
        // This triggers line 262-263 in matchesFieldName
        assertNull(doc.get("ab"));      // 2 chars
        assertNull(doc.get("abcd"));    // 4 chars
        assertNull(doc.get("abcdef"));  // 6 chars

        // Verify correct fields work
        assertEquals(100, doc.getInt32("abc"));
        assertEquals(200, doc.getInt32("abcde"));
    }

    /**
     * Cover line 118: while loop with data[pos] == 0 (empty fields case).
     */
    @Test
    public void testParse_WhileLoop_DataPosEqualsZero() {
        // Create minimal document with immediate terminator
        ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(5); // Length = 5 bytes (4 for length + 1 for terminator)
        buffer.put((byte) 0x00); // Immediate terminator

        byte[] bson = Arrays.copyOf(buffer.array(), 5);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        assertEquals(0, doc.size());
        assertTrue(doc.isEmpty());
    }

    /**
     * Cover line 66 in IndexedBsonArray: while loop with data[pos] == 0.
     */
    @Test
    public void testIndexedBsonArray_Parse_WhileLoop_DataPosEqualsZero() {
        // Create minimal array with immediate terminator
        ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(5); // Length = 5 bytes
        buffer.put((byte) 0x00); // Immediate terminator

        byte[] bson = Arrays.copyOf(buffer.array(), 5);

        IndexedBsonArray array = IndexedBsonArray.parse(bson, 0, 5);

        assertEquals(0, array.size());
        assertTrue(array.isEmpty());
    }

    /**
     * Cover line 484: getArray with cache != null && cache[index] != null.
     */
    @Test
    public void testGetArray_CacheHitBranch() {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Add array field
        buffer.put((byte) 0x04); // Array type
        buffer.put("arr\0".getBytes(StandardCharsets.UTF_8));

        int arrayStart = buffer.position();
        buffer.putInt(0);
        buffer.put((byte) 0x10);
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(777);
        buffer.put((byte) 0x00);
        int arrayEnd = buffer.position();
        buffer.putInt(arrayStart, arrayEnd - arrayStart);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // First access - populate cache
        BsonArray arr1 = doc.getArray("arr");
        assertNotNull(arr1);

        // Second access - should hit cache at line 484
        BsonArray arr2 = doc.getArray("arr");
        assertNotNull(arr2);
        assertSame(arr1, arr2); // Should be same cached instance
    }

    /**
     * Cover line 594: countCached with cache == null.
     */
    @Test
    public void testCountCached_WithNullCache() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        buffer.put((byte) 0x10);
        buffer.put("x\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(42);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // Ensure cache is null
        Field cacheField = IndexedBsonDocument.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(doc, null);

        // Call toString which calls countCached - should return 0 at line 594
        String str = doc.toString();
        assertTrue(str.contains("cached=0"));
    }

    /**
     * Cover line 607: toBson with offset != 0 or length != data.length.
     */
    @Test
    public void testToBson_WithNonZeroOffset() throws Exception {
        // Create document as part of larger array
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);

        // Add some padding
        buffer.put(new byte[100]);

        // Now add document at offset 100
        int startPos = buffer.position();
        buffer.putInt(0);

        buffer.put((byte) 0x10);
        buffer.put("field\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(999);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        byte[] fullArray = Arrays.copyOf(buffer.array(), endPos + 100);

        // Parse with non-zero offset
        int docLength = endPos - startPos;
        IndexedBsonDocument doc = IndexedBsonDocument.parse(fullArray, startPos, docLength);

        // toBson should copy slice at line 611-613
        byte[] result = doc.toBson();
        assertNotNull(result);
        assertEquals(docLength, result.length);

        // Verify it's a copy, not the full array
        assertNotSame(fullArray, result);
    }

    /**
     * Cover line 722: getString with defaultValue - non-STRING type branch.
     */
    @Test
    public void testGetString_WithDefaultValue_WrongType() {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Add int32 field (not string)
        buffer.put((byte) 0x10); // INT32
        buffer.put("num\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(123);

        // Add boolean field (not string)
        buffer.put((byte) 0x08); // BOOLEAN
        buffer.put("flag\0".getBytes(StandardCharsets.UTF_8));
        buffer.put((byte) 1);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // getString with default value on non-STRING type - should return default
        // This tests line 722: type != BsonType.STRING && ...
        String result1 = doc.getString("num", "default1");
        assertEquals("default1", result1);

        String result2 = doc.getString("flag", "default2");
        assertEquals("default2", result2);

        // Non-existent field should also return default
        String result3 = doc.getString("missing", "default3");
        assertEquals("default3", result3);
    }

    /**
     * Cover line 141 in IndexedBsonArray: ensureCache race condition.
     */
    @Test
    public void testIndexedBsonArray_EnsureCache_RaceCondition() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        buffer.put((byte) 0x10);
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(100);

        buffer.put((byte) 0x10);
        buffer.put("1\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(200);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonArray array = IndexedBsonArray.parse(bson, 0, bson.length);

        // Set cache to null via reflection
        Field cacheField = IndexedBsonArray.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(array, null);

        // Access element - triggers ensureCache
        assertEquals(100, array.getInt32(0));

        // Verify cache was created
        Object[] cache = (Object[]) cacheField.get(array);
        assertNotNull(cache);

        // Reset and access again
        cacheField.set(array, null);
        assertEquals(200, array.getInt32(1));

        cache = (Object[]) cacheField.get(array);
        assertNotNull(cache);
    }

    /**
     * Cover line 337 and 369: IndexedBsonArray getDocument and getArray cache hits.
     */
    @Test
    public void testIndexedBsonArray_GetDocument_CacheHit() {
        // Test getDocument cache hit (line 337)
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Add document element
        buffer.put((byte) 0x03); // Document
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));

        int docStart = buffer.position();
        buffer.putInt(0);
        buffer.put((byte) 0x10);
        buffer.put("val\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(555);
        buffer.put((byte) 0x00);
        int docEnd = buffer.position();
        buffer.putInt(docStart, docEnd - docStart);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonArray array = IndexedBsonArray.parse(bson, 0, bson.length);

        // First access
        BsonDocument doc1 = array.getDocument(0);
        assertNotNull(doc1);

        // Second access - cache hit at line 337
        BsonDocument doc2 = array.getDocument(0);
        assertSame(doc1, doc2);
    }

    @Test
    public void testIndexedBsonArray_GetArray_CacheHit() {
        // Test getArray cache hit (line 369)
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Add array element
        buffer.put((byte) 0x04); // Array
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));

        int arrayStart = buffer.position();
        buffer.putInt(0);
        buffer.put((byte) 0x10);
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(888);
        buffer.put((byte) 0x00);
        int arrayEnd = buffer.position();
        buffer.putInt(arrayStart, arrayEnd - arrayStart);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonArray array = IndexedBsonArray.parse(bson, 0, bson.length);

        // First access
        BsonArray nested1 = array.getArray(0);
        assertNotNull(nested1);

        // Second access - cache hit at line 369
        BsonArray nested2 = array.getArray(0);
        assertSame(nested1, nested2);
    }

    /**
     * Cover line 500: IndexedBsonArray countCached with cache == null.
     */
    @Test
    public void testIndexedBsonArray_CountCached_NullCache() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        buffer.put((byte) 0x10);
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(123);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonArray array = IndexedBsonArray.parse(bson, 0, bson.length);

        // Ensure cache is null
        Field cacheField = IndexedBsonArray.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(array, null);

        // Call toString which calls countCached - should return 0 at line 500
        String str = array.toString();
        assertTrue(str.contains("cached=0"));
    }

    /**
     * Cover line 304: IndexedBsonDocument ensureCache - double-check locking.
     */
    @Test
    public void testEnsureCache_DoubleCheckLocking() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        buffer.put((byte) 0x10);
        buffer.put("f1\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(10);

        buffer.put((byte) 0x10);
        buffer.put("f2\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(20);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // Use reflection to set cache to null
        Field cacheField = IndexedBsonDocument.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(doc, null);

        // Access field - triggers ensureCache with cache == null at line 304
        assertEquals(10, doc.getInt32("f1"));

        // Verify cache was created
        Object[] cache = (Object[]) cacheField.get(doc);
        assertNotNull(cache);

        // Access again - should use existing cache
        assertEquals(10, doc.getInt32("f1"));

        // Set cache to null again and access different field
        cacheField.set(doc, null);
        assertEquals(20, doc.getInt32("f2"));

        cache = (Object[]) cacheField.get(doc);
        assertNotNull(cache);
    }
}
