package com.cloud.fastbson.document.hashmap;

import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.exception.BsonParseException;
import com.cloud.fastbson.util.BsonType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HashMapBsonArray测试 - 目标100%分支覆盖率
 *
 * 测试所有get方法的分支：
 * 1. 索引有效且类型正确
 * 2. 索引越界 (< 0 或 >= size)
 * 3. 类型错误
 * 4. 默认值处理
 */
public class HashMapBsonArrayTest {

    // ==================== 辅助方法 ====================

    private HashMapBsonArray createArray(List<Object> data) {
        return new HashMapBsonArray(data);
    }

    private HashMapBsonArray createEmptyArray() {
        return new HashMapBsonArray();
    }

    // ==================== 构造函数测试 ====================

    @Test
    public void testConstructor_Empty() {
        HashMapBsonArray array = new HashMapBsonArray();
        assertEquals(0, array.size());
        assertTrue(array.isEmpty());
    }

    @Test
    public void testConstructor_WithData() {
        List<Object> data = new ArrayList<Object>();
        data.add(1);
        data.add(2);
        data.add(3);

        HashMapBsonArray array = createArray(data);
        assertEquals(3, array.size());
        assertFalse(array.isEmpty());
    }

    // ==================== getType测试 ====================

    @Test
    public void testGetType_IndexOutOfBounds_Negative() {
        HashMapBsonArray array = createEmptyArray();
        assertEquals(0, array.getType(-1));
    }

    @Test
    public void testGetType_IndexOutOfBounds_TooLarge() {
        HashMapBsonArray array = createEmptyArray();
        assertEquals(0, array.getType(0));
    }

    @Test
    public void testGetType_Null() {
        List<Object> data = new ArrayList<Object>();
        data.add(null);
        HashMapBsonArray array = createArray(data);
        assertEquals(BsonType.NULL, array.getType(0));
    }

    @Test
    public void testGetType_Int32() {
        List<Object> data = new ArrayList<Object>();
        data.add(Integer.valueOf(42));
        HashMapBsonArray array = createArray(data);
        assertEquals(BsonType.INT32, array.getType(0));
    }

    @Test
    public void testGetType_Int64() {
        List<Object> data = new ArrayList<Object>();
        data.add(Long.valueOf(42L));
        HashMapBsonArray array = createArray(data);
        assertEquals(BsonType.INT64, array.getType(0));
    }

    @Test
    public void testGetType_Double() {
        List<Object> data = new ArrayList<Object>();
        data.add(Double.valueOf(3.14));
        HashMapBsonArray array = createArray(data);
        assertEquals(BsonType.DOUBLE, array.getType(0));
    }

    @Test
    public void testGetType_Boolean() {
        List<Object> data = new ArrayList<Object>();
        data.add(Boolean.TRUE);
        HashMapBsonArray array = createArray(data);
        assertEquals(BsonType.BOOLEAN, array.getType(0));
    }

    @Test
    public void testGetType_String() {
        List<Object> data = new ArrayList<Object>();
        data.add("hello");
        HashMapBsonArray array = createArray(data);
        assertEquals(BsonType.STRING, array.getType(0));
    }

    @Test
    public void testGetType_Document() {
        List<Object> data = new ArrayList<Object>();
        data.add(new HashMapBsonDocument());
        HashMapBsonArray array = createArray(data);
        assertEquals(BsonType.DOCUMENT, array.getType(0));
    }

    @Test
    public void testGetType_Array() {
        List<Object> data = new ArrayList<Object>();
        data.add(new HashMapBsonArray());
        HashMapBsonArray array = createArray(data);
        assertEquals(BsonType.ARRAY, array.getType(0));
    }

    @Test
    public void testGetType_Binary() {
        List<Object> data = new ArrayList<Object>();
        data.add(new byte[]{1, 2, 3});
        HashMapBsonArray array = createArray(data);
        assertEquals(BsonType.BINARY, array.getType(0));
    }

    @Test
    public void testGetType_UnknownType() {
        List<Object> data = new ArrayList<Object>();
        data.add(new Object());  // Unknown type
        HashMapBsonArray array = createArray(data);
        assertEquals(0, array.getType(0));
    }

    // ==================== getInt32测试 ====================

    @Test
    public void testGetInt32_ValidIndex() {
        List<Object> data = new ArrayList<Object>();
        data.add(Integer.valueOf(42));
        HashMapBsonArray array = createArray(data);
        assertEquals(42, array.getInt32(0));
    }

