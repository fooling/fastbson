package com.cloud.fastbson;

import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.document.BsonDocumentFactory;
import com.cloud.fastbson.document.IndexedBsonDocument;
import com.cloud.fastbson.document.fast.FastBsonDocumentFactory;
import com.cloud.fastbson.reader.BsonReader;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FastBson main entry point.
 *
 * Tests all public API methods to achieve 100% branch coverage.
 */
public class FastBsonTest {

    // ==================== Helper Methods ====================

    /**
     * Creates a simple BSON document: { "name": "Alice", "age": 30 }
     */
    private byte[] createSimpleBsonDocument() {
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

        // End of document
        buffer.put((byte) 0x00);

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);

        return Arrays.copyOf(buffer.array(), endPos);
    }

    // ==================== parse(byte[]) Tests ====================

    @Test
    public void testParse_ByteArray_ReturnsIndexedBsonDocument() {
        // Arrange
        byte[] bsonData = createSimpleBsonDocument();

        // Act
        BsonDocument doc = FastBson.parse(bsonData);

        // Assert
        assertNotNull(doc);
        assertTrue(doc instanceof IndexedBsonDocument);
        assertEquals("Alice", doc.getString("name"));
        assertEquals(30, doc.getInt32("age"));
    }

    @Test
    public void testParse_ByteArray_ZeroCopyBehavior() {
        // Arrange
        byte[] bsonData = createSimpleBsonDocument();

        // Act
        BsonDocument doc = FastBson.parse(bsonData);

        // Assert - toBson() should return original data (zero-copy)
        byte[] result = doc.toBson();
        assertArrayEquals(bsonData, result);
    }

    // ==================== parse(BsonReader) Tests ====================

    @Test
    public void testParse_BsonReader_ReturnsDocument() {
        // Arrange
        byte[] bsonData = createSimpleBsonDocument();
        BsonReader reader = new BsonReader(bsonData);

        // Act
        BsonDocument doc = FastBson.parse(reader);

        // Assert
        assertNotNull(doc);
        assertEquals("Alice", doc.getString("name"));
        assertEquals(30, doc.getInt32("age"));
    }

    @Test
    public void testParse_BsonReader_AdvancesPosition() {
        // Arrange
        byte[] bsonData = createSimpleBsonDocument();
        BsonReader reader = new BsonReader(bsonData);
        int initialPosition = reader.position();

        // Act
        BsonDocument doc = FastBson.parse(reader);

        // Assert
        assertTrue(reader.position() > initialPosition);
        assertEquals(bsonData.length, reader.position());
    }

    // ==================== Factory Methods Tests ====================

    @Test
    public void testGetDocumentFactory_DefaultIsFastFactory() {
        // Act
        BsonDocumentFactory factory = FastBson.getDocumentFactory();

        // Assert
        assertNotNull(factory);
        assertEquals(FastBsonDocumentFactory.INSTANCE, factory);
    }

    @Test
    public void testSetDocumentFactory_FastFactory() {
        // Arrange
        BsonDocumentFactory originalFactory = FastBson.getDocumentFactory();

        // Act
        FastBson.setDocumentFactory(FastBsonDocumentFactory.INSTANCE);
        BsonDocumentFactory factory = FastBson.getDocumentFactory();

        // Assert
        assertEquals(FastBsonDocumentFactory.INSTANCE, factory);

        // Restore
        FastBson.setDocumentFactory(originalFactory);
    }

    @Test
    public void testUseFastFactory() {
        // Arrange
        BsonDocumentFactory originalFactory = FastBson.getDocumentFactory();

        // Act
        FastBson.useFastFactory();
        BsonDocumentFactory factory = FastBson.getDocumentFactory();

        // Assert
        assertEquals(FastBsonDocumentFactory.INSTANCE, factory);

        // Restore
        FastBson.setDocumentFactory(originalFactory);
    }

    // ==================== Integration Tests ====================

    @Test
    public void testParse_LargeDocument() {
        // Arrange - Create document with 50 fields
        ByteBuffer buffer = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN);
        int startPos = buffer.position();
        buffer.putInt(0); // placeholder

        for (int i = 0; i < 50; i++) {
            buffer.put((byte) 0x10); // Int32
            buffer.put(("field" + i + "\0").getBytes(StandardCharsets.UTF_8));
            buffer.putInt(i * 100);
        }

        buffer.put((byte) 0x00); // End

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bsonData = Arrays.copyOf(buffer.array(), endPos);

        // Act
        BsonDocument doc = FastBson.parse(bsonData);

        // Assert
        assertNotNull(doc);
        assertEquals(0, doc.getInt32("field0"));
        assertEquals(100, doc.getInt32("field1"));
        assertEquals(4900, doc.getInt32("field49"));
    }

    @Test
    public void testParse_EmptyDocument() {
        // Arrange
        ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(5); // document length
        buffer.put((byte) 0x00); // terminator
        byte[] bsonData = Arrays.copyOf(buffer.array(), 5);

        // Act
        BsonDocument doc = FastBson.parse(bsonData);

        // Assert
        assertNotNull(doc);
    }

    @Test
    public void testParse_NestedDocument() {
        // Arrange
        ByteBuffer buffer = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN);

        int startPos = buffer.position();
        buffer.putInt(0); // placeholder for outer doc

        // Nested document: "user": { "name": "Bob", "age": 25 }
        buffer.put((byte) 0x03); // Document
        buffer.put("user\0".getBytes(StandardCharsets.UTF_8));

        int nestedStartPos = buffer.position();
        buffer.putInt(0); // placeholder for nested doc

        buffer.put((byte) 0x02); // String
        buffer.put("name\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(4);
        buffer.put("Bob\0".getBytes(StandardCharsets.UTF_8));

        buffer.put((byte) 0x10); // Int32
        buffer.put("age\0".getBytes(StandardCharsets.UTF_8));
        buffer.putInt(25);

        buffer.put((byte) 0x00); // End of nested doc

        int nestedEndPos = buffer.position();
        buffer.putInt(nestedStartPos, nestedEndPos - nestedStartPos);

        buffer.put((byte) 0x00); // End of outer doc

        int endPos = buffer.position();
        buffer.putInt(startPos, endPos - startPos);
        byte[] bsonData = Arrays.copyOf(buffer.array(), endPos);

        // Act
        BsonDocument doc = FastBson.parse(bsonData);

        // Assert
        assertNotNull(doc);
        BsonDocument user = doc.getDocument("user");
        assertNotNull(user);
        assertEquals("Bob", user.getString("name"));
        assertEquals(25, user.getInt32("age"));
    }
}
