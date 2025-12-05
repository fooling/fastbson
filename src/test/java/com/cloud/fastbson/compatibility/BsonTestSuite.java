package com.cloud.fastbson.compatibility;

import com.cloud.fastbson.compatibility.BsonTestCase.TestExpectation;
import org.bson.BsonBinaryWriter;
import org.bson.BsonDocument;
import org.bson.BsonWriter;
import org.bson.io.BasicOutputBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Comprehensive BSON test suite generator.
 * Creates test cases covering all BSON types and complex structures.
 */
public class BsonTestSuite {

    /**
     * Get all test cases for comprehensive compatibility testing.
     */
    public static List<BsonTestCase> getAllTestCases() {
        List<BsonTestCase> testCases = new ArrayList<>();

        // Basic types
        testCases.add(createInt32TestCase());
        testCases.add(createInt64TestCase());
        testCases.add(createDoubleTestCase());
        testCases.add(createStringTestCase());
        testCases.add(createBooleanTestCase());
        testCases.add(createNullTestCase());

        // Date and time
        testCases.add(createDateTimeTestCase());
        testCases.add(createTimestampTestCase());

        // Binary and special types
        testCases.add(createBinaryTestCase());
        testCases.add(createObjectIdTestCase());
        testCases.add(createDecimal128TestCase());
        testCases.add(createRegexTestCase());
        testCases.add(createMinMaxKeyTestCase());

        // Complex structures
        testCases.add(createNestedDocumentTestCase());
        testCases.add(createNestedArrayTestCase());
        testCases.add(createMixedTypesTestCase());
        testCases.add(createDeepNestingTestCase());

        // Edge cases
        testCases.add(createEmptyDocumentTestCase());
        testCases.add(createLargeDocumentTestCase());
        testCases.add(createUnicodeTestCase());

        // === Enhanced test cases based on BSON spec ===

        // Deprecated types (for compatibility testing)
        testCases.add(createUndefinedTestCase());
        testCases.add(createJavaScriptCodeTestCase());
        testCases.add(createSymbolTestCase());

        // Binary subtypes (BSON spec compliant)
        testCases.add(createBinarySubtype1TestCase());
        testCases.add(createBinarySubtype4UUIDTestCase());
        testCases.add(createBinarySubtype5MD5TestCase());

        // Boundary value tests
        testCases.add(createInt32BoundaryTestCase());
        testCases.add(createInt64BoundaryTestCase());
        testCases.add(createDoubleSpecialValuesTestCase());

        // Edge cases from spec
        testCases.add(createRegexMultipleOptionsTestCase());
        testCases.add(createEmptyStringTestCase());

        // Cross-library compatibility with org.bson
        testCases.addAll(createOrgBsonCompatibilityTestCases());

        return testCases;
    }

    private static BsonTestCase createInt32TestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        buffer.put((byte) 0x10); // Int32
        buffer.put("value\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(42);

        buffer.put((byte) 0x10); // Int32
        buffer.put("negative\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(-100);

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "Int32 Values",
            "Tests positive and negative 32-bit integers",
            Arrays.copyOf(buffer.array(), end),
            new TestExpectation("value", 42, Integer.class),
            new TestExpectation("negative", -100, Integer.class)
        );
    }

