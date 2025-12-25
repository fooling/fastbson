package com.cloud.fastbson.parser;

import com.cloud.fastbson.FastBson;
import com.cloud.fastbson.annotation.BsonField;
import com.cloud.fastbson.annotation.BsonSchema;
import com.cloud.fastbson.schema.SchemaIntrospector;
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
 * Integration tests for annotation-based PartialParser API.
 *
 * @author FastBSON
 * @since Phase 3.4
 */
public class AnnotationBasedParserTest {

    @BeforeEach
    public void setUp() {
        SchemaIntrospector.clearAllCache();
    }

    @AfterEach
    public void tearDown() {
        SchemaIntrospector.clearAllCache();
    }

    // ==================== Test Schema Classes ====================

    @BsonSchema("User")
    static class UserEntity {
        @BsonField(value = "_id", order = 1)
        private String id;

        @BsonField(value = "name", order = 2)
        private String name;

        @BsonField(value = "age", order = 3)
        private Integer age;

        @BsonField(value = "email", order = 4)
        private String email;

        @BsonField(value = "city", order = 5)
        private String city;
    }

    @BsonSchema("Order")
    static class OrderEntity {
        @BsonField(value = "_id", order = 1)
        private String id;

        @BsonField(value = "orderId", order = 2)
        private String orderId;

        @BsonField(value = "userId", order = 3)
        private Integer userId;

        @BsonField(value = "total", order = 4)
        private Integer total;

        @BsonField(value = "status", order = 5)
        private String status;
    }

    static class NoSchemaEntity {
        private String field1;
        private String field2;
    }

    // ==================== Tests ====================

    @Test
    public void testConstructor_WithClass_Success() {
        // Arrange
        BsonDocument doc = new BsonDocument()
            .append("_id", new BsonInt32(1))
            .append("name", new BsonString("Alice"))
            .append("age", new BsonInt32(30))
            .append("email", new BsonString("alice@example.com"))
            .append("city", new BsonString("NYC"));

        byte[] bsonData = serializeDocument(doc);

        // Act
        PartialParser parser = new PartialParser(UserEntity.class, "name", "email");
        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertEquals(2, result.size());
        assertEquals("Alice", result.get("name"));
        assertEquals("alice@example.com", result.get("email"));
    }

