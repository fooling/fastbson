package com.cloud.fastbson.compatibility;

import com.cloud.fastbson.FastBson;
import com.cloud.fastbson.compatibility.BsonTestCase.TestExpectation;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.document.IndexedBsonDocument;
import com.cloud.fastbson.document.fast.FastBsonDocumentFactory;
import com.cloud.fastbson.document.hashmap.HashMapBsonDocumentFactory;
import com.cloud.fastbson.handler.parsers.DocumentParser;
import com.cloud.fastbson.reader.BsonReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive compatibility test suite for BSON implementations.
 *
 * Tests that different parsing approaches produce consistent results
 * when parsing the same BSON data.
 */
public class BsonCompatibilityTest {

    /**
     * Provide all test cases for parameterized testing.
     */
    static Stream<BsonTestCase> provideTestCases() {
        return BsonTestSuite.getAllTestCases().stream();
    }

    /**
     * Test that all implementations can parse all test cases without errors.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideTestCases")
    public void testAllImplementationsCanParse(BsonTestCase testCase) {
        byte[] data = testCase.getBsonData();

        // 1. IndexedBsonDocument (zero-copy, lazy parsing)
        assertDoesNotThrow(() -> {
            IndexedBsonDocument.parse(data, 0, data.length);
        }, "IndexedBsonDocument failed to parse: " + testCase.getName());

        // 2. FastBsonDocument (fastutil-based, zero-boxing)
        assertDoesNotThrow(() -> {
            parseWithFactory(data, FastBsonDocumentFactory.INSTANCE);
        }, "FastBsonDocument failed to parse: " + testCase.getName());

        // 3. HashMapBsonDocument (HashMap-based, zero-dependency)
        assertDoesNotThrow(() -> {
            parseWithFactory(data, HashMapBsonDocumentFactory.INSTANCE);
        }, "HashMapBsonDocument failed to parse: " + testCase.getName());
    }

    /**
     * Helper method to parse BSON using a specific factory.
     */
    private BsonDocument parseWithFactory(byte[] data, com.cloud.fastbson.document.BsonDocumentFactory factory) {
        synchronized (FastBson.class) {
            try {
                // Set the factory before parsing
                FastBson.setDocumentFactory(factory);

                // Use FastBson.parse with BsonReader to use the factory
                BsonReader reader = new BsonReader(data);
                return FastBson.parse(reader);
            } finally {
                // Restore default factory
                FastBson.setDocumentFactory(FastBsonDocumentFactory.INSTANCE);
            }
        }
    }

