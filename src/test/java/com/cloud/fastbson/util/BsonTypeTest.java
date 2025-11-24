package com.cloud.fastbson.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BsonType.
 */
public class BsonTypeTest {

    // ==================== isValidType ====================

    @Test
    public void testIsValidType_ValidTypes() {
        // All valid BSON types from 0x01 to 0x13
        for (byte type = 0x01; type <= 0x13; type++) {
            assertTrue(BsonType.isValidType(type), "Type 0x" + Integer.toHexString(type) + " should be valid");
        }
    }

    @Test
    public void testIsValidType_MinKey() {
        assertTrue(BsonType.isValidType(BsonType.MIN_KEY));
        assertTrue(BsonType.isValidType((byte) 0xFF));
    }

    @Test
    public void testIsValidType_MaxKey() {
        assertTrue(BsonType.isValidType(BsonType.MAX_KEY));
        assertTrue(BsonType.isValidType((byte) 0x7F));
    }

    @Test
    public void testIsValidType_EndOfDocument() {
        assertTrue(BsonType.isValidType(BsonType.END_OF_DOCUMENT));
        assertTrue(BsonType.isValidType((byte) 0x00));
    }

    @Test
    public void testIsValidType_InvalidTypes() {
        // Some invalid types
        assertFalse(BsonType.isValidType((byte) 0x14));
        assertFalse(BsonType.isValidType((byte) 0x20));
        assertFalse(BsonType.isValidType((byte) 0x50));
        assertFalse(BsonType.isValidType((byte) 0x7E));
        assertFalse(BsonType.isValidType((byte) 0x80));
        assertFalse(BsonType.isValidType((byte) 0xFE));
    }

    // ==================== getTypeName ====================

    @Test
    public void testGetTypeName_AllKnownTypes() {
        assertEquals("double", BsonType.getTypeName(BsonType.DOUBLE));
        assertEquals("string", BsonType.getTypeName(BsonType.STRING));
        assertEquals("document", BsonType.getTypeName(BsonType.DOCUMENT));
        assertEquals("array", BsonType.getTypeName(BsonType.ARRAY));
        assertEquals("binary", BsonType.getTypeName(BsonType.BINARY));
        assertEquals("undefined", BsonType.getTypeName(BsonType.UNDEFINED));
        assertEquals("objectId", BsonType.getTypeName(BsonType.OBJECT_ID));
        assertEquals("boolean", BsonType.getTypeName(BsonType.BOOLEAN));
        assertEquals("dateTime", BsonType.getTypeName(BsonType.DATE_TIME));
        assertEquals("null", BsonType.getTypeName(BsonType.NULL));
        assertEquals("regex", BsonType.getTypeName(BsonType.REGEX));
        assertEquals("dbPointer", BsonType.getTypeName(BsonType.DB_POINTER));
        assertEquals("javascript", BsonType.getTypeName(BsonType.JAVASCRIPT));
        assertEquals("symbol", BsonType.getTypeName(BsonType.SYMBOL));
        assertEquals("javascriptWithScope", BsonType.getTypeName(BsonType.JAVASCRIPT_WITH_SCOPE));
        assertEquals("int32", BsonType.getTypeName(BsonType.INT32));
        assertEquals("timestamp", BsonType.getTypeName(BsonType.TIMESTAMP));
        assertEquals("int64", BsonType.getTypeName(BsonType.INT64));
        assertEquals("decimal128", BsonType.getTypeName(BsonType.DECIMAL128));
        assertEquals("minKey", BsonType.getTypeName(BsonType.MIN_KEY));
        assertEquals("maxKey", BsonType.getTypeName(BsonType.MAX_KEY));
        assertEquals("endOfDocument", BsonType.getTypeName(BsonType.END_OF_DOCUMENT));
    }

    @Test
    public void testGetTypeName_UnknownType() {
        String result = BsonType.getTypeName((byte) 0x20);
        assertTrue(result.startsWith("unknown(0x"));
        assertTrue(result.contains("20"));
    }

    // ==================== isFixedLength ====================

    @Test
    public void testIsFixedLength_FixedTypes() {
        assertTrue(BsonType.isFixedLength(BsonType.DOUBLE));
        assertTrue(BsonType.isFixedLength(BsonType.OBJECT_ID));
        assertTrue(BsonType.isFixedLength(BsonType.BOOLEAN));
        assertTrue(BsonType.isFixedLength(BsonType.DATE_TIME));
        assertTrue(BsonType.isFixedLength(BsonType.NULL));
        assertTrue(BsonType.isFixedLength(BsonType.INT32));
        assertTrue(BsonType.isFixedLength(BsonType.TIMESTAMP));
        assertTrue(BsonType.isFixedLength(BsonType.INT64));
        assertTrue(BsonType.isFixedLength(BsonType.MIN_KEY));
        assertTrue(BsonType.isFixedLength(BsonType.MAX_KEY));
    }

