package com.cloud.fastbson.util;

import com.cloud.fastbson.annotation.BranchOrder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BranchOrderHelper}.
 *
 * @author FastBSON
 * @since Phase 3.5
 */
class BranchOrderHelperTest {

    // Test classes with different branch order configurations

    @BranchOrder({BsonType.INT64, BsonType.DOUBLE, BsonType.STRING})
    static class TimeSeriesParser {
    }

    @BranchOrder(value = {BsonType.STRING, BsonType.INT32, BsonType.BOOLEAN},
                workload = "Web API documents")
    static class WebApiParser {
    }

    static class UnannotatedParser {
    }

    @Test
    void testGetBranchOrder_Annotated() {
        // Arrange & Act
        byte[] order = BranchOrderHelper.getBranchOrder(TimeSeriesParser.class);

        // Assert
        assertNotNull(order);
        assertEquals(3, order.length);
        assertEquals(BsonType.INT64, order[0]);
        assertEquals(BsonType.DOUBLE, order[1]);
        assertEquals(BsonType.STRING, order[2]);
    }

    @Test
    void testGetBranchOrder_Unannotated_ReturnsDefault() {
        // Arrange & Act
        byte[] order = BranchOrderHelper.getBranchOrder(UnannotatedParser.class);

        // Assert
        assertNotNull(order);
        assertArrayEquals(BranchOrderHelper.DEFAULT_ORDER, order);
    }

    @Test
    void testGetWorkloadDescription_Annotated() {
        // Arrange & Act
        String workload = BranchOrderHelper.getWorkloadDescription(WebApiParser.class);

        // Assert
        assertEquals("Web API documents", workload);
    }

    @Test
    void testGetWorkloadDescription_Unannotated() {
        // Arrange & Act
        String workload = BranchOrderHelper.getWorkloadDescription(UnannotatedParser.class);

        // Assert
        assertEquals("", workload);
    }

    @Test
    void testIsPriorityType_InList() {
        // Arrange
        byte[] order = {BsonType.INT64, BsonType.DOUBLE, BsonType.STRING};

        // Act & Assert
        assertTrue(BranchOrderHelper.isPriorityType(order, BsonType.INT64));
        assertTrue(BranchOrderHelper.isPriorityType(order, BsonType.DOUBLE));
        assertTrue(BranchOrderHelper.isPriorityType(order, BsonType.STRING));
    }

    @Test
    void testIsPriorityType_NotInList() {
        // Arrange
        byte[] order = {BsonType.INT64, BsonType.DOUBLE, BsonType.STRING};

        // Act & Assert
        assertFalse(BranchOrderHelper.isPriorityType(order, BsonType.INT32));
        assertFalse(BranchOrderHelper.isPriorityType(order, BsonType.BOOLEAN));
    }

    @Test
    void testGetPriorityIndex_Found() {
        // Arrange
        byte[] order = {BsonType.INT64, BsonType.DOUBLE, BsonType.STRING};

        // Act & Assert
        assertEquals(0, BranchOrderHelper.getPriorityIndex(order, BsonType.INT64));
        assertEquals(1, BranchOrderHelper.getPriorityIndex(order, BsonType.DOUBLE));
        assertEquals(2, BranchOrderHelper.getPriorityIndex(order, BsonType.STRING));
    }

    @Test
    void testGetPriorityIndex_NotFound() {
        // Arrange
        byte[] order = {BsonType.INT64, BsonType.DOUBLE, BsonType.STRING};

        // Act & Assert
        assertEquals(-1, BranchOrderHelper.getPriorityIndex(order, BsonType.INT32));
    }

    @Test
    void testGetPriorityTypes() {
        // Arrange
        byte[] order = {BsonType.INT64, BsonType.DOUBLE, BsonType.STRING};

        // Act
        List<Byte> types = BranchOrderHelper.getPriorityTypes(order);

        // Assert
        assertEquals(3, types.size());
        assertEquals(Byte.valueOf(BsonType.INT64), types.get(0));
        assertEquals(Byte.valueOf(BsonType.DOUBLE), types.get(1));
        assertEquals(Byte.valueOf(BsonType.STRING), types.get(2));
    }