    /**
     * Test that implementations produce consistent field counts.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideTestCases")
    public void testConsistentFieldCount(BsonTestCase testCase) {
        byte[] data = testCase.getBsonData();

        IndexedBsonDocument indexedDoc = IndexedBsonDocument.parse(data, 0, data.length);
        BsonDocument fastBsonDoc = parseWithFactory(data, FastBsonDocumentFactory.INSTANCE);
        BsonDocument hashMapDoc = parseWithFactory(data, HashMapBsonDocumentFactory.INSTANCE);

        int indexedSize = indexedDoc.size();
        int fastBsonSize = fastBsonDoc.size();
        int hashMapSize = hashMapDoc.size();

        assertEquals(indexedSize, fastBsonSize,
            String.format("Field count mismatch between IndexedBsonDocument (%d) and FastBsonDocument (%d) for: %s",
                indexedSize, fastBsonSize, testCase.getName()));
        assertEquals(indexedSize, hashMapSize,
            String.format("Field count mismatch between IndexedBsonDocument (%d) and HashMapBsonDocument (%d) for: %s",
                indexedSize, hashMapSize, testCase.getName()));
    }

    /**
     * Test that implementations return consistent values for expected fields.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideTestCases")
    public void testConsistentFieldValues(BsonTestCase testCase) {
        if (testCase.getExpectations().length == 0) {
            return; // Skip cases without expectations
        }

        byte[] data = testCase.getBsonData();

        IndexedBsonDocument indexedDoc = IndexedBsonDocument.parse(data, 0, data.length);
        BsonDocument fastBsonDoc = parseWithFactory(data, FastBsonDocumentFactory.INSTANCE);
        BsonDocument hashMapDoc = parseWithFactory(data, HashMapBsonDocumentFactory.INSTANCE);

        for (TestExpectation expectation : testCase.getExpectations()) {
            String fieldPath = expectation.getFieldPath();
            Object expectedValue = expectation.getExpectedValue();

            // Get values from all implementations
            Object indexedValue = getNestedValue(indexedDoc, fieldPath);
            Object fastBsonValue = getNestedValue(fastBsonDoc, fieldPath);
            Object hashMapValue = getNestedValue(hashMapDoc, fieldPath);

            // Compare each implementation with expected value
            assertValuesEqual(expectedValue, indexedValue,
                String.format("IndexedBsonDocument value mismatch for field '%s' in test: %s",
                    fieldPath, testCase.getName()));

            assertValuesEqual(expectedValue, fastBsonValue,
                String.format("FastBsonDocument value mismatch for field '%s' in test: %s",
                    fieldPath, testCase.getName()));

            assertValuesEqual(expectedValue, hashMapValue,
                String.format("HashMapBsonDocument value mismatch for field '%s' in test: %s",
                    fieldPath, testCase.getName()));

            // Cross-implementation consistency
            assertValuesEqual(indexedValue, fastBsonValue,
                String.format("Value mismatch between IndexedBsonDocument and FastBsonDocument for field '%s' in test: %s",
                    fieldPath, testCase.getName()));

            assertValuesEqual(indexedValue, hashMapValue,
                String.format("Value mismatch between IndexedBsonDocument and HashMapBsonDocument for field '%s' in test: %s",
                    fieldPath, testCase.getName()));
        }
    }

    /**
     * Test that isEmpty() is consistent across implementations.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideTestCases")
    public void testConsistentIsEmpty(BsonTestCase testCase) {
        byte[] data = testCase.getBsonData();

        IndexedBsonDocument indexedDoc = IndexedBsonDocument.parse(data, 0, data.length);
        BsonDocument fastBsonDoc = parseWithFactory(data, FastBsonDocumentFactory.INSTANCE);
        BsonDocument hashMapDoc = parseWithFactory(data, HashMapBsonDocumentFactory.INSTANCE);

        boolean indexedEmpty = indexedDoc.isEmpty();
        boolean fastBsonEmpty = fastBsonDoc.isEmpty();
        boolean hashMapEmpty = hashMapDoc.isEmpty();

        assertEquals(indexedEmpty, fastBsonEmpty,
            String.format("isEmpty() mismatch between IndexedBsonDocument and FastBsonDocument for: %s",
                testCase.getName()));
        assertEquals(indexedEmpty, hashMapEmpty,
            String.format("isEmpty() mismatch between IndexedBsonDocument and HashMapBsonDocument for: %s",
                testCase.getName()));
    }

    /**
     * Test that toJson() produces valid JSON for supported types,
     * or throws UnsupportedOperationException for unsupported types.
     *
     * Note: HashMapBsonDocument doesn't implement toJson() (throws for all types),
     * so it's excluded from these tests.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideTestCases")
    public void testToJsonProducesValidJson(BsonTestCase testCase) {
        byte[] data = testCase.getBsonData();

        IndexedBsonDocument indexedDoc = IndexedBsonDocument.parse(data, 0, data.length);
        BsonDocument fastBsonDoc = parseWithFactory(data, FastBsonDocumentFactory.INSTANCE);
        // Skip HashMapBsonDocument - it doesn't implement toJson()

        if (testCase.isToJsonSupported()) {
            // For supported types: should produce valid JSON
            String indexedJson = assertDoesNotThrow(() -> indexedDoc.toJson(),
                "IndexedBsonDocument toJson() failed for: " + testCase.getName());
            String fastBsonJson = assertDoesNotThrow(() -> fastBsonDoc.toJson(),
                "FastBsonDocument toJson() failed for: " + testCase.getName());

            assertNotNull(indexedJson);
            assertNotNull(fastBsonJson);

            // Both should start with { and end with }
            assertTrue(indexedJson.startsWith("{") && indexedJson.endsWith("}"),
                "IndexedBsonDocument JSON should be wrapped in braces");
            assertTrue(fastBsonJson.startsWith("{") && fastBsonJson.endsWith("}"),
                "FastBsonDocument JSON should be wrapped in braces");
        } else {
            // For unsupported types:
            // - IndexedBsonDocument throws UnsupportedOperationException
            assertThrows(UnsupportedOperationException.class, () -> indexedDoc.toJson(),
                "IndexedBsonDocument toJson() should throw UnsupportedOperationException for: " + testCase.getName());

            // - FastBsonDocument doesn't throw, it outputs "<unsupported>" instead
            // So we just verify it produces valid JSON structure
            String fastBsonJson = assertDoesNotThrow(() -> fastBsonDoc.toJson(),
                "FastBsonDocument toJson() should not throw for: " + testCase.getName());
            assertNotNull(fastBsonJson);
            assertTrue(fastBsonJson.startsWith("{") && fastBsonJson.endsWith("}"),
                "FastBsonDocument JSON should be wrapped in braces");
        }
    }

    /**
     * Test summary report showing compatibility results.
     */
    @Test
    public void testCompatibilitySummary() {
        List<BsonTestCase> allCases = BsonTestSuite.getAllTestCases();

        System.out.println("\n=== BSON Compatibility Test Summary ===");
        System.out.println("Total test cases: " + allCases.size());
        System.out.println("\nTest cases:");

        int passed = 0;
        int failed = 0;
        List<String> failedCases = new ArrayList<>();

        for (BsonTestCase testCase : allCases) {
            try {
                byte[] data = testCase.getBsonData();
                FastBson.parse(data);
                IndexedBsonDocument.parse(data, 0, data.length);

                System.out.println("  ✓ " + testCase.getName() + " - " + testCase.getDescription());
                passed++;
            } catch (Exception e) {
                System.out.println("  ✗ " + testCase.getName() + " - FAILED: " + e.getMessage());
                failedCases.add(testCase.getName());
                failed++;
            }
        }

        System.out.println("\n=== Results ===");
        System.out.println("Passed: " + passed + "/" + allCases.size());
        System.out.println("Failed: " + failed + "/" + allCases.size());

        if (!failedCases.isEmpty()) {
            System.out.println("\nFailed cases:");
            failedCases.forEach(name -> System.out.println("  - " + name));
        }

        System.out.println("\n=== Implementation Coverage ===");
        System.out.println("All 3 implementations tested with all cases:");
        System.out.println("  1. IndexedBsonDocument  - Zero-copy, lazy parsing");
        System.out.println("  2. FastBsonDocument     - Fastutil-based, zero-boxing");
        System.out.println("  3. HashMapBsonDocument  - HashMap-based, zero-dependency");

        // Count toJson support
        long toJsonSupported = allCases.stream().filter(BsonTestCase::isToJsonSupported).count();
        long toJsonUnsupported = allCases.size() - toJsonSupported;
        System.out.println("\n=== toJson() Coverage ===");
        System.out.println("Supported types: " + toJsonSupported + "/" + allCases.size() + " (" + (toJsonSupported * 100 / allCases.size()) + "%)");
        if (toJsonUnsupported > 0) {
            System.out.println("Unsupported types: " + toJsonUnsupported);
            System.out.println("\nUnsupported types (tracked as expected behavior):");
            allCases.stream()
                .filter(tc -> !tc.isToJsonSupported())
                .forEach(tc -> System.out.println("  - " + tc.getName() + " (" + tc.getDescription() + ")"));
        }
        System.out.println("========================================\n");

        assertEquals(0, failed, "Some compatibility tests failed");
    }

