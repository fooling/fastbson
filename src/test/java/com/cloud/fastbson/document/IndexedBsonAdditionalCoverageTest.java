package com.cloud.fastbson.document;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional coverage tests to reach 100% branch coverage.
 *
 * This test specifically targets:
 * 1. Backward linearSearch branches (lines 286-291)
 * 2. Cache hit scenarios for various types
 * 3. Field name length mismatch (line 263)
 * 4. Other edge cases
 */
public class IndexedBsonAdditionalCoverageTest {

    /**
     * Test backward search by creating document where binary search lands
     * on a field with collision, but target is BEFORE that position.
     *
     * This requires careful ordering of fields by hash code.
     */
    @Test
    public void testLinearSearch_BackwardBranch_DirectTest() throws Exception {
        // Create BSON with fields that will trigger backward search
        // We need fields sorted by hash, with a collision group

        ByteBuffer buffer = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0); // placeholder

        // Use strings that we know collide: FB and Ea both hash to 2236
        String field1 = "FB";  // hash = 2236
        String field2 = "Ea";  // hash = 2236 (same as FB!)
        String field3 = "DD";  // hash = 2145 (different)

        // Verify collision
        if (field1.hashCode() != field2.hashCode()) {
            // Fallback if hashes changed
            field1 = "Aa";
            field2 = "BB";
            field3 = "CC";
        }

        // Add fields in order: field1, field2, field3
        // After sorting by hash, if field2's hash > field1's hash,
        // binary search might land on field2, requiring backward search to find field1

        buffer.put((byte) 0x10);
        buffer.put((field1 + "\0").getBytes(StandardCharsets.UTF_8));
        buffer.putInt(100);

        buffer.put((byte) 0x10);
        buffer.put((field2 + "\0").getBytes(StandardCharsets.UTF_8));
        buffer.putInt(200);

