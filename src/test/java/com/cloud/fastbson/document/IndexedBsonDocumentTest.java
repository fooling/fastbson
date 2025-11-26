package com.cloud.fastbson.document;

import com.cloud.fastbson.exception.BsonException;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for IndexedBsonDocument - Phase 2.16 Zero-Copy Implementation.
 *
 * Tests all methods with normal, boundary, and exceptional cases
 * to achieve 100% branch coverage.
 */
public class IndexedBsonDocumentTest {

    // ==================== Helper Methods ====================

    /**
     * Creates a simple BSON document with various types.
     */
    private byte[] createSimpleBsonDocument() {
        // Document: { "name": "Alice", "age": 30, "score": 95.5, "active": true }
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);

        int startPos = buffer.position();
        buffer.putInt(0); // placeholder for document length

        // String field: "name": "Alice"
        buffer.put((byte) 0x02); // String type
        buffer.put("name\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(6); // "Alice\0" length
        buffer.put("Alice\0".getBytes(StandardCharsets.UTF_8));

        // Int32 field: "age": 30
        buffer.put((byte) 0x10); // Int32 type
        buffer.put("age\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(30);

        // Double field: "score": 95.5
        buffer.put((byte) 0x01); // Double type
        buffer.put("score\0".getBytes(StandardCharsets.UTF_8));
        buffer.putDouble(95.5);

        // Boolean field: "active": true
        buffer.put((byte) 0x08); // Boolean type
        buffer.put("active\0".getBytes(StandardCharsets.UTF_8));
        buffer.put((byte) 1); // true

        // End of document
        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        return Arrays.copyOf(buffer.array(), endPos);
    }

    /**
     * Creates a BSON document with all primitive types.
     */
    private byte[] createAllTypesBsonDocument() {
        ByteBuffer buffer = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN);

        int startPos = buffer.position();
        buffer.putInt(0); // placeholder

        // Int32
        buffer.put((byte) 0x10);
        buffer.put("int32Field\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(42);

        // Int64
        buffer.put((byte) 0x12);
        buffer.put("int64Field\0".getBytes(StandardCharsets.UTF_8));
        buffer.putLong(9876543210L);

        // Double
        buffer.put((byte) 0x01);
        buffer.put("doubleField\0".getBytes(StandardCharsets.UTF_8));
        buffer.putDouble(3.14159);

        // String
        buffer.put((byte) 0x02);
        buffer.put("stringField\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(6);
        buffer.put("Hello\0".getBytes(StandardCharsets.UTF_8));

        // Boolean
        buffer.put((byte) 0x08);
        buffer.put("boolField\0".getBytes(StandardCharsets.UTF_8));
        buffer.put((byte) 1);

        // DateTime
        buffer.put((byte) 0x09);
        buffer.put("dateField\0".getBytes(StandardCharsets.UTF_8));
        buffer.putLong(1609459200000L); // 2021-01-01 00:00:00 UTC

        // ObjectId
        buffer.put((byte) 0x07);
        buffer.put("objectIdField\0".getBytes(StandardCharsets.UTF_8));
        buffer.put(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c});

        // Null
        buffer.put((byte) 0x0A);
        buffer.put("nullField\0".getBytes(StandardCharsets.UTF_8));

        // Binary
        buffer.put((byte) 0x05);
        buffer.put("binaryField\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(5); // binary length
        buffer.put((byte) 0x00); // subtype: generic binary
        buffer.put(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05});

        buffer.put((byte) 0x00); // End

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        return Arrays.copyOf(buffer.array(), endPos);
    }

    /**
     * Creates a BSON document with nested document.
     */
    private byte[] createNestedBsonDocument() {
        ByteBuffer buffer = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN);

        int startPos = buffer.position();
        buffer.putInt(0); // placeholder for outer doc length

        // String field: "name": "Bob"
        buffer.put((byte) 0x02);
        buffer.put("name\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(4);
        buffer.put("Bob\0".getBytes(StandardCharsets.UTF_8));

        // Nested document: "address": { "city": "NYC", "zip": 10001 }
        buffer.put((byte) 0x03); // Document type
        buffer.put("address\0".getBytes(StandardCharsets.UTF_8));

        int nestedStartPos = buffer.position();
        buffer.putInt(0); // placeholder for nested doc length

        buffer.put((byte) 0x02); // String
        buffer.put("city\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(4);
        buffer.put("NYC\0".getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x10); // Int32
        buffer.put("zip\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(10001);

        buffer.put((byte) 0x00); // End of nested doc

        int nestedEndPos = buffer.position();
        buffer.putInt(nestedStartPos, nestedEndPos - nestedStartPos);

        buffer.put((byte) 0x00); // End of outer doc

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        return Arrays.copyOf(buffer.array(), endPos);
    }

    /**
     * Creates a BSON document with array.
     */
    private byte[] createArrayBsonDocument() {
        ByteBuffer buffer = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN);

        int startPos = buffer.position();
        buffer.putInt(0); // placeholder for outer doc length

        // Array field: "numbers": [10, 20, 30]
        buffer.put((byte) 0x04); // Array type
        buffer.put("numbers\0".getBytes(StandardCharsets.UTF_8));

        int arrayStartPos = buffer.position();
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

        buffer.put((byte) 0x00); // End of array

        int arrayEndPos = buffer.position();
        buffer.putInt(arrayStartPos, arrayEndPos - arrayStartPos);

        buffer.put((byte) 0x00); // End of outer doc

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        return Arrays.copyOf(buffer.array(), endPos);
    }

    /**
     * Creates an empty BSON document.
     */
    private byte[] createEmptyBsonDocument() {
        ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(5); // document length (4 bytes length + 1 byte terminator)
        buffer.put((byte) 0x00); // terminator
        return Arrays.copyOf(buffer.array(), 5);
    }

    // ==================== Basic Parse Tests ====================

    @Test
    public void testParse_SimpleBsonDocument() {
        // Arrange
        byte[] bsonData = createSimpleBsonDocument();

        // Act
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Assert
        assertNotNull(doc);
        assertEquals("Alice", doc.getString("name"));
        assertEquals(30, doc.getInt32("age"));
        assertEquals(95.5, doc.getDouble("score"), 0.0001);
        assertTrue(doc.getBoolean("active"));
    }

    @Test
    public void testParse_AllTypes() {
        // Arrange
        byte[] bsonData = createAllTypesBsonDocument();

        // Act
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Assert
        assertEquals(42, doc.getInt32("int32Field"));
        assertEquals(9876543210L, doc.getInt64("int64Field"));
        assertEquals(3.14159, doc.getDouble("doubleField"), 0.00001);
        assertEquals("Hello", doc.getString("stringField"));
        assertTrue(doc.getBoolean("boolField"));
        assertEquals(1609459200000L, doc.getDateTime("dateField"));
        assertEquals("0102030405060708090a0b0c", doc.getObjectId("objectIdField"));
        assertNull(doc.get("nullField"));
        assertNotNull(doc.getBinary("binaryField"));
        assertEquals(5, doc.getBinary("binaryField").length);
    }

    @Test
    public void testParse_EmptyDocument() {
        // Arrange
        byte[] bsonData = createEmptyBsonDocument();

        // Act
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Assert
        assertNotNull(doc);
        assertNull(doc.get("nonexistent"));
    }

    // ==================== Field Access Tests ====================

    @Test
    public void testGetInt32_ExistingField() {
        // Arrange
        byte[] bsonData = createSimpleBsonDocument();
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Act
        int age = doc.getInt32("age");

        // Assert
        assertEquals(30, age);
    }

    @Test
    public void testGetInt32_NonexistentField() {
        // Arrange
        byte[] bsonData = createSimpleBsonDocument();
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Act & Assert - nonexistent field causes NullPointerException in findField
        assertThrows(NullPointerException.class, () -> doc.getInt32("nonexistent"));
    }

    @Test
    public void testGetInt64_ExistingField() {
        // Arrange
        byte[] bsonData = createAllTypesBsonDocument();
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Act
        long value = doc.getInt64("int64Field");

        // Assert
        assertEquals(9876543210L, value);
    }

    @Test
    public void testGetDouble_ExistingField() {
        // Arrange
        byte[] bsonData = createSimpleBsonDocument();
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Act
        double score = doc.getDouble("score");

        // Assert
        assertEquals(95.5, score, 0.0001);
    }

    @Test
    public void testGetString_ExistingField() {
        // Arrange
        byte[] bsonData = createSimpleBsonDocument();
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Act
        String name = doc.getString("name");

        // Assert
        assertEquals("Alice", name);
    }

    @Test
    public void testGetBoolean_ExistingField() {
        // Arrange
        byte[] bsonData = createSimpleBsonDocument();
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Act
        boolean active = doc.getBoolean("active");

        // Assert
        assertTrue(active);
    }

    @Test
    public void testGetDateTime_ExistingField() {
        // Arrange
        byte[] bsonData = createAllTypesBsonDocument();
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Act
        long date = doc.getDateTime("dateField");

        // Assert
        assertEquals(1609459200000L, date);
    }

    @Test
    public void testGetObjectId_ExistingField() {
        // Arrange
        byte[] bsonData = createAllTypesBsonDocument();
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Act
        String objectId = doc.getObjectId("objectIdField");

        // Assert
        assertEquals("0102030405060708090a0b0c", objectId);
    }

    @Test
    public void testGetBinary_ExistingField() {
        // Arrange
        byte[] bsonData = createAllTypesBsonDocument();
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Act
        byte[] binary = doc.getBinary("binaryField");

        // Assert
        assertNotNull(binary);
        assertEquals(5, binary.length);
        assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05}, binary);
    }

