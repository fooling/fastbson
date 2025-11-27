package com.cloud.fastbson.document.hashmap;

import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.exception.BsonParseException;
import com.cloud.fastbson.types.*;
import com.cloud.fastbson.util.BsonType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HashMapBsonDocument测试 - 目标100%分支覆盖率
 *
 * 测试所有get方法的4个分支：
 * 1. 字段存在且类型正确
 * 2. 字段不存在
 * 3. 字段存在但类型错误
 * 4. 默认值处理
 */
public class HashMapBsonDocumentTest {

    // ==================== 辅助方法 ====================

    private HashMapBsonDocument createDocument(Map<String, Object> data, Map<String, Byte> types) {
        return new HashMapBsonDocument(data, types);
    }

    private HashMapBsonDocument createEmptyDocument() {
        return new HashMapBsonDocument();
    }

    // ==================== 构造函数测试 ====================

    @Test
    public void testConstructor_Empty() {
        HashMapBsonDocument doc = createEmptyDocument();
        assertNotNull(doc);
        assertEquals(0, doc.size());
        assertTrue(doc.isEmpty());
    }

    @Test
    public void testConstructor_WithData() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("field1", 42);
        types.put("field1", BsonType.INT32);

        HashMapBsonDocument doc = createDocument(data, types);
        assertNotNull(doc);
        assertEquals(1, doc.size());
        assertFalse(doc.isEmpty());
    }

    @Test
    public void testCreateDirect_ZeroCopy() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("field1", 100);
        types.put("field1", BsonType.INT32);

        HashMapBsonDocument doc = HashMapBsonDocument.createDirect(data, types);
        assertNotNull(doc);
        assertEquals(100, doc.getInt32("field1"));
    }

    // ==================== contains/getType/isNull测试 ====================

    @Test
    public void testContains_FieldExists() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("name", "test");
        types.put("name", BsonType.STRING);

        HashMapBsonDocument doc = createDocument(data, types);
        assertTrue(doc.contains("name"));
    }

    @Test
    public void testContains_FieldNotExists() {
        HashMapBsonDocument doc = createEmptyDocument();
        assertFalse(doc.contains("nonexistent"));
    }

    @Test
    public void testGetType_FieldExists() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("age", 25);
        types.put("age", BsonType.INT32);

        HashMapBsonDocument doc = createDocument(data, types);
        assertEquals(BsonType.INT32, doc.getType("age"));
    }

    @Test
    public void testGetType_FieldNotExists() {
        HashMapBsonDocument doc = createEmptyDocument();
        assertEquals(0, doc.getType("nonexistent"));
    }

    @Test
    public void testIsNull_FieldIsNull() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("nullable", null);
        types.put("nullable", BsonType.NULL);

        HashMapBsonDocument doc = createDocument(data, types);
        assertTrue(doc.isNull("nullable"));
    }

    @Test
    public void testIsNull_FieldNotNull() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("value", 123);
        types.put("value", BsonType.INT32);

        HashMapBsonDocument doc = createDocument(data, types);
        assertFalse(doc.isNull("value"));
    }

    @Test
    public void testIsNull_FieldNotExists() {
        HashMapBsonDocument doc = createEmptyDocument();
        assertFalse(doc.isNull("nonexistent"));
    }

    @Test
    public void testFieldNames() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("field1", 1);
        data.put("field2", "value");
        types.put("field1", BsonType.INT32);
        types.put("field2", BsonType.STRING);

        HashMapBsonDocument doc = createDocument(data, types);
        Set<String> names = doc.fieldNames();
        assertEquals(2, names.size());
        assertTrue(names.contains("field1"));
        assertTrue(names.contains("field2"));
    }

    // ==================== getInt32测试 ====================

    @Test
    public void testGetInt32_FieldExists() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("age", 25);
        types.put("age", BsonType.INT32);

        HashMapBsonDocument doc = createDocument(data, types);
        assertEquals(25, doc.getInt32("age"));
    }

    @Test
    public void testGetInt32_FieldNotExists() {
        HashMapBsonDocument doc = createEmptyDocument();
        assertThrows(BsonParseException.class, () -> {
            doc.getInt32("nonexistent");
        });
    }

    @Test
    public void testGetInt32_WrongType() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("name", "notAnInt");
        types.put("name", BsonType.STRING);

        HashMapBsonDocument doc = createDocument(data, types);
        assertThrows(BsonParseException.class, () -> {
            doc.getInt32("name");
        });
    }

    @Test
    public void testGetInt32_WithDefault_FieldExists() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("count", 100);
        types.put("count", BsonType.INT32);

        HashMapBsonDocument doc = createDocument(data, types);
        assertEquals(100, doc.getInt32("count", 999));
    }

    @Test
    public void testGetInt32_WithDefault_FieldNotExists() {
        HashMapBsonDocument doc = createEmptyDocument();
        assertEquals(999, doc.getInt32("nonexistent", 999));
    }

    @Test
    public void testGetInt32_WithDefault_WrongType() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("value", "string");
        types.put("value", BsonType.STRING);

        HashMapBsonDocument doc = createDocument(data, types);
        assertEquals(999, doc.getInt32("value", 999));
    }

    @Test
    public void testGetInt32_WithDefault_NullValue() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("nullable", null);
        types.put("nullable", BsonType.NULL);

        HashMapBsonDocument doc = createDocument(data, types);
        assertEquals(999, doc.getInt32("nullable", 999));
    }

    // ==================== getInt64测试 ====================

    @Test
    public void testGetInt64_FieldExists() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("bigNumber", 1234567890L);
        types.put("bigNumber", BsonType.INT64);

        HashMapBsonDocument doc = createDocument(data, types);
        assertEquals(1234567890L, doc.getInt64("bigNumber"));
    }

    @Test
    public void testGetInt64_FieldNotExists() {
        HashMapBsonDocument doc = createEmptyDocument();
        assertThrows(BsonParseException.class, () -> {
            doc.getInt64("nonexistent");
        });
    }

    @Test
    public void testGetInt64_WrongType() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("value", 123);
        types.put("value", BsonType.INT32);

        HashMapBsonDocument doc = createDocument(data, types);
        assertThrows(BsonParseException.class, () -> {
            doc.getInt64("value");
        });
    }

    @Test
    public void testGetInt64_WithDefault_FieldExists() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("timestamp", 9999L);
        types.put("timestamp", BsonType.INT64);

        HashMapBsonDocument doc = createDocument(data, types);
        assertEquals(9999L, doc.getInt64("timestamp", -1L));
    }

    @Test
    public void testGetInt64_WithDefault_FieldNotExists() {
        HashMapBsonDocument doc = createEmptyDocument();
        assertEquals(-1L, doc.getInt64("nonexistent", -1L));
    }

    @Test
    public void testGetInt64_WithDefault_WrongType() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("value", "string");
        types.put("value", BsonType.STRING);

        HashMapBsonDocument doc = createDocument(data, types);
        assertEquals(-1L, doc.getInt64("value", -1L));
    }

    @Test
    public void testGetInt64_WithDefault_NullValue() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("nullable", null);
        types.put("nullable", BsonType.NULL);

        HashMapBsonDocument doc = createDocument(data, types);
        assertEquals(-1L, doc.getInt64("nullable", -1L));
    }

    // ==================== getDouble测试 ====================

    @Test
    public void testGetDouble_FieldExists() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("price", 99.99);
        types.put("price", BsonType.DOUBLE);

        HashMapBsonDocument doc = createDocument(data, types);
        assertEquals(99.99, doc.getDouble("price"), 0.001);
    }

    @Test
    public void testGetDouble_FieldNotExists() {
        HashMapBsonDocument doc = createEmptyDocument();
        assertThrows(BsonParseException.class, () -> {
            doc.getDouble("nonexistent");
        });
    }

    @Test
    public void testGetDouble_WrongType() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("value", 123);
        types.put("value", BsonType.INT32);

        HashMapBsonDocument doc = createDocument(data, types);
        assertThrows(BsonParseException.class, () -> {
            doc.getDouble("value");
        });
    }

    @Test
    public void testGetDouble_WithDefault_FieldExists() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("ratio", 0.75);
        types.put("ratio", BsonType.DOUBLE);

        HashMapBsonDocument doc = createDocument(data, types);
        assertEquals(0.75, doc.getDouble("ratio", -1.0), 0.001);
    }

    @Test
    public void testGetDouble_WithDefault_FieldNotExists() {
        HashMapBsonDocument doc = createEmptyDocument();
        assertEquals(-1.0, doc.getDouble("nonexistent", -1.0), 0.001);
    }

    @Test
    public void testGetDouble_WithDefault_WrongType() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("value", "string");
        types.put("value", BsonType.STRING);

        HashMapBsonDocument doc = createDocument(data, types);
        assertEquals(-1.0, doc.getDouble("value", -1.0), 0.001);
    }

    @Test
    public void testGetDouble_WithDefault_NullValue() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("nullable", null);
        types.put("nullable", BsonType.NULL);

        HashMapBsonDocument doc = createDocument(data, types);
        assertEquals(-1.0, doc.getDouble("nullable", -1.0), 0.001);
    }

    // ==================== getBoolean测试 ====================

    @Test
    public void testGetBoolean_FieldExists_True() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("active", Boolean.TRUE);
        types.put("active", BsonType.BOOLEAN);

        HashMapBsonDocument doc = createDocument(data, types);
        assertTrue(doc.getBoolean("active"));
    }

    @Test
    public void testGetBoolean_FieldExists_False() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("inactive", Boolean.FALSE);
        types.put("inactive", BsonType.BOOLEAN);

        HashMapBsonDocument doc = createDocument(data, types);
        assertFalse(doc.getBoolean("inactive"));
    }

    @Test
    public void testGetBoolean_FieldNotExists() {
        HashMapBsonDocument doc = createEmptyDocument();
        assertThrows(BsonParseException.class, () -> {
            doc.getBoolean("nonexistent");
        });
    }

    @Test
    public void testGetBoolean_WrongType() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("value", 1);
        types.put("value", BsonType.INT32);

        HashMapBsonDocument doc = createDocument(data, types);
        assertThrows(BsonParseException.class, () -> {
            doc.getBoolean("value");
        });
    }

    @Test
    public void testGetBoolean_WithDefault_FieldExists() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("enabled", Boolean.TRUE);
        types.put("enabled", BsonType.BOOLEAN);

        HashMapBsonDocument doc = createDocument(data, types);
        assertTrue(doc.getBoolean("enabled", false));
    }

    @Test
    public void testGetBoolean_WithDefault_FieldNotExists() {
        HashMapBsonDocument doc = createEmptyDocument();
        assertTrue(doc.getBoolean("nonexistent", true));
    }

    @Test
    public void testGetBoolean_WithDefault_WrongType() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("value", "string");
        types.put("value", BsonType.STRING);

        HashMapBsonDocument doc = createDocument(data, types);
        assertTrue(doc.getBoolean("value", true));
    }

    @Test
    public void testGetBoolean_WithDefault_NullValue() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("nullable", null);
        types.put("nullable", BsonType.NULL);

        HashMapBsonDocument doc = createDocument(data, types);
        assertTrue(doc.getBoolean("nullable", true));
    }

    // ==================== getString测试 ====================

    @Test
    public void testGetString_FieldExists() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("name", "Alice");
        types.put("name", BsonType.STRING);

        HashMapBsonDocument doc = createDocument(data, types);
        assertEquals("Alice", doc.getString("name"));
    }

    @Test
    public void testGetString_FieldNotExists() {
        HashMapBsonDocument doc = createEmptyDocument();
        assertNull(doc.getString("nonexistent"));
    }

    @Test
    public void testGetString_WrongType() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("value", 123);
        types.put("value", BsonType.INT32);

        HashMapBsonDocument doc = createDocument(data, types);
        assertThrows(BsonParseException.class, () -> {
            doc.getString("value");
        });
    }

    @Test
    public void testGetString_WithDefault_FieldExists() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("city", "Beijing");
        types.put("city", BsonType.STRING);

        HashMapBsonDocument doc = createDocument(data, types);
        assertEquals("Beijing", doc.getString("city", "default"));
    }

    @Test
    public void testGetString_WithDefault_FieldNotExists() {
        HashMapBsonDocument doc = createEmptyDocument();
        assertEquals("default", doc.getString("nonexistent", "default"));
    }

    @Test
    public void testGetString_WithDefault_WrongType() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("value", 999);
        types.put("value", BsonType.INT32);

        HashMapBsonDocument doc = createDocument(data, types);
        assertEquals("default", doc.getString("value", "default"));
    }

    @Test
    public void testGetString_WithDefault_NullValue() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("nullable", null);
        types.put("nullable", BsonType.NULL);

        HashMapBsonDocument doc = createDocument(data, types);
        assertEquals("default", doc.getString("nullable", "default"));
    }

    // ==================== size/isEmpty测试 ====================

    @Test
    public void testSize_EmptyDocument() {
        HashMapBsonDocument doc = createEmptyDocument();
        assertEquals(0, doc.size());
    }

    @Test
    public void testSize_NonEmptyDocument() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("field1", 1);
        data.put("field2", 2);
        data.put("field3", 3);
        types.put("field1", BsonType.INT32);
        types.put("field2", BsonType.INT32);
        types.put("field3", BsonType.INT32);

        HashMapBsonDocument doc = createDocument(data, types);
        assertEquals(3, doc.size());
    }

    @Test
    public void testIsEmpty_EmptyDocument() {
        HashMapBsonDocument doc = createEmptyDocument();
        assertTrue(doc.isEmpty());
    }

    @Test
    public void testIsEmpty_NonEmptyDocument() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("field", "value");
        types.put("field", BsonType.STRING);

        HashMapBsonDocument doc = createDocument(data, types);
        assertFalse(doc.isEmpty());
    }

    // ==================== getDocument测试 ====================

    @Test
    public void testGetDocument_FieldExists() {
        Map<String, Object> nestedData = new HashMap<String, Object>();
        Map<String, Byte> nestedTypes = new HashMap<String, Byte>();
        nestedData.put("nested", "value");
        nestedTypes.put("nested", BsonType.STRING);
        HashMapBsonDocument nestedDoc = createDocument(nestedData, nestedTypes);

        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("doc", nestedDoc);
        types.put("doc", BsonType.DOCUMENT);

        HashMapBsonDocument doc = createDocument(data, types);
        BsonDocument result = doc.getDocument("doc");
        assertNotNull(result);
        assertEquals("value", result.getString("nested"));
    }

    @Test
    public void testGetDocument_FieldNotExists() {
        HashMapBsonDocument doc = createEmptyDocument();
        assertNull(doc.getDocument("nonexistent"));
    }

    @Test
    public void testGetDocument_WrongType() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("value", "string");
        types.put("value", BsonType.STRING);

        HashMapBsonDocument doc = createDocument(data, types);
        assertThrows(BsonParseException.class, () -> {
            doc.getDocument("value");
        });
    }

    // ==================== getArray测试 ====================

    @Test
    public void testGetArray_FieldExists() {
        // Create array using builder
        HashMapBsonArrayBuilder builder = new HashMapBsonArrayBuilder();
        builder.addInt32(1).addInt32(2).addInt32(3);
        BsonArray builtArray = builder.build();

        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("arr", builtArray);
        types.put("arr", BsonType.ARRAY);

        HashMapBsonDocument doc = createDocument(data, types);
        BsonArray result = doc.getArray("arr");
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testGetArray_FieldNotExists() {
        HashMapBsonDocument doc = createEmptyDocument();
        assertNull(doc.getArray("nonexistent"));
    }

    @Test
    public void testGetArray_WrongType() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("value", "string");
        types.put("value", BsonType.STRING);

        HashMapBsonDocument doc = createDocument(data, types);
        assertThrows(BsonParseException.class, () -> {
            doc.getArray("value");
        });
    }

    // ==================== getObjectId测试 ====================

    @Test
    public void testGetObjectId_FieldExists_String() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("id", "507f1f77bcf86cd799439011");
        types.put("id", BsonType.OBJECT_ID);

        HashMapBsonDocument doc = createDocument(data, types);
        assertEquals("507f1f77bcf86cd799439011", doc.getObjectId("id"));
    }

    @Test
    public void testGetObjectId_FieldExists_ByteArray() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        byte[] objectIdBytes = new byte[12];
        for (int i = 0; i < 12; i++) {
            objectIdBytes[i] = (byte) i;
        }
        data.put("id", objectIdBytes);
        types.put("id", BsonType.OBJECT_ID);

        HashMapBsonDocument doc = createDocument(data, types);
        assertEquals("000102030405060708090a0b", doc.getObjectId("id"));
    }

    @Test
    public void testGetObjectId_FieldNotExists() {
        HashMapBsonDocument doc = createEmptyDocument();
        assertNull(doc.getObjectId("nonexistent"));
    }

    @Test
    public void testGetObjectId_WrongType() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("value", 123);
        types.put("value", BsonType.INT32);

        HashMapBsonDocument doc = createDocument(data, types);
        assertThrows(BsonParseException.class, () -> {
            doc.getObjectId("value");
        });
    }

    @Test
    public void testGetObjectId_InvalidByteArrayLength() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        byte[] invalidBytes = new byte[10]; // Should be 12
        data.put("id", invalidBytes);
        types.put("id", BsonType.OBJECT_ID);

        HashMapBsonDocument doc = createDocument(data, types);
        assertThrows(BsonParseException.class, () -> {
            doc.getObjectId("id");
        });
    }

    // ==================== getDateTime测试 ====================

    @Test
    public void testGetDateTime_FieldExists() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("timestamp", 1638360000000L);
        types.put("timestamp", BsonType.DATE_TIME);

        HashMapBsonDocument doc = createDocument(data, types);
        assertEquals(1638360000000L, doc.getDateTime("timestamp"));
    }

    @Test
    public void testGetDateTime_FieldNotExists() {
        HashMapBsonDocument doc = createEmptyDocument();
        assertThrows(BsonParseException.class, () -> {
            doc.getDateTime("nonexistent");
        });
    }

    @Test
    public void testGetDateTime_WrongType() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("value", "string");
        types.put("value", BsonType.STRING);

        HashMapBsonDocument doc = createDocument(data, types);
        assertThrows(BsonParseException.class, () -> {
            doc.getDateTime("value");
        });
    }

    @Test
    public void testGetDateTime_WithDefault_FieldExists() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("timestamp", 1638360000000L);
        types.put("timestamp", BsonType.DATE_TIME);

        HashMapBsonDocument doc = createDocument(data, types);
        assertEquals(1638360000000L, doc.getDateTime("timestamp", 0L));
    }

    @Test
    public void testGetDateTime_WithDefault_FieldNotExists() {
        HashMapBsonDocument doc = createEmptyDocument();
        assertEquals(999L, doc.getDateTime("nonexistent", 999L));
    }

    @Test
    public void testGetDateTime_WithDefault_WrongType() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("value", "string");
        types.put("value", BsonType.STRING);

        HashMapBsonDocument doc = createDocument(data, types);
        assertEquals(888L, doc.getDateTime("value", 888L));
    }

    @Test
    public void testGetDateTime_WithDefault_NullValue() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("nullable", null);
        types.put("nullable", BsonType.NULL);

        HashMapBsonDocument doc = createDocument(data, types);
        assertEquals(777L, doc.getDateTime("nullable", 777L));
    }

    // ==================== equals/hashCode/toString测试 ====================

    @Test
    public void testEquals_SameInstance() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("field", "value");
        types.put("field", BsonType.STRING);

        HashMapBsonDocument doc = createDocument(data, types);
        assertEquals(doc, doc);
    }

    @Test
    public void testEquals_EqualDocuments() {
        Map<String, Object> data1 = new HashMap<String, Object>();
        Map<String, Byte> types1 = new HashMap<String, Byte>();
        data1.put("field", "value");
        types1.put("field", BsonType.STRING);

        Map<String, Object> data2 = new HashMap<String, Object>();
        Map<String, Byte> types2 = new HashMap<String, Byte>();
        data2.put("field", "value");
        types2.put("field", BsonType.STRING);

        HashMapBsonDocument doc1 = createDocument(data1, types1);
        HashMapBsonDocument doc2 = createDocument(data2, types2);
        assertEquals(doc1, doc2);
    }

    @Test
    public void testEquals_DifferentType() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("field", "value");
        types.put("field", BsonType.STRING);

        HashMapBsonDocument doc = createDocument(data, types);
        assertNotEquals(doc, "not a document");
    }

    @Test
    public void testHashCode() {
        Map<String, Object> data1 = new HashMap<String, Object>();
        Map<String, Byte> types1 = new HashMap<String, Byte>();
        data1.put("field", "value");
        types1.put("field", BsonType.STRING);

        Map<String, Object> data2 = new HashMap<String, Object>();
        Map<String, Byte> types2 = new HashMap<String, Byte>();
        data2.put("field", "value");
        types2.put("field", BsonType.STRING);

        HashMapBsonDocument doc1 = createDocument(data1, types1);
        HashMapBsonDocument doc2 = createDocument(data2, types2);
        assertEquals(doc1.hashCode(), doc2.hashCode());
    }

    @Test
    public void testToString() {
        Map<String, Object> data = new HashMap<String, Object>();
        Map<String, Byte> types = new HashMap<String, Byte>();
        data.put("field", "value");
        types.put("field", BsonType.STRING);

        HashMapBsonDocument doc = createDocument(data, types);
        String str = doc.toString();
        assertNotNull(str);
        assertTrue(str.contains("HashMapBsonDocument"));
    }
}
