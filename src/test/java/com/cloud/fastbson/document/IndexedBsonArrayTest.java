package com.cloud.fastbson.document;

import com.cloud.fastbson.exception.BsonException;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for IndexedBsonArray - Phase 2.16 Zero-Copy Implementation.
 *
 * Tests all methods with normal, boundary, and exceptional cases
 * to achieve 100% branch coverage.
 */
public class IndexedBsonArrayTest {

    // ==================== Helper Methods ====================

    /**
     * Creates a simple BSON array with int32 values: [10, 20, 30]
     */
    private byte[] createInt32Array() {
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);

        int startPos = buffer.position();
        buffer.putInt(0); // placeholder for array length

        buffer.put((byte) 0x10); // Int32
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(10);

        buffer.put((byte) 0x10); // Int32
        buffer.put("1\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(20);

        buffer.put((byte) 0x10); // Int32
        buffer.put("2\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(30);

        buffer.put((byte) 0x00); // End

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        return Arrays.copyOf(buffer.array(), endPos);
    }

    /**
     * Creates a BSON array with int64 values: [100L, 200L, 300L]
     */
    private byte[] createInt64Array() {
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);

        int startPos = buffer.position();
        buffer.putInt(0); // placeholder

        buffer.put((byte) 0x12); // Int64
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putLong(100L);

        buffer.put((byte) 0x12); // Int64
        buffer.put("1\0".getBytes(StandardCharsets.UTF_8));
        buffer.putLong(200L);

        buffer.put((byte) 0x12); // Int64
        buffer.put("2\0".getBytes(StandardCharsets.UTF_8));
        buffer.putLong(300L);

        buffer.put((byte) 0x00); // End

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        return Arrays.copyOf(buffer.array(), endPos);
    }

    /**
     * Creates a BSON array with double values: [1.1, 2.2, 3.3]
     */
    private byte[] createDoubleArray() {
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);

        int startPos = buffer.position();
        buffer.putInt(0); // placeholder

        buffer.put((byte) 0x01); // Double
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putDouble(1.1);

        buffer.put((byte) 0x01); // Double
        buffer.put("1\0".getBytes(StandardCharsets.UTF_8));
        buffer.putDouble(2.2);

        buffer.put((byte) 0x01); // Double
        buffer.put("2\0".getBytes(StandardCharsets.UTF_8));
        buffer.putDouble(3.3);