    @Test
    public void testIsFixedLength_VariableTypes() {
        assertFalse(BsonType.isFixedLength(BsonType.STRING));
        assertFalse(BsonType.isFixedLength(BsonType.DOCUMENT));
        assertFalse(BsonType.isFixedLength(BsonType.ARRAY));
        assertFalse(BsonType.isFixedLength(BsonType.BINARY));
        assertFalse(BsonType.isFixedLength(BsonType.UNDEFINED));
        assertFalse(BsonType.isFixedLength(BsonType.REGEX));
        assertFalse(BsonType.isFixedLength(BsonType.DB_POINTER));
        assertFalse(BsonType.isFixedLength(BsonType.JAVASCRIPT));
        assertFalse(BsonType.isFixedLength(BsonType.SYMBOL));
        assertFalse(BsonType.isFixedLength(BsonType.JAVASCRIPT_WITH_SCOPE));
        assertFalse(BsonType.isFixedLength(BsonType.DECIMAL128));
    }

    // ==================== getFixedLength ====================

    @Test
    public void testGetFixedLength_FixedTypes() {
        assertEquals(8, BsonType.getFixedLength(BsonType.DOUBLE));
        assertEquals(12, BsonType.getFixedLength(BsonType.OBJECT_ID));
        assertEquals(1, BsonType.getFixedLength(BsonType.BOOLEAN));
        assertEquals(8, BsonType.getFixedLength(BsonType.DATE_TIME));
        assertEquals(0, BsonType.getFixedLength(BsonType.NULL));
        assertEquals(4, BsonType.getFixedLength(BsonType.INT32));
        assertEquals(8, BsonType.getFixedLength(BsonType.TIMESTAMP));
        assertEquals(8, BsonType.getFixedLength(BsonType.INT64));
        assertEquals(0, BsonType.getFixedLength(BsonType.MIN_KEY));
        assertEquals(0, BsonType.getFixedLength(BsonType.MAX_KEY));
    }

    @Test
    public void testGetFixedLength_VariableTypes() {
        assertEquals(-1, BsonType.getFixedLength(BsonType.STRING));
        assertEquals(-1, BsonType.getFixedLength(BsonType.DOCUMENT));
        assertEquals(-1, BsonType.getFixedLength(BsonType.ARRAY));
        assertEquals(-1, BsonType.getFixedLength(BsonType.BINARY));
        assertEquals(-1, BsonType.getFixedLength(BsonType.UNDEFINED));
        assertEquals(-1, BsonType.getFixedLength(BsonType.REGEX));
        assertEquals(-1, BsonType.getFixedLength(BsonType.DB_POINTER));
        assertEquals(-1, BsonType.getFixedLength(BsonType.JAVASCRIPT));
        assertEquals(-1, BsonType.getFixedLength(BsonType.SYMBOL));
        assertEquals(-1, BsonType.getFixedLength(BsonType.JAVASCRIPT_WITH_SCOPE));
        assertEquals(-1, BsonType.getFixedLength(BsonType.DECIMAL128));
    }

    @Test
    public void testGetFixedLength_UnknownType() {
        assertEquals(-1, BsonType.getFixedLength((byte) 0x20));
    }

    // ==================== Type Constants ====================

    @Test
    public void testTypeConstants_Values() {
        assertEquals(0x01, BsonType.DOUBLE);
        assertEquals(0x02, BsonType.STRING);
        assertEquals(0x03, BsonType.DOCUMENT);
        assertEquals(0x04, BsonType.ARRAY);
        assertEquals(0x05, BsonType.BINARY);
        assertEquals(0x06, BsonType.UNDEFINED);
        assertEquals(0x07, BsonType.OBJECT_ID);
        assertEquals(0x08, BsonType.BOOLEAN);
        assertEquals(0x09, BsonType.DATE_TIME);
        assertEquals(0x0A, BsonType.NULL);
        assertEquals(0x0B, BsonType.REGEX);
        assertEquals(0x0C, BsonType.DB_POINTER);
        assertEquals(0x0D, BsonType.JAVASCRIPT);
        assertEquals(0x0E, BsonType.SYMBOL);
        assertEquals(0x0F, BsonType.JAVASCRIPT_WITH_SCOPE);
        assertEquals(0x10, BsonType.INT32);
        assertEquals(0x11, BsonType.TIMESTAMP);
        assertEquals(0x12, BsonType.INT64);
        assertEquals(0x13, BsonType.DECIMAL128);
        assertEquals((byte) 0xFF, BsonType.MIN_KEY);
        assertEquals(0x7F, BsonType.MAX_KEY);
        assertEquals(0x00, BsonType.END_OF_DOCUMENT);
    }

    // ==================== Constructor Test ====================

    @Test
    public void testConstructor_CannotInstantiate() throws Exception {
        Constructor<BsonType> constructor = BsonType.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        try {
            constructor.newInstance();
            fail("Expected AssertionError");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof AssertionError);
            assertEquals("Cannot instantiate BsonType", e.getCause().getMessage());
        }
    }

    // ==================== Edge Cases ====================

    @Test
    public void testIsValidType_BoundaryValues() {
        assertTrue(BsonType.isValidType((byte) 0x01)); // First valid type
        assertTrue(BsonType.isValidType((byte) 0x13)); // Last valid type
        assertFalse(BsonType.isValidType((byte) 0x14)); // Just after last valid
    }

    @Test
    public void testGetTypeName_Decimal128() {
        // Decimal128 is 0x13, added in MongoDB 3.4
        assertEquals("decimal128", BsonType.getTypeName((byte) 0x13));
    }
}
