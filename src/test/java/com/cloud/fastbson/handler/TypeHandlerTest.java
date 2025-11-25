package com.cloud.fastbson.handler;

import com.cloud.fastbson.exception.InvalidBsonTypeException;
import com.cloud.fastbson.reader.BsonReader;
import com.cloud.fastbson.util.BsonType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TypeHandler.
 * Covers all 21 BSON types according to MongoDB 3.4 specification.
 */
public class TypeHandlerTest {

    private TypeHandler handler = new TypeHandler();

    // ==================== Type: Double (0x01) ====================

    @Test
    public void testParseValue_Double() {
        byte[] data = createDoubleData(3.14159);
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.DOUBLE);

        assertTrue(result instanceof Double);
        assertEquals(3.14159, (Double) result, 0.00001);
    }

    // ==================== Type: String (0x02) ====================

    @Test
    public void testParseValue_String() {
        byte[] data = createStringData("Hello BSON");
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.STRING);

        assertTrue(result instanceof String);
        assertEquals("Hello BSON", result);
    }

    @Test
    public void testParseValue_String_Empty() {
        byte[] data = createStringData("");
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.STRING);

        assertEquals("", result);
    }

    // ==================== Type: Document (0x03) ====================

    @Test
    public void testParseValue_Document() {
        byte[] data = createSimpleDocument();
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.DOCUMENT);

        assertTrue(result instanceof Map);
        Map<String, Object> doc = (Map<String, Object>) result;
        assertEquals("John", doc.get("name"));
        assertEquals(25, doc.get("age"));
    }

    @Test
    public void testParseDocument_Empty() {
        byte[] data = createEmptyDocument();
        BsonReader reader = new BsonReader(data);

        Map<String, Object> result = handler.parseDocument(reader);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testParseDocument_Nested() {
        byte[] data = createNestedDocument();
        BsonReader reader = new BsonReader(data);

        Map<String, Object> result = handler.parseDocument(reader);

        assertTrue(result.containsKey("nested"));
        assertTrue(result.get("nested") instanceof Map);
    }

    @Test
    public void testParseDocument_WithoutTerminator() {
        // Test document parsing where loop exits naturally via position >= endPosition
        // rather than via END_OF_DOCUMENT marker
        byte[] data = createDocumentWithoutTerminator();
        BsonReader reader = new BsonReader(data);

        Map<String, Object> result = handler.parseDocument(reader);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(42, result.get("value"));
    }

    // ==================== Type: Array (0x04) ====================

    @Test
    public void testParseValue_Array() {
        byte[] data = createArrayData();
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.ARRAY);

        assertTrue(result instanceof List);
        List<Object> array = (List<Object>) result;
        assertEquals(3, array.size());
        assertEquals(10, array.get(0));
        assertEquals(20, array.get(1));
        assertEquals(30, array.get(2));
    }

    @Test
    public void testParseValue_Array_Empty() {
        byte[] data = createEmptyArray();
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.ARRAY);

        assertTrue(result instanceof List);
        assertTrue(((List<?>) result).isEmpty());
    }

    @Test
    public void testParseValue_Array_NonSequentialIndices() {
        // Test array with non-sequential indices (e.g., "1", "2" without "0")
        // This tests the else branch in parseArray when containsKey returns false
        byte[] data = createArrayWithNonSequentialIndices();
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.ARRAY);

        assertTrue(result instanceof List);
        // Should break at first missing index and return empty list
        assertTrue(((List<?>) result).isEmpty());
    }

    @Test
    public void testParseValue_Array_SingleElement() {
        // Test array with single element to ensure if-branch is covered
        byte[] data = createSingleElementArray();
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.ARRAY);

        assertTrue(result instanceof List);
        List<Object> array = (List<Object>) result;
        assertEquals(1, array.size());
        assertEquals(42, array.get(0));
    }

    // ==================== Type: Binary (0x05) ====================

    @Test
    public void testParseValue_Binary() {
        byte[] binaryContent = new byte[]{0x01, 0x02, 0x03, 0x04};
        byte[] data = createBinaryData((byte) 0x00, binaryContent);
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.BINARY);

        assertTrue(result instanceof TypeHandler.BinaryData);
        TypeHandler.BinaryData binary = (TypeHandler.BinaryData) result;
        assertEquals((byte) 0x00, binary.subtype);
        assertArrayEquals(binaryContent, binary.data);
    }

    // ==================== Type: Undefined (0x06) ====================

    @Test
    public void testParseValue_Undefined() {
        byte[] data = new byte[0];
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.UNDEFINED);

        assertNull(result);
    }

    // ==================== Type: ObjectId (0x07) ====================

    @Test
    public void testParseValue_ObjectId() {
        byte[] objectIdBytes = new byte[]{
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
            0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C
        };
        BsonReader reader = new BsonReader(objectIdBytes);

        Object result = handler.parseValue(reader, BsonType.OBJECT_ID);

        assertTrue(result instanceof String);
        assertEquals("0102030405060708090a0b0c", result);
    }

    // ==================== Type: Boolean (0x08) ====================

    @Test
    public void testParseValue_Boolean_True() {
        byte[] data = new byte[]{0x01};
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.BOOLEAN);

        assertTrue(result instanceof Boolean);
        assertTrue((Boolean) result);
    }

    @Test
    public void testParseValue_Boolean_False() {
        byte[] data = new byte[]{0x00};
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.BOOLEAN);

        assertTrue(result instanceof Boolean);
        assertFalse((Boolean) result);
    }

    // ==================== Type: DateTime (0x09) ====================

    @Test
    public void testParseValue_DateTime() {
        long timestamp = 1234567890123L;
        byte[] data = createInt64Data(timestamp);
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.DATE_TIME);

        assertTrue(result instanceof Date);
        assertEquals(timestamp, ((Date) result).getTime());
    }

    // ==================== Type: Null (0x0A) ====================

    @Test
    public void testParseValue_Null() {
        byte[] data = new byte[0];
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.NULL);

        assertNull(result);
    }

    // ==================== Type: Regex (0x0B) ====================

    @Test
    public void testParseValue_Regex() {
        byte[] data = createRegexData("^[a-z]+$", "i");
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.REGEX);

        assertTrue(result instanceof TypeHandler.RegexValue);
        TypeHandler.RegexValue regex = (TypeHandler.RegexValue) result;
        assertEquals("^[a-z]+$", regex.pattern);
        assertEquals("i", regex.options);
    }

    // ==================== Type: DBPointer (0x0C) ====================

    @Test
    public void testParseValue_DBPointer() {
        byte[] objectIdBytes = new byte[]{
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
            0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C
        };
        byte[] data = createDBPointerData("db.collection", objectIdBytes);
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.DB_POINTER);

        assertTrue(result instanceof TypeHandler.DBPointer);
        TypeHandler.DBPointer dbPointer = (TypeHandler.DBPointer) result;
        assertEquals("db.collection", dbPointer.namespace);
        assertEquals("0102030405060708090a0b0c", dbPointer.id);
    }

    // ==================== Type: JavaScript (0x0D) ====================

    @Test
    public void testParseValue_JavaScript() {
        byte[] data = createStringData("function() { return 42; }");
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.JAVASCRIPT);

        assertTrue(result instanceof String);
        assertEquals("function() { return 42; }", result);
    }

    // ==================== Type: Symbol (0x0E) ====================

    @Test
    public void testParseValue_Symbol() {
        byte[] data = createStringData("mySymbol");
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.SYMBOL);

        assertTrue(result instanceof String);
        assertEquals("mySymbol", result);
    }

    // ==================== Type: JavaScriptWithScope (0x0F) ====================

    @Test
    public void testParseValue_JavaScriptWithScope() {
        byte[] data = createJavaScriptWithScopeData("function() { return x; }");
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.JAVASCRIPT_WITH_SCOPE);

        assertTrue(result instanceof TypeHandler.JavaScriptWithScope);
        TypeHandler.JavaScriptWithScope js = (TypeHandler.JavaScriptWithScope) result;
        assertEquals("function() { return x; }", js.code);
        assertNotNull(js.scope);
    }

    // ==================== Type: Int32 (0x10) ====================

    @Test
    public void testParseValue_Int32() {
        byte[] data = createInt32Data(12345);
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.INT32);

        assertTrue(result instanceof Integer);
        assertEquals(12345, result);
    }

    @Test
    public void testParseValue_Int32_Negative() {
        byte[] data = createInt32Data(-12345);
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.INT32);

        assertEquals(-12345, result);
    }

    // ==================== Type: Timestamp (0x11) ====================

    @Test
    public void testParseValue_Timestamp() {
        byte[] data = createTimestampData(1000, 5);
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.TIMESTAMP);

        assertTrue(result instanceof TypeHandler.Timestamp);
        TypeHandler.Timestamp timestamp = (TypeHandler.Timestamp) result;
        assertEquals(1000, timestamp.seconds);
        assertEquals(5, timestamp.increment);
    }

    // ==================== Type: Int64 (0x12) ====================

    @Test
    public void testParseValue_Int64() {
        byte[] data = createInt64Data(9876543210L);
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.INT64);

        assertTrue(result instanceof Long);
        assertEquals(9876543210L, result);
    }

    @Test
    public void testParseValue_Int64_Negative() {
        byte[] data = createInt64Data(-9876543210L);
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.INT64);

        assertEquals(-9876543210L, result);
    }

    // ==================== Type: Decimal128 (0x13) ====================

    @Test
    public void testParseValue_Decimal128() {
        byte[] decimal128Bytes = new byte[16];
        for (int i = 0; i < 16; i++) {
            decimal128Bytes[i] = (byte) i;
        }
        BsonReader reader = new BsonReader(decimal128Bytes);

        Object result = handler.parseValue(reader, BsonType.DECIMAL128);

        assertTrue(result instanceof TypeHandler.Decimal128);
        assertArrayEquals(decimal128Bytes, ((TypeHandler.Decimal128) result).bytes);
    }

    // ==================== Type: MinKey (0xFF) ====================

    @Test
    public void testParseValue_MinKey() {
        byte[] data = new byte[0];
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.MIN_KEY);

        assertTrue(result instanceof TypeHandler.MinKey);
        assertEquals("MinKey", result.toString());
    }

    // ==================== Type: MaxKey (0x7F) ====================

    @Test
    public void testParseValue_MaxKey() {
        byte[] data = new byte[0];
        BsonReader reader = new BsonReader(data);

        Object result = handler.parseValue(reader, BsonType.MAX_KEY);

        assertTrue(result instanceof TypeHandler.MaxKey);
        assertEquals("MaxKey", result.toString());
    }

    // ==================== Invalid Type ====================

    @Test
    public void testParseValue_InvalidType() {
        byte[] data = new byte[0];
        BsonReader reader = new BsonReader(data);
        // 0x20 is not a valid BSON type
        final byte invalidType = (byte) 0x20;

        assertThrows(InvalidBsonTypeException.class, () -> {
            handler.parseValue(reader, invalidType);
        });
    }

    // ==================== Inner Class Tests ====================

    @Test
    public void testDecimal128_InvalidLength() {
        byte[] invalidBytes = new byte[15]; // Wrong length

        assertThrows(IllegalArgumentException.class, () -> {
            new TypeHandler.Decimal128(invalidBytes);
        });
    }

    @Test
    public void testDecimal128_Null() {
        assertThrows(IllegalArgumentException.class, () -> {
            new TypeHandler.Decimal128(null);
        });
    }

    @Test
    public void testParseValue_Array_EdgeCase_DocLengthBoundary() {
        // Test edge case where docLength = 4 (minimal boundary)
        // This tests the while loop exit condition: reader.position() >= endPosition
        // In this case, endPosition = position + 4 - 4 = position, so loop never executes
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(4);  // Document length = 4 (boundary case)

        BsonReader reader = new BsonReader(buffer.array());

        Object result = handler.parseValue(reader, BsonType.ARRAY);

        // Should return empty array even with this edge case
        assertTrue(result instanceof List);
        assertTrue(((List<?>) result).isEmpty());
    }

    // ==================== Helper Methods ====================

    private byte[] createDoubleData(double value) {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(value);
        return buffer.array();
    }

    private byte[] createInt32Data(int value) {
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(value);
        return buffer.array();
    }

    private byte[] createInt64Data(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(value);
        return buffer.array();
    }

    private byte[] createStringData(String value) {
        byte[] strBytes = value.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(4 + strBytes.length + 1).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(strBytes.length + 1); // length includes null terminator
        buffer.put(strBytes);
        buffer.put((byte) 0); // null terminator
        return buffer.array();
    }

    private byte[] createSimpleDocument() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // name: "John" (string)
            out.write(BsonType.STRING);
            out.write("name\0".getBytes(StandardCharsets.UTF_8));
            out.write(createStringData("John"));

            // age: 25 (int32)
            out.write(BsonType.INT32);
            out.write("age\0".getBytes(StandardCharsets.UTF_8));
            out.write(createInt32Data(25));

            // End of document
            out.write(0x00);

            byte[] content = out.toByteArray();
            ByteBuffer buffer = ByteBuffer.allocate(4 + content.length).order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(4 + content.length);
            buffer.put(content);

            return buffer.array();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] createEmptyDocument() {
        ByteBuffer buffer = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(5); // length: 4 bytes for length + 1 byte for terminator
        buffer.put((byte) 0x00); // terminator
        return buffer.array();
    }

    private byte[] createNestedDocument() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // nested: { inner: "value" }
            out.write(BsonType.DOCUMENT);
            out.write("nested\0".getBytes(StandardCharsets.UTF_8));

            ByteArrayOutputStream innerOut = new ByteArrayOutputStream();
            innerOut.write(BsonType.STRING);
            innerOut.write("inner\0".getBytes(StandardCharsets.UTF_8));
            innerOut.write(createStringData("value"));
            innerOut.write(0x00); // end of inner document

            byte[] innerContent = innerOut.toByteArray();
            ByteBuffer innerBuffer = ByteBuffer.allocate(4 + innerContent.length).order(ByteOrder.LITTLE_ENDIAN);
            innerBuffer.putInt(4 + innerContent.length);
            innerBuffer.put(innerContent);
            out.write(innerBuffer.array());

            // End of outer document
            out.write(0x00);

            byte[] content = out.toByteArray();
            ByteBuffer buffer = ByteBuffer.allocate(4 + content.length).order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(4 + content.length);
            buffer.put(content);

            return buffer.array();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] createArrayData() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // "0": 10
            out.write(BsonType.INT32);
            out.write("0\0".getBytes(StandardCharsets.UTF_8));
            out.write(createInt32Data(10));

            // "1": 20
            out.write(BsonType.INT32);
            out.write("1\0".getBytes(StandardCharsets.UTF_8));
            out.write(createInt32Data(20));

            // "2": 30
            out.write(BsonType.INT32);
            out.write("2\0".getBytes(StandardCharsets.UTF_8));
            out.write(createInt32Data(30));

            // End
            out.write(0x00);

            byte[] content = out.toByteArray();
            ByteBuffer buffer = ByteBuffer.allocate(4 + content.length).order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(4 + content.length);
            buffer.put(content);

            return buffer.array();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] createEmptyArray() {
        return createEmptyDocument();
    }

    private byte[] createBinaryData(byte subtype, byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(4 + 1 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(data.length);
        buffer.put(subtype);
        buffer.put(data);
        return buffer.array();
    }

    private byte[] createRegexData(String pattern, String options) {
        byte[] patternBytes = (pattern + "\0").getBytes(StandardCharsets.UTF_8);
        byte[] optionsBytes = (options + "\0").getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[patternBytes.length + optionsBytes.length];
        System.arraycopy(patternBytes, 0, result, 0, patternBytes.length);
        System.arraycopy(optionsBytes, 0, result, patternBytes.length, optionsBytes.length);
        return result;
    }

    private byte[] createDBPointerData(String namespace, byte[] objectId) {
        byte[] namespaceData = createStringData(namespace);
        byte[] result = new byte[namespaceData.length + objectId.length];
        System.arraycopy(namespaceData, 0, result, 0, namespaceData.length);
        System.arraycopy(objectId, 0, result, namespaceData.length, objectId.length);
        return result;
    }

    private byte[] createJavaScriptWithScopeData(String code) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // code
            byte[] codeData = createStringData(code);
            out.write(codeData);

            // scope (empty document)
            out.write(createEmptyDocument());

            byte[] content = out.toByteArray();
            ByteBuffer buffer = ByteBuffer.allocate(4 + content.length).order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(4 + content.length);
            buffer.put(content);

            return buffer.array();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] createTimestampData(int seconds, int increment) {
        long value = ((long) seconds << 32) | (increment & 0xFFFFFFFFL);
        return createInt64Data(value);
    }

    private byte[] createArrayWithNonSequentialIndices() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // Create array with indices "1" and "2", missing "0"
            // This will cause parseArray to break when looking for "0"

            // "1": 100
            out.write(BsonType.INT32);
            out.write("1\0".getBytes(StandardCharsets.UTF_8));
            out.write(createInt32Data(100));

            // "2": 200
            out.write(BsonType.INT32);
            out.write("2\0".getBytes(StandardCharsets.UTF_8));
            out.write(createInt32Data(200));

            // End
            out.write(0x00);

            byte[] content = out.toByteArray();
            ByteBuffer buffer = ByteBuffer.allocate(4 + content.length).order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(4 + content.length);
            buffer.put(content);

            return buffer.array();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] createSingleElementArray() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // "0": 42
            out.write(BsonType.INT32);
            out.write("0\0".getBytes(StandardCharsets.UTF_8));
            out.write(createInt32Data(42));

            // End
            out.write(0x00);

            byte[] content = out.toByteArray();
            ByteBuffer buffer = ByteBuffer.allocate(4 + content.length).order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(4 + content.length);
            buffer.put(content);

            return buffer.array();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] createDocumentWithoutTerminator() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // value: 42 (int32)
            out.write(BsonType.INT32);
            out.write("value\0".getBytes(StandardCharsets.UTF_8));
            out.write(createInt32Data(42));

            // NO END_OF_DOCUMENT marker (0x00)
            // This causes the loop to exit naturally when position >= endPosition

            byte[] content = out.toByteArray();
            ByteBuffer buffer = ByteBuffer.allocate(4 + content.length).order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(4 + content.length);
            buffer.put(content);

            return buffer.array();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
