package com.cloud.fastbson.document.fast;

import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.util.BsonType;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FastBsonArray测试 - 目标100%分支覆盖率 (103个分支 + Iterator 2个分支)
 *
 * FastBsonArray使用fastutil零装箱存储，测试需要覆盖：
 * 1. 所有get方法的分支
 * 2. Iterator实现
 * 3. toJson()和所有类型的JSON序列化
 * 4. equals/hashCode
 */
public class FastBsonArrayTest {

    // ==================== 辅助方法 ====================

    private FastBsonArray createEmptyArray() {
        return (FastBsonArray) new FastBsonArrayBuilder().build();
    }

    // ==================== size/isEmpty测试 ====================

    @Test
    public void testSize_EmptyArray() {
        FastBsonArray array = createEmptyArray();
        assertEquals(0, array.size());
    }

    @Test
    public void testSize_NonEmptyArray() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt32(1).addInt32(2).addInt32(3);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals(3, array.size());
    }

    @Test
    public void testIsEmpty_EmptyArray() {
        FastBsonArray array = createEmptyArray();
        assertTrue(array.isEmpty());
    }

    @Test
    public void testIsEmpty_NonEmptyArray() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt32(1);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertFalse(array.isEmpty());
    }

    // ==================== getType测试 ====================

    @Test
    public void testGetType_IndexOutOfBounds_Negative() {
        FastBsonArray array = createEmptyArray();
        assertEquals(0, array.getType(-1));
    }

    @Test
    public void testGetType_IndexOutOfBounds_TooLarge() {
        FastBsonArray array = createEmptyArray();
        assertEquals(0, array.getType(0));
    }

    @Test
    public void testGetType_ValidIndex() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt32(42);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals(BsonType.INT32, array.getType(0));
    }

    // ==================== getInt32测试 ====================

    @Test
    public void testGetInt32_ValidIndex() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt32(42);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals(42, array.getInt32(0));
    }

    @Test
    public void testGetInt32_IndexOutOfBounds_Negative() {
        FastBsonArray array = createEmptyArray();
        assertThrows(IndexOutOfBoundsException.class, () -> {
            array.getInt32(-1);
        });
    }

    @Test
    public void testGetInt32_IndexOutOfBounds_TooLarge() {
        FastBsonArray array = createEmptyArray();
        assertThrows(IndexOutOfBoundsException.class, () -> {
            array.getInt32(0);
        });
    }

    @Test
    public void testGetInt32_WrongType() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addString("not an int");
        FastBsonArray array = (FastBsonArray) builder.build();
        assertThrows(ClassCastException.class, () -> {
            array.getInt32(0);
        });
    }

    @Test
    public void testGetInt32_WithDefault_ValidIndex() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt32(42);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals(42, array.getInt32(0, 999));
    }

    @Test
    public void testGetInt32_WithDefault_IndexOutOfBounds() {
        FastBsonArray array = createEmptyArray();
        assertEquals(999, array.getInt32(0, 999));
    }

    @Test
    public void testGetInt32_WithDefault_WrongType() {
        // Test branch: type mismatch in getInt32 with default should return default
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addString("not an int");
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals(999, array.getInt32(0, 999));
    }

    @Test
    public void testGetInt32_WithDefault_NegativeIndex() {
        // Test branch: negative index should return default
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt32(42);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals(999, array.getInt32(-1, 999));
    }

    // ==================== getInt64测试 ====================

    @Test
    public void testGetInt64_ValidIndex() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt64(42L);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals(42L, array.getInt64(0));
    }

    @Test
    public void testGetInt64_IndexOutOfBounds_Negative() {
        FastBsonArray array = createEmptyArray();
        assertThrows(IndexOutOfBoundsException.class, () -> {
            array.getInt64(-1);
        });
    }

    @Test
    public void testGetInt64_IndexOutOfBounds_TooLarge() {
        FastBsonArray array = createEmptyArray();
        assertThrows(IndexOutOfBoundsException.class, () -> {
            array.getInt64(0);
        });
    }

    @Test
    public void testGetInt64_WrongType() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addString("not a long");
        FastBsonArray array = (FastBsonArray) builder.build();
        assertThrows(ClassCastException.class, () -> {
            array.getInt64(0);
        });
    }

    @Test
    public void testGetInt64_WithDefault_ValidIndex() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt64(42L);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals(42L, array.getInt64(0, 999L));
    }

    @Test
    public void testGetInt64_WithDefault_IndexOutOfBounds() {
        FastBsonArray array = createEmptyArray();
        assertEquals(999L, array.getInt64(0, 999L));
    }

    @Test
    public void testGetInt64_WithDefault_WrongType() {
        // Test branch: type mismatch in getInt64 with default should return default
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addString("not a long");
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals(999L, array.getInt64(0, 999L));
    }

    @Test
    public void testGetInt64_WithDefault_NegativeIndex() {
        // Test branch: negative index should return default
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt64(42L);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals(999L, array.getInt64(-1, 999L));
    }

    // ==================== getDouble测试 ====================

    @Test
    public void testGetDouble_ValidIndex() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addDouble(3.14);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals(3.14, array.getDouble(0));
    }

    @Test
    public void testGetDouble_IndexOutOfBounds_Negative() {
        FastBsonArray array = createEmptyArray();
        assertThrows(IndexOutOfBoundsException.class, () -> {
            array.getDouble(-1);
        });
    }

    @Test
    public void testGetDouble_IndexOutOfBounds_TooLarge() {
        FastBsonArray array = createEmptyArray();
        assertThrows(IndexOutOfBoundsException.class, () -> {
            array.getDouble(0);
        });
    }

    @Test
    public void testGetDouble_WrongType() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addString("not a double");
        FastBsonArray array = (FastBsonArray) builder.build();
        assertThrows(ClassCastException.class, () -> {
            array.getDouble(0);
        });
    }

    @Test
    public void testGetDouble_WithDefault_ValidIndex() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addDouble(3.14);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals(3.14, array.getDouble(0, 9.99));
    }

    @Test
    public void testGetDouble_WithDefault_IndexOutOfBounds() {
        FastBsonArray array = createEmptyArray();
        assertEquals(9.99, array.getDouble(0, 9.99));
    }

    @Test
    public void testGetDouble_WithDefault_WrongType() {
        // Test branch: type mismatch in getDouble with default should return default
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addString("not a double");
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals(9.99, array.getDouble(0, 9.99));
    }

    @Test
    public void testGetDouble_WithDefault_NegativeIndex() {
        // Test branch: negative index should return default
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addDouble(3.14);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals(9.99, array.getDouble(-1, 9.99));
    }

    // ==================== getBoolean测试 ====================

    @Test
    public void testGetBoolean_ValidIndex_True() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addBoolean(true);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertTrue(array.getBoolean(0));
    }

    @Test
    public void testGetBoolean_ValidIndex_False() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addBoolean(false);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertFalse(array.getBoolean(0));
    }

    @Test
    public void testGetBoolean_IndexOutOfBounds_Negative() {
        FastBsonArray array = createEmptyArray();
        assertThrows(IndexOutOfBoundsException.class, () -> {
            array.getBoolean(-1);
        });
    }

    @Test
    public void testGetBoolean_IndexOutOfBounds_TooLarge() {
        FastBsonArray array = createEmptyArray();
        assertThrows(IndexOutOfBoundsException.class, () -> {
            array.getBoolean(0);
        });
    }

    @Test
    public void testGetBoolean_WrongType() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addString("not a boolean");
        FastBsonArray array = (FastBsonArray) builder.build();
        assertThrows(ClassCastException.class, () -> {
            array.getBoolean(0);
        });
    }

    @Test
    public void testGetBoolean_WithDefault_ValidIndex() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addBoolean(true);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertTrue(array.getBoolean(0, false));
    }

    @Test
    public void testGetBoolean_WithDefault_IndexOutOfBounds() {
        FastBsonArray array = createEmptyArray();
        assertTrue(array.getBoolean(0, true));
    }

    @Test
    public void testGetBoolean_WithDefault_WrongType() {
        // Test branch: type mismatch in getBoolean with default should return default
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addString("not a boolean");
        FastBsonArray array = (FastBsonArray) builder.build();
        assertTrue(array.getBoolean(0, true));
    }

    @Test
    public void testGetBoolean_WithDefault_NegativeIndex() {
        // Test branch: negative index should return default
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addBoolean(true);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertFalse(array.getBoolean(-1, false));
    }

    // ==================== getString测试 ====================

    @Test
    public void testGetString_ValidIndex() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addString("hello");
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals("hello", array.getString(0));
    }

    @Test
    public void testGetString_IndexOutOfBounds_Negative() {
        FastBsonArray array = createEmptyArray();
        assertNull(array.getString(-1));
    }

    @Test
    public void testGetString_IndexOutOfBounds_TooLarge() {
        FastBsonArray array = createEmptyArray();
        assertNull(array.getString(0));
    }

    @Test
    public void testGetString_WithDefault_ValidIndex() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addString("hello");
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals("hello", array.getString(0, "default"));
    }

    @Test
    public void testGetString_WithDefault_IndexOutOfBounds() {
        FastBsonArray array = createEmptyArray();
        assertEquals("default", array.getString(0, "default"));
    }

    @Test
    public void testGetString_WithDefault_NullValue() {
        // Test branch: when string value is null, should return default
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addString(null);  // Add a null string
        FastBsonArray array = (FastBsonArray) builder.build();
        // Getting a null string with default should return default
        assertEquals("default", array.getString(0, "default"));
    }

    @Test
    public void testGetString_WithDefault_NegativeIndex() {
        // Test branch: negative index should return default
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addString("hello");
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals("default", array.getString(-1, "default"));
    }

    // ==================== getDocument测试 ====================

    @Test
    public void testGetDocument_ValidIndex() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        FastBsonDocument doc = (FastBsonDocument) new FastBsonDocumentBuilder().putInt32("value", 42).build();
        builder.addDocument(doc);
        FastBsonArray array = (FastBsonArray) builder.build();
        BsonDocument result = array.getDocument(0);
        assertNotNull(result);
        assertEquals(42, result.getInt32("value"));
    }

    @Test
    public void testGetDocument_IndexOutOfBounds_Negative() {
        FastBsonArray array = createEmptyArray();
        assertNull(array.getDocument(-1));
    }

    @Test
    public void testGetDocument_IndexOutOfBounds_TooLarge() {
        FastBsonArray array = createEmptyArray();
        assertNull(array.getDocument(0));
    }

    // ==================== getArray测试 ====================

    @Test
    public void testGetArray_ValidIndex() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        FastBsonArray nested = (FastBsonArray) new FastBsonArrayBuilder().addInt32(1).build();
        builder.addArray(nested);
        FastBsonArray array = (FastBsonArray) builder.build();
        BsonArray result = array.getArray(0);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testGetArray_IndexOutOfBounds_Negative() {
        FastBsonArray array = createEmptyArray();
        assertNull(array.getArray(-1));
    }

    @Test
    public void testGetArray_IndexOutOfBounds_TooLarge() {
        FastBsonArray array = createEmptyArray();
        assertNull(array.getArray(0));
    }

    // ==================== get(int)测试 - 测试所有switch分支 ====================

    @Test
    public void testGet_IndexOutOfBounds_Negative() {
        FastBsonArray array = createEmptyArray();
        assertNull(array.get(-1));
    }

    @Test
    public void testGet_IndexOutOfBounds_TooLarge() {
        FastBsonArray array = createEmptyArray();
        assertNull(array.get(0));
    }

    @Test
    public void testGet_Int32() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt32(42);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals(42, array.get(0));
    }

    @Test
    public void testGet_Int64() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt64(42L);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals(42L, array.get(0));
    }

    @Test
    public void testGet_Double() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addDouble(3.14);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals(3.14, (Double) array.get(0));
    }

    @Test
    public void testGet_Boolean() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addBoolean(true);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals(true, array.get(0));
    }

    @Test
    public void testGet_String() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addString("hello");
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals("hello", array.get(0));
    }

    @Test
    public void testGet_Document() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        FastBsonDocument doc = (FastBsonDocument) new FastBsonDocumentBuilder().putInt32("value", 42).build();
        builder.addDocument(doc);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertTrue(array.get(0) instanceof BsonDocument);
    }

    @Test
    public void testGet_Array() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        FastBsonArray nested = (FastBsonArray) new FastBsonArrayBuilder().addInt32(1).build();
        builder.addArray(nested);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertTrue(array.get(0) instanceof BsonArray);
    }

    @Test
    public void testGet_Null() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addNull();
        FastBsonArray array = (FastBsonArray) builder.build();
        assertNull(array.get(0));
    }

    @Test
    public void testGet_ObjectId() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addObjectId("507f1f77bcf86cd799439011");
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals("507f1f77bcf86cd799439011", array.get(0));
    }

    @Test
    public void testGet_DateTime() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addDateTime(1638360000000L);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals(1638360000000L, array.get(0));
    }

    @Test
    public void testGet_Binary() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        byte[] data = new byte[]{1, 2, 3};
        builder.addBinary((byte) 0, data);
        FastBsonArray array = (FastBsonArray) builder.build();
        // Binary is stored as BinaryData object
        Object result = array.get(0);
        assertNotNull(result);
        assertTrue(result.toString().contains("BinaryData") || result instanceof byte[]);
    }

    @Test
    public void testGet_UnsupportedType() {
        // Test the default case in get() switch for an unsupported type
        // We need to manually create an array with an unsupported type
        // Since we can't easily do this via the builder, we test by ensuring
        // all known types are covered above. The default case would handle
        // any future types like REGEX, JAVASCRIPT, TIMESTAMP, etc.
        // This test ensures we have comprehensive coverage
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addObjectId("507f1f77bcf86cd799439011");
        FastBsonArray array = (FastBsonArray) builder.build();
        // ObjectId goes through the OBJECT_ID/BINARY/DOCUMENT/ARRAY case
        assertNotNull(array.get(0));
    }

    // ==================== Iterator测试 ====================

    @Test
    public void testIterator_EmptyArray() {
        FastBsonArray array = createEmptyArray();
        Iterator<Object> iter = array.iterator();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_NonEmptyArray() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt32(1).addInt32(2).addInt32(3);
        FastBsonArray array = (FastBsonArray) builder.build();

        Iterator<Object> iter = array.iterator();
        assertTrue(iter.hasNext());
        assertEquals(1, iter.next());
        assertTrue(iter.hasNext());
        assertEquals(2, iter.next());
        assertTrue(iter.hasNext());
        assertEquals(3, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_Remove_ThrowsException() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt32(1);
        FastBsonArray array = (FastBsonArray) builder.build();

        Iterator<Object> iter = array.iterator();
        assertThrows(UnsupportedOperationException.class, () -> {
            iter.remove();
        });
    }

    // ==================== toJson测试 - 测试所有类型的JSON序列化 ====================

    @Test
    public void testToJson_EmptyArray() {
        FastBsonArray array = createEmptyArray();
        assertEquals("[]", array.toJson());
    }

    @Test
    public void testToJson_Int32() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt32(42);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals("[42]", array.toJson());
    }

    @Test
    public void testToJson_Int64() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt64(123L);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals("[123]", array.toJson());
    }

    @Test
    public void testToJson_Double() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addDouble(3.14);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals("[3.14]", array.toJson());
    }

    @Test
    public void testToJson_Boolean() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addBoolean(true);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals("[true]", array.toJson());
    }

    @Test
    public void testToJson_String() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addString("hello");
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals("[\"hello\"]", array.toJson());
    }

    @Test
    public void testToJson_StringWithEscapes() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addString("hello\"world\n\r\t\\test");
        FastBsonArray array = (FastBsonArray) builder.build();
        String json = array.toJson();
        assertTrue(json.contains("\\\""));  // escaped quote
        assertTrue(json.contains("\\n"));   // escaped newline
        assertTrue(json.contains("\\r"));   // escaped carriage return
        assertTrue(json.contains("\\t"));   // escaped tab
        assertTrue(json.contains("\\\\"));  // escaped backslash
    }

    @Test
    public void testToJson_NullStringElement() {
        // Test escapeJson with null string element in array
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addString(null);
        FastBsonArray array = (FastBsonArray) builder.build();
        String json = array.toJson();
        // Null string should be escaped to empty string ""
        assertEquals("[\"\"]", json);
    }

    @Test
    public void testToJson_Null() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addNull();
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals("[null]", array.toJson());
    }

    @Test
    public void testToJson_Document() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        FastBsonDocument doc = (FastBsonDocument) new FastBsonDocumentBuilder().putInt32("value", 42).build();
        builder.addDocument(doc);
        FastBsonArray array = (FastBsonArray) builder.build();
        String json = array.toJson();
        assertTrue(json.contains("\"value\""));
        assertTrue(json.contains("42"));
    }

    @Test
    public void testToJson_Array() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        FastBsonArray nested = (FastBsonArray) new FastBsonArrayBuilder().addInt32(1).build();
        builder.addArray(nested);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals("[[1]]", array.toJson());
    }

    @Test
    public void testToJson_ObjectId() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addObjectId("507f1f77bcf86cd799439011");
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals("[\"507f1f77bcf86cd799439011\"]", array.toJson());
    }

    @Test
    public void testToJson_DateTime() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addDateTime(1638360000000L);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals("[1638360000000]", array.toJson());
    }

    @Test
    public void testToJson_MultipleElements() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt32(1).addInt32(2).addInt32(3);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals("[1,2,3]", array.toJson());
    }

    @Test
    public void testToJson_MixedTypes() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt32(42).addNull();
        FastBsonArray array = (FastBsonArray) builder.build();
        String json = array.toJson();
        assertTrue(json.contains("42"));
        assertTrue(json.contains("null"));
    }

    @Test
    public void testToJson_NullString() {
        // Test escapeJson with null string (edge case)
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addNull();  // This will trigger null handling
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals("[null]", array.toJson());
    }

    @Test
    public void testToJson_Binary() {
        // Test toJson with Binary type (unsupported in appendValueAsJson)
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        byte[] data = new byte[]{1, 2, 3};
        builder.addBinary((byte) 0, data);
        FastBsonArray array = (FastBsonArray) builder.build();
        String json = array.toJson();
        // Binary falls through to default case and should output "<unsupported>"
        assertTrue(json.contains("<unsupported>"));
    }

    // ==================== toString测试 ====================

    @Test
    public void testToString() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt32(42);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals("[42]", array.toString());
    }

    // ==================== equals/hashCode测试 ====================

    @Test
    public void testEquals_SameInstance() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt32(42);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertEquals(array, array);
    }

    @Test
    public void testEquals_EqualArrays() {
        FastBsonArrayBuilder builder1 = new FastBsonArrayBuilder();
        builder1.addInt32(1).addString("hello").addBoolean(true);
        FastBsonArray array1 = (FastBsonArray) builder1.build();

        FastBsonArrayBuilder builder2 = new FastBsonArrayBuilder();
        builder2.addInt32(1).addString("hello").addBoolean(true);
        FastBsonArray array2 = (FastBsonArray) builder2.build();

        assertEquals(array1, array2);
    }

    @Test
    public void testEquals_Null() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt32(42);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertNotEquals(null, array);
    }

    @Test
    public void testEquals_DifferentClass() {
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt32(42);
        FastBsonArray array = (FastBsonArray) builder.build();
        assertNotEquals("not an array", array);
    }

    @Test
    public void testEquals_DifferentBsonArrayImplementation() {
        // Test where o is a different BsonArray implementation (not FastBsonArray)
        FastBsonArrayBuilder builder1 = new FastBsonArrayBuilder();
        builder1.addInt32(42);
        FastBsonArray array1 = (FastBsonArray) builder1.build();

        // Create a different BsonArray implementation (e.g., HashMap-based)
        com.cloud.fastbson.document.hashmap.HashMapBsonArrayBuilder builder2 =
            new com.cloud.fastbson.document.hashmap.HashMapBsonArrayBuilder();
        builder2.addInt32(42);
        com.cloud.fastbson.document.BsonArray array2 = builder2.build();

        // These are not equal because they're different classes
        assertNotEquals(array1, array2);
    }

    @Test
    public void testHashCode_EqualArrays() {
        FastBsonArrayBuilder builder1 = new FastBsonArrayBuilder();
        builder1.addInt32(1).addString("hello");
        FastBsonArray array1 = (FastBsonArray) builder1.build();

        FastBsonArrayBuilder builder2 = new FastBsonArrayBuilder();
        builder2.addInt32(1).addString("hello");
        FastBsonArray array2 = (FastBsonArray) builder2.build();

        assertEquals(array1.hashCode(), array2.hashCode());
    }

    @Test
    public void testEquals_DifferentTypes() {
        // Test branch: types list not equal
        FastBsonArrayBuilder builder1 = new FastBsonArrayBuilder();
        builder1.addInt32(1);
        FastBsonArray array1 = (FastBsonArray) builder1.build();

        FastBsonArrayBuilder builder2 = new FastBsonArrayBuilder();
        builder2.addInt64(1L);
        FastBsonArray array2 = (FastBsonArray) builder2.build();

        assertNotEquals(array1, array2);
    }

    @Test
    public void testEquals_DifferentIntElements() {
        // Test branch: intElements list not equal
        FastBsonArrayBuilder builder1 = new FastBsonArrayBuilder();
        builder1.addInt32(1);
        FastBsonArray array1 = (FastBsonArray) builder1.build();

        FastBsonArrayBuilder builder2 = new FastBsonArrayBuilder();
        builder2.addInt32(2);
        FastBsonArray array2 = (FastBsonArray) builder2.build();

        assertNotEquals(array1, array2);
    }

    @Test
    public void testEquals_DifferentLongElements() {
        // Test branch: longElements list not equal
        FastBsonArrayBuilder builder1 = new FastBsonArrayBuilder();
        builder1.addInt64(1L);
        FastBsonArray array1 = (FastBsonArray) builder1.build();

        FastBsonArrayBuilder builder2 = new FastBsonArrayBuilder();
        builder2.addInt64(2L);
        FastBsonArray array2 = (FastBsonArray) builder2.build();

        assertNotEquals(array1, array2);
    }

    @Test
    public void testEquals_DifferentDoubleElements() {
        // Test branch: doubleElements list not equal
        FastBsonArrayBuilder builder1 = new FastBsonArrayBuilder();
        builder1.addDouble(1.0);
        FastBsonArray array1 = (FastBsonArray) builder1.build();

        FastBsonArrayBuilder builder2 = new FastBsonArrayBuilder();
        builder2.addDouble(2.0);
        FastBsonArray array2 = (FastBsonArray) builder2.build();

        assertNotEquals(array1, array2);
    }

    @Test
    public void testEquals_DifferentBooleanElements() {
        // Test branch: booleanElements list not equal
        FastBsonArrayBuilder builder1 = new FastBsonArrayBuilder();
        builder1.addBoolean(true);
        FastBsonArray array1 = (FastBsonArray) builder1.build();

        FastBsonArrayBuilder builder2 = new FastBsonArrayBuilder();
        builder2.addBoolean(false);
        FastBsonArray array2 = (FastBsonArray) builder2.build();

        assertNotEquals(array1, array2);
    }

    @Test
    public void testEquals_DifferentStringElements() {
        // Test branch: stringElements list not equal
        FastBsonArrayBuilder builder1 = new FastBsonArrayBuilder();
        builder1.addString("hello");
        FastBsonArray array1 = (FastBsonArray) builder1.build();

        FastBsonArrayBuilder builder2 = new FastBsonArrayBuilder();
        builder2.addString("world");
        FastBsonArray array2 = (FastBsonArray) builder2.build();

        assertNotEquals(array1, array2);
    }

    @Test
    public void testEquals_DifferentComplexElements() {
        // Test branch: complexElements list not equal
        FastBsonArrayBuilder builder1 = new FastBsonArrayBuilder();
        FastBsonDocument doc1 = (FastBsonDocument) new FastBsonDocumentBuilder().putInt32("value", 1).build();
        builder1.addDocument(doc1);
        FastBsonArray array1 = (FastBsonArray) builder1.build();

        FastBsonArrayBuilder builder2 = new FastBsonArrayBuilder();
        FastBsonDocument doc2 = (FastBsonDocument) new FastBsonDocumentBuilder().putInt32("value", 2).build();
        builder2.addDocument(doc2);
        FastBsonArray array2 = (FastBsonArray) builder2.build();

        assertNotEquals(array1, array2);
    }

    // ==================== 额外的分支覆盖测试 ====================

    @Test
    public void testEquals_ShortCircuitOnTypes() {
        // 确保types不同时，后续字段不会被比较（短路）
        FastBsonArrayBuilder builder1 = new FastBsonArrayBuilder();
        builder1.addInt32(1).addInt64(2L).addDouble(3.0).addBoolean(true).addString("test");
        FastBsonArray array1 = (FastBsonArray) builder1.build();

        FastBsonArrayBuilder builder2 = new FastBsonArrayBuilder();
        builder2.addInt32(1).addInt64(2L).addDouble(3.0).addBoolean(true).addInt32(999);
        FastBsonArray array2 = (FastBsonArray) builder2.build();

        assertNotEquals(array1, array2);
    }

    @Test
    public void testEquals_ShortCircuitOnIntElements() {
        // 确保types相同但intElements不同时短路
        FastBsonArrayBuilder builder1 = new FastBsonArrayBuilder();
        builder1.addInt32(1).addInt32(2).addInt64(3L).addDouble(4.0);
        FastBsonArray array1 = (FastBsonArray) builder1.build();

        FastBsonArrayBuilder builder2 = new FastBsonArrayBuilder();
        builder2.addInt32(1).addInt32(999).addInt64(3L).addDouble(4.0);
        FastBsonArray array2 = (FastBsonArray) builder2.build();

        assertNotEquals(array1, array2);
    }

    @Test
    public void testGet_WithAllFieldTypes() {
        // 测试一个包含所有字段类型的数组
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt32(1)
               .addInt64(2L)
               .addDouble(3.0)
               .addBoolean(true)
               .addString("test")
               .addDocument(new FastBsonDocumentBuilder().putInt32("key", 123).build());

        FastBsonArray array = (FastBsonArray) builder.build();

        // 验证每个元素
        assertEquals(1, array.get(0));
        assertEquals(2L, array.get(1));
        assertEquals(3.0, array.get(2));
        assertEquals(true, array.get(3));
        assertEquals("test", array.get(4));
        assertNotNull(array.get(5));
    }

    @Test
    public void testEquals_IdenticalEmptyArrays() {
        // Test equals with two separate empty array instances
        FastBsonArray array1 = (FastBsonArray) new FastBsonArrayBuilder().build();
        FastBsonArray array2 = (FastBsonArray) new FastBsonArrayBuilder().build();
        assertEquals(array1, array2);
        assertEquals(array2, array1);
    }

    @Test
    public void testEquals_WithAllFieldsMatching() {
        // Comprehensive test with all field types matching
        FastBsonArrayBuilder builder1 = new FastBsonArrayBuilder();
        builder1.addInt32(1).addInt64(2L).addDouble(3.0)
                .addBoolean(true).addString("test");
        FastBsonArray array1 = (FastBsonArray) builder1.build();

        FastBsonArrayBuilder builder2 = new FastBsonArrayBuilder();
        builder2.addInt32(1).addInt64(2L).addDouble(3.0)
                .addBoolean(true).addString("test");
        FastBsonArray array2 = (FastBsonArray) builder2.build();

        assertTrue(array1.equals(array2));
        assertTrue(array2.equals(array1));
    }

    @Test
    public void testEquals_WithComplexElements() {
        // Test equals with all fields matching including complex elements
        // This ensures we cover the branch where o != null AND getClass() == o.getClass()
        // and all internal collections match
        FastBsonDocumentBuilder docBuilder1 = new FastBsonDocumentBuilder();
        docBuilder1.putInt32("key", 123);

        FastBsonArrayBuilder builder1 = new FastBsonArrayBuilder();
        builder1.addInt32(1).addInt64(2L).addDouble(3.0)
                .addBoolean(true).addString("test")
                .addDocument(docBuilder1.build());
        FastBsonArray array1 = (FastBsonArray) builder1.build();

        FastBsonDocumentBuilder docBuilder2 = new FastBsonDocumentBuilder();
        docBuilder2.putInt32("key", 123);

        FastBsonArrayBuilder builder2 = new FastBsonArrayBuilder();
        builder2.addInt32(1).addInt64(2L).addDouble(3.0)
                .addBoolean(true).addString("test")
                .addDocument(docBuilder2.build());
        FastBsonArray array2 = (FastBsonArray) builder2.build();

        // Both should equal
        assertTrue(array1.equals(array2));
        assertTrue(array2.equals(array1));
    }

    @Test
    public void testGet_ArrayType() {
        // Explicitly test ARRAY type in switch case
        FastBsonArrayBuilder innerArray = new FastBsonArrayBuilder();
        innerArray.addInt32(1).addInt32(2);

        FastBsonArrayBuilder outerArray = new FastBsonArrayBuilder();
        outerArray.addArray(innerArray.build());

        FastBsonArray array = (FastBsonArray) outerArray.build();
        Object result = array.get(0);
        assertNotNull(result);
        assertTrue(result instanceof BsonArray);
    }

    @Test
    public void testGet_DocumentType() {
        // Explicitly test DOCUMENT type in switch case
        FastBsonDocumentBuilder doc = new FastBsonDocumentBuilder();
        doc.putInt32("key", 123);

        FastBsonArrayBuilder array = new FastBsonArrayBuilder();
        array.addDocument(doc.build());

        FastBsonArray fastArray = (FastBsonArray) array.build();
        Object result = fastArray.get(0);
        assertNotNull(result);
        assertTrue(result instanceof BsonDocument);
    }

    @Test
    public void testEquals_WithNull() {
        // Test equals with null
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt32(42);
        FastBsonArray array = (FastBsonArray) builder.build();

        assertFalse(array.equals(null));
    }

    @Test
    public void testEquals_WithDifferentClass() {
        // Test equals with different class
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt32(42);
        FastBsonArray array = (FastBsonArray) builder.build();

        assertFalse(array.equals("not an array"));
        assertFalse(array.equals(Integer.valueOf(42)));
    }

    @Test
    public void testEquals_WithDifferentElements() {
        // Test equals with different elements
        FastBsonArrayBuilder builder1 = new FastBsonArrayBuilder();
        builder1.addInt32(42);
        FastBsonArray array1 = (FastBsonArray) builder1.build();

        FastBsonArrayBuilder builder2 = new FastBsonArrayBuilder();
        builder2.addInt32(99);  // Different value
        FastBsonArray array2 = (FastBsonArray) builder2.build();

        assertFalse(array1.equals(array2));
    }

    @Test
    public void testGet_WithUnsupportedType() throws Exception {
        // Test Line 275: default case in get(int)
        FastBsonArrayBuilder builder = new FastBsonArrayBuilder();
        builder.addInt32(42);
        FastBsonArray array = (FastBsonArray) builder.build();

        // Use reflection to inject unsupported BSON type
        java.lang.reflect.Field typesField = FastBsonArray.class.getDeclaredField("types");
        typesField.setAccessible(true);
        java.lang.reflect.Field localIndicesField = FastBsonArray.class.getDeclaredField("localIndices");
        localIndicesField.setAccessible(true);
        java.lang.reflect.Field complexElementsField = FastBsonArray.class.getDeclaredField("complexElements");
        complexElementsField.setAccessible(true);

        it.unimi.dsi.fastutil.bytes.ByteArrayList types =
            (it.unimi.dsi.fastutil.bytes.ByteArrayList) typesField.get(array);
        it.unimi.dsi.fastutil.ints.IntArrayList localIndices =
            (it.unimi.dsi.fastutil.ints.IntArrayList) localIndicesField.get(array);
        @SuppressWarnings("unchecked")
        it.unimi.dsi.fastutil.objects.ObjectArrayList<Object> complexElements =
            (it.unimi.dsi.fastutil.objects.ObjectArrayList<Object>) complexElementsField.get(array);

        // Inject REGEX type (0x0B) which will trigger default case
        types.add((byte) 0x0B);  // REGEX type
        localIndices.add(complexElements.size());  // Local index in complexElements
        complexElements.add("regex_value");

        // Get the injected element - should trigger default branch
        int newIndex = types.size() - 1;
        Object result = array.get(newIndex);
        assertEquals("regex_value", result);
    }

}