    @Test
    public void testConstructor_WithClass_NullClass_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            new PartialParser((Class<?>) null, "field1"));
    }

    @Test
    public void testConstructor_WithClass_NoTargetFields_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            new PartialParser(UserEntity.class, new String[0]));
    }

    @Test
    public void testBuilderAPI_Success() {
        // Arrange
        BsonDocument doc = new BsonDocument()
            .append("_id", new BsonInt32(1))
            .append("name", new BsonString("Bob"))
            .append("age", new BsonInt32(25))
            .append("email", new BsonString("bob@example.com"))
            .append("city", new BsonString("LA"));

        byte[] bsonData = serializeDocument(doc);

        // Act
        PartialParser parser = FastBson.forClass(UserEntity.class)
            .selectFields("name", "city")
            .setEarlyExit(true)
            .build();

        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertEquals(2, result.size());
        assertEquals("Bob", result.get("name"));
        assertEquals("LA", result.get("city"));
    }

    @Test
    public void testBuilderAPI_WithoutSelectFields_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
            FastBson.forClass(UserEntity.class).build());
    }

    @Test
    public void testBuilderAPI_NullClass_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            FastBson.forClass(null));
    }

    @Test
    public void testBuilderAPI_ChainedCalls() {
        // Arrange
        BsonDocument doc = new BsonDocument()
            .append("_id", new BsonInt32(1))
            .append("orderId", new BsonString("ORD-001"))
            .append("userId", new BsonInt32(100))
            .append("total", new BsonInt32(1500))
            .append("status", new BsonString("completed"));

        byte[] bsonData = serializeDocument(doc);

        // Act
        PartialParser parser = FastBson.forClass(OrderEntity.class)
            .selectFields("orderId", "total")
            .setEarlyExit(false)
            .build();

        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertEquals(2, result.size());
        assertEquals("ORD-001", result.get("orderId"));
        assertEquals(1500, result.get("total"));
    }

    @Test
    public void testWithSchemaClass_PerfectOrderMatch() {
        // Arrange - Document with perfect field order matching schema
        BsonDocument doc = new BsonDocument()
            .append("_id", new BsonInt32(1))
            .append("name", new BsonString("Charlie"))
            .append("age", new BsonInt32(35))
            .append("email", new BsonString("charlie@example.com"))
            .append("city", new BsonString("SF"));

        byte[] bsonData = serializeDocument(doc);

        // Act
        PartialParser parser = new PartialParser(UserEntity.class, "name", "email", "city");
        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertEquals(3, result.size());
        assertEquals("Charlie", result.get("name"));
        assertEquals("charlie@example.com", result.get("email"));
        assertEquals("SF", result.get("city"));
    }

    @Test
    public void testWithSchemaClass_ExtraFieldsInDocument() {
        // Arrange - Document has extra fields not in schema
        BsonDocument doc = new BsonDocument()
            .append("_id", new BsonInt32(1))
            .append("name", new BsonString("Dave"))
            .append("age", new BsonInt32(28))
            .append("email", new BsonString("dave@example.com"))
            .append("city", new BsonString("Boston"))
            .append("country", new BsonString("USA"))  // Extra field
            .append("phone", new BsonString("123-456-7890"));  // Extra field

        byte[] bsonData = serializeDocument(doc);

        // Act
        PartialParser parser = new PartialParser(UserEntity.class, "name", "email");
        Map<String, Object> result = parser.parse(bsonData);

        // Assert - Should still parse successfully
        assertEquals(2, result.size());
        assertEquals("Dave", result.get("name"));
        assertEquals("dave@example.com", result.get("email"));
    }

    @Test
    public void testWithSchemaClass_MissingFieldsInDocument() {
        // Arrange - Document is missing some fields from schema
        BsonDocument doc = new BsonDocument()
            .append("_id", new BsonInt32(1))
            .append("name", new BsonString("Eve"))
            .append("email", new BsonString("eve@example.com"));
        // Missing: age, city

        byte[] bsonData = serializeDocument(doc);

        // Act
        PartialParser parser = new PartialParser(UserEntity.class, "name", "email", "city");
        Map<String, Object> result = parser.parse(bsonData);

        // Assert - Should parse available fields
        assertEquals(2, result.size());
        assertEquals("Eve", result.get("name"));
        assertEquals("eve@example.com", result.get("email"));
        assertFalse(result.containsKey("city"));
    }

    @Test
    public void testWithSchemaClass_NoSchemaMetadata() {
        // Arrange - Class without @BsonField annotations
        BsonDocument doc = new BsonDocument()
            .append("field1", new BsonString("value1"))
            .append("field2", new BsonString("value2"));

        byte[] bsonData = serializeDocument(doc);

        // Act - Should fall back to standard FieldMatcher
        PartialParser parser = new PartialParser(NoSchemaEntity.class, "field1");
        Map<String, Object> result = parser.parse(bsonData);

        // Assert - Should still work (no optimization, but functional)
        assertEquals(1, result.size());
        assertEquals("value1", result.get("field1"));
    }

    @Test
    public void testMultipleInstances_SameClass() {
        // Arrange
        BsonDocument doc1 = new BsonDocument()
            .append("_id", new BsonInt32(1))
            .append("name", new BsonString("User1"))
            .append("age", new BsonInt32(25))
            .append("email", new BsonString("user1@example.com"))
            .append("city", new BsonString("NYC"));

        BsonDocument doc2 = new BsonDocument()
            .append("_id", new BsonInt32(2))
            .append("name", new BsonString("User2"))
            .append("age", new BsonInt32(30))
            .append("email", new BsonString("user2@example.com"))
            .append("city", new BsonString("LA"));

        byte[] bsonData1 = serializeDocument(doc1);
        byte[] bsonData2 = serializeDocument(doc2);

        // Act - Create multiple parsers with same schema class
        PartialParser parser1 = new PartialParser(UserEntity.class, "name", "email");
        PartialParser parser2 = new PartialParser(UserEntity.class, "name", "city");

        Map<String, Object> result1 = parser1.parse(bsonData1);
        Map<String, Object> result2 = parser2.parse(bsonData2);

        // Assert
        assertEquals(2, result1.size());
        assertEquals("User1", result1.get("name"));
        assertEquals("user1@example.com", result1.get("email"));

        assertEquals(2, result2.size());
        assertEquals("User2", result2.get("name"));
        assertEquals("LA", result2.get("city"));

        // Verify schema was cached only once
        assertEquals(1, SchemaIntrospector.getCacheSize());
    }

    @Test
    public void testDifferentClasses() {
        // Arrange
        BsonDocument userDoc = new BsonDocument()
            .append("_id", new BsonInt32(1))
            .append("name", new BsonString("Alice"))
            .append("age", new BsonInt32(30))
            .append("email", new BsonString("alice@example.com"))
            .append("city", new BsonString("NYC"));

        BsonDocument orderDoc = new BsonDocument()
            .append("_id", new BsonInt32(1))
            .append("orderId", new BsonString("ORD-001"))
            .append("userId", new BsonInt32(100))
            .append("total", new BsonInt32(1500))
            .append("status", new BsonString("completed"));

        byte[] userBsonData = serializeDocument(userDoc);
        byte[] orderBsonData = serializeDocument(orderDoc);

        // Act
        PartialParser userParser = new PartialParser(UserEntity.class, "name", "email");
        PartialParser orderParser = new PartialParser(OrderEntity.class, "orderId", "total");

        Map<String, Object> userResult = userParser.parse(userBsonData);
        Map<String, Object> orderResult = orderParser.parse(orderBsonData);

        // Assert
        assertEquals("Alice", userResult.get("name"));
        assertEquals("alice@example.com", userResult.get("email"));

        assertEquals("ORD-001", orderResult.get("orderId"));
        assertEquals(1500, orderResult.get("total"));

        // Verify both schemas were cached
        assertEquals(2, SchemaIntrospector.getCacheSize());
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
