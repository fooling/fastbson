package com.cloud.fastbson.matcher;

import com.cloud.fastbson.util.StringPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OrderedFieldMatcher.
 *
 * @author FastBSON
 * @since Phase 3.4
 */
public class OrderedFieldMatcherTest {

    @BeforeEach
    public void setUp() {
        // Clear string pool before each test
        StringPool.clear();
    }

    @AfterEach
    public void tearDown() {
        // Clear string pool after each test
        StringPool.clear();
    }

    @Test
    public void testConstructor_ValidInput() {
        // Arrange
        String[] targetFields = {"name", "email"};
        String[] expectedOrder = {"_id", "name", "age", "email", "city"};

        // Act
        OrderedFieldMatcher matcher = new OrderedFieldMatcher(targetFields, expectedOrder);

        // Assert
        assertNotNull(matcher);
        assertEquals(2, matcher.getTargetFieldCount());
    }

    @Test
    public void testConstructor_NullTargetFields_ThrowsException() {
        // Arrange
        String[] expectedOrder = {"_id", "name"};

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            new OrderedFieldMatcher((String[]) null, expectedOrder));
    }

    @Test
    public void testConstructor_EmptyTargetFields_ThrowsException() {
        // Arrange
        String[] expectedOrder = {"_id", "name"};

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            new OrderedFieldMatcher(new String[0], expectedOrder));
    }

    @Test
    public void testConstructor_NullExpectedOrder_ThrowsException() {
        // Arrange
        String[] targetFields = {"name", "email"};

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            new OrderedFieldMatcher(targetFields, null));
    }

    @Test
    public void testConstructor_EmptyExpectedOrder_ThrowsException() {
        // Arrange
        String[] targetFields = {"name", "email"};

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            new OrderedFieldMatcher(targetFields, new String[0]));
    }

    @Test
    public void testMatches_FastPath_PerfectOrder() {
        // Arrange
        String[] targetFields = {"name", "email", "city"};
        String[] expectedOrder = {"_id", "name", "age", "email", "phone", "city", "country"};
        OrderedFieldMatcher matcher = new OrderedFieldMatcher(targetFields, expectedOrder);

        // Act & Assert - Simulate document with perfect order
        matcher.reset();
        assertFalse(matcher.matches("_id"));      // Position 0: not target
        assertTrue(matcher.matches("name"));      // Position 1: target, fast path
        assertFalse(matcher.matches("age"));      // Position 2: not target
        assertTrue(matcher.matches("email"));     // Position 3: target, fast path
        assertFalse(matcher.matches("phone"));    // Position 4: not target
        assertTrue(matcher.matches("city"));      // Position 5: target, fast path

        // Check statistics
        assertEquals(3, matcher.getFastPathHits(), "All 3 target fields should hit fast path");
        assertEquals(0, matcher.getSlowPathFallbacks(), "No slow path fallbacks");
        assertEquals(1.0, matcher.getFastPathHitRate(), 0.001);
    }

    @Test
    public void testMatches_SlowPath_OutOfOrder() {
        // Arrange
        String[] targetFields = {"name", "email"};
        String[] expectedOrder = {"_id", "name", "age", "email"};
        OrderedFieldMatcher matcher = new OrderedFieldMatcher(targetFields, expectedOrder);

        // Act & Assert - Document with fields out of order
        matcher.reset();
        assertFalse(matcher.matches("_id"));      // Position 0
        assertFalse(matcher.matches("age"));      // Position 1: wrong field, fallback
        assertTrue(matcher.matches("email"));     // Position 2: target, slow path
        assertTrue(matcher.matches("name"));      // Position 3: target, slow path

        // Check statistics
        assertTrue(matcher.getSlowPathFallbacks() > 0, "Should have slow path fallbacks");
        assertTrue(matcher.getFastPathHitRate() < 1.0, "Hit rate should be < 100%");
    }

    @Test
    public void testMatches_MixedPath() {
        // Arrange
        String[] targetFields = {"name", "email", "city"};
        String[] expectedOrder = {"_id", "name", "age", "email", "city"};
        OrderedFieldMatcher matcher = new OrderedFieldMatcher(targetFields, expectedOrder);

        // Act & Assert - Document with extra field inserted
        matcher.reset();
        assertFalse(matcher.matches("_id"));           // Position 0: not target, matches expected, no fallback
        assertTrue(matcher.matches("name"));           // Position 1: target, matches expected, fast path!
        assertFalse(matcher.matches("extraField"));    // Position 2: unexpected field, fallback
        assertTrue(matcher.matches("email"));          // Position 3: target, matches expected[3], fast path!
        assertTrue(matcher.matches("city"));           // Position 4: target, matches expected[4], fast path!

        // Check statistics
        assertEquals(3, matcher.getFastPathHits(), "All 3 targets hit fast path");
        assertEquals(1, matcher.getSlowPathFallbacks(), "Only extraField causes fallback");
    }

    @Test
    public void testMatches_NullFieldName_ReturnsFalse() {
        // Arrange
        String[] targetFields = {"name"};
        String[] expectedOrder = {"_id", "name"};
        OrderedFieldMatcher matcher = new OrderedFieldMatcher(targetFields, expectedOrder);

        // Act
        matcher.reset();
        boolean result = matcher.matches(null);

        // Assert
        assertFalse(result);
    }

    @Test
    public void testReset() {
        // Arrange
        String[] targetFields = {"name", "email"};
        String[] expectedOrder = {"_id", "name", "email"};
        OrderedFieldMatcher matcher = new OrderedFieldMatcher(targetFields, expectedOrder);

        // Act - Parse first document
        matcher.reset();
        assertFalse(matcher.matches("_id"));   // Position 0: not target
        assertTrue(matcher.matches("name"));   // Position 1: target, fast path
        assertTrue(matcher.matches("email"));  // Position 2: target, fast path

        // Parse second document without reset - positions continue beyond array
        assertTrue(matcher.matches("name"));  // Position 3: beyond expected order, slow path

        // Parse third document with reset
        matcher.reset();
        assertFalse(matcher.matches("_id"));   // Position 0: not target
        assertTrue(matcher.matches("name"));   // Position 1: target, fast path again

        // Assert
        assertTrue(matcher.getFastPathHits() >= 3, "Should have fast path hits after reset");
    }

    @Test
    public void testStatistics_ResetStatistics() {
        // Arrange
        String[] targetFields = {"name"};
        String[] expectedOrder = {"_id", "name"};
        OrderedFieldMatcher matcher = new OrderedFieldMatcher(targetFields, expectedOrder);

        // Act
        matcher.reset();
        matcher.matches("_id");    // Position 0: not target, no statistics change
        matcher.matches("name");   // Position 1: target, fast path hit
        assertEquals(1, matcher.getFastPathHits());

        matcher.resetStatistics();

        // Assert
        assertEquals(0, matcher.getFastPathHits());
        assertEquals(0, matcher.getSlowPathFallbacks());
        assertEquals(0.0, matcher.getFastPathHitRate(), 0.001);
    }

    @Test
    public void testMatches_AllTargetFieldsInOrder() {
        // Arrange
        String[] targetFields = {"_id", "name", "age", "email"};
        String[] expectedOrder = {"_id", "name", "age", "email"};
        OrderedFieldMatcher matcher = new OrderedFieldMatcher(targetFields, expectedOrder);

        // Act
        matcher.reset();
        assertTrue(matcher.matches("_id"));
        assertTrue(matcher.matches("name"));
        assertTrue(matcher.matches("age"));
        assertTrue(matcher.matches("email"));

        // Assert
        assertEquals(4, matcher.getFastPathHits());
        assertEquals(0, matcher.getSlowPathFallbacks());
        assertEquals(1.0, matcher.getFastPathHitRate(), 0.001);
    }

    @Test
    public void testMatches_NoTargetFieldsMatch() {
        // Arrange
        String[] targetFields = {"foo", "bar"};
        String[] expectedOrder = {"_id", "name", "age"};
        OrderedFieldMatcher matcher = new OrderedFieldMatcher(targetFields, expectedOrder);

        // Act
        matcher.reset();
        boolean match1 = matcher.matches("_id");
        boolean match2 = matcher.matches("name");
        boolean match3 = matcher.matches("age");

        // Assert
        assertFalse(match1);
        assertFalse(match2);
        assertFalse(match3);
        assertEquals(0, matcher.getFastPathHits());
    }

    @Test
    public void testMatches_TargetFieldNotInExpectedOrder() {
        // Arrange
        String[] targetFields = {"extraField"};
        String[] expectedOrder = {"_id", "name"};
        OrderedFieldMatcher matcher = new OrderedFieldMatcher(targetFields, expectedOrder);

        // Act
        matcher.reset();
        matcher.matches("_id");
        matcher.matches("name");
        boolean match = matcher.matches("extraField");

        // Assert
        assertTrue(match, "Should match via slow path even if not in expected order");
        assertTrue(matcher.getSlowPathFallbacks() > 0);
    }

    @Test
    public void testGetTargetFieldCount() {
        // Arrange
        String[] targetFields = {"name", "email", "city"};
        String[] expectedOrder = {"_id", "name", "age", "email", "phone", "city"};
        OrderedFieldMatcher matcher = new OrderedFieldMatcher(targetFields, expectedOrder);

        // Act & Assert
        assertEquals(3, matcher.getTargetFieldCount());
    }

    @Test
    public void testStringInterning() {
        // Arrange
        String[] targetFields = {"name"};
        String[] expectedOrder = {"_id", "name"};
        OrderedFieldMatcher matcher = new OrderedFieldMatcher(targetFields, expectedOrder);

        // Act - Create two separate string instances with same content
        String name1 = new String("name");
        String name2 = new String("name");

        // Verify they are different objects
        assertNotSame(name1, name2, "String instances should be different");

        // Both should match due to interning
        matcher.reset();
        matcher.matches("_id");    // Position 0: skip non-target
        assertTrue(matcher.matches(name1));  // Position 1: target, fast path

        matcher.reset();
        matcher.matches("_id");    // Position 0: skip non-target
        assertTrue(matcher.matches(name2));  // Position 1: target, fast path

        // Assert - Both should use fast path (reference equality after interning)
        assertEquals(2, matcher.getFastPathHits(), "Both matches should use fast path via interning");
    }
}
