package com.cloud.fastbson.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CapacityEstimator}.
 *
 * @author FastBSON
 * @since Phase 3.5
 */
class CapacityEstimatorTest {

    @Test
    void testDefaults() {
        // Arrange & Act
        CapacityEstimator estimator = CapacityEstimator.defaults();

        // Assert
        assertNotNull(estimator);
        assertEquals(20, estimator.getDocumentBytesPerField());
        assertEquals(15, estimator.getArrayBytesPerElement());
        assertEquals(4, estimator.getMinCapacity());
        assertEquals(0.75, estimator.getLoadFactor(), 0.001);
    }

    @Test
    void testEstimateDocumentFields_Default() {
        // Arrange
        CapacityEstimator estimator = CapacityEstimator.defaults();

        // Act & Assert
        assertEquals(4, estimator.estimateDocumentFields(50));   // 50/20=2.5 -> min 4
        assertEquals(10, estimator.estimateDocumentFields(200));  // 200/20=10
        assertEquals(25, estimator.estimateDocumentFields(500));  // 500/20=25
    }

    @Test
    void testEstimateArrayElements_Default() {
        // Arrange
        CapacityEstimator estimator = CapacityEstimator.defaults();

        // Act & Assert
        assertEquals(4, estimator.estimateArrayElements(30));    // 30/15=2 -> min 4
        assertEquals(10, estimator.estimateArrayElements(150));   // 150/15=10
        assertEquals(33, estimator.estimateArrayElements(500));   // 500/15=33.33 -> 33
    }

    @Test
    void testHashMapCapacity_Default() {
        // Arrange
        CapacityEstimator estimator = CapacityEstimator.defaults();

        // Act & Assert
        // Formula: (estimatedFields / 0.75) + 1
        assertEquals(6, estimator.hashMapCapacity(4));   // 4/0.75=5.33 -> 6
        assertEquals(14, estimator.hashMapCapacity(10)); // 10/0.75=13.33 -> 14
        assertEquals(34, estimator.hashMapCapacity(25)); // 25/0.75=33.33 -> 34
    }

    @Test
    void testBuilder_CustomDocumentBytesPerField() {
        // Arrange & Act
        CapacityEstimator estimator = CapacityEstimator.builder()
            .documentBytesPerField(10)  // Dense documents
            .build();

        // Assert
        assertEquals(10, estimator.getDocumentBytesPerField());
        assertEquals(20, estimator.estimateDocumentFields(200)); // 200/10=20
    }

    @Test
    void testBuilder_CustomArrayBytesPerElement() {
        // Arrange & Act
        CapacityEstimator estimator = CapacityEstimator.builder()
            .arrayBytesPerElement(8)  // Small array elements
            .build();

        // Assert
        assertEquals(8, estimator.getArrayBytesPerElement());
        assertEquals(25, estimator.estimateArrayElements(200)); // 200/8=25
    }

    @Test
    void testBuilder_CustomMinCapacity() {
        // Arrange & Act
        CapacityEstimator estimator = CapacityEstimator.builder()
            .minCapacity(8)  // Higher minimum
            .build();

        // Assert
        assertEquals(8, estimator.getMinCapacity());
        assertEquals(8, estimator.estimateDocumentFields(50));  // 50/20=2.5 -> min 8
        assertEquals(8, estimator.estimateArrayElements(30));   // 30/15=2 -> min 8
    }

    @Test
    void testBuilder_CustomLoadFactor() {
        // Arrange & Act
        CapacityEstimator estimator = CapacityEstimator.builder()
            .loadFactor(0.5)  // Lower load factor
            .build();

        // Assert
        assertEquals(0.5, estimator.getLoadFactor(), 0.001);
        assertEquals(9, estimator.hashMapCapacity(4));  // 4/0.5=8 -> 9
    }

    @Test
    void testBuilder_AllCustomValues() {
        // Arrange & Act
        CapacityEstimator estimator = CapacityEstimator.builder()
            .documentBytesPerField(10)
            .arrayBytesPerElement(8)
            .minCapacity(8)
            .loadFactor(0.6)
            .build();

        // Assert
        assertEquals(10, estimator.getDocumentBytesPerField());
        assertEquals(8, estimator.getArrayBytesPerElement());
        assertEquals(8, estimator.getMinCapacity());
        assertEquals(0.6, estimator.getLoadFactor(), 0.001);

        // Verify calculations
        assertEquals(20, estimator.estimateDocumentFields(200)); // 200/10=20
        assertEquals(25, estimator.estimateArrayElements(200));  // 200/8=25
        assertEquals(34, estimator.hashMapCapacity(20));         // 20/0.6=33.33 -> 34
    }

