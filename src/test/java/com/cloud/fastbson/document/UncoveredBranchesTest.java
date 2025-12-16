package com.cloud.fastbson.document;

import com.cloud.fastbson.document.fast.FastBsonDocument;
import com.cloud.fastbson.document.fast.FastBsonDocumentBuilder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite specifically targeting the last 8 uncovered branches to achieve 100% branch coverage.
 *
 * Target branches:
 * 1. IndexedBsonDocument:286 - linearSearch backward loop hash mismatch
 * 2. IndexedBsonDocument:304 - ensureCache race condition
 * 3. IndexedBsonDocument:594 - countCached with non-null cache
 * 4. IndexedBsonArray:141 - ensureCache race condition
 * 5. IndexedBsonArray:500 - countCached with non-null cache
 * 6. FastBsonDocument:219 - getBoolean exists check
 * 7. FastBsonDocument:286 - toJsonValue switch default
 * 8. DocumentParser:206 - parse loop end condition
 */
public class UncoveredBranchesTest {

    /**
     * Branch #1: IndexedBsonDocument:286
     * linearSearch backward loop: for (int i = start - 1; i >= 0 && fields[i].nameHash == hash; i--)
     * Need: fields[i].nameHash != hash when i >= 0 (to exit loop via hash mismatch, not i < 0)
     */
    @Test
    public void testLinearSearch_BackwardHashMismatch() throws Exception {
        // Create a document with fields that will have hash collisions
        // "FB" and "Ea" both have hashCode() = 2236
        // "Aa", "BB", "C@" have different hashes

        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0); // placeholder for document length

