package com.cloud.fastbson.util;

import com.cloud.fastbson.reader.BsonReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ObjectPool}.
 *
 * <p>Tests all methods with normal, boundary, and exceptional cases
 * to achieve 100% branch coverage.
 */
public class ObjectPoolTest {

    @AfterEach
    public void tearDown() {
        // Clear pools after each test to ensure isolation
        ObjectPool.clearCurrentThreadPools();
    }

    // ==================== Constructor Tests ====================

    @Test
    public void testConstructor_ThrowsAssertionError() throws Exception {
        // Use reflection to access private constructor
        java.lang.reflect.Constructor<ObjectPool> constructor =
            ObjectPool.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // Act & Assert
        java.lang.reflect.InvocationTargetException exception = assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> constructor.newInstance()
        );

        // Verify the cause is AssertionError
        Throwable cause = exception.getCause();
        assertTrue(cause instanceof AssertionError);
        assertTrue(cause.getMessage().contains("Cannot instantiate"));
    }

    // ==================== BsonReader Pool Tests ====================

    @Test
    public void testBorrowReader_FirstTime() {
        // Arrange
        byte[] data = new byte[]{0x01, 0x02, 0x03};

        // Act
        BsonReader reader = ObjectPool.borrowReader(data);

        // Assert
        assertNotNull(reader);
        assertEquals(0, reader.position());
        assertSame(data, reader.getBuffer());
    }

    @Test
    public void testBorrowReader_SecondTime_SameInstance() {
        // Arrange
        byte[] data1 = new byte[]{0x01, 0x02};
        byte[] data2 = new byte[]{0x03, 0x04, 0x05};

        // Act
        BsonReader reader1 = ObjectPool.borrowReader(data1);
        BsonReader reader2 = ObjectPool.borrowReader(data2);

        // Assert
        assertSame(reader1, reader2); // Same instance (pooled)
        assertEquals(0, reader2.position()); // Reset to position 0
        assertSame(data2, reader2.getBuffer()); // New data
    }

    @Test
    public void testBorrowReader_NullData_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ObjectPool.borrowReader(null)
        );
        assertTrue(exception.getMessage().contains("null"));
    }

    @Test
    public void testBorrowReader_ResetsPosition() {
        // Arrange
        byte[] data1 = new byte[]{0x01, 0x02, 0x03};
        byte[] data2 = new byte[]{0x04, 0x05};

        // Act
        BsonReader reader1 = ObjectPool.borrowReader(data1);
        reader1.skip(2); // Move position to 2
        assertEquals(2, reader1.position());

        BsonReader reader2 = ObjectPool.borrowReader(data2);

        // Assert
        assertSame(reader1, reader2);
        assertEquals(0, reader2.position()); // Position reset to 0
    }

    // ==================== HashMap Pool Tests ====================

    @Test
    public void testBorrowMap_FirstTime() {
        // Act
        HashMap<String, Object> map = ObjectPool.borrowMap();

        // Assert
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testBorrowMap_SecondTime_SameInstance() {
        // Act
        HashMap<String, Object> map1 = ObjectPool.borrowMap();
        HashMap<String, Object> map2 = ObjectPool.borrowMap();

        // Assert
        assertSame(map1, map2); // Same instance (pooled)
    }

    @Test
    public void testBorrowMap_ClearsContent() {
        // Arrange
        HashMap<String, Object> map1 = ObjectPool.borrowMap();
        map1.put("key1", "value1");
        map1.put("key2", "value2");
        assertEquals(2, map1.size());

        // Act
        HashMap<String, Object> map2 = ObjectPool.borrowMap();

        // Assert
        assertSame(map1, map2);
        assertTrue(map2.isEmpty()); // Cleared
    }

    // ==================== StringBuilder Pool Tests ====================

    @Test
    public void testBorrowStringBuilder_FirstTime() {
        // Act
        StringBuilder sb = ObjectPool.borrowStringBuilder();

        // Assert
        assertNotNull(sb);
        assertEquals(0, sb.length());
    }

    @Test
    public void testBorrowStringBuilder_SecondTime_SameInstance() {
        // Act
        StringBuilder sb1 = ObjectPool.borrowStringBuilder();
        StringBuilder sb2 = ObjectPool.borrowStringBuilder();

        // Assert
        assertSame(sb1, sb2); // Same instance (pooled)
    }

    @Test
    public void testBorrowStringBuilder_ClearsContent() {
        // Arrange
        StringBuilder sb1 = ObjectPool.borrowStringBuilder();
        sb1.append("Hello World");
        assertEquals(11, sb1.length());

        // Act
        StringBuilder sb2 = ObjectPool.borrowStringBuilder();

        // Assert
        assertSame(sb1, sb2);
        assertEquals(0, sb2.length()); // Cleared
    }

    // ==================== Thread Isolation Tests ====================

    @Test
    public void testThreadIsolation_DifferentThreadsGetDifferentInstances() throws Exception {
        // Arrange
        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        BsonReader[] readers = new BsonReader[threadCount];
        HashMap<?, ?>[] maps = new HashMap<?, ?>[threadCount];
        StringBuilder[] builders = new StringBuilder[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    readers[index] = ObjectPool.borrowReader(new byte[]{(byte) index});
                    maps[index] = ObjectPool.borrowMap();
                    builders[index] = ObjectPool.borrowStringBuilder();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // Act
        startLatch.countDown();
        doneLatch.await();

        // Assert - each thread should get its own instance
        for (int i = 0; i < threadCount; i++) {
            assertNotNull(readers[i]);
            assertNotNull(maps[i]);
            assertNotNull(builders[i]);

            for (int j = i + 1; j < threadCount; j++) {
                assertNotSame(readers[i], readers[j]);
                assertNotSame(maps[i], maps[j]);
                assertNotSame(builders[i], builders[j]);
            }
        }
    }

    @Test
    public void testThreadIsolation_SameThreadGetsSameInstances() {
        // Act
        BsonReader reader1 = ObjectPool.borrowReader(new byte[]{0x01});
        HashMap<String, Object> map1 = ObjectPool.borrowMap();
        StringBuilder sb1 = ObjectPool.borrowStringBuilder();

        BsonReader reader2 = ObjectPool.borrowReader(new byte[]{0x02});
        HashMap<String, Object> map2 = ObjectPool.borrowMap();
        StringBuilder sb2 = ObjectPool.borrowStringBuilder();

        // Assert
        assertSame(reader1, reader2);
        assertSame(map1, map2);
        assertSame(sb1, sb2);
    }

    // ==================== Pool Management Tests ====================

    @Test
    public void testClearCurrentThreadPools() {
        // Arrange
        BsonReader reader1 = ObjectPool.borrowReader(new byte[]{0x01});
        HashMap<String, Object> map1 = ObjectPool.borrowMap();
        StringBuilder sb1 = ObjectPool.borrowStringBuilder();

        // Act
        ObjectPool.clearCurrentThreadPools();

        BsonReader reader2 = ObjectPool.borrowReader(new byte[]{0x02});
        HashMap<String, Object> map2 = ObjectPool.borrowMap();
        StringBuilder sb2 = ObjectPool.borrowStringBuilder();

        // Assert - different instances after clear
        assertNotSame(reader1, reader2);
        assertNotSame(map1, map2);
        assertNotSame(sb1, sb2);
    }

    @Test
    public void testGetCurrentThreadReader() {
        // Arrange
        BsonReader borrowed = ObjectPool.borrowReader(new byte[]{0x01});

        // Act
        BsonReader current = ObjectPool.getCurrentThreadReader();

        // Assert
        assertSame(borrowed, current);
    }

    @Test
    public void testGetCurrentThreadMap() {
        // Arrange
        HashMap<String, Object> borrowed = ObjectPool.borrowMap();

        // Act
        HashMap<String, Object> current = ObjectPool.getCurrentThreadMap();

        // Assert
        assertSame(borrowed, current);
    }

    @Test
    public void testGetCurrentThreadStringBuilder() {
        // Arrange
        StringBuilder borrowed = ObjectPool.borrowStringBuilder();

        // Act
        StringBuilder current = ObjectPool.getCurrentThreadStringBuilder();

        // Assert
        assertSame(borrowed, current);
    }

    // ==================== Concurrent Access Tests ====================

    @Test
    public void testConcurrentAccess_MultipleThreads() throws Exception {
        // Arrange
        int threadCount = 10;
        int iterationsPerThread = 1000;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < iterationsPerThread; j++) {
                        BsonReader reader = ObjectPool.borrowReader(new byte[]{(byte) j});
                        HashMap<String, Object> map = ObjectPool.borrowMap();
                        StringBuilder sb = ObjectPool.borrowStringBuilder();

                        assertNotNull(reader);
                        assertNotNull(map);
                        assertNotNull(sb);
                        assertTrue(map.isEmpty());
                        assertEquals(0, sb.length());
                    }
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // Act
        startLatch.countDown();
        doneLatch.await();

        // Assert
        assertEquals(threadCount, successCount.get());
    }

    // ==================== Integration Tests ====================

    @Test
    public void testRealWorldUsage_ParseMultipleDocuments() {
        // Simulate parsing multiple BSON documents in sequence
        for (int i = 0; i < 100; i++) {
            byte[] docData = new byte[]{0x01, 0x02, (byte) i};
            BsonReader reader = ObjectPool.borrowReader(docData);
            HashMap<String, Object> result = ObjectPool.borrowMap();

            // Simulate parsing
            result.put("field" + i, i);

            // Verify objects are reused
            if (i > 0) {
                BsonReader nextReader = ObjectPool.borrowReader(new byte[]{0x00});
                assertSame(reader, nextReader);
            }
        }
    }
}