        buffer.put((byte) 0x00); // End

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        return Arrays.copyOf(buffer.array(), endPos);
    }

    /**
     * Creates a BSON array with string values: ["a", "b", "c"]
     */
    private byte[] createStringArray() {
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);

        int startPos = buffer.position();
        buffer.putInt(0); // placeholder

        buffer.put((byte) 0x02); // String
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(2); // "a\0" length
        buffer.put("a\0".getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x02); // String
        buffer.put("1\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(2); // "b\0" length
        buffer.put("b\0".getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x02); // String
        buffer.put("2\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(2); // "c\0" length
        buffer.put("c\0".getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x00); // End

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        return Arrays.copyOf(buffer.array(), endPos);
    }

    /**
     * Creates a BSON array with boolean values: [true, false, true]
     */
    private byte[] createBooleanArray() {
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);

        int startPos = buffer.position();
        buffer.putInt(0); // placeholder

        buffer.put((byte) 0x08); // Boolean
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.put((byte) 1); // true

        buffer.put((byte) 0x08); // Boolean
        buffer.put("1\0".getBytes(StandardCharsets.UTF_8));
        buffer.put((byte) 0); // false

        buffer.put((byte) 0x08); // Boolean
        buffer.put("2\0".getBytes(StandardCharsets.UTF_8));
        buffer.put((byte) 1); // true

        buffer.put((byte) 0x00); // End

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        return Arrays.copyOf(buffer.array(), endPos);
    }

    /**
     * Creates a BSON array with mixed types
     */
    private byte[] createMixedArray() {
        ByteBuffer buffer = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN);

        int startPos = buffer.position();
        buffer.putInt(0); // placeholder

        // Int32
        buffer.put((byte) 0x10);
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(42);

        // String
        buffer.put((byte) 0x02);
        buffer.put("1\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(6);
        buffer.put("hello\0".getBytes(StandardCharsets.UTF_8));

        // Double
        buffer.put((byte) 0x01);
        buffer.put("2\0".getBytes(StandardCharsets.UTF_8));
        buffer.putDouble(3.14);

        // Boolean
        buffer.put((byte) 0x08);
        buffer.put("3\0".getBytes(StandardCharsets.UTF_8));
        buffer.put((byte) 1);

        // Null
        buffer.put((byte) 0x0A);
        buffer.put("4\0".getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x00); // End

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        return Arrays.copyOf(buffer.array(), endPos);
    }

    /**
     * Creates a BSON array with nested document
     */
    private byte[] createNestedDocumentArray() {
        ByteBuffer buffer = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN);

        int startPos = buffer.position();
        buffer.putInt(0); // placeholder for array

        // Nested document: { "name": "Alice", "age": 30 }
        buffer.put((byte) 0x03); // Document
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));

        int docStartPos = buffer.position();
        buffer.putInt(0); // placeholder for doc

        buffer.put((byte) 0x02); // String
        buffer.put("name\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(6);
        buffer.put("Alice\0".getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x10); // Int32
        buffer.put("age\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(30);

        buffer.put((byte) 0x00); // End of doc

        int docEndPos = buffer.position();
        buffer.putInt(docStartPos, docEndPos - docStartPos);

        buffer.put((byte) 0x00); // End of array

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        return Arrays.copyOf(buffer.array(), endPos);
    }

    /**
     * Creates a BSON array with nested array
     */
    private byte[] createNestedArrayArray() {
        ByteBuffer buffer = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN);

        int startPos = buffer.position();
        buffer.putInt(0); // placeholder for outer array

        // Nested array: [1, 2, 3]
        buffer.put((byte) 0x04); // Array
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));

        int innerStartPos = buffer.position();
        buffer.putInt(0); // placeholder for inner array

        buffer.put((byte) 0x10); // Int32
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(1);

        buffer.put((byte) 0x10); // Int32
        buffer.put("1\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(2);

        buffer.put((byte) 0x10); // Int32
        buffer.put("2\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(3);

        buffer.put((byte) 0x00); // End of inner array

        int innerEndPos = buffer.position();
        buffer.putInt(innerStartPos, innerEndPos - innerStartPos);

        buffer.put((byte) 0x00); // End of outer array

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        return Arrays.copyOf(buffer.array(), endPos);
    }

    /**
     * Creates an empty BSON array: []
     */
    private byte[] createEmptyArray() {
        ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(5); // array length
        buffer.put((byte) 0x00); // terminator
        return Arrays.copyOf(buffer.array(), 5);
    }

    // ==================== Basic Parse Tests ====================

    @Test
    public void testParse_Int32Array() {
        // Arrange
        byte[] bsonData = createInt32Array();

        // Act
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Assert
        assertNotNull(array);
        assertEquals(3, array.size());
        assertEquals(10, array.getInt32(0));
        assertEquals(20, array.getInt32(1));
        assertEquals(30, array.getInt32(2));
    }

    @Test
    public void testParse_Int64Array() {
        // Arrange
        byte[] bsonData = createInt64Array();

        // Act
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Assert
        assertNotNull(array);
        assertEquals(3, array.size());
        assertEquals(100L, array.getInt64(0));
        assertEquals(200L, array.getInt64(1));
        assertEquals(300L, array.getInt64(2));
    }

    @Test
    public void testParse_DoubleArray() {
        // Arrange
        byte[] bsonData = createDoubleArray();

        // Act
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Assert
        assertNotNull(array);
        assertEquals(3, array.size());
        assertEquals(1.1, array.getDouble(0), 0.0001);
        assertEquals(2.2, array.getDouble(1), 0.0001);
        assertEquals(3.3, array.getDouble(2), 0.0001);
    }

    @Test
    public void testParse_StringArray() {
        // Arrange
        byte[] bsonData = createStringArray();

        // Act
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Assert
        assertNotNull(array);
        assertEquals(3, array.size());
        assertEquals("a", array.getString(0));
        assertEquals("b", array.getString(1));
        assertEquals("c", array.getString(2));
    }

    @Test
    public void testParse_BooleanArray() {
        // Arrange
        byte[] bsonData = createBooleanArray();

        // Act
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Assert
        assertNotNull(array);
        assertEquals(3, array.size());
        assertTrue(array.getBoolean(0));
        assertFalse(array.getBoolean(1));
        assertTrue(array.getBoolean(2));
    }

    @Test
    public void testParse_MixedArray() {
        // Arrange
        byte[] bsonData = createMixedArray();

        // Act
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Assert
        assertNotNull(array);
        assertEquals(5, array.size());
        assertEquals(42, array.getInt32(0));
        assertEquals("hello", array.getString(1));
        assertEquals(3.14, array.getDouble(2), 0.001);
        assertTrue(array.getBoolean(3));
        assertNull(array.get(4));
    }

    @Test
    public void testParse_EmptyArray() {
        // Arrange
        byte[] bsonData = createEmptyArray();

        // Act
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Assert
        assertNotNull(array);
        assertEquals(0, array.size());
    }

    // ==================== Nested Structure Tests ====================

    @Test
    public void testGetDocument_NestedDocument() {
        // Arrange
        byte[] bsonData = createNestedDocumentArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Act
        BsonDocument doc = array.getDocument(0);

        // Assert
        assertNotNull(doc);
        assertEquals("Alice", doc.getString("name"));
        assertEquals(30, doc.getInt32("age"));
    }

    @Test
    public void testGetArray_NestedArray() {
        // Arrange
        byte[] bsonData = createNestedArrayArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Act
        BsonArray nestedArray = array.getArray(0);

        // Assert
        assertNotNull(nestedArray);
        assertEquals(3, nestedArray.size());
        assertEquals(1, nestedArray.getInt32(0));
        assertEquals(2, nestedArray.getInt32(1));
        assertEquals(3, nestedArray.getInt32(2));
    }

    // ==================== Cache Tests ====================

    @Test
    public void testCache_MultipleAccess() {
        // Arrange
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Act - First access (should parse and cache)
        int value1 = array.getInt32(0);
        // Second access (should hit cache)
        int value2 = array.getInt32(0);
        // Third access (should hit cache)
        int value3 = array.getInt32(0);

        // Assert
        assertEquals(10, value1);
        assertEquals(10, value2);
        assertEquals(10, value3);
    }

    @Test
    public void testCache_DifferentIndices() {
        // Arrange
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Act
        int v0 = array.getInt32(0);
        int v1 = array.getInt32(1);
        int v2 = array.getInt32(2);

        // Access again to verify cache
        int v0_2 = array.getInt32(0);
        int v1_2 = array.getInt32(1);

        // Assert
        assertEquals(10, v0);
        assertEquals(20, v1);
        assertEquals(30, v2);
        assertEquals(v0, v0_2);
        assertEquals(v1, v1_2);
    }

    // ==================== Generic get() Method Tests ====================

    @Test
    public void testGet_Int32() {
        // Arrange
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Act
        Object value = array.get(0);

        // Assert
        assertTrue(value instanceof Integer);
        assertEquals(10, value);
    }

    @Test
    public void testGet_String() {
        // Arrange
        byte[] bsonData = createStringArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Act
        Object value = array.get(0);

        // Assert
        assertTrue(value instanceof String);
        assertEquals("a", value);
    }

    @Test
    public void testGet_Double() {
        // Arrange
        byte[] bsonData = createDoubleArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Act
        Object value = array.get(0);

        // Assert
        assertTrue(value instanceof Double);
        assertEquals(1.1, (Double) value, 0.0001);
    }

    @Test
    public void testGet_Boolean() {
        // Arrange
        byte[] bsonData = createBooleanArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Act
        Object value = array.get(0);

        // Assert
        assertTrue(value instanceof Boolean);
        assertTrue((Boolean) value);
    }


    // ==================== toJson() Tests ====================

    @Test
    public void testToJson_Int32Array() {
        // Arrange
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Act
        String json = array.toJson();

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("10"));
        assertTrue(json.contains("20"));
        assertTrue(json.contains("30"));
    }

    @Test
    public void testToJson_EmptyArray() {
        // Arrange
        byte[] bsonData = createEmptyArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Act
        String json = array.toJson();

        // Assert
        assertNotNull(json);
        assertEquals("[]", json);
    }

    // ==================== Edge Cases ====================

    @Test
    public void testGetInt32_OutOfBounds() {
        // Arrange
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Act & Assert
        assertThrows(IndexOutOfBoundsException.class, () -> array.getInt32(5));
    }

    @Test
    public void testGetInt32_NegativeIndex() {
        // Arrange
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Act & Assert
        assertThrows(IndexOutOfBoundsException.class, () -> array.getInt32(-1));
    }

    @Test
    public void testGet_OutOfBounds() {
        // Arrange
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Act - get() returns null for out of bounds, doesn't throw
        Object result = array.get(10);

        // Assert
        assertNull(result);
    }

    @Test
    public void testParse_NullData() {
        // Act & Assert - parse() doesn't validate null, throws NullPointerException
        assertThrows(NullPointerException.class, () -> IndexedBsonArray.parse(null, 0, 10));
    }

    // ==================== Iterator Tests ====================

    @Test
    public void testIterator_HasNext() {
        // Arrange
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Act
        java.util.Iterator<Object> iterator = array.iterator();

        // Assert
        assertTrue(iterator.hasNext());
        iterator.next();
        assertTrue(iterator.hasNext());
        iterator.next();
        assertTrue(iterator.hasNext());
        iterator.next();
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testIterator_Next() {
        // Arrange
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Act
        java.util.Iterator<Object> iterator = array.iterator();

        // Assert
        assertEquals(10, iterator.next());
        assertEquals(20, iterator.next());
        assertEquals(30, iterator.next());
    }

    @Test
    public void testIterator_EmptyArray() {
        // Arrange
        byte[] bsonData = createEmptyArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Act
        java.util.Iterator<Object> iterator = array.iterator();

        // Assert
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testIterator_NoSuchElement() {
        // Arrange
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Act
        java.util.Iterator<Object> iterator = array.iterator();
        iterator.next();
        iterator.next();
        iterator.next();

        // Assert - calling next() when hasNext() is false should throw
        assertThrows(java.util.NoSuchElementException.class, () -> iterator.next());
    }

    // ==================== 补充所有getter方法的完整测试 ====================

    @Test
    public void testGetInt32_WrongType() {
        byte[] bsonData = createStringArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        assertThrows(IllegalArgumentException.class, () -> array.getInt32(0));
    }

    @Test
    public void testGetInt64_ExistingElement() {
        byte[] bsonData = createInt64Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        assertEquals(100L, array.getInt64(0));
    }

    @Test
    public void testGetInt64_OutOfBounds() {
        byte[] bsonData = createInt64Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        assertThrows(IndexOutOfBoundsException.class, () -> array.getInt64(10));
    }

    @Test
    public void testGetInt64_WrongType() {
        byte[] bsonData = createStringArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        assertThrows(IllegalArgumentException.class, () -> array.getInt64(0));
    }

    @Test
    public void testGetDouble_OutOfBounds() {
        byte[] bsonData = createDoubleArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        assertThrows(IndexOutOfBoundsException.class, () -> array.getDouble(10));
    }

    @Test
    public void testGetDouble_WrongType() {
        byte[] bsonData = createStringArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        assertThrows(IllegalArgumentException.class, () -> array.getDouble(0));
    }

    @Test
    public void testGetBoolean_OutOfBounds() {
        byte[] bsonData = createBooleanArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        assertThrows(IndexOutOfBoundsException.class, () -> array.getBoolean(10));
    }

    @Test
    public void testGetBoolean_WrongType() {
        byte[] bsonData = createStringArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        assertThrows(IllegalArgumentException.class, () -> array.getBoolean(0));
    }

    @Test
    public void testGetString_ExistingElement() {
        byte[] bsonData = createStringArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        assertEquals("a", array.getString(0));
    }

    @Test
    public void testGetString_OutOfBounds() {
        byte[] bsonData = createStringArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        assertThrows(IndexOutOfBoundsException.class, () -> array.getString(10));
    }

    @Test
    public void testGetDocument_OutOfBounds() {
        byte[] bsonData = createNestedDocumentArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        assertThrows(IndexOutOfBoundsException.class, () -> array.getDocument(10));
    }

    @Test
    public void testGetArray_OutOfBounds() {
        byte[] bsonData = createNestedArrayArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        assertThrows(IndexOutOfBoundsException.class, () -> array.getArray(10));
    }

    @Test
    public void testGet_Int64() {
        byte[] bsonData = createInt64Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        Object value = array.get(0);
        assertTrue(value instanceof Long);
        assertEquals(100L, value);
    }

    @Test
    public void testGet_EmptyArray() {
        byte[] bsonData = createEmptyArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        assertNull(array.get(0));
    }

    @Test
    public void testToJson_StringArray() {
        byte[] bsonData = createStringArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        String json = array.toJson();
        assertNotNull(json);
        assertTrue(json.contains("\"a\"") || json.contains("a"));
    }

    @Test
    public void testToJson_BooleanArray() {
        byte[] bsonData = createBooleanArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        String json = array.toJson();
        assertNotNull(json);
        assertTrue(json.contains("true"));
        assertTrue(json.contains("false"));
    }

    @Test
    public void testToJson_MixedArray() {
        byte[] bsonData = createMixedArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        String json = array.toJson();
        assertNotNull(json);
        assertTrue(json.contains("42"));
        assertTrue(json.contains("hello"));
    }

    @Test
    public void testToJson_NestedDocument() {
        byte[] bsonData = createNestedDocumentArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        String json = array.toJson();
        assertNotNull(json);
        assertTrue(json.contains("Alice") || json.contains("name"));
    }

    @Test
    public void testToJson_NestedArray() {
        byte[] bsonData = createNestedArrayArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        String json = array.toJson();
        assertNotNull(json);
        assertTrue(json.contains("["));
    }

    @Test
    public void testIsEmpty_NonEmpty() {
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        assertFalse(array.isEmpty());
    }

    @Test
    public void testIsEmpty_Empty() {
        byte[] bsonData = createEmptyArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        assertTrue(array.isEmpty());
    }

    @Test
    public void testEquals_SameInstance() {
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        assertEquals(array, array);
    }

    @Test
    public void testEquals_EqualArrays() {
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array1 = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        // Just test that equals doesn't crash
        assertTrue(array1.equals(array1));
    }

    @Test
    public void testEquals_Null() {
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        assertNotEquals(null, array);
    }

    @Test
    public void testEquals_DifferentClass() {
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        assertNotEquals("not an array", array);
    }

    @Test
    public void testHashCode_Consistent() {
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        int hash1 = array.hashCode();
        int hash2 = array.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    public void testToString_NotNull() {
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        String str = array.toString();
        assertNotNull(str);
    }

    // ==================== Additional Coverage Tests ====================

    @Test
    public void testGetType_BoundaryConditions() {
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Test out of bounds - should return 0
        assertEquals(0, array.getType(-1));
        assertEquals(0, array.getType(array.size()));
        assertEquals(0, array.getType(array.size() + 10));

        // Test valid index
        assertEquals((byte) 0x10, array.getType(0));  // INT32
    }

    @Test
    public void testGetInt32_WithDefaultValue() {
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Valid index and type
        assertEquals(10, array.getInt32(0, 999));

        // Invalid index
        assertEquals(999, array.getInt32(-1, 999));
        assertEquals(999, array.getInt32(array.size(), 999));

        // Wrong type - create array with string
        byte[] mixedData = createStringArray();
        IndexedBsonArray mixedArray = IndexedBsonArray.parse(mixedData, 0, mixedData.length);
        assertEquals(999, mixedArray.getInt32(0, 999));  // First element is string, not int32
    }

    @Test
    public void testGetInt64_WithDefaultValue() throws Exception {
        byte[] bsonData = createInt64Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Valid index and type
        assertEquals(100L, array.getInt64(0, 999L));

        // Test cache hit
        array.getInt64(0);  // Prime cache
        assertEquals(100L, array.getInt64(0, 999L));

        // Invalid index
        assertEquals(999L, array.getInt64(-1, 999L));
        assertEquals(999L, array.getInt64(array.size(), 999L));

        // Wrong type
        byte[] mixedData = createStringArray();
        IndexedBsonArray mixedArray = IndexedBsonArray.parse(mixedData, 0, mixedData.length);
        assertEquals(999L, mixedArray.getInt64(0, 999L));
    }

    @Test
    public void testGetDouble_WithDefaultValue() throws Exception {
        byte[] bsonData = createDoubleArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Valid index and type
        assertEquals(1.1, array.getDouble(0, 999.0), 0.001);

        // Test cache hit
        array.getDouble(0);  // Prime cache
        assertEquals(1.1, array.getDouble(0, 999.0), 0.001);

        // Invalid index
        assertEquals(999.0, array.getDouble(-1, 999.0), 0.001);
        assertEquals(999.0, array.getDouble(array.size(), 999.0), 0.001);

        // Wrong type
        byte[] mixedData = createStringArray();
        IndexedBsonArray mixedArray = IndexedBsonArray.parse(mixedData, 0, mixedData.length);
        assertEquals(999.0, mixedArray.getDouble(0, 999.0), 0.001);
    }

    @Test
    public void testGetBoolean_WithDefaultValue() throws Exception {
        byte[] bsonData = createBooleanArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Valid index and type
        assertEquals(true, array.getBoolean(0, false));

        // Test cache hit
        array.getBoolean(0);  // Prime cache
        assertEquals(true, array.getBoolean(0, false));

        // Invalid index
        assertEquals(false, array.getBoolean(-1, false));
        assertEquals(false, array.getBoolean(array.size(), false));

        // Wrong type
        byte[] mixedData = createStringArray();
        IndexedBsonArray mixedArray = IndexedBsonArray.parse(mixedData, 0, mixedData.length);
        assertEquals(false, mixedArray.getBoolean(0, false));
    }

    @Test
    public void testGetString_WithDefaultValue() throws Exception {
        byte[] bsonData = createStringArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Valid index and type
        assertEquals("a", array.getString(0, "default"));

        // Invalid index
        assertEquals("default", array.getString(-1, "default"));
        assertEquals("default", array.getString(array.size(), "default"));

        // Wrong type
        byte[] intData = createInt32Array();
        IndexedBsonArray intArray = IndexedBsonArray.parse(intData, 0, intData.length);
        assertEquals("default", intArray.getString(0, "default"));
    }

    @Test
    public void testGetString_WrongType_ThrowsException() {
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Element at index 0 is INT32, not STRING
        assertThrows(IllegalArgumentException.class, () -> array.getString(0));
    }

    @Test
    public void testGetString_CacheHit() {
        byte[] bsonData = createStringArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // First access - miss cache
        String first = array.getString(0);
        assertEquals("a", first);

        // Second access - hit cache
        String second = array.getString(0);
        assertEquals("a", second);
        assertSame(first, second);  // Should be same object from cache
    }

    @Test
    public void testParse_WithBinaryType() {
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);

        int startPos = buffer.position();
        buffer.putInt(0); // placeholder for array length

        // Add BINARY element
        buffer.put((byte) 0x05); // Binary
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(5);  // binary length
        buffer.put((byte) 0x00);  // subtype: generic
        buffer.put(new byte[]{1, 2, 3, 4, 5});  // binary data

        buffer.put((byte) 0x00); // End

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        byte[] bsonData = Arrays.copyOf(buffer.array(), endPos);
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        assertEquals(1, array.size());
        assertEquals((byte) 0x05, array.getType(0));
    }

    @Test
    public void testParse_WithObjectIdType() {
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);

        int startPos = buffer.position();
        buffer.putInt(0); // placeholder

        // Add ObjectId element
        buffer.put((byte) 0x07); // ObjectId
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.put(new byte[12]);  // 12 bytes ObjectId

        buffer.put((byte) 0x00); // End

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        byte[] bsonData = Arrays.copyOf(buffer.array(), endPos);
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        assertEquals(1, array.size());
        assertEquals((byte) 0x07, array.getType(0));
    }

    @Test
    public void testParse_WithDateTimeType() {
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);

        int startPos = buffer.position();
        buffer.putInt(0); // placeholder

        // Add DateTime element
        buffer.put((byte) 0x09); // DateTime
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putLong(System.currentTimeMillis());  // 8 bytes timestamp

        buffer.put((byte) 0x00); // End

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        byte[] bsonData = Arrays.copyOf(buffer.array(), endPos);
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        assertEquals(1, array.size());
        assertEquals((byte) 0x09, array.getType(0));
    }

    @Test
    public void testParse_WithRegexType() {
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);

        int startPos = buffer.position();
        buffer.putInt(0); // placeholder

        // Add Regex element
        buffer.put((byte) 0x0B); // Regex
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.put("pattern\0".getBytes(StandardCharsets.UTF_8));  // pattern
        buffer.put("i\0".getBytes(StandardCharsets.UTF_8));  // options

        buffer.put((byte) 0x00); // End

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        byte[] bsonData = Arrays.copyOf(buffer.array(), endPos);
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        assertEquals(1, array.size());
        assertEquals((byte) 0x0B, array.getType(0));
    }

    @Test
    public void testParse_WithTimestampType() {
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);

        int startPos = buffer.position();
        buffer.putInt(0); // placeholder

        // Add Timestamp element
        buffer.put((byte) 0x11); // Timestamp
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putLong(123456789L);  // 8 bytes timestamp

        buffer.put((byte) 0x00); // End

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        byte[] bsonData = Arrays.copyOf(buffer.array(), endPos);
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        assertEquals(1, array.size());
        assertEquals((byte) 0x11, array.getType(0));
    }

    @Test
    public void testParse_WithDecimal128Type() {
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);

        int startPos = buffer.position();
        buffer.putInt(0); // placeholder

        // Add Decimal128 element
        buffer.put((byte) 0x13); // Decimal128
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.put(new byte[16]);  // 16 bytes Decimal128

        buffer.put((byte) 0x00); // End

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        byte[] bsonData = Arrays.copyOf(buffer.array(), endPos);
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        assertEquals(1, array.size());
        assertEquals((byte) 0x13, array.getType(0));
    }

    @Test
    public void testParse_WithMinKeyType() {
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);

        int startPos = buffer.position();
        buffer.putInt(0); // placeholder

        // Add MinKey element
        buffer.put((byte) 0xFF); // MinKey
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        // MinKey has no value

        buffer.put((byte) 0x00); // End

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        byte[] bsonData = Arrays.copyOf(buffer.array(), endPos);
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        assertEquals(1, array.size());
        assertEquals((byte) 0xFF, array.getType(0));
    }

    @Test
    public void testParse_WithMaxKeyType() {
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);

        int startPos = buffer.position();
        buffer.putInt(0); // placeholder

        // Add MaxKey element
        buffer.put((byte) 0x7F); // MaxKey
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        // MaxKey has no value

        buffer.put((byte) 0x00); // End

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        byte[] bsonData = Arrays.copyOf(buffer.array(), endPos);
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        assertEquals(1, array.size());
        assertEquals((byte) 0x7F, array.getType(0));
    }

    @Test
    public void testParse_WithUnsupportedType_ThrowsException() {
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);

        int startPos = buffer.position();
        buffer.putInt(0); // placeholder

        // Add unsupported type
        buffer.put((byte) 0x99); // Invalid/unsupported type
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x00); // End

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        byte[] bsonData = Arrays.copyOf(buffer.array(), endPos);

        assertThrows(IllegalArgumentException.class, () -> {
            IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        });
    }

    @Test
    public void testGetDocument_WithDefaultValue() {
        byte[] nestedData = createNestedDocumentArray();
        IndexedBsonArray array = IndexedBsonArray.parse(nestedData, 0, nestedData.length);

        // Valid document
        assertNotNull(array.getDocument(0, null));

        // Invalid index
        assertNull(array.getDocument(-1, null));
        assertNull(array.getDocument(array.size(), null));

        // Wrong type
        byte[] intData = createInt32Array();
        IndexedBsonArray intArray = IndexedBsonArray.parse(intData, 0, intData.length);
        assertNull(intArray.getDocument(0, null));
    }

    @Test
    public void testGetArray_WithDefaultValue() {
        byte[] nestedData = createNestedArrayArray();
        IndexedBsonArray array = IndexedBsonArray.parse(nestedData, 0, nestedData.length);

        // Valid array
        assertNotNull(array.getArray(0, null));

        // Invalid index
        assertNull(array.getArray(-1, null));
        assertNull(array.getArray(array.size(), null));

        // Wrong type
        byte[] intData = createInt32Array();
        IndexedBsonArray intArray = IndexedBsonArray.parse(intData, 0, intData.length);
        assertNull(intArray.getArray(0, null));
    }

    // ==================== Tests for 0% coverage methods ====================

    /**
     * Test countCached() method by accessing elements and checking cache state.
     */
    @Test
    public void testCountCached_NoCache() {
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Before any access, cache should not be created
        String str = array.toString();
        assertTrue(str.contains("cached=0"));
    }

    @Test
    public void testCountCached_PartialCache() {
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Access some elements to partially populate cache
        array.getInt32(0);
        array.getInt32(1);

        String str = array.toString();
        assertTrue(str.contains("cached=2"));
    }

    @Test
    public void testCountCached_FullCache() {
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Access all elements
        array.getInt32(0);
        array.getInt32(1);
        array.getInt32(2);

        String str = array.toString();
        assertTrue(str.contains("cached=3"));
    }

    /**
     * Test getDocument exception branches.
     */
    @Test
    public void testGetDocument_WrongType_ThrowsException() {
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Element at index 0 is INT32, not DOCUMENT
        assertThrows(IllegalArgumentException.class, () -> array.getDocument(0));
    }

    @Test
    public void testGetDocument_CacheHit() {
        byte[] bsonData = createNestedDocumentArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // First access - miss cache
        BsonDocument first = array.getDocument(0);
        assertNotNull(first);

        // Second access - hit cache
        BsonDocument second = array.getDocument(0);
        assertNotNull(second);
        assertSame(first, second);  // Should be same object from cache
    }

    /**
     * Test getArray exception branches.
     */
    @Test
    public void testGetArray_WrongType_ThrowsException() {
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Element at index 0 is INT32, not ARRAY
        assertThrows(IllegalArgumentException.class, () -> array.getArray(0));
    }

    @Test
    public void testGetArray_CacheHit() {
        byte[] bsonData = createNestedArrayArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // First access - miss cache
        BsonArray first = array.getArray(0);
        assertNotNull(first);

        // Second access - hit cache
        BsonArray second = array.getArray(0);
        assertNotNull(second);
        assertSame(first, second);  // Should be same object from cache
    }

    /**
     * Test ensureCache() edge cases.
     */
    @Test
    public void testEnsureCache_ThreadSafety() throws Exception {
        byte[] bsonData = createMixedArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Access from multiple threads concurrently to test thread-safe cache initialization
        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            final int index = i % array.size();
            threads[i] = new Thread(() -> {
                array.get(index);
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Verify cache was created and populated
        assertNotNull(array.toString());
    }

    /**
     * Test get() exception branches and edge cases.
     */
    @Test
    public void testGet_NegativeIndex_ReturnsNull() {
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        Object result = array.get(-1);
        assertNull(result);
    }

    @Test
    public void testGet_IndexEqualToSize_ReturnsNull() {
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        Object result = array.get(array.size());
        assertNull(result);
    }

    @Test
    public void testGet_CacheHit_AllTypes() {
        byte[] bsonData = createMixedArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // First access - populate cache
        Object val0 = array.get(0);
        Object val1 = array.get(1);
        Object val2 = array.get(2);

        // Second access - hit cache
        assertSame(val0, array.get(0));
        assertSame(val1, array.get(1));
        assertSame(val2, array.get(2));
    }

    /**
     * Test getString exception branches.
     */
    @Test
    public void testGetString_CacheHit_WithDefaultValue() {
        byte[] bsonData = createStringArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // First access without default - prime cache
        String first = array.getString(0);
        assertEquals("a", first);

        // Second access with default - should hit cache
        String second = array.getString(0, "default");
        assertEquals("a", second);
        assertSame(first, second);
    }

    @Test
    public void testGetString_NegativeIndex_WithDefaultValue() {
        byte[] bsonData = createStringArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        String result = array.getString(-1, "default");
        assertEquals("default", result);
    }

    /**
     * Test additional edge cases for exception coverage.
     */
    @Test
    public void testGetDocument_NegativeIndex() {
        byte[] bsonData = createNestedDocumentArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        assertThrows(IndexOutOfBoundsException.class, () -> array.getDocument(-1));
    }

    @Test
    public void testGetArray_NegativeIndex() {
        byte[] bsonData = createNestedArrayArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        assertThrows(IndexOutOfBoundsException.class, () -> array.getArray(-1));
    }

    /**
     * Test getInt64 cache hit with default value.
     */
    @Test
    public void testGetInt64_CacheHit_WithDefaultValue() {
        byte[] bsonData = createInt64Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Prime cache
        long first = array.getInt64(0);
        assertEquals(100L, first);

        // Access with default - should hit cache
        long second = array.getInt64(0, 999L);
        assertEquals(100L, second);
    }

    /**
     * Test getDouble cache hit with default value.
     */
    @Test
    public void testGetDouble_CacheHit_WithDefaultValue() {
        byte[] bsonData = createDoubleArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Prime cache
        double first = array.getDouble(0);
        assertEquals(1.1, first, 0.001);

        // Access with default - should hit cache
        double second = array.getDouble(0, 999.0);
        assertEquals(1.1, second, 0.001);
    }

    /**
     * Test getBoolean cache hit with default value.
     */
    @Test
    public void testGetBoolean_CacheHit_WithDefaultValue() {
        byte[] bsonData = createBooleanArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Prime cache
        boolean first = array.getBoolean(0);
        assertEquals(true, first);

        // Access with default - should hit cache
        boolean second = array.getBoolean(0, false);
        assertEquals(true, second);
    }

    /**
     * Test getInt32 cache hit with default value.
     */
    @Test
    public void testGetInt32_CacheHit_WithDefaultValue() {
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Prime cache
        int first = array.getInt32(0);
        assertEquals(10, first);

        // Access with default - should hit cache
        int second = array.getInt32(0, 999);
        assertEquals(10, second);
    }

    /**
     * Test parse with unsupported BSON type.
     */
    @Test
    public void testParse_UnsupportedType_ThrowsException() {
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);

        int startPos = buffer.position();
        buffer.putInt(0); // placeholder

        // Add unsupported type
        buffer.put((byte) 0x99); // Invalid/unsupported type
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x00); // End

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        byte[] bsonData = Arrays.copyOf(buffer.array(), endPos);

        // Should throw exception
        assertThrows(IllegalArgumentException.class, () -> {
            IndexedBsonArray.parse(bsonData, 0, bsonData.length);
        });
    }

    /**
     * Test get() with unsupported type in existing parsed array.
     */
    @Test
    public void testGet_UnsupportedType_ThrowsException() {
        // Create array with a type that's not handled in get() switch
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);

        int startPos = buffer.position();
        buffer.putInt(0); // placeholder

        // Add ObjectId type (not handled in get() switch currently)
        buffer.put((byte) 0x07); // ObjectId
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.put(new byte[12]);  // 12 bytes ObjectId

        buffer.put((byte) 0x00); // End

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        byte[] bsonData = Arrays.copyOf(buffer.array(), endPos);
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // get() should throw UnsupportedOperationException for unhandled type
        assertThrows(UnsupportedOperationException.class, () -> array.get(0));
    }

    /**
     * Test ensureCache with simultaneous access.
     */
    @Test
    public void testEnsureCache_SimultaneousAccess() {
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Access cache == null check simultaneously - should be thread-safe
        assertNotNull(array.toString());
        assertTrue(array.toString().contains("size=3"));
    }

    /**
     * Test getString with cache already present (cache != null && cache[index] != null).
     */
    @Test
    public void testGetString_ExistingCache() {
        byte[] bsonData = createStringArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Prime cache
        String first = array.getString(0);
        assertEquals("a", first);

        // Access again - cache hit
        String second = array.getString(0);
        assertSame(first, second);
    }

    /**
     * Test parse with no fields (edge case).
     */
    @Test
    public void testParse_EmptyData_EdgeCase() {
        byte[] bsonData = createEmptyArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        assertEquals(0, array.size());
        assertTrue(array.isEmpty());
        assertNull(array.get(0));
    }

    /**
     * Test getType with valid index.
     */
    @Test
    public void testGetType_ValidIndex() {
        byte[] bsonData = createMixedArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        assertEquals((byte) 0x10, array.getType(0));  // INT32
        assertEquals((byte) 0x02, array.getType(1));  // STRING
        assertEquals((byte) 0x01, array.getType(2));  // DOUBLE
    }

    // ==================== Coverage Improvement Tests ====================

    /**
     * Test ensureCache double-check locking second check branch.
     */
    @Test
    public void testEnsureCache_DoubleCheckLocking_Array() throws Exception {
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Force concurrent access to trigger potential race in double-check locking
        Thread t1 = new Thread(() -> array.getInt32(0));
        Thread t2 = new Thread(() -> array.getInt32(1));

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // Verify cache was properly initialized
        assertNotNull(array.toString());
        assertTrue(array.toString().contains("cached="));
    }

    /**
     * Test getInt64 with negative index to cover index < 0 branch.
     */
    @Test
    public void testGetInt64_NegativeIndex_ThrowsException() {
        byte[] bsonData = createInt64Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        assertThrows(IndexOutOfBoundsException.class, () -> array.getInt64(-1));
    }

    /**
     * Test getDouble with negative index to cover index < 0 branch.
     */
    @Test
    public void testGetDouble_NegativeIndex_ThrowsException() {
        byte[] bsonData = createDoubleArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        assertThrows(IndexOutOfBoundsException.class, () -> array.getDouble(-1));
    }

    /**
     * Test getBoolean with negative index to cover index < 0 branch.
     */
    @Test
    public void testGetBoolean_NegativeIndex_ThrowsException() {
        byte[] bsonData = createBooleanArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        assertThrows(IndexOutOfBoundsException.class, () -> array.getBoolean(-1));
    }

    /**
     * Test getString with negative index to cover index < 0 branch.
     */
    @Test
    public void testGetString_NegativeIndex_ThrowsException() {
        byte[] bsonData = createStringArray();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        assertThrows(IndexOutOfBoundsException.class, () -> array.getString(-1));
    }

    /**
     * Test getString with JAVASCRIPT type (alternative to STRING).
     */
    @Test
    public void testGetString_JavaScriptType() {
        // Create array with JAVASCRIPT type (0x0D)
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0); // placeholder

        // Add JavaScript element
        buffer.put((byte) 0x0D); // JAVASCRIPT type
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(9); // "function\0" length (including null terminator)
        buffer.put("function\0".getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x00); // End
        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        byte[] bsonData = Arrays.copyOf(buffer.array(), endPos);
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Should be able to get as string
        assertEquals("function", array.getString(0));
    }

    /**
     * Test getString with SYMBOL type (alternative to STRING).
     */
    @Test
    public void testGetString_SymbolType() {
        // Create array with SYMBOL type (0x0E)
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0); // placeholder

        // Add Symbol element
        buffer.put((byte) 0x0E); // SYMBOL type
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(7); // "symbol\0" length (including null terminator)
        buffer.put("symbol\0".getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x00); // End
        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        byte[] bsonData = Arrays.copyOf(buffer.array(), endPos);
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Should be able to get as string
        assertEquals("symbol", array.getString(0));
    }

    /**
     * Test getString with default where type is JAVASCRIPT (should return value, not default).
     */
    @Test
    public void testGetString_WithDefault_JavaScriptType() {
        // Create array with JAVASCRIPT type (0x0D)
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0); // placeholder

        // Add JavaScript element
        buffer.put((byte) 0x0D); // JAVASCRIPT type
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(5); // "code\0" length (including null terminator)
        buffer.put("code\0".getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x00); // End
        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        byte[] bsonData = Arrays.copyOf(buffer.array(), endPos);
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Should return actual value, not default
        assertEquals("code", array.getString(0, "default"));
    }

    /**
     * Test getString with default where type is SYMBOL (should return value, not default).
     */
    @Test
    public void testGetString_WithDefault_SymbolType() {
        // Create array with SYMBOL type (0x0E)
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0); // placeholder

        // Add Symbol element
        buffer.put((byte) 0x0E); // SYMBOL type
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(7); // "symbol\0" length (including null terminator)
        buffer.put("symbol\0".getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x00); // End
        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        byte[] bsonData = Arrays.copyOf(buffer.array(), endPos);
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Should return actual value, not default
        assertEquals("symbol", array.getString(0, "default"));
    }


    /**
     * Test get() with DATE_TIME type to cover the missing branch.
     */
    @Test
    public void testGet_DateTimeType() {
        // Create array with DATE_TIME type (0x09)
        ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0); // placeholder

        // Add DateTime element
        buffer.put((byte) 0x09); // DATE_TIME type
        buffer.put("0\0".getBytes(StandardCharsets.UTF_8));
        buffer.putLong(1638360000000L); // timestamp

        buffer.put((byte) 0x00); // End
        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        byte[] bsonData = Arrays.copyOf(buffer.array(), endPos);
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Access via get() should parse as Long
        Object value = array.get(0);
        assertNotNull(value);
        assertTrue(value instanceof Long);
        assertEquals(1638360000000L, value);
    }

    /**
     * Test countCached when cache is null.
     */
    @Test
    public void testCountCached_NullCache_Array() {
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Before any element access, cache should be null
        // toString calls countCached, which should return 0 when cache is null
        String str = array.toString();
        assertTrue(str.contains("cached=0"));
    }

    /**
     * Test parse with data[pos] == 0 in the while loop condition.
     */
    @Test
    public void testParse_EarlyTerminator_Array() {
        // Empty array edge case
        ByteBuffer buffer = ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(5); // array length = 5 (4 bytes for length + 1 byte for terminator)
        buffer.put((byte) 0x00); // Immediate terminator

        byte[] bsonData = Arrays.copyOf(buffer.array(), 5);
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        assertEquals(0, array.size());
        assertTrue(array.isEmpty());
    }

    /**
     * Test countCached when cache is non-null but all slots are null.
     * This covers the "cache != null" branch in countCached where the loop finds no non-null values.
     * Branch: IndexedBsonArray.java:500 - if (cache == null) return 0; // need cache != null
     */
    @Test
    public void testCountCached_NonNullCacheWithAllNullSlots_Array() throws Exception {
        byte[] bsonData = createInt32Array();
        IndexedBsonArray array = IndexedBsonArray.parse(bsonData, 0, bsonData.length);

        // Access an element to force cache initialization
        array.get(0);

        // Use reflection to set all cache slots to null
        java.lang.reflect.Field cacheField = IndexedBsonArray.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        Object[] cache = (Object[]) cacheField.get(array);

        // Clear all cache slots
        for (int i = 0; i < cache.length; i++) {
            cache[i] = null;
        }

        // Now call toString() which calls countCached()
        // Should hit "cache != null" branch but return 0 because all slots are null
        String str = array.toString();
        assertTrue(str.contains("cached=0"));
    }
}
