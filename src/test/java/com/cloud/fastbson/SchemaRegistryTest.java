package com.cloud.fastbson;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FastBson schema registry functionality.
 *
 * @author FastBSON
 * @since Phase 3.4
 */
public class SchemaRegistryTest {

    @BeforeEach
    public void setUp() {
        // Clear schemas before each test
        FastBson.clearSchemas();
    }

    @AfterEach
    public void tearDown() {
        // Clear schemas after each test
        FastBson.clearSchemas();
    }

    @Test
    public void testRegisterSchema() {
        // Arrange
        String schemaName = "User";
        String[] fieldOrder = {"_id", "name", "age", "email", "city"};

        // Act
        FastBson.registerSchema(schemaName, fieldOrder);

        // Assert
        assertTrue(FastBson.isSchemaRegistered(schemaName));
        assertArrayEquals(fieldOrder, FastBson.getSchemaFieldOrder(schemaName));
    }

    @Test
    public void testRegisterSchema_NullName_ThrowsException() {
        // Arrange
        String[] fieldOrder = {"_id", "name"};

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            FastBson.registerSchema(null, fieldOrder));
    }

    @Test
    public void testRegisterSchema_EmptyName_ThrowsException() {
        // Arrange
        String[] fieldOrder = {"_id", "name"};

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            FastBson.registerSchema("", fieldOrder));
    }

    @Test
    public void testRegisterSchema_NullFieldOrder_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            FastBson.registerSchema("User", (String[]) null));
    }

    @Test
    public void testRegisterSchema_EmptyFieldOrder_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            FastBson.registerSchema("User", new String[0]));
    }

    @Test
    public void testRegisterSchema_Overwrite() {
        // Arrange
        String schemaName = "User";
        String[] originalOrder = {"_id", "name", "age"};
        String[] newOrder = {"_id", "name", "age", "email", "city"};

        // Act
        FastBson.registerSchema(schemaName, originalOrder);
        FastBson.registerSchema(schemaName, newOrder);

        // Assert
        assertArrayEquals(newOrder, FastBson.getSchemaFieldOrder(schemaName),
            "Schema should be overwritten with new field order");
    }

    @Test
    public void testGetSchemaFieldOrder_NotRegistered_ReturnsNull() {
        // Act
        String[] fieldOrder = FastBson.getSchemaFieldOrder("NonExistent");

        // Assert
        assertNull(fieldOrder);
    }

    @Test
    public void testIsSchemaRegistered_True() {
        // Arrange
        FastBson.registerSchema("User", "_id", "name");

        // Act & Assert
        assertTrue(FastBson.isSchemaRegistered("User"));
    }

    @Test
    public void testIsSchemaRegistered_False() {
        // Act & Assert
        assertFalse(FastBson.isSchemaRegistered("NonExistent"));
    }

    @Test
    public void testGetSchemaCount() {
        // Arrange
        assertEquals(0, FastBson.getSchemaCount(), "Initial count should be 0");

        // Act
        FastBson.registerSchema("User", "_id", "name");
        FastBson.registerSchema("Order", "_id", "orderId");

        // Assert
        assertEquals(2, FastBson.getSchemaCount());
    }

    @Test
    public void testClearSchemas() {
        // Arrange
        FastBson.registerSchema("User", "_id", "name");
        FastBson.registerSchema("Order", "_id", "orderId");
        assertEquals(2, FastBson.getSchemaCount());

        // Act
        FastBson.clearSchemas();

        // Assert
        assertEquals(0, FastBson.getSchemaCount());
        assertFalse(FastBson.isSchemaRegistered("User"));
        assertFalse(FastBson.isSchemaRegistered("Order"));
    }

    @Test
    public void testMultipleSchemas() {
        // Arrange
        String[] userOrder = {"_id", "name", "age", "email"};
        String[] orderOrder = {"_id", "orderId", "userId", "total"};
        String[] productOrder = {"_id", "productId", "name", "price"};

        // Act
        FastBson.registerSchema("User", userOrder);
        FastBson.registerSchema("Order", orderOrder);
        FastBson.registerSchema("Product", productOrder);

        // Assert
        assertEquals(3, FastBson.getSchemaCount());
        assertArrayEquals(userOrder, FastBson.getSchemaFieldOrder("User"));
        assertArrayEquals(orderOrder, FastBson.getSchemaFieldOrder("Order"));
        assertArrayEquals(productOrder, FastBson.getSchemaFieldOrder("Product"));
    }

    @Test
    public void testSchemaIsolation() {
        // Arrange
        String[] userOrder = {"_id", "name", "age"};
        FastBson.registerSchema("User", userOrder);

        // Act - Get schema and modify the returned array
        String[] retrieved = FastBson.getSchemaFieldOrder("User");
        retrieved[0] = "modified";

        // Assert - Schema returns same reference (no defensive copy for performance)
        String[] original = FastBson.getSchemaFieldOrder("User");
        assertEquals("modified", original[0], "Schema should return same reference (not defensive copy)");
        assertEquals("modified", retrieved[0], "Retrieved array was modified");
        assertSame(retrieved, original, "Should return same array reference");
    }
}