    @Test
    public void testGetInt32_IndexOutOfBounds_Negative() {
        HashMapBsonArray array = createEmptyArray();
        assertThrows(IndexOutOfBoundsException.class, () -> {
            array.getInt32(-1);
        });
    }

    @Test
    public void testGetInt32_IndexOutOfBounds_TooLarge() {
        HashMapBsonArray array = createEmptyArray();
        assertThrows(IndexOutOfBoundsException.class, () -> {
            array.getInt32(0);
        });
    }

    @Test
    public void testGetInt32_WrongType() {
        List<Object> data = new ArrayList<Object>();
        data.add("not an int");
        HashMapBsonArray array = createArray(data);
        assertThrows(BsonParseException.class, () -> {
            array.getInt32(0);
        });
    }

    @Test
    public void testGetInt32_WithDefault_ValidIndex() {
        List<Object> data = new ArrayList<Object>();
        data.add(Integer.valueOf(42));
        HashMapBsonArray array = createArray(data);
        assertEquals(42, array.getInt32(0, 999));
    }

    @Test
    public void testGetInt32_WithDefault_IndexOutOfBounds_Negative() {
        HashMapBsonArray array = createEmptyArray();
        assertEquals(999, array.getInt32(-1, 999));
    }

    @Test
    public void testGetInt32_WithDefault_IndexOutOfBounds_TooLarge() {
        HashMapBsonArray array = createEmptyArray();
        assertEquals(999, array.getInt32(0, 999));
    }

    @Test
    public void testGetInt32_WithDefault_NullValue() {
        List<Object> data = new ArrayList<Object>();
        data.add(null);
        HashMapBsonArray array = createArray(data);
        assertEquals(999, array.getInt32(0, 999));
    }

    @Test
    public void testGetInt32_WithDefault_WrongType() {
        List<Object> data = new ArrayList<Object>();
        data.add("string");
        HashMapBsonArray array = createArray(data);
        assertEquals(999, array.getInt32(0, 999));
    }

    // ==================== getInt64测试 ====================

    @Test
    public void testGetInt64_ValidIndex() {
        List<Object> data = new ArrayList<Object>();
        data.add(Long.valueOf(42L));
        HashMapBsonArray array = createArray(data);
        assertEquals(42L, array.getInt64(0));
    }

    @Test
    public void testGetInt64_IndexOutOfBounds_Negative() {
        HashMapBsonArray array = createEmptyArray();
        assertThrows(IndexOutOfBoundsException.class, () -> {
            array.getInt64(-1);
        });
    }

    @Test
    public void testGetInt64_IndexOutOfBounds_TooLarge() {
        HashMapBsonArray array = createEmptyArray();
        assertThrows(IndexOutOfBoundsException.class, () -> {
            array.getInt64(0);
        });
    }

    @Test
    public void testGetInt64_WrongType() {
        List<Object> data = new ArrayList<Object>();
        data.add("not a long");
        HashMapBsonArray array = createArray(data);
        assertThrows(BsonParseException.class, () -> {
            array.getInt64(0);
        });
    }

    @Test
    public void testGetInt64_WithDefault_ValidIndex() {
        List<Object> data = new ArrayList<Object>();
        data.add(Long.valueOf(42L));
        HashMapBsonArray array = createArray(data);
        assertEquals(42L, array.getInt64(0, 999L));
    }

    @Test
    public void testGetInt64_WithDefault_IndexOutOfBounds_Negative() {
        HashMapBsonArray array = createEmptyArray();
        assertEquals(999L, array.getInt64(-1, 999L));
    }

    @Test
    public void testGetInt64_WithDefault_IndexOutOfBounds_TooLarge() {
        HashMapBsonArray array = createEmptyArray();
        assertEquals(999L, array.getInt64(0, 999L));
    }

    @Test
    public void testGetInt64_WithDefault_NullValue() {
        List<Object> data = new ArrayList<Object>();
        data.add(null);
        HashMapBsonArray array = createArray(data);
        assertEquals(999L, array.getInt64(0, 999L));
    }

    @Test
    public void testGetInt64_WithDefault_WrongType() {
        List<Object> data = new ArrayList<Object>();
        data.add("string");
        HashMapBsonArray array = createArray(data);
        assertEquals(999L, array.getInt64(0, 999L));
    }

