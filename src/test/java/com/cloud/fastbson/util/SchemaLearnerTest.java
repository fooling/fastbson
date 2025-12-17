package com.cloud.fastbson.util;

import com.cloud.fastbson.FastBson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SchemaLearner.
 *
 * @author FastBSON
 * @since Phase 3.4
 */
public class SchemaLearnerTest {

    @BeforeEach
    public void setUp() {
        // Clear all learned schemas before each test
        SchemaLearner.clearAll();
        FastBson.clearSchemas();
    }

    @AfterEach
    public void tearDown() {
        // Clear all learned schemas after each test
        SchemaLearner.clearAll();
        FastBson.clearSchemas();
    }

    @Test
    public void testObserveField() {
        // Arrange
        String schemaId = "User";

        // Act
        SchemaLearner.observeField(schemaId, "_id");
        SchemaLearner.observeField(schemaId, "name");
        SchemaLearner.observeField(schemaId, "age");

        // Assert
        assertTrue(SchemaLearner.isLearning(schemaId));
        assertFalse(SchemaLearner.isLearned(schemaId));
    }

    @Test
    public void testObserveField_NullSchemaId_NoError() {
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> SchemaLearner.observeField(null, "field"));
    }

    @Test
    public void testObserveField_NullFieldName_NoError() {
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> SchemaLearner.observeField("Schema", null));
    }

    @Test
    public void testFinishLearning() {
        // Arrange
        String schemaId = "User";
        SchemaLearner.observeField(schemaId, "_id");
        SchemaLearner.observeField(schemaId, "name");
        SchemaLearner.observeField(schemaId, "age");

        // Act
        SchemaLearner.finishLearning(schemaId);

        // Assert
        assertTrue(SchemaLearner.isLearned(schemaId));
        assertFalse(SchemaLearner.isLearning(schemaId));

        String[] learnedOrder = SchemaLearner.getLearnedFieldOrder(schemaId);
        assertNotNull(learnedOrder);
        assertArrayEquals(new String[]{"_id", "name", "age"}, learnedOrder);
    }

    @Test
    public void testFinishLearning_NullSchemaId_NoError() {
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> SchemaLearner.finishLearning(null));
    }

    @Test
    public void testFinishLearning_NonExistentSchema_NoError() {
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> SchemaLearner.finishLearning("NonExistent"));
    }

    @Test
    public void testFinishLearning_RegistersToGlobalRegistry() {
        // Arrange
        String schemaId = "User";
        SchemaLearner.observeField(schemaId, "_id");
        SchemaLearner.observeField(schemaId, "name");

        // Act
        SchemaLearner.finishLearning(schemaId);

        // Assert - Should be registered to FastBson global registry
        assertTrue(FastBson.isSchemaRegistered(schemaId));
        String[] globalOrder = FastBson.getSchemaFieldOrder(schemaId);
        assertArrayEquals(new String[]{"_id", "name"}, globalOrder);
    }

    @Test
    public void testFinishLearning_Idempotent() {
        // Arrange
        String schemaId = "User";
        SchemaLearner.observeField(schemaId, "_id");
        SchemaLearner.observeField(schemaId, "name");

        // Act - Finish learning twice
        SchemaLearner.finishLearning(schemaId);
        String[] firstResult = SchemaLearner.getLearnedFieldOrder(schemaId);

        SchemaLearner.finishLearning(schemaId);
        String[] secondResult = SchemaLearner.getLearnedFieldOrder(schemaId);

        // Assert - Should return same result
        assertArrayEquals(firstResult, secondResult);
    }

    @Test
    public void testObserveField_AfterFinishLearning_Ignored() {
        // Arrange
        String schemaId = "User";
        SchemaLearner.observeField(schemaId, "_id");
        SchemaLearner.finishLearning(schemaId);

        String[] beforeAdditional = SchemaLearner.getLearnedFieldOrder(schemaId);

        // Act - Try to observe more fields after learning finished
        SchemaLearner.observeField(schemaId, "name");
        SchemaLearner.observeField(schemaId, "age");

        // Assert - Additional fields should be ignored
        String[] afterAdditional = SchemaLearner.getLearnedFieldOrder(schemaId);
        assertArrayEquals(beforeAdditional, afterAdditional);
    }

    @Test
    public void testGetLearnedFieldOrder_NotLearned_ReturnsNull() {
        // Arrange
        String schemaId = "User";
        SchemaLearner.observeField(schemaId, "_id");
        // Don't finish learning

        // Act
        String[] result = SchemaLearner.getLearnedFieldOrder(schemaId);

        // Assert
        assertNull(result);
    }

    @Test
    public void testGetLearnedFieldOrder_NullSchemaId_ReturnsNull() {
        // Act
        String[] result = SchemaLearner.getLearnedFieldOrder(null);

        // Assert
        assertNull(result);
    }

    @Test
    public void testGetLearnedFieldOrder_NonExistent_ReturnsNull() {
        // Act
        String[] result = SchemaLearner.getLearnedFieldOrder("NonExistent");

        // Assert
        assertNull(result);
    }

    @Test
    public void testIsLearned_True() {
        // Arrange
        String schemaId = "User";
        SchemaLearner.observeField(schemaId, "_id");
        SchemaLearner.finishLearning(schemaId);

        // Act & Assert
        assertTrue(SchemaLearner.isLearned(schemaId));
    }

    @Test
    public void testIsLearned_False_NotFinished() {
        // Arrange
        String schemaId = "User";
        SchemaLearner.observeField(schemaId, "_id");
        // Don't finish learning

        // Act & Assert
        assertFalse(SchemaLearner.isLearned(schemaId));
    }

    @Test
    public void testIsLearned_False_NonExistent() {
        // Act & Assert
        assertFalse(SchemaLearner.isLearned("NonExistent"));
    }

    @Test
    public void testIsLearned_NullSchemaId_ReturnsFalse() {
        // Act & Assert
        assertFalse(SchemaLearner.isLearned(null));
    }

    @Test
    public void testIsLearning_True() {
        // Arrange
        String schemaId = "User";
        SchemaLearner.observeField(schemaId, "_id");

        // Act & Assert
        assertTrue(SchemaLearner.isLearning(schemaId));
    }

    @Test
    public void testIsLearning_False_AfterFinished() {
        // Arrange
        String schemaId = "User";
        SchemaLearner.observeField(schemaId, "_id");
        SchemaLearner.finishLearning(schemaId);

        // Act & Assert
        assertFalse(SchemaLearner.isLearning(schemaId));
    }

    @Test
    public void testIsLearning_False_NonExistent() {
        // Act & Assert
        assertFalse(SchemaLearner.isLearning("NonExistent"));
    }

    @Test
    public void testIsLearning_NullSchemaId_ReturnsFalse() {
        // Act & Assert
        assertFalse(SchemaLearner.isLearning(null));
    }

    @Test
    public void testClearSchema() {
        // Arrange
        String schemaId = "User";
        SchemaLearner.observeField(schemaId, "_id");
        SchemaLearner.finishLearning(schemaId);
        assertTrue(SchemaLearner.isLearned(schemaId));

        // Act
        SchemaLearner.clearSchema(schemaId);

        // Assert
        assertFalse(SchemaLearner.isLearned(schemaId));
        assertNull(SchemaLearner.getLearnedFieldOrder(schemaId));
    }

    @Test
    public void testClearSchema_NullSchemaId_NoError() {
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> SchemaLearner.clearSchema(null));
    }

    @Test
    public void testClearAll() {
        // Arrange
        SchemaLearner.observeField("User", "_id");
        SchemaLearner.finishLearning("User");
        SchemaLearner.observeField("Order", "_id");
        SchemaLearner.finishLearning("Order");

        assertEquals(2, SchemaLearner.getSchemaCount());

        // Act
        SchemaLearner.clearAll();

        // Assert
        assertEquals(0, SchemaLearner.getSchemaCount());
        assertFalse(SchemaLearner.isLearned("User"));
        assertFalse(SchemaLearner.isLearned("Order"));
    }

    @Test
    public void testGetSchemaCount() {
        // Arrange
        assertEquals(0, SchemaLearner.getSchemaCount());

        // Act
        SchemaLearner.observeField("User", "_id");
        assertEquals(1, SchemaLearner.getSchemaCount());

        SchemaLearner.observeField("Order", "_id");
        assertEquals(2, SchemaLearner.getSchemaCount());

        // Finish learning doesn't change count
        SchemaLearner.finishLearning("User");
        assertEquals(2, SchemaLearner.getSchemaCount());
    }

    @Test
    public void testGetLearnedSchemaCount() {
        // Arrange
        assertEquals(0, SchemaLearner.getLearnedSchemaCount());

        // Act
        SchemaLearner.observeField("User", "_id");
        assertEquals(0, SchemaLearner.getLearnedSchemaCount(), "Learning not finished");

        SchemaLearner.finishLearning("User");
        assertEquals(1, SchemaLearner.getLearnedSchemaCount());

        SchemaLearner.observeField("Order", "_id");
        assertEquals(1, SchemaLearner.getLearnedSchemaCount(), "Order not finished");

        SchemaLearner.finishLearning("Order");
        assertEquals(2, SchemaLearner.getLearnedSchemaCount());
    }

    @Test
    public void testMultipleSchemas() {
        // Arrange
        String[] userFields = {"_id", "name", "age"};
        String[] orderFields = {"_id", "orderId", "total"};

        // Act - Learn User schema
        for (String field : userFields) {
            SchemaLearner.observeField("User", field);
        }
        SchemaLearner.finishLearning("User");

        // Learn Order schema
        for (String field : orderFields) {
            SchemaLearner.observeField("Order", field);
        }
        SchemaLearner.finishLearning("Order");

        // Assert
        assertArrayEquals(userFields, SchemaLearner.getLearnedFieldOrder("User"));
        assertArrayEquals(orderFields, SchemaLearner.getLearnedFieldOrder("Order"));
        assertEquals(2, SchemaLearner.getLearnedSchemaCount());
    }

    @Test
    public void testLearnEmptySchema_FinishLearning_NoRegistration() {
        // Arrange
        String schemaId = "Empty";
        // Don't observe any fields

        // Act
        SchemaLearner.finishLearning(schemaId);

        // Assert - Empty schema should not be registered to global registry
        assertFalse(FastBson.isSchemaRegistered(schemaId));
    }
}