    // ==================== Helper Methods ====================

    /**
     * Get nested value from document using dot-notation path.
     */
    private Object getNestedValue(BsonDocument doc, String fieldPath) {
        String[] parts = fieldPath.split("\\.");
        Object current = doc;

        for (String part : parts) {
            if (current instanceof BsonDocument) {
                BsonDocument currentDoc = (BsonDocument) current;

                // Check if field exists using hasField (correct API)
                if (!currentDoc.contains(part)) {
                    return null;
                }

                byte type = currentDoc.getType(part);

                switch (type) {
                    case 0x10: // Int32
                        current = currentDoc.getInt32(part);
                        break;
                    case 0x12: // Int64
                        current = currentDoc.getInt64(part);
                        break;
                    case 0x01: // Double
                        current = currentDoc.getDouble(part);
                        break;
                    case 0x02: // String
                        current = currentDoc.getString(part);
                        break;
                    case 0x08: // Boolean
                        current = currentDoc.getBoolean(part);
                        break;
                    case 0x09: // DateTime (UTC milliseconds)
                        current = currentDoc.getDateTime(part);
                        break;
                    case 0x0A: // Null
                        return null;
                    case 0x03: // Document
                        current = currentDoc.getDocument(part);
                        break;
                    default:
                        return null;
                }
            } else {
                return null;
            }
        }

        return current;
    }

    /**
     * Assert that two values are equal, handling special cases.
     */
    private void assertValuesEqual(Object expected, Object actual, String message) {
        if (expected == null) {
            assertNull(actual, message);
        } else if (expected instanceof Double) {
            assertTrue(actual instanceof Double, message + " - type mismatch");
            assertEquals((Double) expected, (Double) actual, 0.0001, message);
        } else {
            assertEquals(expected, actual, message);
        }
    }
}