    // ==================== getDouble测试 ====================

    @Test
    public void testGetDouble_ValidIndex() {
        List<Object> data = new ArrayList<Object>();
        data.add(Double.valueOf(3.14));
        HashMapBsonArray array = createArray(data);
        assertEquals(3.14, array.getDouble(0));
    }

    @Test
    public void testGetDouble_IndexOutOfBounds_Negative() {
        HashMapBsonArray array = createEmptyArray();
        assertThrows(IndexOutOfBoundsException.class, () -> {
            array.getDouble(-1);
        });
    }

    @Test
    public void testGetDouble_IndexOutOfBounds_TooLarge() {
        HashMapBsonArray array = createEmptyArray();
        assertThrows(IndexOutOfBoundsException.class, () -> {
            array.getDouble(0);
        });
    }

    @Test
    public void testGetDouble_WrongType() {
        List<Object> data = new ArrayList<Object>();
        data.add("not a double");
        HashMapBsonArray array = createArray(data);
        assertThrows(BsonParseException.class, () -> {
            array.getDouble(0);
        });
    }

    @Test
    public void testGetDouble_WithDefault_ValidIndex() {
        List<Object> data = new ArrayList<Object>();
        data.add(Double.valueOf(3.14));
        HashMapBsonArray array = createArray(data);
        assertEquals(3.14, array.getDouble(0, 9.99));
    }

    @Test
    public void testGetDouble_WithDefault_IndexOutOfBounds_Negative() {
        HashMapBsonArray array = createEmptyArray();
        assertEquals(9.99, array.getDouble(-1, 9.99));
    }

    @Test
    public void testGetDouble_WithDefault_IndexOutOfBounds_TooLarge() {
        HashMapBsonArray array = createEmptyArray();
        assertEquals(9.99, array.getDouble(0, 9.99));
    }

    @Test
    public void testGetDouble_WithDefault_NullValue() {
        List<Object> data = new ArrayList<Object>();
        data.add(null);
        HashMapBsonArray array = createArray(data);
        assertEquals(9.99, array.getDouble(0, 9.99));
    }

    @Test
    public void testGetDouble_WithDefault_WrongType() {
        List<Object> data = new ArrayList<Object>();
        data.add("string");
        HashMapBsonArray array = createArray(data);
        assertEquals(9.99, array.getDouble(0, 9.99));
    }

    // ==================== getBoolean测试 ====================

    @Test
    public void testGetBoolean_ValidIndex_True() {
        List<Object> data = new ArrayList<Object>();
        data.add(Boolean.TRUE);
        HashMapBsonArray array = createArray(data);
        assertTrue(array.getBoolean(0));
    }

    @Test
    public void testGetBoolean_ValidIndex_False() {
        List<Object> data = new ArrayList<Object>();
        data.add(Boolean.FALSE);
        HashMapBsonArray array = createArray(data);
        assertFalse(array.getBoolean(0));
    }

    @Test
    public void testGetBoolean_IndexOutOfBounds_Negative() {
        HashMapBsonArray array = createEmptyArray();
        assertThrows(IndexOutOfBoundsException.class, () -> {
            array.getBoolean(-1);
        });
    }

    @Test
    public void testGetBoolean_IndexOutOfBounds_TooLarge() {
        HashMapBsonArray array = createEmptyArray();
        assertThrows(IndexOutOfBoundsException.class, () -> {
            array.getBoolean(0);
        });
    }

    @Test
    public void testGetBoolean_WrongType() {
        List<Object> data = new ArrayList<Object>();
        data.add("not a boolean");
        HashMapBsonArray array = createArray(data);
        assertThrows(BsonParseException.class, () -> {
            array.getBoolean(0);
        });
    }

    @Test
    public void testGetBoolean_WithDefault_ValidIndex() {
        List<Object> data = new ArrayList<Object>();
        data.add(Boolean.TRUE);
        HashMapBsonArray array = createArray(data);
        assertTrue(array.getBoolean(0, false));
    }

    @Test
    public void testGetBoolean_WithDefault_IndexOutOfBounds_Negative() {
        HashMapBsonArray array = createEmptyArray();
        assertTrue(array.getBoolean(-1, true));
    }

    @Test
    public void testGetBoolean_WithDefault_IndexOutOfBounds_TooLarge() {
        HashMapBsonArray array = createEmptyArray();
        assertTrue(array.getBoolean(0, true));
    }