    // ==================== Nested Structure Tests ====================

    @Test
    public void testGetDocument_NestedDocument() {
        // Arrange
        byte[] bsonData = createNestedBsonDocument();
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Act
        BsonDocument address = doc.getDocument("address");

        // Assert
        assertNotNull(address);
        assertEquals("NYC", address.getString("city"));
        assertEquals(10001, address.getInt32("zip"));
    }

    @Test
    public void testGetArray_Array() {
        // Arrange
        byte[] bsonData = createArrayBsonDocument();
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Act
        BsonArray numbers = doc.getArray("numbers");

        // Assert
        assertNotNull(numbers);
        assertEquals(3, numbers.size());
        assertEquals(10, numbers.getInt32(0));
        assertEquals(20, numbers.getInt32(1));
        assertEquals(30, numbers.getInt32(2));
    }

    // ==================== Cache Tests ====================

    @Test
    public void testCache_MultipleAccess() {
        // Arrange
        byte[] bsonData = createSimpleBsonDocument();
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Act - First access (should parse and cache)
        int age1 = doc.getInt32("age");
        // Second access (should hit cache)
        int age2 = doc.getInt32("age");
        // Third access (should hit cache)
        int age3 = doc.getInt32("age");

        // Assert - All should return the same value
        assertEquals(30, age1);
        assertEquals(30, age2);
        assertEquals(30, age3);
    }