    @Test
    void testBuilder_InvalidDocumentBytesPerField_Zero() {
        // Arrange
        CapacityEstimator.Builder builder = CapacityEstimator.builder();

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            builder.documentBytesPerField(0);
        });
        assertTrue(ex.getMessage().contains("must be positive"));
    }

    @Test
    void testBuilder_InvalidDocumentBytesPerField_Negative() {
        // Arrange
        CapacityEstimator.Builder builder = CapacityEstimator.builder();

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            builder.documentBytesPerField(-10);
        });
        assertTrue(ex.getMessage().contains("must be positive"));
    }

    @Test
    void testBuilder_InvalidArrayBytesPerElement_Zero() {
        // Arrange
        CapacityEstimator.Builder builder = CapacityEstimator.builder();

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            builder.arrayBytesPerElement(0);
        });
        assertTrue(ex.getMessage().contains("must be positive"));
    }

    @Test
    void testBuilder_InvalidArrayBytesPerElement_Negative() {
        // Arrange
        CapacityEstimator.Builder builder = CapacityEstimator.builder();

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            builder.arrayBytesPerElement(-5);
        });
        assertTrue(ex.getMessage().contains("must be positive"));
    }

    @Test
    void testBuilder_InvalidMinCapacity_Zero() {
        // Arrange
        CapacityEstimator.Builder builder = CapacityEstimator.builder();

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            builder.minCapacity(0);
        });
        assertTrue(ex.getMessage().contains("must be positive"));
    }

    @Test
    void testBuilder_InvalidMinCapacity_Negative() {
        // Arrange
        CapacityEstimator.Builder builder = CapacityEstimator.builder();

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            builder.minCapacity(-3);
        });
        assertTrue(ex.getMessage().contains("must be positive"));
    }

    @Test
    void testBuilder_InvalidLoadFactor_Zero() {
        // Arrange
        CapacityEstimator.Builder builder = CapacityEstimator.builder();

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            builder.loadFactor(0.0);
        });
        assertTrue(ex.getMessage().contains("must be in range"));
    }

    @Test
    void testBuilder_InvalidLoadFactor_Negative() {
        // Arrange
        CapacityEstimator.Builder builder = CapacityEstimator.builder();

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            builder.loadFactor(-0.5);
        });
        assertTrue(ex.getMessage().contains("must be in range"));
    }

    @Test
    void testBuilder_InvalidLoadFactor_GreaterThanOne() {
        // Arrange
        CapacityEstimator.Builder builder = CapacityEstimator.builder();

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            builder.loadFactor(1.1);
        });
        assertTrue(ex.getMessage().contains("must be in range"));
    }

    @Test
    void testBuilder_ValidLoadFactor_Boundary() {
        // Arrange & Act
        CapacityEstimator estimator1 = CapacityEstimator.builder()
            .loadFactor(0.1)  // Minimum valid
            .build();

        CapacityEstimator estimator2 = CapacityEstimator.builder()
            .loadFactor(1.0)  // Maximum valid
            .build();

        // Assert
        assertEquals(0.1, estimator1.getLoadFactor(), 0.001);
        assertEquals(1.0, estimator2.getLoadFactor(), 0.001);
    }

    @Test
    void testRealWorldScenario_DenseDocuments() {
        // Arrange: Documents with many small fields (e.g., config objects)
        CapacityEstimator estimator = CapacityEstimator.builder()
            .documentBytesPerField(10)  // Small fields
            .minCapacity(16)            // Expect at least 16 fields
            .build();

        // Act
        int estimatedFields = estimator.estimateDocumentFields(500);
        int capacity = estimator.hashMapCapacity(estimatedFields);

        // Assert
        assertEquals(50, estimatedFields);  // 500/10=50
        assertEquals(67, capacity);         // 50/0.75=66.67 -> 67
    }

    @Test
    void testRealWorldScenario_SparseDocuments() {
        // Arrange: Documents with few large fields (e.g., content documents)
        CapacityEstimator estimator = CapacityEstimator.builder()
            .documentBytesPerField(50)  // Large fields
            .minCapacity(2)             // Can have as few as 2 fields
            .build();

        // Act
        int estimatedFields = estimator.estimateDocumentFields(500);
        int capacity = estimator.hashMapCapacity(estimatedFields);

        // Assert
        assertEquals(10, estimatedFields);  // 500/50=10
        assertEquals(14, capacity);         // 10/0.75=13.33 -> 14
    }

    @Test
    void testRealWorldScenario_PrimitiveArrays() {
        // Arrange: Arrays of primitive types (e.g., time-series data)
        CapacityEstimator estimator = CapacityEstimator.builder()
            .arrayBytesPerElement(8)  // Int32/Double are ~8 bytes
            .minCapacity(10)          // Expect at least 10 elements
            .build();

        // Act
        int estimatedElements = estimator.estimateArrayElements(800);

        // Assert
        assertEquals(100, estimatedElements);  // 800/8=100
    }

    @Test
    void testRealWorldScenario_ComplexArrays() {
        // Arrange: Arrays of nested objects (e.g., order items)
        CapacityEstimator estimator = CapacityEstimator.builder()
            .arrayBytesPerElement(30)  // Nested objects are larger
            .minCapacity(4)
            .build();

        // Act
        int estimatedElements = estimator.estimateArrayElements(300);

        // Assert
        assertEquals(10, estimatedElements);  // 300/30=10
    }
}
