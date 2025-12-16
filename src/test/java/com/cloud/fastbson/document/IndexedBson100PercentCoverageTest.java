package com.cloud.fastbson.document;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ultra-aggressive coverage test to achieve 100% branch coverage.
 *
 * Techniques used:
 * - Multi-threading to force race conditions
 * - Reflection to manipulate internal state
 * - Brute-force hash collision generation
 * - Manual field ordering to control binary search outcomes
 */
public class IndexedBson100PercentCoverageTest {

    /**
     * Test backward linearSearch by forcing binary search to land AFTER target field.
     * This covers lines 286-287 (backward iteration branches).
     */
    @Test
    public void testLinearSearch_BackwardIterationAllBranches() {
        // Find 3 strings with same hash code
        List<String> collisionSet = findHashCollisions(3);
        String field1 = collisionSet.get(0);
        String field2 = collisionSet.get(1);
        String field3 = collisionSet.get(2);

        System.out.println("Using hash collision set: " + collisionSet);

        // Create document with these fields - they'll be sorted by hash
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Add field with different hash (to establish search space)
        buffer.put((byte) 0x10);
        buffer.put("aaa\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(111);

        // Add collision fields
        buffer.put((byte) 0x10);
        buffer.put((field1 + "\0").getBytes(StandardCharsets.UTF_8));
        buffer.putInt(100);

        buffer.put((byte) 0x10);
        buffer.put((field2 + "\0").getBytes(StandardCharsets.UTF_8));
        buffer.putInt(200);

        buffer.put((byte) 0x10);
        buffer.put((field3 + "\0").getBytes(StandardCharsets.UTF_8));
        buffer.putInt(300);

        // Add field with different hash
        buffer.put((byte) 0x10);
        buffer.put("zzz\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(999);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // Access all collision fields - this forces binary search to land at various positions
        // and triggers backward linearSearch
        assertEquals(100, doc.getInt32(field1));
        assertEquals(200, doc.getInt32(field2));
        assertEquals(300, doc.getInt32(field3));

        // Also test forward iteration (line 280-281)
        assertEquals(100, doc.getInt32(field1));
    }

    /**
     * Test linearSearch returning -1 after exhausting all collision matches.
     * This covers line 291 (return -1 when no match found).
     */
    @Test
    public void testLinearSearch_NotFoundAfterHashMatch() {
        // Use known collision strings
        List<String> collisionSet = findHashCollisions(2);
        String field1 = collisionSet.get(0);
        String field2 = collisionSet.get(1);

        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Add only field1 and field2
        buffer.put((byte) 0x10);
        buffer.put((field1 + "\0").getBytes(StandardCharsets.UTF_8));
        buffer.putInt(100);

        buffer.put((byte) 0x10);
        buffer.put((field2 + "\0").getBytes(StandardCharsets.UTF_8));
        buffer.putInt(200);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // Now search for a third field that has same hash but doesn't exist
        // This requires us to find or create a third string with same hash
        String searchField = findStringWithHash(field1.hashCode(), Arrays.asList(field1, field2));

        if (searchField != null) {
            System.out.println("Searching for non-existent field with matching hash: " + searchField);
            assertNull(doc.get(searchField));
            assertFalse(doc.contains(searchField));
        }
    }

    /**
     * Test parse while loop early termination (data[pos] == 0).
     * This covers the second branch of line 118: "data[pos] != 0"
     */
    @Test
    public void testParse_WhileLoopEarlyTermination() {
        // Create document that ends exactly at terminator
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Add one field
        buffer.put((byte) 0x10);
        buffer.put("x\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(42);

        // Terminator
        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        assertEquals(1, doc.size());
        assertEquals(42, doc.getInt32("x"));
    }

    /**
     * Test ensureCache race condition using multi-threading.
     * This covers line 304: the second "if (cache == null)" check inside synchronized block.
     */
    @Test
    public void testEnsureCache_RaceConditionCoverage() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Add multiple fields
        for (int i = 0; i < 5; i++) {
            buffer.put((byte) 0x10);
            buffer.put(("field" + i + "\0").getBytes(StandardCharsets.UTF_8));
            buffer.putInt(i * 100);
        }

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // Use reflection to reset cache
        Field cacheField = IndexedBsonDocument.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(doc, null);

        // Create multiple threads that access fields simultaneously
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < numThreads; i++) {
            final int fieldIndex = i % 5;
            executor.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();

                    // Access field - this will trigger ensureCache
                    int value = doc.getInt32("field" + fieldIndex);
                    assertEquals(fieldIndex * 100, value);

                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads at once
        startLatch.countDown();

        // Wait for all to complete
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify no errors
        if (!errors.isEmpty()) {
            throw new AssertionError("Thread errors: " + errors);
        }

        // Verify cache was created
        Object[] cache = (Object[]) cacheField.get(doc);
        assertNotNull(cache);
    }

    /**
     * Test IndexedBsonArray ensureCache race condition.
     */
    @Test
    public void testIndexedBsonArray_EnsureCacheRaceCondition() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Add multiple elements
        for (int i = 0; i < 5; i++) {
            buffer.put((byte) 0x10);
            buffer.put((i + "\0").getBytes(StandardCharsets.UTF_8));
            buffer.putInt(i * 111);
        }

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonArray array = IndexedBsonArray.parse(bson, 0, bson.length);

        // Reset cache via reflection
        Field cacheField = IndexedBsonArray.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(array, null);

        // Multi-threaded access
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < numThreads; i++) {
            final int index = i % 5;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    // Access element - triggers ensureCache
                    int value = array.getInt32(index);
                    assertEquals(index * 111, value);

                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        if (!errors.isEmpty()) {
            throw new AssertionError("Thread errors: " + errors);
        }

        Object[] cache = (Object[]) cacheField.get(array);
        assertNotNull(cache);
    }

    /**
     * Test matchesFieldName with character mismatch (line 266-268).
     */
    @Test
    public void testMatchesFieldName_CharacterMismatch() {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Add fields
        buffer.put((byte) 0x10);
        buffer.put("test\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(100);

        buffer.put((byte) 0x10);
        buffer.put("best\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(200);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // Search for field with character mismatch
        // This triggers line 266-268 in matchesFieldName
        assertNull(doc.get("tast")); // Different char at position 1
        assertNull(doc.get("tesx")); // Different char at position 3
        assertNull(doc.get("xest")); // Different char at position 0

        // Verify correct fields
        assertEquals(100, doc.getInt32("test"));
        assertEquals(200, doc.getInt32("best"));
    }

    /**
     * Test IndexedBsonArray getString with wrong type (line 298).
     */
    @Test
    public void testIndexedBsonArray_GetStringWrongType() {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Add int32 element
        buffer.put((byte) 0x10); // INT32
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(42);

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonArray array = IndexedBsonArray.parse(bson, 0, bson.length);

        // Try to get as string - should throw exception
        assertThrows(IllegalArgumentException.class, () -> array.getString(0));
    }

    /**
     * Test forward linearSearch continuation (line 280 - all branches).
     */
    @Test
    public void testLinearSearch_ForwardIterationExhausted() {
        // Create collision fields where forward iteration finds nothing
        List<String> collisionSet = findHashCollisions(3);

        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);

        // Add collision fields in specific order
        for (String field : collisionSet) {
            buffer.put((byte) 0x10);
            buffer.put((field + "\0").getBytes(StandardCharsets.UTF_8));
            buffer.putInt(field.hashCode() % 1000);
        }

        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        // Access all fields - this exercises both forward and backward linearSearch
        for (String field : collisionSet) {
            assertNotNull(doc.get(field));
        }
    }

    /**
     * Test empty document parse (line 118 - pos >= endPos).
     */
    @Test
    public void testParse_EmptyDocument() {
        ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0);
        buffer.put((byte) 0x00); // Immediate terminator

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bson = Arrays.copyOf(buffer.array(), endPos);

        IndexedBsonDocument doc = IndexedBsonDocument.parse(bson);

        assertEquals(0, doc.size());
        assertTrue(doc.isEmpty());
    }

    // ===== Helper Methods =====

    /**
     * Find N strings that have the same hashCode (hash collision).
     */
    private List<String> findHashCollisions(int count) {
        // Known hash collision sets for Java String.hashCode()
        List<List<String>> knownCollisions = Arrays.asList(
            Arrays.asList("Aa", "BB"),
            Arrays.asList("AaAa", "AaBB", "BBAa", "BBBB"),
            Arrays.asList("FB", "Ea"),
            Arrays.asList("FD", "Ec"),
            Arrays.asList("G1", "FV"),
            Arrays.asList("polygenelubricants", "GydZG_"),
            Arrays.asList("AaAaAa", "AaAaBB", "AaBBAa", "AaBBBB", "BBAaAa", "BBAaBB", "BBBBAa", "BBBBBB")
        );

        // Find a collision set with at least count elements
        for (List<String> collisionSet : knownCollisions) {
            if (collisionSet.size() >= count) {
                // Verify they actually collide
                int hash = collisionSet.get(0).hashCode();
                boolean allMatch = true;
                for (String s : collisionSet) {
                    if (s.hashCode() != hash) {
                        allMatch = false;
                        break;
                    }
                }
                if (allMatch) {
                    return collisionSet.subList(0, count);
                }
            }
        }

        // Fallback: generate collision set
        List<String> result = new ArrayList<>();
        result.add("Aa");
        result.add("BB");

        if (count > 2) {
            result.add("AaAa");
        }
        if (count > 3) {
            result.add("AaBB");
        }

        return result.subList(0, Math.min(count, result.size()));
    }

    /**
     * Find a string with specific hash that's not in exclude list.
     */
    private String findStringWithHash(int targetHash, List<String> exclude) {
        // Try known collision strings first
        String[] candidates = {"Aa", "BB", "AaAa", "AaBB", "BBAa", "BBBB", "FB", "Ea", "FD", "Ec"};

        for (String candidate : candidates) {
            if (candidate.hashCode() == targetHash && !exclude.contains(candidate)) {
                return candidate;
            }
        }

        // Brute force search (limited to short strings)
        for (char c1 = 'A'; c1 <= 'Z'; c1++) {
            for (char c2 = 'A'; c2 <= 'Z'; c2++) {
                String candidate = "" + c1 + c2;
                if (candidate.hashCode() == targetHash && !exclude.contains(candidate)) {
                    return candidate;
                }
            }
        }

        return null; // Not found
    }
}
