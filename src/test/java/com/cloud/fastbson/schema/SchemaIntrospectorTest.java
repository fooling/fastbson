package com.cloud.fastbson.schema;

import com.cloud.fastbson.annotation.BsonField;
import com.cloud.fastbson.annotation.BsonSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SchemaIntrospector.
 *
 * @author FastBSON
 * @since Phase 3.4
 */
public class SchemaIntrospectorTest {

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
    }

    @BsonSchema
    static class ProductEntity {
        @BsonField(value = "_id", order = 1)
        private String id;

        @BsonField(value = "productId", order = 2)
        private String productId;

        @BsonField(value = "price", order = 3)
        private Double price;
    }

    static class NoAnnotationEntity {
        private String field1;
        private String field2;
    }

    static class PartialAnnotationEntity {
        @BsonField(value = "field1", order = 1)
        private String field1;

        private String field2; // No annotation

        @BsonField(value = "field3", order = 2)
        private String field3;
    }

    static class UnorderedFieldsEntity {
        @BsonField(value = "field1", order = -1)
        private String field1;

        @BsonField(value = "field2", order = -1)
        private String field2;

        @BsonField(value = "field3", order = 1)
        private String field3;
    }

    static class DefaultFieldNameEntity {
        @BsonField(order = 1)
        private String fieldName;  // Uses Java field name

        @BsonField(value = "customName", order = 2)
        private String anotherField;  // Uses custom name
    }

    // ==================== Tests ====================

    @Test
    public void testGetFieldOrder_WithAnnotations() {
        // Act
        String[] fieldOrder = SchemaIntrospector.getFieldOrder(UserEntity.class);

        // Assert
        assertNotNull(fieldOrder);
        assertArrayEquals(new String[]{"_id", "name", "age", "email"}, fieldOrder);
    }

    @Test
    public void testGetFieldOrder_NoAnnotations() {
        // Act
        String[] fieldOrder = SchemaIntrospector.getFieldOrder(NoAnnotationEntity.class);

        // Assert
        assertNotNull(fieldOrder);
        assertEquals(0, fieldOrder.length);
    }

    @Test
    public void testGetFieldOrder_PartialAnnotations() {
        // Act
        String[] fieldOrder = SchemaIntrospector.getFieldOrder(PartialAnnotationEntity.class);

        // Assert
        assertNotNull(fieldOrder);
        assertArrayEquals(new String[]{"field1", "field3"}, fieldOrder);
    }

    @Test
    public void testGetFieldOrder_UnorderedFields() {
        // Act
        String[] fieldOrder = SchemaIntrospector.getFieldOrder(UnorderedFieldsEntity.class);

        // Assert
        assertNotNull(fieldOrder);
        assertEquals(3, fieldOrder.length);
        assertEquals("field3", fieldOrder[0], "Ordered field should come first");
        // field1 and field2 with order=-1 come after, order among them is unspecified
    }

    @Test
    public void testGetFieldOrder_DefaultFieldName() {
        // Act
        String[] fieldOrder = SchemaIntrospector.getFieldOrder(DefaultFieldNameEntity.class);

        // Assert
        assertNotNull(fieldOrder);
        assertArrayEquals(new String[]{"fieldName", "customName"}, fieldOrder);
    }

    @Test
    public void testGetFieldOrder_NullClass() {
        // Act
        String[] fieldOrder = SchemaIntrospector.getFieldOrder(null);

        // Assert
        assertNull(fieldOrder);
    }

    @Test
    public void testGetFieldOrder_Caching() {
        // Act - First call
        String[] first = SchemaIntrospector.getFieldOrder(UserEntity.class);
        String[] second = SchemaIntrospector.getFieldOrder(UserEntity.class);

        // Assert - Should return same instance (cached)
        assertSame(first, second);
        assertEquals(1, SchemaIntrospector.getCacheSize());
    }

    @Test
    public void testGetSchemaName_WithAnnotation() {
        // Act
        String schemaName = SchemaIntrospector.getSchemaName(UserEntity.class);

        // Assert
        assertEquals("User", schemaName);
    }

    @Test
    public void testGetSchemaName_WithoutValue() {
        // Act
        String schemaName = SchemaIntrospector.getSchemaName(ProductEntity.class);

        // Assert
        assertEquals("ProductEntity", schemaName);
    }

    @Test
    public void testGetSchemaName_NoAnnotation() {
        // Act
        String schemaName = SchemaIntrospector.getSchemaName(NoAnnotationEntity.class);

        // Assert
        assertEquals("NoAnnotationEntity", schemaName);
    }

    @Test
    public void testGetSchemaName_NullClass() {
        // Act
        String schemaName = SchemaIntrospector.getSchemaName(null);

        // Assert
        assertNull(schemaName);
    }

    @Test
    public void testHasSchemaMetadata_True() {
        // Act
        boolean hasMetadata = SchemaIntrospector.hasSchemaMetadata(UserEntity.class);

        // Assert
        assertTrue(hasMetadata);
    }

    @Test
    public void testHasSchemaMetadata_False() {
        // Act
        boolean hasMetadata = SchemaIntrospector.hasSchemaMetadata(NoAnnotationEntity.class);

        // Assert
        assertFalse(hasMetadata);
    }

    @Test
    public void testHasSchemaMetadata_Partial() {
        // Act
        boolean hasMetadata = SchemaIntrospector.hasSchemaMetadata(PartialAnnotationEntity.class);

        // Assert
        assertTrue(hasMetadata);
    }

    @Test
    public void testHasSchemaMetadata_NullClass() {
        // Act
        boolean hasMetadata = SchemaIntrospector.hasSchemaMetadata(null);

        // Assert
        assertFalse(hasMetadata);
    }

    @Test
    public void testClearCache() {
        // Arrange
        SchemaIntrospector.getFieldOrder(UserEntity.class);
        SchemaIntrospector.getFieldOrder(ProductEntity.class);
        assertEquals(2, SchemaIntrospector.getCacheSize());

        // Act
        SchemaIntrospector.clearCache(UserEntity.class);

        // Assert
        assertEquals(1, SchemaIntrospector.getCacheSize());
    }

    @Test
    public void testClearCache_NullClass() {
        // Arrange
        SchemaIntrospector.getFieldOrder(UserEntity.class);
        assertEquals(1, SchemaIntrospector.getCacheSize());

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> SchemaIntrospector.clearCache(null));
        assertEquals(1, SchemaIntrospector.getCacheSize());
    }

    @Test
    public void testClearAllCache() {
        // Arrange
        SchemaIntrospector.getFieldOrder(UserEntity.class);
        SchemaIntrospector.getFieldOrder(ProductEntity.class);
        assertEquals(2, SchemaIntrospector.getCacheSize());

        // Act
        SchemaIntrospector.clearAllCache();

        // Assert
        assertEquals(0, SchemaIntrospector.getCacheSize());
    }

    @Test
    public void testGetCacheSize() {
        // Arrange
        assertEquals(0, SchemaIntrospector.getCacheSize());

        // Act
        SchemaIntrospector.getFieldOrder(UserEntity.class);
        SchemaIntrospector.getFieldOrder(ProductEntity.class);
        SchemaIntrospector.getFieldOrder(NoAnnotationEntity.class);

        // Assert
        assertEquals(3, SchemaIntrospector.getCacheSize());
    }

    @Test
    public void testMultipleClasses() {
        // Act
        String[] userOrder = SchemaIntrospector.getFieldOrder(UserEntity.class);
        String[] productOrder = SchemaIntrospector.getFieldOrder(ProductEntity.class);

        // Assert
        assertNotNull(userOrder);
        assertNotNull(productOrder);
        assertArrayEquals(new String[]{"_id", "name", "age", "email"}, userOrder);
        assertArrayEquals(new String[]{"_id", "productId", "price"}, productOrder);
        assertEquals(2, SchemaIntrospector.getCacheSize());
    }
}