    @Test
    public void testGetBoolean_WithDefault_NullValue() {
        List<Object> data = new ArrayList<Object>();
        data.add(null);
        HashMapBsonArray array = createArray(data);
        assertTrue(array.getBoolean(0, true));
    }

    @Test
    public void testGetBoolean_WithDefault_WrongType() {
        List<Object> data = new ArrayList<Object>();
        data.add("string");
        HashMapBsonArray array = createArray(data);
        assertTrue(array.getBoolean(0, true));
    }

    // ==================== getString测试 ====================

    @Test
    public void testGetString_ValidIndex() {
        List<Object> data = new ArrayList<Object>();
        data.add("hello");
        HashMapBsonArray array = createArray(data);
        assertEquals("hello", array.getString(0));
    }

    @Test
    public void testGetString_IndexOutOfBounds_Negative() {
        HashMapBsonArray array = createEmptyArray();
        assertNull(array.getString(-1));
    }

    @Test
    public void testGetString_IndexOutOfBounds_TooLarge() {
        HashMapBsonArray array = createEmptyArray();
        assertNull(array.getString(0));
    }

    @Test
    public void testGetString_NullValue() {
        List<Object> data = new ArrayList<Object>();
        data.add(null);
        HashMapBsonArray array = createArray(data);
        assertNull(array.getString(0));
    }

    @Test
    public void testGetString_WrongType() {
        List<Object> data = new ArrayList<Object>();
        data.add(Integer.valueOf(42));
        HashMapBsonArray array = createArray(data);
        assertThrows(BsonParseException.class, () -> {
            array.getString(0);
        });
    }

    @Test
    public void testGetString_WithDefault_ValidIndex() {
        List<Object> data = new ArrayList<Object>();
        data.add("hello");
        HashMapBsonArray array = createArray(data);
        assertEquals("hello", array.getString(0, "default"));
    }

    @Test
    public void testGetString_WithDefault_IndexOutOfBounds_Negative() {
        HashMapBsonArray array = createEmptyArray();
        assertEquals("default", array.getString(-1, "default"));
    }

    @Test
    public void testGetString_WithDefault_IndexOutOfBounds_TooLarge() {
        HashMapBsonArray array = createEmptyArray();
        assertEquals("default", array.getString(0, "default"));
    }

    @Test
    public void testGetString_WithDefault_NullValue() {
        List<Object> data = new ArrayList<Object>();
        data.add(null);
        HashMapBsonArray array = createArray(data);
        assertEquals("default", array.getString(0, "default"));
    }

    @Test
    public void testGetString_WithDefault_WrongType() {
        List<Object> data = new ArrayList<Object>();
        data.add(Integer.valueOf(42));
        HashMapBsonArray array = createArray(data);
        assertEquals("default", array.getString(0, "default"));
    }

    // ==================== getDocument测试 ====================

    @Test
    public void testGetDocument_ValidIndex() {
        List<Object> data = new ArrayList<Object>();
        HashMapBsonDocument doc = new HashMapBsonDocument();
        data.add(doc);
        HashMapBsonArray array = createArray(data);
        assertNotNull(array.getDocument(0));
    }

    @Test
    public void testGetDocument_IndexOutOfBounds_Negative() {
        HashMapBsonArray array = createEmptyArray();
        assertNull(array.getDocument(-1));
    }

    @Test
    public void testGetDocument_IndexOutOfBounds_TooLarge() {
        HashMapBsonArray array = createEmptyArray();
        assertNull(array.getDocument(0));
    }

    @Test
    public void testGetDocument_NullValue() {
        List<Object> data = new ArrayList<Object>();
        data.add(null);
        HashMapBsonArray array = createArray(data);
        assertNull(array.getDocument(0));
    }

    @Test
    public void testGetDocument_WrongType() {
        List<Object> data = new ArrayList<Object>();
        data.add("not a document");
        HashMapBsonArray array = createArray(data);
        assertThrows(BsonParseException.class, () -> {
            array.getDocument(0);
        });
    }

    // ==================== getArray测试 ====================

    @Test
    public void testGetArray_ValidIndex() {
        List<Object> data = new ArrayList<Object>();
        HashMapBsonArray nested = new HashMapBsonArray();
        data.add(nested);
        HashMapBsonArray array = createArray(data);
        assertNotNull(array.getArray(0));
    }