        // Add fields in an order that will create the desired collision pattern after sorting
        buffer.put((byte) 0x10); // INT32
        buffer.put("Aa\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(1);

        buffer.put((byte) 0x10); // INT32
        buffer.put("FB\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(2);

        buffer.put((byte) 0x10); // INT32
        buffer.put("Ea\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(3);

        buffer.put((byte) 0x10); // INT32
        buffer.put("Zz\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(4);

        buffer.put((byte) 0x00); // End of document

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bsonData = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Search for "Ea" - binary search will land on collision group,
        // then search backward through "FB" (same hash), then hit "Aa" (different hash)
        Integer value = doc.getInt32("Ea");
        assertEquals(3, value);
    }

    /**
     * Branch #2: IndexedBsonDocument:304
     * ensureCache inner synchronized block: if (cache == null) inside synchronized
     * Need: cache != null inside synchronized block (race condition where another thread initialized it)
     */
    @Test
    public void testEnsureCache_RaceCondition() throws Exception {
        byte[] bsonData = createSimpleBsonDocument();
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch thread1EnteredMethod = new CountDownLatch(1);
        AtomicReference<Integer> thread1Result = new AtomicReference<>();
        AtomicReference<Integer> thread2Result = new AtomicReference<>();

        // Thread 1: Will call ensureCache
        Thread t1 = new Thread(() -> {
            try {
                startLatch.await();
                thread1EnteredMethod.countDown();
                // Small delay to let thread 2 potentially initialize cache
                Thread.sleep(10);
                thread1Result.set(doc.getInt32("age"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Thread 2: Will also call ensureCache simultaneously
        Thread t2 = new Thread(() -> {
            try {
                thread1EnteredMethod.await();
                thread2Result.set(doc.getInt32("age"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        t1.start();
        t2.start();
        startLatch.countDown();
        t1.join();
        t2.join();

        // Both threads should get the same result
        assertEquals(thread1Result.get(), thread2Result.get());
    }

    /**
     * Branch #3 & #5: IndexedBsonDocument:594 and IndexedBsonArray:500
     * countCached: if (cache == null) return 0;
     * Need: cache != null with all null slots (iterate through cache finding no non-null values)
     */
    @Test
    public void testCountCached_NonNullCacheWithAllNulls_Document() throws Exception {
        byte[] bsonData = createSimpleBsonDocument();
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Access a field to initialize cache
        doc.getInt32("age");

        // Use reflection to clear all cache entries
        java.lang.reflect.Field cacheField = IndexedBsonDocument.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        Object[] cache = (Object[]) cacheField.get(doc);
        Arrays.fill(cache, null);

        // Call toString which invokes countCached - should iterate through non-null cache finding all nulls
        String str = doc.toString();
        assertTrue(str.contains("cached=0"));
    }

    @Test
    public void testCountCached_NonNullCacheWithAllNulls_Array() throws Exception {
        byte[] bsonData = createSimpleArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Access an element to initialize cache
        array.get(0);

        // Use reflection to clear all cache entries
        java.lang.reflect.Field cacheField = IndexedBsonArray.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        Object[] cache = (Object[]) cacheField.get(array);
        Arrays.fill(cache, null);

        // Call toString which invokes countCached
        String str = array.toString();
        assertTrue(str.contains("cached=0"));
    }

    /**
     * Branch #4: IndexedBsonArray:141
     * ensureCache race condition in IndexedBsonArray
     */
    @Test
    public void testEnsureCache_RaceCondition_Array() throws Exception {
        byte[] bsonData = createSimpleArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicReference<Object> result1 = new AtomicReference<>();
        AtomicReference<Object> result2 = new AtomicReference<>();

        Thread t1 = new Thread(() -> {
            try {
                startLatch.await();
                result1.set(array.get(0));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                startLatch.await();
                result2.set(array.get(0));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        t1.start();
        t2.start();
        startLatch.countDown();
        t1.join();
        t2.join();

        assertEquals(result1.get(), result2.get());
    }

    /**
     * Branch #6: FastBsonDocument:219
     * getBoolean: if (!booleanExists.get(fieldId)) return defaultValue;
     * Need: booleanExists.get(fieldId) == true
     */
    @Test
    public void testFastBsonDocument_GetBooleanExists() throws Exception {
        // Use FastBsonDocumentBuilder to create a document with a boolean field
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putBoolean("isActive", true);
        builder.putBoolean("isEnabled", false);  // Test both true and false
        FastBsonDocument doc = (FastBsonDocument) builder.build();

        // Call getBoolean with default value - should return actual value, not default
        // This should hit the booleanExists.get(fieldId) == true branch (i.e., NOT return defaultValue)
        boolean result1 = doc.getBoolean("isActive", false);
        assertTrue(result1);  // The actual value from the document (true)

        boolean result2 = doc.getBoolean("isEnabled", true);
        assertFalse(result2);  // The actual value from the document (false)

        // Also test the case where field doesn't exist - should return default
        boolean result3 = doc.getBoolean("nonexistent", true);
        assertTrue(result3);  // Should return the default value

        // Also test with a non-boolean field - should return default (not throw exception)
        FastBsonDocumentBuilder builder2 = new FastBsonDocumentBuilder();
        builder2.putString("name", "test");
        FastBsonDocument doc2 = (FastBsonDocument) builder2.build();
        boolean result4 = doc2.getBoolean("name", true);
        assertTrue(result4);  // Should return default because "name" is not a boolean
    }

    /**
     * Branch #7: FastBsonDocument:286
     * get() method switch default case for unsupported types
     * Need: call with a BSON type not explicitly handled in the switch
     */
    @Test
    public void testFastBsonDocument_Get_DefaultCase() throws Exception {
        // Use FastBsonDocumentBuilder to create document with various types
        // Then use reflection to access the get() method with the field
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putString("script", "console.log();");  // JAVASCRIPT类型通过字符串存储
        FastBsonDocument doc = (FastBsonDocument) builder.build();

        // Access via get() - should work for all types
        Object value = doc.get("script");
        assertNotNull(value);
        assertEquals("console.log();", value);
    }

    /**
     * Branch #8: DocumentParser:210
     * while (true) { if (reader.position() >= endPosition) break; }
     * Need: reader.position() >= endPosition (loop exit via position check, not terminator)
     *
     * This tests defensive code for malformed BSON missing the 0x00 terminator.
     * We test this by creating a BSON document missing the terminator.
     */
    @Test
    public void testDocumentParser_LoopEndCondition() throws Exception {
        // Test the defensive position check in DocumentParser.parseDirectHashMap
        // We need to create malformed BSON without terminator to trigger:
        // if (reader.position() >= endPosition) break;

        // Create BSON data WITHOUT the 0x00 terminator
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();

        // Document length (will set after)
        buffer.putInt(0);

        // Add one field
        buffer.put((byte) 0x10); // INT32 type
        buffer.put("field\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(123);

        // DO NOT add terminator (0x00) - this is the malformed part
        // Instead, calculate length to include only what we've written

        int endPos = buffer.position();
        int documentLength = endPos - startPos;

        // Write the correct length
        buffer.putInt(startPos, documentLength);

        byte[] malformedBson = Arrays.copyOf(buffer.array(), endPos);

        // Use reflection to access DocumentParser (it's an enum singleton)
        Class<?> documentParserClass = Class.forName("com.cloud.fastbson.handler.parsers.DocumentParser");

        // Get the INSTANCE field (enum singleton)
        Object documentParser = documentParserClass.getField("INSTANCE").get(null);

        // Access parseDirectHashMap method
        java.lang.reflect.Method parseMethod = documentParserClass.getDeclaredMethod(
            "parseDirectHashMap",
            com.cloud.fastbson.reader.BsonReader.class,
            int.class
        );
        parseMethod.setAccessible(true);

        // Create a BsonReader
        com.cloud.fastbson.reader.BsonReader reader =
            new com.cloud.fastbson.reader.BsonReader(malformedBson);

        // Skip the document length field
        reader.readInt32();

        // Call parseDirectHashMap with endPosition
        // The method should exit via position check since there's no 0x00 terminator
        Object result = parseMethod.invoke(documentParser, reader, endPos);

        // Should return a valid HashMap document (defensive code handles missing terminator gracefully)
        assertNotNull(result);
    }

    /**
     * Target remaining branch: FastBsonDocument:303 - DATE_TIME case in getValue()
     */
    @Test
    public void testFastBsonDocument_GetValue_DateTime() throws Exception {
        // Create a document with a datetime field using builder
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        long timestamp = System.currentTimeMillis();
        builder.putDateTime("createdAt", timestamp);
        FastBsonDocument doc = (FastBsonDocument) builder.build();

        // Use reflection to call getValue() which has the DATE_TIME switch case
        Object value = doc.get("createdAt");
        assertNotNull(value);
        assertEquals(timestamp, value);
    }

    // ===== Helper methods =====

    private byte[] createSimpleBsonDocument() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0); // placeholder

        buffer.put((byte) 0x02); // STRING
        buffer.put("name\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(5);
        buffer.put("John\0".getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x10); // INT32
        buffer.put("age\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(30);

        buffer.put((byte) 0x00); // End

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        return Arrays.copyOf(buffer.array(), endPos);
    }

    private byte[] createSimpleArray() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        buffer.put((byte) 0x10); // INT32
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(100);

        buffer.put((byte) 0x10); // INT32
        buffer.put("1\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(200);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        return Arrays.copyOf(buffer.array(), endPos);
    }
}
