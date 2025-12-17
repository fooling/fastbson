package com.cloud.fastbson.parser;

import com.cloud.fastbson.FastBson;
import com.cloud.fastbson.util.SchemaLearner;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.bson.io.BasicOutputBuffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PartialParser with schema optimization.
 *
 * @author FastBSON
 * @since Phase 3.4
 */
public class PartialParserSchemaTest {

    @BeforeEach
    public void setUp() {
        FastBson.clearSchemas();
        SchemaLearner.clearAll();
    }

    @AfterEach
    public void tearDown() {
        FastBson.clearSchemas();
        SchemaLearner.clearAll();
    }

    // ==================== forSchema() Tests ====================

    @Test
    public void testForSchema_RegisteredSchema_Success() {
        // Arrange
        FastBson.registerSchema("User", "_id", "name", "age", "email", "city");

        BsonDocument doc = new BsonDocument()
            .append("_id", new BsonInt32(1))
            .append("name", new BsonString("Alice"))
            .append("age", new BsonInt32(30))
            .append("email", new BsonString("alice@example.com"))
            .append("city", new BsonString("NYC"));

        byte[] bsonData = serializeDocument(doc);

        // Act
        PartialParser parser = new PartialParser("name", "email")
            .forSchema("User");

        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertEquals(2, result.size());
        assertEquals("Alice", result.get("name"));
        assertEquals("alice@example.com", result.get("email"));
    }

