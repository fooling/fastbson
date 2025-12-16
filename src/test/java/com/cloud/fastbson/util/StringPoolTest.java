package com.cloud.fastbson.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link StringPool}.
 *
 * <p>Tests all methods with normal, boundary, and exceptional cases
 * to achieve 100% branch coverage.
 */
public class StringPoolTest {

    @BeforeEach
    public void setUp() {
        StringPool.clear();
    }

    @AfterEach
    public void tearDown() {
        StringPool.clear();
    }

    // ==================== Constructor Tests ====================

    @Test
    public void testConstructor_ThrowsAssertionError() throws Exception {
        // Use reflection to access private constructor
        java.lang.reflect.Constructor<StringPool> constructor =
            StringPool.class.getDeclaredConstructor();
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

    // ==================== Basic Functionality Tests ====================

    @Test
    public void testIntern_FirstTime() {
        // Arrange
        String s1 = new String("fieldName");

        // Act
        String interned = StringPool.intern(s1);

        // Assert
        assertNotNull(interned);
        assertEquals("fieldName", interned);
        assertEquals(1, StringPool.getPoolSize());
    }

    @Test
    public void testIntern_SecondTime_ReturnsSameInstance() {
        // Arrange
        String s1 = new String("fieldName");
        String s2 = new String("fieldName"); // Different object, same content

        // Act
        String interned1 = StringPool.intern(s1);
        String interned2 = StringPool.intern(s2);

        // Assert
        assertSame(interned1, interned2); // Same reference (key assertion)
        assertEquals(1, StringPool.getPoolSize()); // Still only one entry
    }

    @Test
    public void testIntern_MultipleStrings() {
        // Arrange & Act
        String name = StringPool.intern("name");
        String age = StringPool.intern("age");
        String email = StringPool.intern("email");

        // Assert
        assertEquals(3, StringPool.getPoolSize());
        assertNotSame(name, age);
        assertNotSame(age, email);
        assertNotSame(name, email);
    }

    @Test
    public void testIntern_SameStringMultipleTimes() {
        // Arrange
        String field = "field";

        // Act
        String int1 = StringPool.intern(field);
        String int2 = StringPool.intern(field);
        String int3 = StringPool.intern(field);

        // Assert
        assertSame(int1, int2);
        assertSame(int2, int3);
        assertEquals(1, StringPool.getPoolSize());
    }

    // ==================== Exception Handling Tests ====================

    @Test
    public void testIntern_Null_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> StringPool.intern(null)
        );
        assertTrue(exception.getMessage().contains("null"));
    }

    // ==================== Edge Cases ====================

    @Test
    public void testIntern_EmptyString() {
        // Act
        String s = StringPool.intern("");

        // Assert
        assertEquals("", s);
        assertEquals(1, StringPool.getPoolSize());
    }

    @Test
    public void testIntern_EmptyString_MultipleTimes() {
        // Arrange
        String empty1 = new String("");
        String empty2 = new String("");

        // Act
        String int1 = StringPool.intern(empty1);
        String int2 = StringPool.intern(empty2);

        // Assert
        assertSame(int1, int2);
        assertEquals(1, StringPool.getPoolSize());
    }

    @Test
    public void testIntern_UnicodeStrings() {
        // Act
        String chinese = StringPool.intern("姓名");
        String japanese = StringPool.intern("メール");
        String emoji = StringPool.intern("😀");
        String mixed = StringPool.intern("Hello世界🌍");

        // Assert
        assertEquals(4, StringPool.getPoolSize());
        assertEquals("姓名", chinese);
        assertEquals("メール", japanese);
        assertEquals("😀", emoji);
        assertEquals("Hello世界🌍", mixed);
    }

    @Test
    public void testIntern_LongString() {
        // Arrange
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("field");
        }
        String longString = sb.toString();

        // Act
        String interned = StringPool.intern(longString);

        // Assert
        assertEquals(longString, interned);
        assertEquals(1, StringPool.getPoolSize());
    }

    @Test
    public void testIntern_WhitespaceStrings() {
        // Act
        String space = StringPool.intern(" ");
        String tab = StringPool.intern("\t");
        String newline = StringPool.intern("\n");
        String multiple = StringPool.intern("   ");

        // Assert
        assertEquals(4, StringPool.getPoolSize());
        assertEquals(" ", space);
        assertEquals("\t", tab);
        assertEquals("\n", newline);
        assertEquals("   ", multiple);
    }

    @Test
    public void testIntern_SpecialCharacters() {
        // Act
        String dot = StringPool.intern("field.name");
        String underscore = StringPool.intern("field_name");
        String dollar = StringPool.intern("$field");
        String at = StringPool.intern("@field");

        // Assert
        assertEquals(4, StringPool.getPoolSize());
        assertEquals("field.name", dot);
        assertEquals("field_name", underscore);
        assertEquals("$field", dollar);
        assertEquals("@field", at);
    }

    // ==================== Thread Safety / Concurrent Access Tests ====================

    @Test
    public void testIntern_ConcurrentAccess() throws Exception {
        // Arrange
        int threadCount = 10;
        int iterationsPerThread = 1000;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    for (int j = 0; j < iterationsPerThread; j++) {
                        String s = StringPool.intern("field" + (j % 100));
                        assertNotNull(s);
                    }
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
            threads[i].start();
        }

        // Act - release all threads at once
        startLatch.countDown();
        doneLatch.await();

        // Assert
        assertEquals(threadCount, successCount.get()); // All threads succeeded
        assertEquals(100, StringPool.getPoolSize()); // Should have exactly 100 unique strings
    }

    @Test
    public void testIntern_ConcurrentSameString() throws Exception {
        // Arrange
        int threadCount = 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        String[] results = new String[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    results[index] = StringPool.intern("sameField");
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
        assertEquals(1, StringPool.getPoolSize());
        // All threads should get the same reference
        for (int i = 1; i < threadCount; i++) {
            assertSame(results[0], results[i]);
        }
    }

    // ==================== Pool Management Tests ====================

    @Test
    public void testGetPoolSize_InitiallyZero() {
        // Assert
        assertEquals(0, StringPool.getPoolSize());
    }

    @Test
    public void testGetPoolSize_AfterInterning() {
        // Arrange & Act
        StringPool.intern("field1");
        int size1 = StringPool.getPoolSize();

        StringPool.intern("field2");
        int size2 = StringPool.getPoolSize();

        StringPool.intern("field1"); // Duplicate
        int size3 = StringPool.getPoolSize();

        // Assert
        assertEquals(1, size1);
        assertEquals(2, size2);
        assertEquals(2, size3); // No increase for duplicate
    }

    @Test
    public void testClear_EmptiesPool() {
        // Arrange
        StringPool.intern("field1");
        StringPool.intern("field2");
        StringPool.intern("field3");
        assertEquals(3, StringPool.getPoolSize());

        // Act
        StringPool.clear();

        // Assert
        assertEquals(0, StringPool.getPoolSize());
    }

    @Test
    public void testClear_AfterClear_CanInternAgain() {
        // Arrange
        String s1 = StringPool.intern(new String("field"));
        assertEquals(1, StringPool.getPoolSize());

        // Act
        StringPool.clear();
        String s2 = StringPool.intern(new String("field"));

        // Assert
        assertNotSame(s1, s2); // Different instances after clear
        assertEquals("field", s2);
        assertEquals(1, StringPool.getPoolSize());
    }

    @Test
    public void testClear_MultipleTimes() {
        // Act & Assert - should not throw
        StringPool.clear();
        StringPool.clear();
        StringPool.clear();

        assertEquals(0, StringPool.getPoolSize());
    }

    // ==================== Integration / Realistic Usage Tests ====================

    @Test
    public void testIntern_TypicalBsonFieldNames() {
        // Simulate typical BSON document field names
        String[] fieldNames = {
            "_id", "name", "age", "email", "address",
            "city", "state", "zip", "country",
            "created_at", "updated_at", "status", "type"
        };

        // Act
        String[] interned = new String[fieldNames.length];
        for (int i = 0; i < fieldNames.length; i++) {
            interned[i] = StringPool.intern(fieldNames[i]);
        }

        // Parse same document again (common scenario)
        String[] interned2 = new String[fieldNames.length];
        for (int i = 0; i < fieldNames.length; i++) {
            interned2[i] = StringPool.intern(fieldNames[i]);
        }

        // Assert
        assertEquals(fieldNames.length, StringPool.getPoolSize());
        for (int i = 0; i < fieldNames.length; i++) {
            assertSame(interned[i], interned2[i]); // Same references
        }
    }
}