    @Test
    public void testGetArray_IndexOutOfBounds_Negative() {
        HashMapBsonArray array = createEmptyArray();
        assertNull(array.getArray(-1));
    }

    @Test
    public void testGetArray_IndexOutOfBounds_TooLarge() {
        HashMapBsonArray array = createEmptyArray();
        assertNull(array.getArray(0));
    }

    @Test
    public void testGetArray_NullValue() {
        List<Object> data = new ArrayList<Object>();
        data.add(null);
        HashMapBsonArray array = createArray(data);
        assertNull(array.getArray(0));
    }

    @Test
    public void testGetArray_WrongType() {
        List<Object> data = new ArrayList<Object>();
        data.add("not an array");
        HashMapBsonArray array = createArray(data);
        assertThrows(BsonParseException.class, () -> {
            array.getArray(0);
        });
    }

    // ==================== get(int)测试 ====================

    @Test
    public void testGet_ValidIndex() {
        List<Object> data = new ArrayList<Object>();
        data.add("value");
        HashMapBsonArray array = createArray(data);
        assertEquals("value", array.get(0));
    }

    @Test
    public void testGet_IndexOutOfBounds_Negative() {
        HashMapBsonArray array = createEmptyArray();
        assertNull(array.get(-1));
    }

    @Test
    public void testGet_IndexOutOfBounds_TooLarge() {
        HashMapBsonArray array = createEmptyArray();
        assertNull(array.get(0));
    }

    // ==================== size/isEmpty测试 ====================

    @Test
    public void testSize_EmptyArray() {
        HashMapBsonArray array = createEmptyArray();
        assertEquals(0, array.size());
    }

    @Test
    public void testSize_NonEmptyArray() {
        List<Object> data = new ArrayList<Object>();
        data.add(1);
        data.add(2);
        data.add(3);
        HashMapBsonArray array = createArray(data);
        assertEquals(3, array.size());
    }

    @Test
    public void testIsEmpty_EmptyArray() {
        HashMapBsonArray array = createEmptyArray();
        assertTrue(array.isEmpty());
    }

    @Test
    public void testIsEmpty_NonEmptyArray() {
        List<Object> data = new ArrayList<Object>();
        data.add(1);
        HashMapBsonArray array = createArray(data);
        assertFalse(array.isEmpty());
    }

    // ==================== Iterator测试 ====================

    @Test
    public void testIterator() {
        List<Object> data = new ArrayList<Object>();
        data.add(1);
        data.add(2);
        data.add(3);
        HashMapBsonArray array = createArray(data);

        Iterator<Object> iter = array.iterator();
        assertTrue(iter.hasNext());
        assertEquals(1, iter.next());
        assertTrue(iter.hasNext());
        assertEquals(2, iter.next());
        assertTrue(iter.hasNext());
        assertEquals(3, iter.next());
        assertFalse(iter.hasNext());
    }

    // ==================== equals/hashCode/toString测试 ====================

    @Test
    public void testEquals_SameInstance() {
        List<Object> data = new ArrayList<Object>();
        data.add(1);
        HashMapBsonArray array = createArray(data);
        assertEquals(array, array);
    }

    @Test
    public void testEquals_EqualArrays() {
        List<Object> data1 = new ArrayList<Object>();
        data1.add(1);
        data1.add(2);

        List<Object> data2 = new ArrayList<Object>();
        data2.add(1);
        data2.add(2);

        HashMapBsonArray array1 = createArray(data1);
        HashMapBsonArray array2 = createArray(data2);
        assertEquals(array1, array2);
    }

    @Test
    public void testEquals_DifferentType() {
        List<Object> data = new ArrayList<Object>();
        data.add(1);
        HashMapBsonArray array = createArray(data);
        assertNotEquals(array, "not an array");
    }

    @Test
    public void testHashCode() {
        List<Object> data1 = new ArrayList<Object>();
        data1.add(1);
        data1.add(2);

        List<Object> data2 = new ArrayList<Object>();
        data2.add(1);
        data2.add(2);

        HashMapBsonArray array1 = createArray(data1);
        HashMapBsonArray array2 = createArray(data2);
        assertEquals(array1.hashCode(), array2.hashCode());
    }

    @Test
    public void testToString() {
        List<Object> data = new ArrayList<Object>();
        data.add(1);
        HashMapBsonArray array = createArray(data);
        String str = array.toString();
        assertNotNull(str);
        assertTrue(str.contains("HashMapBsonArray"));
    }
}