        buffer.put((byte) 0x10);
        buffer.put((field3 + "\0").getBytes(StandardCharsets.UTF_8));
        buffer.putInt(300);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        // Parse document
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // Now we need to access the fields in a specific order to trigger backward search
        // Access field2 first (might be in middle), then field1 (requires backward search)
        assertEquals(200, doc.getInt32(field2));
        assertEquals(100, doc.getInt32(field1));
        assertEquals(300, doc.getInt32(field3));
    }

    /**
     * Test backward search with multiple collision fields.
     * Create a scenario where binary search lands in middle of collision group.
     */
    @Test
    public void testLinearSearch_BackwardWithMultipleCollisions() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Create 4 fields: field0, field1 (collide), field2, field3 (collide with 0,1)
        // When searching, binary search should land in middle, requiring both forward and backward
        String[] fields = {"Aa", "BB", "CC", "DD"};  // These may collide in pairs

        // Add all fields
        for (int i = 0; i < fields.length; i++) {
            buffer.put((byte) 0x10);
            buffer.put((fields[i] + "\0").getBytes(StandardCharsets.UTF_8));
            buffer.putInt(100 + i);
        }

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // Access all fields
        for (int i = 0; i < fields.length; i++) {
            assertEquals(100 + i, doc.getInt32(fields[i]));
        }
    }

    /**
     * Test field name length mismatch branch (line 263).
     * This happens when searching for a field with different length than found field.
     */
    @Test
    public void testMatchesFieldName_LengthMismatch() {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Add a field
        buffer.put((byte) 0x10);
        buffer.put("short\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(123);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // Search for field with different length
        assertNull(doc.get("shorter"));   // Longer name
        assertNull(doc.get("shrt"));      // Shorter name
        assertNull(doc.get("shortfield")); // Much longer name
    }

    /**
     * Test cache hit for Double values (line 380).
     * Access same Double field twice to hit cache.
     */
    @Test
    public void testDoubleCache_Hit() {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        buffer.put((byte) 0x01); // Double
        buffer.put("value\0".getBytes(StandardCharsets.UTF_8));
        buffer.putDouble(3.14159);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // First access - miss
        double value1 = doc.getDouble("value");
        // Second access - hit cache (line 380)
        double value2 = doc.getDouble("value");

        assertEquals(3.14159, value1, 0.00001);
        assertEquals(3.14159, value2, 0.00001);
    }

    /**
     * Test cache hit for Boolean values (line 406).
     */
    @Test
    public void testBooleanCache_Hit() {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        buffer.put((byte) 0x08); // Boolean
        buffer.put("flag\0".getBytes(StandardCharsets.UTF_8));
        buffer.put((byte) 1); // true

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // First access - miss
        boolean value1 = doc.getBoolean("flag");
        // Second access - hit cache (line 406)
        boolean value2 = doc.getBoolean("flag");

        assertTrue(value1);
        assertTrue(value2);
    }

    /**
     * Test cache hit for DateTime values (line 741).
     */
    @Test
    public void testDateTimeCache_Hit() {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        buffer.put((byte) 0x09); // DateTime
        buffer.put("timestamp\0".getBytes(StandardCharsets.UTF_8));
        buffer.putLong(1234567890000L);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // First access - miss
        long value1 = doc.getDateTime("timestamp");
        // Second access - hit cache (line 741)
        long value2 = doc.getDateTime("timestamp");

        assertEquals(1234567890000L, value1);
        assertEquals(1234567890000L, value2);
    }

    /**
     * Test cache hit for ObjectId values (line 775).
     */
    @Test
    public void testObjectIdCache_Hit() {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        buffer.put((byte) 0x07); // ObjectId
        buffer.put("_id\0".getBytes(StandardCharsets.UTF_8));
        buffer.put(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12}); // 12 bytes

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // First access - miss
        String value1 = doc.getObjectId("_id");
        // Second access - hit cache (line 775)
        String value2 = doc.getObjectId("_id");

        assertNotNull(value1);
        assertEquals(value1, value2);
    }

    /**
     * Test getString with JavaScript and Symbol types (line 722).
     */
    @Test
    public void testGetString_JavaScriptType() {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // JavaScript type (0x0D)
        buffer.put((byte) 0x0D);
        buffer.put("code\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(15); // length including null
        buffer.put("console.log();\0".getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // Should be able to get JavaScript as string
        String code = doc.getString("code");
        assertEquals("console.log();", code);

        // Also test with default value version
        String code2 = doc.getString("code", "default");
        assertEquals("console.log();", code2);
    }

    /**
     * Test getString with Symbol type (line 722).
     */
    @Test
    public void testGetString_SymbolType() {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Symbol type (0x0E)
        buffer.put((byte) 0x0E);
        buffer.put("sym\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(6); // length including null
        buffer.put("value\0".getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // Should be able to get Symbol as string
        String symbol = doc.getString("sym");
        assertEquals("value", symbol);
    }

    /**
     * Test countCached when cache is null (line 594).
     */
    @Test
    public void testCountCached_NullCache() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        buffer.put((byte) 0x10);
        buffer.put("field\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(123);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // Use reflection to ensure cache is null
        Field cacheField = IndexedBsonDocument.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(doc, null);

        // Call size() which calls countCached() - should handle null cache
        int size = doc.size();
        assertEquals(1, size);
    }

    /**
     * Test toBson when offset is not 0 or length doesn't match data.length (line 607).
     */
    @Test
    public void testToBson_WithOffsetAndLength() throws Exception {
        // Create a larger byte array with BSON in the middle
        byte[] largeArray = new byte[1024];
        int offset = 100;

        ByteBuffer buffer = ByteBuffer.wrap(largeArray, offset, 512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        buffer.put((byte) 0x10);
        buffer.put("value\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(999);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        int length = endPos - startPos;

        // Write correct length
        ByteBuffer.wrap(largeArray, offset, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(length);

        // Use reflection to create document with specific offset and length
        Field dataField = IndexedBsonDocument.class.getDeclaredField("data");
        Field offsetField = IndexedBsonDocument.class.getDeclaredField("offset");
        Field lengthField = IndexedBsonDocument.class.getDeclaredField("length");

        dataField.setAccessible(true);
        offsetField.setAccessible(true);
        lengthField.setAccessible(true);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(largeArray, offset, length);

        // Call toBson - should create a copy (line 607-608)
        byte[] bson = doc.toBson();

        // Verify
        assertNotNull(bson);
        assertEquals(length, bson.length);
        assertEquals(999, doc.getInt32("value"));
    }

    /**
     * Test cache hit for getArray (line 484).
     */
    @Test
    public void testArrayCache_Hit() {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Embedded array
        buffer.put((byte) 0x04); // Array type
        buffer.put("items\0".getBytes(StandardCharsets.UTF_8));

        // Array content
        int arrayStart = buffer.position();
        buffer.putInt(0); // placeholder for array length

        buffer.put((byte) 0x10); // Int32
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(42);

        buffer.put((byte) 0x00); // End of array

        int arrayEnd = buffer.position();
        buffer.putInt(arrayStart, arrayEnd - arrayStart);

        buffer.put((byte) 0x00); // End of document

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // First access - miss
        BsonArray array1 = doc.getArray("items");
        // Second access - hit cache (line 484)
        BsonArray array2 = doc.getArray("items");

        assertNotNull(array1);
        assertNotNull(array2);
        assertSame(array1, array2); // Should be same object from cache
        assertEquals(1, array1.size());
        assertEquals(42, array1.getInt32(0));
    }

    /**
     * Test parse with while loop early exit (line 118).
     */
    @Test
    public void testParse_EmptyDocument() {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // No fields, just terminator
        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // Should be empty
        assertTrue(doc.isEmpty());
        assertEquals(0, doc.size());
    }

    /**
     * Test ensureCache second branch (line 304) - race condition handling.
     */
    @Test
    public void testEnsureCache_ConcurrentAccess() throws Exception {
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

        // Access both fields to trigger cache creation
        assertEquals(100, doc.getInt32("field1"));
        assertEquals(200, doc.getInt32("field2"));

        // Access again to hit cache paths
        assertEquals(100, doc.getInt32("field1"));
        assertEquals(200, doc.getInt32("field2"));
    }

    /**
     * Test linearSearch backward hash mismatch exit (line 290).
     * Create document where binary search lands on a collision group,
     * but backward search exits via hash change instead of bounds.
     *
     * To trigger this, we need:
     * - Fields sorted by hash: [lowHash][midHash][midHash]
     * - Binary search lands on index 2 (second midHash)
     * - Backward search checks index 1 (first midHash) - hash matches, name doesn't match
     * - Then checks index 0 (lowHash) - hash DOESN'T match -> break (line 291)
     */
    @Test
    public void testLinearSearch_BackwardHashMismatch() throws Exception {
        // Find two strings with same hash for collision
        String str1 = "FB";  // hash code
        String str2 = "Ea";  // same hash as FB (both hash to 2236 in Java)
        String str3 = "AA";  // different hash

        // Verify we have a collision
        if (str1.hashCode() != str2.hashCode()) {
            // If hash algorithm changed, skip this test
            System.out.println("Hash collision not found, skipping backward hash mismatch test");
            return;
        }

        ByteBuffer buffer = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Add fields in an order that will create the right scenario after sorting
        // We want: [str3 (low hash)][str1 (mid hash)][str2 (mid hash, same as str1)]

        // Add str3 first
        buffer.put((byte) 0x10);
        buffer.put((str3 + "\0").getBytes(StandardCharsets.UTF_8));
        buffer.putInt(300);

        // Add str1
        buffer.put((byte) 0x10);
        buffer.put((str1 + "\0").getBytes(StandardCharsets.UTF_8));
        buffer.putInt(100);

        // Add str2 (collision with str1)
        buffer.put((byte) 0x10);
        buffer.put((str2 + "\0").getBytes(StandardCharsets.UTF_8));
        buffer.putInt(200);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // Now search for str1
        // Binary search will find a field with matching hash
        // If it lands on str2, it needs to search backward
        // It will check str1 (hash match), then str3 (hash mismatch) -> break!
        assertEquals(100, doc.getInt32(str1));
        assertEquals(200, doc.getInt32(str2));
        assertEquals(300, doc.getInt32(str3));

        // Try accessing in different order to potentially trigger different binary search landing points
        assertEquals(200, doc.getInt32(str2));
        assertEquals(100, doc.getInt32(str1));
    }

    /**
     * Test linearSearch backward hash mismatch using reflection.
     * This directly tests the branch at line 290: if (fields[i].nameHash != hash) break;
     */
    @Test
    public void testLinearSearch_BackwardHashMismatch_ViaReflection() throws Exception {
        // Create a document with multiple fields that we can use for testing
        ByteBuffer buffer = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Create 5 fields with known hash values
        // After sorting by hash: [field0][field1][field2][field3][field4]
        String[] fields = {"field0", "field1", "field2", "field3", "field4"};

        for (int i = 0; i < fields.length; i++) {
            buffer.put((byte) 0x10);
            buffer.put((fields[i] + "\0").getBytes(StandardCharsets.UTF_8));
            buffer.putInt(i * 100);
        }

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // Use reflection to access the private linearSearch method
        java.lang.reflect.Method linearSearchMethod = IndexedBsonDocument.class.getDeclaredMethod(
            "linearSearch", int.class, String.class, int.class);
        linearSearchMethod.setAccessible(true);

        // Get the fields array to understand the structure
        Field fieldsField = IndexedBsonDocument.class.getDeclaredField("fields");
        fieldsField.setAccessible(true);
        Object[] docFields = (Object[]) fieldsField.get(doc);

        // Call linearSearch with parameters that will cause backward search to hit hash mismatch
        // Start at index 2, search for a field with a different hash
        // This should cause the backward loop to check index 1 (different hash) and break
        int result = (Integer) linearSearchMethod.invoke(doc, 2, "nonexistent", "field2".hashCode());
        assertEquals(-1, result);  // Should return -1 (not found)
    }
}