    @Test
    public void testForSchema_NotRegistered_ThrowsException() {
        // Arrange
        PartialParser parser = new PartialParser("name");

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
            parser.forSchema("NonExistent"));
    }

    @Test
    public void testForSchema_PerfectOrderMatch() {
        // Arrange - Register schema
        FastBson.registerSchema("User", "_id", "name", "age", "email", "city");

        // Create document with perfect field order
        BsonDocument doc = new BsonDocument()
            .append("_id", new BsonInt32(1))
            .append("name", new BsonString("Bob"))
            .append("age", new BsonInt32(25))
            .append("email", new BsonString("bob@example.com"))
            .append("city", new BsonString("LA"));

        byte[] bsonData = serializeDocument(doc);

        // Act
        PartialParser parser = new PartialParser("name", "email", "city")
            .forSchema("User");

        Map<String, Object> result = parser.parse(bsonData);

        // Assert - All fields should be found
        assertEquals(3, result.size());
        assertEquals("Bob", result.get("name"));
        assertEquals("bob@example.com", result.get("email"));
        assertEquals("LA", result.get("city"));
    }

    // ==================== withFieldOrder() Tests ====================

    @Test
    public void testWithFieldOrder_DirectUsage() {
        // Arrange
        BsonDocument doc = new BsonDocument()
            .append("_id", new BsonInt32(1))
            .append("name", new BsonString("Charlie"))
            .append("age", new BsonInt32(35))
            .append("email", new BsonString("charlie@example.com"));

        byte[] bsonData = serializeDocument(doc);

        // Act
        PartialParser parser = new PartialParser("name", "email")
            .withFieldOrder("_id", "name", "age", "email");

        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertEquals(2, result.size());
        assertEquals("Charlie", result.get("name"));
        assertEquals("charlie@example.com", result.get("email"));
    }

    @Test
    public void testWithFieldOrder_NullFieldOrder_ThrowsException() {
        // Arrange
        PartialParser parser = new PartialParser("name");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            parser.withFieldOrder((String[]) null));
    }

    @Test
    public void testWithFieldOrder_EmptyFieldOrder_ThrowsException() {
        // Arrange
        PartialParser parser = new PartialParser("name");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            parser.withFieldOrder(new String[0]));
    }

    // ==================== Auto-Learning Tests ====================

    @Test
    public void testAutoLearn_FirstParseLearns_SecondParseOptimizes() {
        // Arrange
        BsonDocument doc1 = new BsonDocument()
            .append("_id", new BsonInt32(1))
            .append("name", new BsonString("Dave"))
            .append("age", new BsonInt32(28))
            .append("email", new BsonString("dave@example.com"));

        BsonDocument doc2 = new BsonDocument()
            .append("_id", new BsonInt32(2))
            .append("name", new BsonString("Eve"))
            .append("age", new BsonInt32(32))
            .append("email", new BsonString("eve@example.com"));

        byte[] bsonData1 = serializeDocument(doc1);
        byte[] bsonData2 = serializeDocument(doc2);

        // Act
        PartialParser parser = new PartialParser("name", "email")
            .withSchemaId("TestUser")
            .enableAutoLearn();

        // First parse: learns field order
        Map<String, Object> result1 = parser.parse(bsonData1);
        assertEquals("Dave", result1.get("name"));
        assertEquals("dave@example.com", result1.get("email"));

        // Check that learning finished
        assertTrue(SchemaLearner.isLearned("TestUser"));

        // Second parse: uses learned order
        Map<String, Object> result2 = parser.parse(bsonData2);
        assertEquals("Eve", result2.get("name"));
        assertEquals("eve@example.com", result2.get("email"));

        // Verify learned order was registered globally
        assertTrue(FastBson.isSchemaRegistered("TestUser"));
        String[] learnedOrder = FastBson.getSchemaFieldOrder("TestUser");
        assertArrayEquals(new String[]{"_id", "name", "age", "email"}, learnedOrder);
    }

    @Test
    public void testAutoLearn_WithoutSchemaId_NoLearning() {
        // Arrange
        BsonDocument doc = new BsonDocument()
            .append("_id", new BsonInt32(1))
            .append("name", new BsonString("Frank"));

        byte[] bsonData = serializeDocument(doc);

        // Act - Enable autoLearn but don't set schemaId
        PartialParser parser = new PartialParser("name")
            .enableAutoLearn(); // No withSchemaId()

        Map<String, Object> result = parser.parse(bsonData);

        // Assert - Should parse successfully but not learn
        assertEquals("Frank", result.get("name"));
        assertEquals(0, SchemaLearner.getLearnedSchemaCount());
    }

    @Test
    public void testFinishLearning_Manual() {
        // Arrange
        BsonDocument doc = new BsonDocument()
            .append("_id", new BsonInt32(1))
            .append("name", new BsonString("Grace"))
            .append("email", new BsonString("grace@example.com"));

        byte[] bsonData = serializeDocument(doc);

        // Act
        PartialParser parser = new PartialParser("name", "email")
            .withSchemaId("ManualUser")
            .enableAutoLearn();

        // Parse once to learn
        parser.parse(bsonData);

        // Manually finish learning (already finished automatically after first parse)
        parser.finishLearning();

        // Assert
        assertTrue(SchemaLearner.isLearned("ManualUser"));
        assertFalse(SchemaLearner.isLearning("ManualUser"));
    }

    // ==================== Method Chaining Tests ====================

    @Test
    public void testMethodChaining() {
        // Arrange
        BsonDocument doc = new BsonDocument()
            .append("_id", new BsonInt32(1))
            .append("name", new BsonString("Henry"));

        byte[] bsonData = serializeDocument(doc);

        // Act - All methods return this for chaining
        PartialParser parser = new PartialParser("name")
            .withSchemaId("ChainTest")
            .enableAutoLearn()
            .setEarlyExit(true);

        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertNotNull(result);
        assertEquals("Henry", result.get("name"));
    }

    // ==================== Integration Tests ====================

    @Test
    public void testIntegration_RegisteredSchema_MultipleParses() {
        // Arrange
        FastBson.registerSchema("Order", "_id", "orderId", "userId", "total", "status");

        BsonDocument[] docs = {
            new BsonDocument()
                .append("_id", new BsonInt32(1))
                .append("orderId", new BsonString("ORD-001"))
                .append("userId", new BsonInt32(100))
                .append("total", new BsonInt32(1500))
                .append("status", new BsonString("completed")),
            new BsonDocument()
                .append("_id", new BsonInt32(2))
                .append("orderId", new BsonString("ORD-002"))
                .append("userId", new BsonInt32(101))
                .append("total", new BsonInt32(2500))
                .append("status", new BsonString("pending")),
            new BsonDocument()
                .append("_id", new BsonInt32(3))
                .append("orderId", new BsonString("ORD-003"))
                .append("userId", new BsonInt32(102))
                .append("total", new BsonInt32(3500))
                .append("status", new BsonString("shipped"))
        };

        // Act
        PartialParser parser = new PartialParser("orderId", "total")
            .forSchema("Order");

        // Parse multiple documents
        for (int i = 0; i < docs.length; i++) {
            byte[] bsonData = serializeDocument(docs[i]);
            Map<String, Object> result = parser.parse(bsonData);

            // Assert
            assertEquals(2, result.size());
            assertNotNull(result.get("orderId"));
            assertNotNull(result.get("total"));
        }
    }

    @Test
    public void testIntegration_AutoLearn_PersistentParser() {
        // Arrange
        BsonDocument[] userDocs = {
            new BsonDocument()
                .append("_id", new BsonInt32(1))
                .append("username", new BsonString("user1"))
                .append("email", new BsonString("user1@test.com"))
                .append("age", new BsonInt32(25)),
            new BsonDocument()
                .append("_id", new BsonInt32(2))
                .append("username", new BsonString("user2"))
                .append("email", new BsonString("user2@test.com"))
                .append("age", new BsonInt32(30)),
            new BsonDocument()
                .append("_id", new BsonInt32(3))
                .append("username", new BsonString("user3"))
                .append("email", new BsonString("user3@test.com"))
                .append("age", new BsonInt32(35))
        };

        // Act
        PartialParser parser = new PartialParser("username", "email")
            .withSchemaId("PersistentUser")
            .enableAutoLearn();

        // First parse: learns
        Map<String, Object> result1 = parser.parse(serializeDocument(userDocs[0]));
        assertEquals("user1", result1.get("username"));

        // Subsequent parses: uses learned order
        Map<String, Object> result2 = parser.parse(serializeDocument(userDocs[1]));
        assertEquals("user2", result2.get("username"));

        Map<String, Object> result3 = parser.parse(serializeDocument(userDocs[2]));
        assertEquals("user3", result3.get("username"));

        // Verify learning happened only once
        assertEquals(1, SchemaLearner.getLearnedSchemaCount());
        String[] learned = SchemaLearner.getLearnedFieldOrder("PersistentUser");
        assertArrayEquals(new String[]{"_id", "username", "email", "age"}, learned);
    }

    // ==================== Edge Cases ====================

    @Test
    public void testEdgeCase_SchemaWithExtraFields() {
        // Arrange - Schema has more fields than document
        FastBson.registerSchema("PartialUser", "_id", "name", "age", "email", "city", "country");

        BsonDocument doc = new BsonDocument()
            .append("_id", new BsonInt32(1))
            .append("name", new BsonString("Incomplete"))
            .append("email", new BsonString("incomplete@test.com"));
        // Missing: age, city, country

        byte[] bsonData = serializeDocument(doc);

        // Act
        PartialParser parser = new PartialParser("name", "email")
            .forSchema("PartialUser");

        Map<String, Object> result = parser.parse(bsonData);

        // Assert - Should still parse successfully
        assertEquals(2, result.size());
        assertEquals("Incomplete", result.get("name"));
        assertEquals("incomplete@test.com", result.get("email"));
    }

    @Test
    public void testEdgeCase_DocumentWithExtraFields() {
        // Arrange - Document has more fields than schema
        FastBson.registerSchema("MinimalUser", "_id", "name");

        BsonDocument doc = new BsonDocument()
            .append("_id", new BsonInt32(1))
            .append("name", new BsonString("Extra"))
            .append("age", new BsonInt32(40))
            .append("email", new BsonString("extra@test.com"))
            .append("city", new BsonString("Boston"));

        byte[] bsonData = serializeDocument(doc);

        // Act
        PartialParser parser = new PartialParser("name", "email")
            .forSchema("MinimalUser");

        Map<String, Object> result = parser.parse(bsonData);

        // Assert - Should still parse successfully (fallback to slow path for email)
        assertEquals(2, result.size());
        assertEquals("Extra", result.get("name"));
        assertEquals("extra@test.com", result.get("email"));
    }

    // ==================== Helper Methods ====================

    private byte[] serializeDocument(BsonDocument doc) {
        BasicOutputBuffer buffer = new BasicOutputBuffer();
        new org.bson.codecs.BsonDocumentCodec().encode(
            new org.bson.BsonBinaryWriter(buffer),
            doc,
            org.bson.codecs.EncoderContext.builder().build()
        );
        return buffer.toByteArray();
    }
}