    @Test
    void testValidate_ValidOrder() {
        // Arrange
        byte[] order = {BsonType.INT64, BsonType.DOUBLE, BsonType.STRING};

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> BranchOrderHelper.validate(order));
    }

    @Test
    void testValidate_NullOrder() {
        // Arrange
        byte[] order = null;

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            BranchOrderHelper.validate(order);
        });
        assertTrue(ex.getMessage().contains("cannot be null or empty"));
    }

    @Test
    void testValidate_EmptyOrder() {
        // Arrange
        byte[] order = new byte[0];

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            BranchOrderHelper.validate(order);
        });
        assertTrue(ex.getMessage().contains("cannot be null or empty"));
    }

    @Test
    void testValidate_DuplicateTypes() {
        // Arrange
        byte[] order = {BsonType.INT64, BsonType.DOUBLE, BsonType.INT64};

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            BranchOrderHelper.validate(order);
        });
        assertTrue(ex.getMessage().contains("Duplicate type"));
    }

    @Test
    void testValidate_InvalidType() {
        // Arrange - use a clearly invalid type byte (not in BSON spec)
        byte[] order = {BsonType.INT64, (byte) 0x99, BsonType.STRING};  // 0x99 is invalid

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            BranchOrderHelper.validate(order);
        });
        assertTrue(ex.getMessage().contains("Invalid BSON type"));
    }

    @Test
    void testFormat() {
        // Arrange
        byte[] order = {BsonType.INT64, BsonType.DOUBLE, BsonType.STRING};

        // Act
        String formatted = BranchOrderHelper.format(order);

        // Assert
        assertEquals("INT64 → DOUBLE → STRING", formatted);
    }

    @Test
    void testFormat_Empty() {
        // Arrange
        byte[] order = new byte[0];

        // Act
        String formatted = BranchOrderHelper.format(order);

        // Assert
        assertEquals("(empty)", formatted);
    }

    @Test
    void testFormat_Null() {
        // Arrange
        byte[] order = null;

        // Act
        String formatted = BranchOrderHelper.format(order);

        // Assert
        assertEquals("(empty)", formatted);
    }

    @Test
    void testGetTypeName_AllCommonTypes() {
        // Act & Assert
        assertEquals("DOUBLE", BranchOrderHelper.getTypeName(BsonType.DOUBLE));
        assertEquals("STRING", BranchOrderHelper.getTypeName(BsonType.STRING));
        assertEquals("DOCUMENT", BranchOrderHelper.getTypeName(BsonType.DOCUMENT));
        assertEquals("ARRAY", BranchOrderHelper.getTypeName(BsonType.ARRAY));
        assertEquals("BINARY", BranchOrderHelper.getTypeName(BsonType.BINARY));
        assertEquals("OBJECT_ID", BranchOrderHelper.getTypeName(BsonType.OBJECT_ID));
        assertEquals("BOOLEAN", BranchOrderHelper.getTypeName(BsonType.BOOLEAN));
        assertEquals("DATE_TIME", BranchOrderHelper.getTypeName(BsonType.DATE_TIME));
        assertEquals("NULL", BranchOrderHelper.getTypeName(BsonType.NULL));
        assertEquals("INT32", BranchOrderHelper.getTypeName(BsonType.INT32));
        assertEquals("INT64", BranchOrderHelper.getTypeName(BsonType.INT64));
    }

    @Test
    void testGetTypeName_UnknownType() {
        // Act - use invalid type 0x99 (not in BSON spec)
        String name = BranchOrderHelper.getTypeName((byte) 0x99);

        // Assert
        assertTrue(name.startsWith("UNKNOWN"));
        assertTrue(name.toLowerCase().contains("99"));  // Check for hex value
    }

    @Test
    void testMerge_NoOverlap() {
        // Arrange
        byte[] custom = {BsonType.INT64, BsonType.DOUBLE};
        byte[] defaults = {BsonType.INT32, BsonType.STRING, BsonType.BOOLEAN};

        // Act
        byte[] merged = BranchOrderHelper.merge(custom, defaults);

        // Assert
        assertEquals(5, merged.length);
        assertEquals(BsonType.INT64, merged[0]);    // Custom first
        assertEquals(BsonType.DOUBLE, merged[1]);   // Custom second
        assertEquals(BsonType.INT32, merged[2]);    // Default first
        assertEquals(BsonType.STRING, merged[3]);   // Default second
        assertEquals(BsonType.BOOLEAN, merged[4]);  // Default third
    }

    @Test
    void testMerge_WithOverlap() {
        // Arrange
        byte[] custom = {BsonType.INT64, BsonType.STRING};
        byte[] defaults = {BsonType.INT32, BsonType.STRING, BsonType.DOUBLE};

        // Act
        byte[] merged = BranchOrderHelper.merge(custom, defaults);

        // Assert
        assertEquals(4, merged.length);
        assertEquals(BsonType.INT64, merged[0]);   // Custom first
        assertEquals(BsonType.STRING, merged[1]);  // Custom second (priority over default)
        assertEquals(BsonType.INT32, merged[2]);   // Default first (not in custom)
        assertEquals(BsonType.DOUBLE, merged[3]);  // Default third (not in custom)

        // STRING should not appear twice
        int stringCount = 0;
        for (byte type : merged) {
            if (type == BsonType.STRING) {
                stringCount++;
            }
        }
        assertEquals(1, stringCount);
    }

    @Test
    void testMerge_EmptyCustom() {
        // Arrange
        byte[] custom = new byte[0];
        byte[] defaults = {BsonType.INT32, BsonType.STRING, BsonType.DOUBLE};

        // Act
        byte[] merged = BranchOrderHelper.merge(custom, defaults);

        // Assert
        assertArrayEquals(defaults, merged);
    }

    @Test
    void testDefaultOrder_IsValid() {
        // Act & Assert - default order should pass validation
        assertDoesNotThrow(() -> BranchOrderHelper.validate(BranchOrderHelper.DEFAULT_ORDER));
    }

    @Test
    void testDefaultOrder_ContainsCommonTypes() {
        // Arrange
        byte[] defaultOrder = BranchOrderHelper.DEFAULT_ORDER;

        // Act & Assert - verify common types are in default order
        assertTrue(BranchOrderHelper.isPriorityType(defaultOrder, BsonType.INT32));
        assertTrue(BranchOrderHelper.isPriorityType(defaultOrder, BsonType.STRING));
        assertTrue(BranchOrderHelper.isPriorityType(defaultOrder, BsonType.DOUBLE));
        assertTrue(BranchOrderHelper.isPriorityType(defaultOrder, BsonType.INT64));
    }

    @Test
    void testDefaultOrder_INT32First() {
        // Arrange
        byte[] defaultOrder = BranchOrderHelper.DEFAULT_ORDER;

        // Act & Assert - INT32 should be first (35% frequency)
        assertEquals(BsonType.INT32, defaultOrder[0]);
    }
}