    private static BsonTestCase createInt64TestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        buffer.put((byte) 0x12); // Int64
        buffer.put("bigNumber\0".getBytes(StandardCharsets.UTF_8));
        buffer.putLong(9223372036854775807L);

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "Int64 Values",
            "Tests 64-bit long integers",
            Arrays.copyOf(buffer.array(), end),
            new TestExpectation("bigNumber", 9223372036854775807L, Long.class)
        );
    }

    private static BsonTestCase createDoubleTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        buffer.put((byte) 0x01); // Double
        buffer.put("pi\0".getBytes(StandardCharsets.UTF_8));
        buffer.putDouble(3.14159);

        buffer.put((byte) 0x01); // Double
        buffer.put("negative\0".getBytes(StandardCharsets.UTF_8));
        buffer.putDouble(-0.5);

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "Double Values",
            "Tests floating-point numbers",
            Arrays.copyOf(buffer.array(), end),
            new TestExpectation("pi", 3.14159, Double.class),
            new TestExpectation("negative", -0.5, Double.class)
        );
    }

    private static BsonTestCase createStringTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        buffer.put((byte) 0x02); // String
        buffer.put("name\0".getBytes(StandardCharsets.UTF_8));
        String value = "Hello, World!";
        buffer.putInt(value.length() + 1);
        buffer.put((value + "\0").getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x02); // String
        buffer.put("empty\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(1);
        buffer.put((byte) 0x00);

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "String Values",
            "Tests string fields including empty strings",
            Arrays.copyOf(buffer.array(), end),
            new TestExpectation("name", "Hello, World!", String.class),
            new TestExpectation("empty", "", String.class)
        );
    }

    private static BsonTestCase createBooleanTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        buffer.put((byte) 0x08); // Boolean
        buffer.put("isTrue\0".getBytes(StandardCharsets.UTF_8));
        buffer.put((byte) 0x01);

        buffer.put((byte) 0x08); // Boolean
        buffer.put("isFalse\0".getBytes(StandardCharsets.UTF_8));
        buffer.put((byte) 0x00);

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "Boolean Values",
            "Tests true and false boolean values",
            Arrays.copyOf(buffer.array(), end),
            new TestExpectation("isTrue", true, Boolean.class),
            new TestExpectation("isFalse", false, Boolean.class)
        );
    }

    private static BsonTestCase createNullTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        buffer.put((byte) 0x0A); // Null
        buffer.put("nullField\0".getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "Null Values",
            "Tests null field values",
            Arrays.copyOf(buffer.array(), end),
            new TestExpectation("nullField", null, Object.class)
        );
    }

    private static BsonTestCase createDateTimeTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        long timestamp = 1609459200000L; // 2021-01-01 00:00:00 UTC
        buffer.put((byte) 0x09); // DateTime
        buffer.put("timestamp\0".getBytes(StandardCharsets.UTF_8));
        buffer.putLong(timestamp);

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "DateTime Values",
            "Tests UTC datetime milliseconds",
            Arrays.copyOf(buffer.array(), end),
            new TestExpectation("timestamp", timestamp, Long.class)
        );
    }

    private static BsonTestCase createTimestampTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        buffer.put((byte) 0x11); // Timestamp
        buffer.put("ts\0".getBytes(StandardCharsets.UTF_8));
        buffer.putLong(123456789L);

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "Timestamp Values",
            "Tests BSON timestamp type",
            Arrays.copyOf(buffer.array(), end),
            false // toJson not yet supported for Timestamp (0x11)
        );
    }

    private static BsonTestCase createBinaryTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        byte[] binaryData = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05};
        buffer.put((byte) 0x05); // Binary
        buffer.put("data\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(binaryData.length);
        buffer.put((byte) 0x00); // Generic binary subtype
        buffer.put(binaryData);

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "Binary Data",
            "Tests binary data with generic subtype",
            Arrays.copyOf(buffer.array(), end)
        );
    }

    private static BsonTestCase createObjectIdTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        buffer.put((byte) 0x07); // ObjectId
        buffer.put("_id\0".getBytes(StandardCharsets.UTF_8));
        buffer.put(new byte[]{0x5f, 0x3d, 0x4c, 0x3b, 0x2a, 0x1f, 0x0e, 0x1d, 0x2c, 0x3b, 0x4a, 0x59});

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "ObjectId",
            "Tests 12-byte MongoDB ObjectId",
            Arrays.copyOf(buffer.array(), end)
        );
    }

    private static BsonTestCase createDecimal128TestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        buffer.put((byte) 0x13); // Decimal128
        buffer.put("decimal\0".getBytes(StandardCharsets.UTF_8));
        buffer.put(new byte[16]); // 16-byte decimal128

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "Decimal128",
            "Tests 128-bit decimal type",
            Arrays.copyOf(buffer.array(), end),
            false // toJson not yet supported for Decimal128 (0x13)
        );
    }

    private static BsonTestCase createRegexTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        buffer.put((byte) 0x0B); // Regex
        buffer.put("pattern\0".getBytes(StandardCharsets.UTF_8));
        buffer.put("^[a-z]+$\0".getBytes(StandardCharsets.UTF_8)); // pattern
        buffer.put("i\0".getBytes(StandardCharsets.UTF_8)); // options

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "Regex Pattern",
            "Tests regular expression with options",
            Arrays.copyOf(buffer.array(), end),
            false // toJson not yet supported for Regex (0x0B)
        );
    }

    private static BsonTestCase createMinMaxKeyTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        buffer.put((byte) 0xFF); // MinKey
        buffer.put("min\0".getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x7F); // MaxKey
        buffer.put("max\0".getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "MinKey and MaxKey",
            "Tests special min/max key types",
            Arrays.copyOf(buffer.array(), end),
            false // toJson not yet supported for MinKey/MaxKey (0x7F/0xFF)
        );
    }

    private static BsonTestCase createNestedDocumentTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int docStart = buffer.position();
        buffer.putInt(0); // placeholder for outer doc

        // Nested document
        buffer.put((byte) 0x03); // Document
        buffer.put("nested\0".getBytes(StandardCharsets.UTF_8));
        int nestedStart = buffer.position();
        buffer.putInt(0); // placeholder for nested doc

        buffer.put((byte) 0x10); // Int32
        buffer.put("x\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(42);

        buffer.put((byte) 0x02); // String
        buffer.put("y\0".getBytes(StandardCharsets.UTF_8));
        String str = "nested value";
        buffer.putInt(str.length() + 1);
        buffer.put((str + "\0").getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x00); // End nested doc
        int nestedEnd = buffer.position();
        buffer.putInt(nestedStart, nestedEnd - nestedStart);

        buffer.put((byte) 0x00); // End outer doc
        int docEnd = buffer.position();
        buffer.putInt(docStart, docEnd - docStart);

        return new BsonTestCase(
            "Nested Document",
            "Tests document with embedded subdocument",
            Arrays.copyOf(buffer.array(), docEnd),
            new TestExpectation("nested.x", 42, Integer.class),
            new TestExpectation("nested.y", "nested value", String.class)
        );
    }

    private static BsonTestCase createNestedArrayTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int docStart = buffer.position();
        buffer.putInt(0); // placeholder

        // Array field
        buffer.put((byte) 0x04); // Array
        buffer.put("items\0".getBytes(StandardCharsets.UTF_8));
        int arrayStart = buffer.position();
        buffer.putInt(0); // placeholder for array

        buffer.put((byte) 0x10); // Int32
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(1);

        buffer.put((byte) 0x10); // Int32
        buffer.put("1\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(2);

        buffer.put((byte) 0x10); // Int32
        buffer.put("2\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(3);

        buffer.put((byte) 0x00); // End array
        int arrayEnd = buffer.position();
        buffer.putInt(arrayStart, arrayEnd - arrayStart);

        buffer.put((byte) 0x00); // End doc
        int docEnd = buffer.position();
        buffer.putInt(docStart, docEnd - docStart);

        return new BsonTestCase(
            "Array Field",
            "Tests document with array of integers",
            Arrays.copyOf(buffer.array(), docEnd)
        );
    }

    private static BsonTestCase createMixedTypesTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        buffer.put((byte) 0x10); // Int32
        buffer.put("intVal\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(100);

        buffer.put((byte) 0x02); // String
        buffer.put("strVal\0".getBytes(StandardCharsets.UTF_8));
        String str = "text";
        buffer.putInt(str.length() + 1);
        buffer.put((str + "\0").getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x08); // Boolean
        buffer.put("boolVal\0".getBytes(StandardCharsets.UTF_8));
        buffer.put((byte) 0x01);

        buffer.put((byte) 0x01); // Double
        buffer.put("doubleVal\0".getBytes(StandardCharsets.UTF_8));
        buffer.putDouble(2.5);

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "Mixed Types",
            "Tests document with multiple different types",
            Arrays.copyOf(buffer.array(), end),
            new TestExpectation("intVal", 100, Integer.class),
            new TestExpectation("strVal", "text", String.class),
            new TestExpectation("boolVal", true, Boolean.class),
            new TestExpectation("doubleVal", 2.5, Double.class)
        );
    }

    private static BsonTestCase createDeepNestingTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);
        int level0Start = buffer.position();
        buffer.putInt(0);

        // Level 1
        buffer.put((byte) 0x03);
        buffer.put("level1\0".getBytes(StandardCharsets.UTF_8));
        int level1Start = buffer.position();
        buffer.putInt(0);

        // Level 2
        buffer.put((byte) 0x03);
        buffer.put("level2\0".getBytes(StandardCharsets.UTF_8));
        int level2Start = buffer.position();
        buffer.putInt(0);

        // Level 3 - actual value
        buffer.put((byte) 0x10);
        buffer.put("value\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(999);

        buffer.put((byte) 0x00); // End level 2
        buffer.putInt(level2Start, buffer.position() - level2Start);

        buffer.put((byte) 0x00); // End level 1
        buffer.putInt(level1Start, buffer.position() - level1Start);

        buffer.put((byte) 0x00); // End level 0
        int end = buffer.position();
        buffer.putInt(level0Start, end - level0Start);

        return new BsonTestCase(
            "Deep Nesting",
            "Tests deeply nested document structure",
            Arrays.copyOf(buffer.array(), end),
            new TestExpectation("level1.level2.value", 999, Integer.class)
        );
    }

    private static BsonTestCase createEmptyDocumentTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(5); // Only length + terminator
        buffer.put((byte) 0x00); // End

        return new BsonTestCase(
            "Empty Document",
            "Tests document with no fields",
            Arrays.copyOf(buffer.array(), 5)
        );
    }

    private static BsonTestCase createLargeDocumentTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        // Add 50 fields
        for (int i = 0; i < 50; i++) {
            buffer.put((byte) 0x10); // Int32
            buffer.put(("field" + i + "\0").getBytes(StandardCharsets.UTF_8));
            buffer.putInt(i);
        }

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        TestExpectation[] expectations = new TestExpectation[5];
        for (int i = 0; i < 5; i++) {
            expectations[i] = new TestExpectation("field" + i, i, Integer.class);
        }

        return new BsonTestCase(
            "Large Document",
            "Tests document with 50 fields",
            Arrays.copyOf(buffer.array(), end),
            expectations
        );
    }

    private static BsonTestCase createUnicodeTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        String[] unicodeStrings = {
            "Hello, 世界!",
            "Привет, мир!",
            "مرحبا بالعالم",
            "🎉🎊🎈"
        };

        for (int i = 0; i < unicodeStrings.length; i++) {
            buffer.put((byte) 0x02); // String
            buffer.put(("text" + i + "\0").getBytes(StandardCharsets.UTF_8));
            String str = unicodeStrings[i];
            byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
            buffer.putInt(strBytes.length + 1);
            buffer.put(strBytes);
            buffer.put((byte) 0x00);
        }

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "Unicode Strings",
            "Tests various Unicode characters including emoji",
            Arrays.copyOf(buffer.array(), end),
            new TestExpectation("text0", "Hello, 世界!", String.class),
            new TestExpectation("text1", "Привет, мир!", String.class),
            new TestExpectation("text2", "مرحبا بالعالم", String.class),
            new TestExpectation("text3", "🎉🎊🎈", String.class)
        );
    }

    // ==================== Enhanced Test Cases Based on BSON Spec ====================

    /**
     * Tests deprecated Undefined type (0x06).
     */
    private static BsonTestCase createUndefinedTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        buffer.put((byte) 0x06); // Undefined (deprecated)
        buffer.put("undefined\0".getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "Undefined Type (Deprecated)",
            "Tests deprecated Undefined type (0x06)",
            Arrays.copyOf(buffer.array(), end)
        );
    }

    /**
     * Tests JavaScript code type (0x0D).
     */
    private static BsonTestCase createJavaScriptCodeTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        buffer.put((byte) 0x0D); // JavaScript code
        buffer.put("code\0".getBytes(StandardCharsets.UTF_8));
        String code = "function() { return 42; }";
        buffer.putInt(code.length() + 1);
        buffer.put((code + "\0").getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "JavaScript Code",
            "Tests JavaScript code type (0x0D)",
            Arrays.copyOf(buffer.array(), end)
        );
    }

    /**
     * Tests deprecated Symbol type (0x0E).
     */
    private static BsonTestCase createSymbolTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        buffer.put((byte) 0x0E); // Symbol (deprecated)
        buffer.put("symbol\0".getBytes(StandardCharsets.UTF_8));
        String symbolValue = "symbolName";
        buffer.putInt(symbolValue.length() + 1);
        buffer.put((symbolValue + "\0").getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "Symbol Type (Deprecated)",
            "Tests deprecated Symbol type (0x0E)",
            Arrays.copyOf(buffer.array(), end)
        );
    }

    /**
     * Tests binary subtype 1 (Function).
     */
    private static BsonTestCase createBinarySubtype1TestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        byte[] functionData = "function_binary_data".getBytes(StandardCharsets.UTF_8);
        buffer.put((byte) 0x05); // Binary
        buffer.put("function\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(functionData.length);
        buffer.put((byte) 0x01); // Subtype 1 - Function
        buffer.put(functionData);

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "Binary Subtype 1 - Function",
            "Tests binary data with Function subtype",
            Arrays.copyOf(buffer.array(), end)
        );
    }

    /**
     * Tests binary subtype 4 (UUID standard).
     */
    private static BsonTestCase createBinarySubtype4UUIDTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        // Standard UUID: 16 bytes
        byte[] uuidBytes = new byte[]{
            0x12, 0x34, 0x56, 0x78, (byte) 0x90, (byte) 0xab, (byte) 0xcd, (byte) 0xef,
            0x12, 0x34, 0x56, 0x78, (byte) 0x90, (byte) 0xab, (byte) 0xcd, (byte) 0xef
        };
        buffer.put((byte) 0x05); // Binary
        buffer.put("uuid\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(uuidBytes.length);
        buffer.put((byte) 0x04); // Subtype 4 - UUID
        buffer.put(uuidBytes);

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "Binary Subtype 4 - UUID",
            "Tests binary data with UUID subtype (standard)",
            Arrays.copyOf(buffer.array(), end)
        );
    }

    /**
     * Tests binary subtype 5 (MD5).
     */
    private static BsonTestCase createBinarySubtype5MD5TestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        // MD5 hash: 16 bytes
        byte[] md5Bytes = new byte[]{
            (byte) 0xd4, 0x1d, (byte) 0x8c, (byte) 0xd9, (byte) 0x8f, 0x00, (byte) 0xb2, 0x04,
            (byte) 0xe9, (byte) 0x80, 0x09, (byte) 0x98, (byte) 0xec, (byte) 0xf8, 0x42, 0x7e
        };
        buffer.put((byte) 0x05); // Binary
        buffer.put("md5\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(md5Bytes.length);
        buffer.put((byte) 0x05); // Subtype 5 - MD5
        buffer.put(md5Bytes);

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "Binary Subtype 5 - MD5",
            "Tests binary data with MD5 subtype",
            Arrays.copyOf(buffer.array(), end)
        );
    }

    /**
     * Tests Int32 boundary values (MIN_VALUE, MAX_VALUE, 0).
     */
    private static BsonTestCase createInt32BoundaryTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        buffer.put((byte) 0x10); // Int32
        buffer.put("minValue\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(Integer.MIN_VALUE);

        buffer.put((byte) 0x10); // Int32
        buffer.put("maxValue\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(Integer.MAX_VALUE);

        buffer.put((byte) 0x10); // Int32
        buffer.put("zero\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(0);

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "Int32 Boundary Values",
            "Tests Int32 with MIN_VALUE, MAX_VALUE, and zero",
            Arrays.copyOf(buffer.array(), end),
            new TestExpectation("minValue", Integer.MIN_VALUE, Integer.class),
            new TestExpectation("maxValue", Integer.MAX_VALUE, Integer.class),
            new TestExpectation("zero", 0, Integer.class)
        );
    }

    /**
     * Tests Int64 boundary values (MIN_VALUE, MAX_VALUE, 0).
     */
    private static BsonTestCase createInt64BoundaryTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        buffer.put((byte) 0x12); // Int64
        buffer.put("minValue\0".getBytes(StandardCharsets.UTF_8));
        buffer.putLong(Long.MIN_VALUE);

        buffer.put((byte) 0x12); // Int64
        buffer.put("maxValue\0".getBytes(StandardCharsets.UTF_8));
        buffer.putLong(Long.MAX_VALUE);

        buffer.put((byte) 0x12); // Int64
        buffer.put("zero\0".getBytes(StandardCharsets.UTF_8));
        buffer.putLong(0L);

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "Int64 Boundary Values",
            "Tests Int64 with MIN_VALUE, MAX_VALUE, and zero",
            Arrays.copyOf(buffer.array(), end),
            new TestExpectation("minValue", Long.MIN_VALUE, Long.class),
            new TestExpectation("maxValue", Long.MAX_VALUE, Long.class),
            new TestExpectation("zero", 0L, Long.class)
        );
    }

    /**
     * Tests Double special values (NaN, Infinity, -Infinity, 0.0, -0.0).
     */
    private static BsonTestCase createDoubleSpecialValuesTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        buffer.put((byte) 0x01); // Double
        buffer.put("nan\0".getBytes(StandardCharsets.UTF_8));
        buffer.putDouble(Double.NaN);

        buffer.put((byte) 0x01); // Double
        buffer.put("inf\0".getBytes(StandardCharsets.UTF_8));
        buffer.putDouble(Double.POSITIVE_INFINITY);

        buffer.put((byte) 0x01); // Double
        buffer.put("negInf\0".getBytes(StandardCharsets.UTF_8));
        buffer.putDouble(Double.NEGATIVE_INFINITY);

        buffer.put((byte) 0x01); // Double
        buffer.put("zero\0".getBytes(StandardCharsets.UTF_8));
        buffer.putDouble(0.0);

        buffer.put((byte) 0x01); // Double
        buffer.put("negZero\0".getBytes(StandardCharsets.UTF_8));
        buffer.putDouble(-0.0);

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "Double Special Values",
            "Tests Double with NaN, Infinity, -Infinity, 0.0, -0.0",
            Arrays.copyOf(buffer.array(), end),
            new TestExpectation("nan", Double.NaN, Double.class),
            new TestExpectation("inf", Double.POSITIVE_INFINITY, Double.class),
            new TestExpectation("negInf", Double.NEGATIVE_INFINITY, Double.class),
            new TestExpectation("zero", 0.0, Double.class),
            new TestExpectation("negZero", -0.0, Double.class)
        );
    }

    /**
     * Tests regex with multiple options (must be alphabetically sorted per BSON spec).
     */
    private static BsonTestCase createRegexMultipleOptionsTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        buffer.put((byte) 0x0B); // Regex
        buffer.put("pattern\0".getBytes(StandardCharsets.UTF_8));
        buffer.put("^[A-Z]+$\0".getBytes(StandardCharsets.UTF_8)); // pattern
        buffer.put("ilmsux\0".getBytes(StandardCharsets.UTF_8)); // options (alphabetically sorted)

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "Regex Multiple Options",
            "Tests regex with alphabetically sorted options (per BSON spec)",
            Arrays.copyOf(buffer.array(), end),
            false // toJson not yet supported for Regex (0x0B)
        );
    }

    /**
     * Tests empty string (different from null).
     */
    private static BsonTestCase createEmptyStringTestCase() {
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int start = buffer.position();
        buffer.putInt(0); // placeholder

        buffer.put((byte) 0x02); // String
        buffer.put("emptyStr\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(1); // length including null terminator
        buffer.put((byte) 0x00);

        buffer.put((byte) 0x0A); // Null
        buffer.put("nullVal\0".getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x00); // End
        int end = buffer.position();
        buffer.putInt(start, end - start);

        return new BsonTestCase(
            "Empty String vs Null",
            "Tests distinction between empty string and null",
            Arrays.copyOf(buffer.array(), end),
            new TestExpectation("emptyStr", "", String.class),
            new TestExpectation("nullVal", null, Object.class)
        );
    }

    /**
     * Creates test cases using org.bson library for cross-library compatibility.
     */
    private static List<BsonTestCase> createOrgBsonCompatibilityTestCases() {
        List<BsonTestCase> testCases = new ArrayList<>();

        // Test case 1: Simple document with all basic types
        BsonDocument doc1 = new BsonDocument();
        doc1.put("int32", new org.bson.BsonInt32(42));
        doc1.put("int64", new org.bson.BsonInt64(9223372036854775807L));
        doc1.put("double", new org.bson.BsonDouble(3.14159));
        doc1.put("string", new org.bson.BsonString("test string"));
        doc1.put("boolean", new org.bson.BsonBoolean(true));
        doc1.put("null", new org.bson.BsonNull());

        testCases.add(new BsonTestCase(
            "org.bson - Basic Types",
            "Tests FastBSON compatibility with org.bson generated basic types",
            serializeOrgBsonDocument(doc1),
            new TestExpectation("int32", 42, Integer.class),
            new TestExpectation("int64", 9223372036854775807L, Long.class),
            new TestExpectation("double", 3.14159, Double.class),
            new TestExpectation("string", "test string", String.class),
            new TestExpectation("boolean", true, Boolean.class),
            new TestExpectation("null", null, Object.class)
        ));

        // Test case 2: Nested document
        BsonDocument nested = new BsonDocument();
        nested.put("x", new org.bson.BsonInt32(100));
        nested.put("y", new org.bson.BsonString("nested"));

        BsonDocument doc2 = new BsonDocument();
        doc2.put("outer", new org.bson.BsonInt32(1));
        doc2.put("nested", nested);

        testCases.add(new BsonTestCase(
            "org.bson - Nested Document",
            "Tests FastBSON compatibility with org.bson nested documents",
            serializeOrgBsonDocument(doc2),
            new TestExpectation("outer", 1, Integer.class),
            new TestExpectation("nested.x", 100, Integer.class),
            new TestExpectation("nested.y", "nested", String.class)
        ));

        // Test case 3: Array
        org.bson.BsonArray array = new org.bson.BsonArray();
        array.add(new org.bson.BsonInt32(1));
        array.add(new org.bson.BsonInt32(2));
        array.add(new org.bson.BsonInt32(3));

        BsonDocument doc3 = new BsonDocument();
        doc3.put("array", array);

        testCases.add(new BsonTestCase(
            "org.bson - Array",
            "Tests FastBSON compatibility with org.bson arrays",
            serializeOrgBsonDocument(doc3)
        ));

        // Test case 4: Binary data
        BsonDocument doc4 = new BsonDocument();
        doc4.put("binary", new org.bson.BsonBinary(new byte[]{0x01, 0x02, 0x03}));
        doc4.put("objectId", new org.bson.BsonObjectId(new org.bson.types.ObjectId()));

        testCases.add(new BsonTestCase(
            "org.bson - Binary and ObjectId",
            "Tests FastBSON compatibility with org.bson binary types",
            serializeOrgBsonDocument(doc4)
        ));

        // Test case 5: DateTime
        BsonDocument doc5 = new BsonDocument();
        doc5.put("datetime", new org.bson.BsonDateTime(1609459200000L));

        testCases.add(new BsonTestCase(
            "org.bson - DateTime",
            "Tests FastBSON compatibility with org.bson datetime",
            serializeOrgBsonDocument(doc5),
            new TestExpectation("datetime", 1609459200000L, Long.class)
        ));

        return testCases;
    }

    /**
     * Serializes org.bson BsonDocument to byte array.
     */
    private static byte[] serializeOrgBsonDocument(BsonDocument document) {
        BasicOutputBuffer buffer = new BasicOutputBuffer();
        BsonBinaryWriter writer = new BsonBinaryWriter(buffer);

        writer.writeStartDocument();
        for (String key : document.keySet()) {
            writer.writeName(key);
            org.bson.BsonValue value = document.get(key);
            writeValue(writer, value);
        }
        writer.writeEndDocument();

        writer.close();
        return buffer.toByteArray();
    }

    /**
     * Writes a BsonValue to the writer.
     */
    private static void writeValue(BsonBinaryWriter writer, org.bson.BsonValue value) {
        switch (value.getBsonType()) {
            case INT32:
                writer.writeInt32(value.asInt32().getValue());
                break;
            case INT64:
                writer.writeInt64(value.asInt64().getValue());
                break;
            case DOUBLE:
                writer.writeDouble(value.asDouble().getValue());
                break;
            case STRING:
                writer.writeString(value.asString().getValue());
                break;
            case BOOLEAN:
                writer.writeBoolean(value.asBoolean().getValue());
                break;
            case NULL:
                writer.writeNull();
                break;
            case DOCUMENT:
                writer.writeStartDocument();
                BsonDocument doc = value.asDocument();
                for (String key : doc.keySet()) {
                    writer.writeName(key);
                    writeValue(writer, doc.get(key));
                }
                writer.writeEndDocument();
                break;
            case ARRAY:
                writer.writeStartArray();
                for (org.bson.BsonValue item : value.asArray()) {
                    writeValue(writer, item);
                }
                writer.writeEndArray();
                break;
            case BINARY:
                org.bson.BsonBinary binary = value.asBinary();
                writer.writeBinaryData(new org.bson.BsonBinary(binary.getType(), binary.getData()));
                break;
            case OBJECT_ID:
                writer.writeObjectId(value.asObjectId().getValue());
                break;
            case DATE_TIME:
                writer.writeDateTime(value.asDateTime().getValue());
                break;
            default:
                throw new UnsupportedOperationException("Unsupported BSON type: " + value.getBsonType());
        }
    }
}
