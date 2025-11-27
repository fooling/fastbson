package com.cloud.fastbson.document.fast;

import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.exception.BsonParseException;
import com.cloud.fastbson.util.BsonType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FastBsonDocument测试 - 目标100%分支覆盖率 (74个分支)
 */
public class FastBsonDocumentTest {

    // ==================== 辅助方法 ====================

    private FastBsonDocument createEmptyDocument() {
        return (FastBsonDocument) new FastBsonDocumentBuilder().build();
    }

    // ==================== 基本方法测试 ====================

    @Test
    public void testSize_EmptyDocument() {
        FastBsonDocument doc = createEmptyDocument();
        assertEquals(0, doc.size());
    }

    @Test
    public void testSize_NonEmptyDocument() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putInt32("a", 1).putInt32("b", 2);
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertEquals(2, doc.size());
    }

    @Test
    public void testIsEmpty_EmptyDocument() {
        FastBsonDocument doc = createEmptyDocument();
        assertTrue(doc.isEmpty());
    }

    @Test
    public void testIsEmpty_NonEmptyDocument() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putInt32("value", 42);
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertFalse(doc.isEmpty());
    }

    @Test
    public void testContains_FieldExists() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putInt32("value", 42);
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertTrue(doc.contains("value"));
    }

    @Test
    public void testContains_FieldNotExists() {
        FastBsonDocument doc = createEmptyDocument();
        assertFalse(doc.contains("nonexistent"));
    }

    @Test
    public void testGetType_FieldExists() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putInt32("value", 42);
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertEquals(BsonType.INT32, doc.getType("value"));
    }

    @Test
    public void testGetType_FieldNotExists() {
        FastBsonDocument doc = createEmptyDocument();
        assertEquals(0, doc.getType("nonexistent"));
    }

    @Test
    public void testIsNull_NullField() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putNull("nullable");
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertTrue(doc.isNull("nullable"));
    }

    @Test
    public void testIsNull_NotNullField() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putInt32("value", 42);
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertFalse(doc.isNull("value"));
    }

    @Test
    public void testIsNull_FieldNotExists() {
        FastBsonDocument doc = createEmptyDocument();
        assertFalse(doc.isNull("nonexistent"));
    }

    @Test
    public void testFieldNames() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putInt32("a", 1).putString("b", "hello");
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        Set<String> names = doc.fieldNames();
        assertEquals(2, names.size());
        assertTrue(names.contains("a"));
        assertTrue(names.contains("b"));
    }

    // ==================== getInt32测试 ====================

    @Test
    public void testGetInt32_FieldExists() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putInt32("value", 42);
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertEquals(42, doc.getInt32("value"));
    }

    @Test
    public void testGetInt32_FieldNotExists() {
        FastBsonDocument doc = createEmptyDocument();
        assertThrows(NullPointerException.class, () -> {
            doc.getInt32("nonexistent");
        });
    }

    @Test
    public void testGetInt32_WrongType() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putString("value", "not an int");
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertThrows(ClassCastException.class, () -> {
            doc.getInt32("value");
        });
    }

    @Test
    public void testGetInt32_WithDefault_FieldExists() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putInt32("value", 42);
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertEquals(42, doc.getInt32("value", 999));
    }

    @Test
    public void testGetInt32_WithDefault_FieldNotExists() {
        FastBsonDocument doc = createEmptyDocument();
        assertEquals(999, doc.getInt32("nonexistent", 999));
    }

    // ==================== getInt64测试 ====================

    @Test
    public void testGetInt64_FieldExists() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putInt64("value", 42L);
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertEquals(42L, doc.getInt64("value"));
    }

    @Test
    public void testGetInt64_FieldNotExists() {
        FastBsonDocument doc = createEmptyDocument();
        assertThrows(NullPointerException.class, () -> {
            doc.getInt64("nonexistent");
        });
    }

    @Test
    public void testGetInt64_WrongType() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putString("value", "not a long");
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertThrows(ClassCastException.class, () -> {
            doc.getInt64("value");
        });
    }

    @Test
    public void testGetInt64_WithDefault_FieldExists() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putInt64("value", 42L);
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertEquals(42L, doc.getInt64("value", 999L));
    }

    @Test
    public void testGetInt64_WithDefault_FieldNotExists() {
        FastBsonDocument doc = createEmptyDocument();
        assertEquals(999L, doc.getInt64("nonexistent", 999L));
    }

    // ==================== getDouble测试 ====================

    @Test
    public void testGetDouble_FieldExists() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putDouble("value", 3.14);
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertEquals(3.14, doc.getDouble("value"));
    }

    @Test
    public void testGetDouble_FieldNotExists() {
        FastBsonDocument doc = createEmptyDocument();
        assertThrows(NullPointerException.class, () -> {
            doc.getDouble("nonexistent");
        });
    }

    @Test
    public void testGetDouble_WrongType() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putString("value", "not a double");
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertThrows(ClassCastException.class, () -> {
            doc.getDouble("value");
        });
    }

    @Test
    public void testGetDouble_WithDefault_FieldExists() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putDouble("value", 3.14);
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertEquals(3.14, doc.getDouble("value", 9.99));
    }

    @Test
    public void testGetDouble_WithDefault_FieldNotExists() {
        FastBsonDocument doc = createEmptyDocument();
        assertEquals(9.99, doc.getDouble("nonexistent", 9.99));
    }

    // ==================== getBoolean测试 ====================

    @Test
    public void testGetBoolean_FieldExists_True() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putBoolean("value", true);
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertTrue(doc.getBoolean("value"));
    }

    @Test
    public void testGetBoolean_FieldExists_False() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putBoolean("value", false);
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertFalse(doc.getBoolean("value"));
    }

    @Test
    public void testGetBoolean_FieldNotExists() {
        FastBsonDocument doc = createEmptyDocument();
        assertThrows(NullPointerException.class, () -> {
            doc.getBoolean("nonexistent");
        });
    }

    @Test
    public void testGetBoolean_WrongType() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putString("value", "not a boolean");
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertThrows(ClassCastException.class, () -> {
            doc.getBoolean("value");
        });
    }

    @Test
    public void testGetBoolean_WithDefault_FieldExists() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putBoolean("value", true);
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertTrue(doc.getBoolean("value", false));
    }

    @Test
    public void testGetBoolean_WithDefault_FieldNotExists() {
        FastBsonDocument doc = createEmptyDocument();
        assertTrue(doc.getBoolean("nonexistent", true));
    }

    // ==================== getString测试 ====================

    @Test
    public void testGetString_FieldExists() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putString("value", "hello");
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertEquals("hello", doc.getString("value"));
    }

    @Test
    public void testGetString_FieldNotExists() {
        FastBsonDocument doc = createEmptyDocument();
        assertNull(doc.getString("nonexistent"));
    }

    @Test
    public void testGetString_WithDefault_FieldExists() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putString("value", "hello");
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertEquals("hello", doc.getString("value", "default"));
    }

    @Test
    public void testGetString_WithDefault_FieldNotExists() {
        FastBsonDocument doc = createEmptyDocument();
        assertEquals("default", doc.getString("nonexistent", "default"));
    }

    // ==================== getDocument测试 ====================

    @Test
    public void testGetDocument_FieldExists() {
        FastBsonDocumentBuilder nestedBuilder = new FastBsonDocumentBuilder();
        nestedBuilder.putInt32("nested", 42);
        FastBsonDocument nested = (FastBsonDocument) nestedBuilder.build();

        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putDocument("doc", nested);
        FastBsonDocument doc = (FastBsonDocument) builder.build();

        BsonDocument result = doc.getDocument("doc");
        assertNotNull(result);
        assertEquals(42, result.getInt32("nested"));
    }

    @Test
    public void testGetDocument_FieldNotExists() {
        FastBsonDocument doc = createEmptyDocument();
        assertNull(doc.getDocument("nonexistent"));
    }

    // ==================== getArray测试 ====================

    @Test
    public void testGetArray_FieldExists() {
        FastBsonArrayBuilder arrayBuilder = new FastBsonArrayBuilder();
        arrayBuilder.addInt32(1).addInt32(2);
        FastBsonArray array = (FastBsonArray) arrayBuilder.build();

        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putArray("arr", array);
        FastBsonDocument doc = (FastBsonDocument) builder.build();

        BsonArray result = doc.getArray("arr");
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testGetArray_FieldNotExists() {
        FastBsonDocument doc = createEmptyDocument();
        assertNull(doc.getArray("nonexistent"));
    }

    // ==================== getObjectId测试 ====================

    @Test
    public void testGetObjectId_FieldExists() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putObjectId("id", "507f1f77bcf86cd799439011");
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertEquals("507f1f77bcf86cd799439011", doc.getObjectId("id"));
    }

    @Test
    public void testGetObjectId_FieldNotExists() {
        FastBsonDocument doc = createEmptyDocument();
        assertNull(doc.getObjectId("nonexistent"));
    }

    // ==================== getDateTime测试 ====================

    @Test
    public void testGetDateTime_FieldExists() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putDateTime("timestamp", 1638360000000L);
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertEquals(1638360000000L, doc.getDateTime("timestamp"));
    }

    @Test
    public void testGetDateTime_FieldNotExists() {
        FastBsonDocument doc = createEmptyDocument();
        assertThrows(NullPointerException.class, () -> {
            doc.getDateTime("nonexistent");
        });
    }

    @Test
    public void testGetDateTime_WithDefault_FieldExists() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putDateTime("timestamp", 1638360000000L);
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertEquals(1638360000000L, doc.getDateTime("timestamp", 0L));
    }

    @Test
    public void testGetDateTime_WithDefault_FieldNotExists() {
        FastBsonDocument doc = createEmptyDocument();
        assertEquals(999L, doc.getDateTime("nonexistent", 999L));
    }

    // ==================== toJson测试 ====================

    @Test
    public void testToJson_EmptyDocument() {
        FastBsonDocument doc = createEmptyDocument();
        assertEquals("{}", doc.toJson());
    }

    @Test
    public void testToJson_SimpleFields() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putInt32("age", 25).putString("name", "Alice");
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        String json = doc.toJson();
        assertTrue(json.contains("\"age\""));
        assertTrue(json.contains("25"));
        assertTrue(json.contains("\"name\""));
        assertTrue(json.contains("\"Alice\""));
    }

    @Test
    public void testToJson_WithNull() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putNull("nullable");
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        String json = doc.toJson();
        assertTrue(json.contains("null"));
    }

    // ==================== toString测试 ====================

    @Test
    public void testToString() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putInt32("value", 42);
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        String str = doc.toString();
        assertNotNull(str);
        assertTrue(str.contains("42"));
    }

    // ==================== equals/hashCode测试 ====================

    @Test
    public void testEquals_SameInstance() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putInt32("value", 42);
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertEquals(doc, doc);
    }

    @Test
    public void testEquals_EqualDocuments() {
        FastBsonDocumentBuilder builder1 = new FastBsonDocumentBuilder();
        builder1.putInt32("a", 1).putString("b", "hello");
        FastBsonDocument doc1 = (FastBsonDocument) builder1.build();

        FastBsonDocumentBuilder builder2 = new FastBsonDocumentBuilder();
        builder2.putInt32("a", 1).putString("b", "hello");
        FastBsonDocument doc2 = (FastBsonDocument) builder2.build();

        assertEquals(doc1, doc2);
    }

    @Test
    public void testEquals_Null() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putInt32("value", 42);
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertNotEquals(null, doc);
    }

    @Test
    public void testEquals_DifferentClass() {
        FastBsonDocumentBuilder builder = new FastBsonDocumentBuilder();
        builder.putInt32("value", 42);
        FastBsonDocument doc = (FastBsonDocument) builder.build();
        assertNotEquals("not a document", doc);
    }

    @Test
    public void testHashCode_EqualDocuments() {
        FastBsonDocumentBuilder builder1 = new FastBsonDocumentBuilder();
        builder1.putInt32("a", 1).putString("b", "hello");
        FastBsonDocument doc1 = (FastBsonDocument) builder1.build();

        FastBsonDocumentBuilder builder2 = new FastBsonDocumentBuilder();
        builder2.putInt32("a", 1).putString("b", "hello");
        FastBsonDocument doc2 = (FastBsonDocument) builder2.build();

        assertEquals(doc1.hashCode(), doc2.hashCode());
    }
}
