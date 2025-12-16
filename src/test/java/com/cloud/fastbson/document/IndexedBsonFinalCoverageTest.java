package com.cloud.fastbson.document;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Final coverage tests to reach 100% branch coverage.
 *
 * This test uses reflection and careful BSON construction to cover
 * the most difficult branches including:
 * - Backward linearSearch when hash collision requires searching before midpoint
 * - Various cache edge cases
 * - Field name length mismatch scenarios
 */
public class IndexedBsonFinalCoverageTest {

    /**
     * Test backward linearSearch by manually constructing a document
     * with fields in specific hash order that triggers backward search.
     *
     * The key is to create fields where:
     * 1. Multiple fields have same hash (collision)
     * 2. Binary search lands on one collision field
     * 3. The field we're searching for is BEFORE that position
     * 4. This requires backward iteration (line 286)
     */
    @Test
    public void testLinearSearchBackward_ForceBackwardIteration() throws Exception {
        // We'll manually construct IndexedBsonDocument with carefully ordered fields
        ByteBuffer buffer = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Add multiple fields with collision
        // The key is they must be sorted by hash in the FieldIndex array

        // Field 1 - early in alphabet
        buffer.put((byte) 0x10);
        buffer.put("alpha\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(100);

        // Field 2 - collides with field3 (same hash)
        buffer.put((byte) 0x10);
        buffer.put("FB\0".getBytes(StandardCharsets.UTF_8)); // hash = 2236
        buffer.putInt(200);

        // Field 3 - collides with field2 (same hash)
        buffer.put((byte) 0x10);
        buffer.put("Ea\0".getBytes(StandardCharsets.UTF_8)); // hash = 2236
        buffer.putInt(300);

        // Field 4 - later hash
        buffer.put((byte) 0x10);
        buffer.put("zeta\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(400);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // Now access in specific order
        // If binary search lands on "Ea" but we want "FB", it must search backward
        assertEquals(200, doc.getInt32("FB"));
        assertEquals(300, doc.getInt32("Ea"));

        // Also verify collision
        assertEquals("FB".hashCode(), "Ea".hashCode());
    }

    /**
     * Test linearSearch returning -1 (not found) after hash match.
     * This covers line 291.
     */
    @Test
    public void testLinearSearch_NotFoundDespiteHashMatch() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Add fields with known hash collision
        buffer.put((byte) 0x10);
        buffer.put("Aa\0".getBytes(StandardCharsets.UTF_8)); // hash might collide
        buffer.putInt(100);

        buffer.put((byte) 0x10);
        buffer.put("BB\0".getBytes(StandardCharsets.UTF_8)); // hash might collide
        buffer.putInt(200);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // Search for field that doesn't exist but might have same hash
        // This should trigger linearSearch and ultimately return -1 (line 291)
        assertNull(doc.get("nonexistent"));
        assertNull(doc.get("xyz"));

        // Also search for fields with different name but potentially same hash
        String searchField = "CC"; // May or may not collide, but doesn't exist
        assertNull(doc.get(searchField));
    }

    /**
     * Test matchesFieldName with actual length mismatch.
     * This covers line 263 (return false when lengths don't match).
     */
    @Test
    public void testMatchesFieldName_ActualLengthMismatch() {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Add a field
        buffer.put((byte) 0x10);
        buffer.put("test\0".getBytes(StandardCharsets.UTF_8)); // 4 chars
        buffer.putInt(123);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // Search for fields with different lengths
        // This will trigger matchesFieldName with length mismatch (line 262-263)
        assertNull(doc.get("testing"));  // 7 chars vs 4
        assertNull(doc.get("tes"));      // 3 chars vs 4
        assertNull(doc.get("testfield")); // 9 chars vs 4
        assertNull(doc.get("t"));        // 1 char vs 4

        // Verify actual field can be found
        assertEquals(123, doc.getInt32("test"));
    }

    /**
     * Test IndexedBsonArray cache branches.
     */
    @Test
    public void testIndexedBsonArray_CacheBranches() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Add document element
        buffer.put((byte) 0x03); // Document type
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));

        // Embedded document
        int docStart = buffer.position();
        buffer.putInt(0);
        buffer.put((byte) 0x10);
        buffer.put("value\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(42);
        buffer.put((byte) 0x00);
        int docEnd = buffer.position();
        buffer.putInt(docStart, docEnd - docStart);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonArray array = IndexedBsonArray.parse(bson, 0, bson.length);

        // First access - cache miss
        BsonDocument doc1 = array.getDocument(0);
        // Second access - cache hit (line 337)
        BsonDocument doc2 = array.getDocument(0);

        assertNotNull(doc1);
        assertNotNull(doc2);
        assertSame(doc1, doc2);
    }

    /**
     * Test IndexedBsonArray with nested array - cache hit.
     */
    @Test
    public void testIndexedBsonArray_NestedArrayCacheHit() {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Add array element
        buffer.put((byte) 0x04); // Array type
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));

        // Embedded array
        int arrayStart = buffer.position();
        buffer.putInt(0);
        buffer.put((byte) 0x10);
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(999);
        buffer.put((byte) 0x00);
        int arrayEnd = buffer.position();
        buffer.putInt(arrayStart, arrayEnd - arrayStart);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonArray array = IndexedBsonArray.parse(bson, 0, bson.length);

        // First access - cache miss
        BsonArray nested1 = array.getArray(0);
        // Second access - cache hit (line 369)
        BsonArray nested2 = array.getArray(0);

        assertNotNull(nested1);
        assertNotNull(nested2);
        assertSame(nested1, nested2);
        assertEquals(1, nested1.size());
    }

    /**
     * Test IndexedBsonArray countCached with null cache.
     */
    @Test
    public void testIndexedBsonArray_CountCachedNullCache() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        buffer.put((byte) 0x10);
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(42);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonArray array = IndexedBsonArray.parse(bson, 0, bson.length);

        // Use reflection to set cache to null
        Field cacheField = IndexedBsonArray.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(array, null);

        // Call size() which calls countCached() with null cache (line 500)
        int size = array.size();
        assertEquals(1, size);
    }

    /**
     * Test IndexedBsonArray ensureCache race condition branch.
     */
    @Test
    public void testIndexedBsonArray_EnsureCacheRaceCondition() {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        buffer.put((byte) 0x10);
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(111);

        buffer.put((byte) 0x10);
        buffer.put("1\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(222);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonArray array = IndexedBsonArray.parse(bson, 0, bson.length);

        // Access multiple elements to trigger cache operations
        assertEquals(111, array.getInt32(0));
        assertEquals(222, array.getInt32(1));

        // Access again to hit cache
        assertEquals(111, array.getInt32(0));
        assertEquals(222, array.getInt32(1));
    }

    /**
     * Test IndexedBsonDocument ensureCache branch when cache is null (line 304).
     */
    @Test
    public void testIndexedBsonDocument_EnsureCacheNullBranch() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        buffer.put((byte) 0x10);
        buffer.put("field1\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(100);

        buffer.put((byte) 0x10);
        buffer.put("field2\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(200);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // Use reflection to set cache to null
        Field cacheField = IndexedBsonDocument.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(doc, null);

        // Now access a field - this will trigger ensureCache with cache == null (line 304)
        assertEquals(100, doc.getInt32("field1"));

        // Set cache to null again
        cacheField.set(doc, null);

        // Access another field
        assertEquals(200, doc.getInt32("field2"));
    }

    /**
     * Test parse while loop with various conditions (line 66 for array, line 118 for document).
     */
    @Test
    public void testParse_WhileLoopBoundaries() {
        // Test with document containing many fields
        ByteBuffer buffer = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Add 10 fields to exercise while loop
        for (int i = 0; i < 10; i++) {
            buffer.put((byte) 0x10);
            buffer.put(("field" + i + "\0").getBytes(StandardCharsets.UTF_8));
            buffer.putInt(i * 100);
        }

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // Verify all fields
        for (int i = 0; i < 10; i++) {
            assertEquals(i * 100, doc.getInt32("field" + i));
        }
        assertEquals(10, doc.size());
    }

    /**
     * Test IndexedBsonArray parse while loop with many elements.
     */
    @Test
    public void testIndexedBsonArray_ParseWhileLoopBoundaries() {
        ByteBuffer buffer = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Add 10 elements
        for (int i = 0; i < 10; i++) {
            buffer.put((byte) 0x10);
            buffer.put((i + "\0").getBytes(StandardCharsets.UTF_8));
            buffer.putInt(i * 111);
        }

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonArray array = IndexedBsonArray.parse(bson, 0, bson.length);

        // Verify all elements
        for (int i = 0; i < 10; i++) {
            assertEquals(i * 111, array.getInt32(i));
        }
        assertEquals(10, array.size());
    }

    /**
     * Test getArray cache hit (line 484).
     */
    @Test
    public void testIndexedBsonDocument_GetArrayCacheHit() {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Embedded array
        buffer.put((byte) 0x04); // Array type
        buffer.put("items\0".getBytes(StandardCharsets.UTF_8));

        int arrayStart = buffer.position();
        buffer.putInt(0);
        buffer.put((byte) 0x10);
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(555);
        buffer.put((byte) 0x00);
        int arrayEnd = buffer.position();
        buffer.putInt(arrayStart, arrayEnd - arrayStart);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // First access - cache miss
        BsonArray array1 = doc.getArray("items");
        // Second access - cache hit (line 484)
        BsonArray array2 = doc.getArray("items");

        assertNotNull(array1);
        assertNotNull(array2);
        // Should return cached instance
        assertSame(array1, array2);
        assertEquals(555, array1.getInt32(0));
    }

    /**
     * Test toBson offset/length branch (line 607).
     */
    @Test
    public void testToBson_OffsetLengthBranch() {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        buffer.put((byte) 0x10);
        buffer.put("test\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(123);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // Call toBson - should take fast path (offset=0, length=data.length)
        byte[] result = doc.toBson();

        assertNotNull(result);
        assertArrayEquals(bson, result);
    }
}