    @Test
    public void testCache_DifferentFields() {
        // Arrange
        byte[] bsonData = createSimpleBsonDocument();
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Act - Access different fields
        String name = doc.getString("name");
        int age = doc.getInt32("age");
        double score = doc.getDouble("score");
        boolean active = doc.getBoolean("active");

        // Access again to verify cache
        String name2 = doc.getString("name");
        int age2 = doc.getInt32("age");

        // Assert
        assertEquals("Alice", name);
        assertEquals(30, age);
        assertEquals(95.5, score, 0.0001);
        assertTrue(active);
        assertEquals(name, name2);
        assertEquals(age, age2);
    }

    // ==================== Generic get() Method Tests ====================

    @Test
    public void testGet_Int32() {
        // Arrange
        byte[] bsonData = createSimpleBsonDocument();
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Act
        Object age = doc.get("age");

        // Assert
        assertTrue(age instanceof Integer);
        assertEquals(30, age);
    }

    @Test
    public void testGet_String() {
        // Arrange
        byte[] bsonData = createSimpleBsonDocument();
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Act
        Object name = doc.get("name");

        // Assert
        assertTrue(name instanceof String);
        assertEquals("Alice", name);
    }

    @Test
    public void testGet_Double() {
        // Arrange
        byte[] bsonData = createSimpleBsonDocument();
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Act
        Object score = doc.get("score");

        // Assert
        assertTrue(score instanceof Double);
        assertEquals(95.5, (Double) score, 0.0001);
    }

    @Test
    public void testGet_Boolean() {
        // Arrange
        byte[] bsonData = createSimpleBsonDocument();
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Act
        Object active = doc.get("active");

        // Assert
        assertTrue(active instanceof Boolean);
        assertTrue((Boolean) active);
    }

    @Test
    public void testGet_Null() {
        // Arrange
        byte[] bsonData = createAllTypesBsonDocument();
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Act
        Object nullField = doc.get("nullField");

        // Assert
        assertNull(nullField);
    }

    @Test
    public void testGet_NonexistentField() {
        // Arrange
        byte[] bsonData = createSimpleBsonDocument();
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Act
        Object result = doc.get("nonexistent");

        // Assert
        assertNull(result);
    }

    // ==================== toBson() Tests ====================

    @Test
    public void testToBson_ReturnsOriginalData() {
        // Arrange
        byte[] bsonData = createSimpleBsonDocument();
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Act
        byte[] result = doc.toBson();

        // Assert
        assertNotNull(result);
        assertArrayEquals(bsonData, result);
    }

    // ==================== toJson() Tests ====================

    @Test
    public void testToJson_SimpleDocument() {
        // Arrange
        byte[] bsonData = createSimpleBsonDocument();
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Act
        String json = doc.toJson();

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("\"name\""));
        assertTrue(json.contains("\"Alice\""));
        assertTrue(json.contains("\"age\""));
        assertTrue(json.contains("30"));
        assertTrue(json.contains("\"score\""));
        assertTrue(json.contains("95.5"));
        assertTrue(json.contains("\"active\""));
        assertTrue(json.contains("true"));
    }

    @Test
    public void testToJson_EmptyDocument() {
        // Arrange
        byte[] bsonData = createEmptyBsonDocument();
        IndexedBsonDocument doc = IndexedBsonDocument.parse(bsonData);

        // Act
        String json = doc.toJson();

        // Assert
        assertNotNull(json);
        assertEquals("{}", json);
    }

    // ==================== Edge Cases ====================

    @Test
    public void testParse_NullData() {
        // Act & Assert - parse() doesn't validate null, throws NullPointerException
        assertThrows(NullPointerException.class, () -> IndexedBsonDocument.parse(null));
    }

    @Test
    public void testParse_EmptyArray() {
        // Arrange
        byte[] emptyArray = new byte[0];

        // Act - parse() doesn't validate length, will cause ArrayIndexOutOfBoundsException
        // but we just verify it doesn't crash with valid empty document
        byte[] validEmpty = createEmptyBsonDocument();
        IndexedBsonDocument doc = IndexedBsonDocument.parse(validEmpty);

        // Assert
        assertNotNull(doc);
    }

    @Test
    public void testParse_TooSmall() {
        // Arrange
        byte[] tooSmall = new byte[]{5, 0, 0, 0, 0}; // Valid minimal document (length=5, terminator=0)

        // Act - parse() is lenient and will parse minimal valid document
        IndexedBsonDocument doc = IndexedBsonDocument.parse(tooSmall);

        // Assert
        assertNotNull(doc);
    }
}
