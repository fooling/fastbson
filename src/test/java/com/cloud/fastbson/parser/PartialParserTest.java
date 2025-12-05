package com.cloud.fastbson.parser;

import org.bson.*;
import org.bson.io.BasicOutputBuffer;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PartialParser 单元测试
 *
 * @author FastBSON
 * @since 1.0.0
 */
public class PartialParserTest {

    // ==================== 构造函数测试 ====================

    @Test
    public void testConstructorWithVarargs() {
        // Arrange & Act
        PartialParser parser = new PartialParser("field1", "field2");

        // Assert
        assertNotNull(parser);
        assertEquals(2, parser.getTargetFieldCount());
        assertTrue(parser.isEarlyExitEnabled()); // 默认启用
    }

    @Test
    public void testConstructorWithSet() {
        // Arrange
        Set<String> fields = new HashSet<String>(Arrays.asList("name", "age"));

        // Act
        PartialParser parser = new PartialParser(fields);

        // Assert
        assertNotNull(parser);
        assertEquals(2, parser.getTargetFieldCount());
    }

    @Test
    public void testConstructorWithNullVarargs() {
        // Arrange & Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new PartialParser((String[]) null);
        });
    }

    @Test
    public void testConstructorWithEmptyVarargs() {
        // Arrange & Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new PartialParser();
        });
    }

    @Test
    public void testConstructorWithNullSet() {
        // Arrange & Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new PartialParser((Set<String>) null);
        });
    }

    @Test
    public void testConstructorWithEmptySet() {
        // Arrange
        Set<String> emptySet = new HashSet<String>();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new PartialParser(emptySet);
        });
    }

    // ==================== earlyExit 开关测试 ====================

    @Test
    public void testSetEarlyExit() {
        // Arrange
        PartialParser parser = new PartialParser("field1");

        // Act
        parser.setEarlyExit(false);

        // Assert
        assertFalse(parser.isEarlyExitEnabled());
    }

    @Test
    public void testDefaultEarlyExitEnabled() {
        // Arrange & Act
        PartialParser parser = new PartialParser("field1");

        // Assert
        assertTrue(parser.isEarlyExitEnabled());
    }

    // ==================== 部分字段解析测试 ====================

    @Test
    public void testParseSingleField() {
        // Arrange
        BsonDocument doc = new BsonDocument()
            .append("name", new BsonString("Alice"))
            .append("age", new BsonInt32(30))
            .append("email", new BsonString("alice@example.com"));

        byte[] bsonData = serializeDocument(doc);
        PartialParser parser = new PartialParser("name");

        // Act
        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Alice", result.get("name"));
        assertNull(result.get("age"));
        assertNull(result.get("email"));
    }

    @Test
    public void testParseMultipleFields() {
        // Arrange
        BsonDocument doc = new BsonDocument()
            .append("name", new BsonString("Bob"))
            .append("age", new BsonInt32(25))
            .append("city", new BsonString("Beijing"))
            .append("country", new BsonString("China"));

        byte[] bsonData = serializeDocument(doc);
        PartialParser parser = new PartialParser("name", "city");

        // Act
        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertEquals(2, result.size());
        assertEquals("Bob", result.get("name"));
        assertEquals("Beijing", result.get("city"));
        assertNull(result.get("age"));
        assertNull(result.get("country"));
    }

    @Test
    public void testParseAllFields() {
        // Arrange
        BsonDocument doc = new BsonDocument()
            .append("field1", new BsonString("value1"))
            .append("field2", new BsonInt32(42));

        byte[] bsonData = serializeDocument(doc);
        PartialParser parser = new PartialParser("field1", "field2");

        // Act
        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertEquals(2, result.size());
        assertEquals("value1", result.get("field1"));
        assertEquals(42, result.get("field2"));
    }

    @Test
    public void testParseNonExistentField() {
        // Arrange
        BsonDocument doc = new BsonDocument()
            .append("name", new BsonString("Charlie"));

        byte[] bsonData = serializeDocument(doc);
        PartialParser parser = new PartialParser("nonExistent");

        // Act
        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertEquals(0, result.size());
        assertNull(result.get("nonExistent"));
    }

    // ==================== 提前退出功能测试 ====================

    @Test
    public void testEarlyExitWhenAllFieldsFound() {
        // Arrange - 目标字段在文档前部
        BsonDocument doc = new BsonDocument()
            .append("field1", new BsonString("value1"))
            .append("field2", new BsonString("value2"))
            .append("field3", new BsonString("value3"))
            .append("field4", new BsonString("value4"))
            .append("field5", new BsonString("value5"));

        byte[] bsonData = serializeDocument(doc);
        PartialParser parser = new PartialParser("field1", "field2");
        parser.setEarlyExit(true);

        // Act
        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertEquals(2, result.size());
        assertEquals("value1", result.get("field1"));
        assertEquals("value2", result.get("field2"));
        // 提前退出后，field3-5 不会被解析
    }

    @Test
    public void testNoEarlyExitWhenDisabled() {
        // Arrange
        BsonDocument doc = new BsonDocument()
            .append("field1", new BsonString("value1"))
            .append("field2", new BsonString("value2"))
            .append("field3", new BsonString("value3"));

        byte[] bsonData = serializeDocument(doc);
        PartialParser parser = new PartialParser("field1", "field2");
        parser.setEarlyExit(false);

        // Act
        Map<String, Object> result = parser.parse(bsonData);

        // Assert - 即使禁用提前退出，结果应该相同（因为只匹配了 field1 和 field2）
        assertEquals(2, result.size());
        assertEquals("value1", result.get("field1"));
        assertEquals("value2", result.get("field2"));
    }

    @Test
    public void testEarlyExitWithTargetFieldsAtEnd() {
        // Arrange - 目标字段在文档尾部
        BsonDocument doc = new BsonDocument()
            .append("field1", new BsonString("value1"))
            .append("field2", new BsonString("value2"))
            .append("field3", new BsonString("value3"))
            .append("targetA", new BsonString("valueA"))
            .append("targetB", new BsonString("valueB"));

        byte[] bsonData = serializeDocument(doc);
        PartialParser parser = new PartialParser("targetA", "targetB");
        parser.setEarlyExit(true);

        // Act
        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertEquals(2, result.size());
        assertEquals("valueA", result.get("targetA"));
        assertEquals("valueB", result.get("targetB"));
    }

    // ==================== 类型测试 ====================

    @Test
    public void testParseMixedTypes() {
        // Arrange
        BsonDocument doc = new BsonDocument()
            .append("stringField", new BsonString("text"))
            .append("int32Field", new BsonInt32(42))
            .append("int64Field", new BsonInt64(9223372036854775807L))
            .append("doubleField", new BsonDouble(3.14))
            .append("booleanField", new BsonBoolean(true))
            .append("nullField", new BsonNull());

        byte[] bsonData = serializeDocument(doc);
        PartialParser parser = new PartialParser(
            "stringField", "int32Field", "doubleField", "nullField"
        );

        // Act
        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertEquals(4, result.size());
        assertEquals("text", result.get("stringField"));
        assertEquals(42, result.get("int32Field"));
        assertEquals(3.14, result.get("doubleField"));
        assertNull(result.get("nullField"));
    }

    @Test
    public void testParseNestedDocument() {
        // Arrange
        BsonDocument nestedDoc = new BsonDocument()
            .append("nestedField", new BsonString("nestedValue"));

        BsonDocument doc = new BsonDocument()
            .append("topLevel", new BsonString("topValue"))
            .append("nested", nestedDoc)
            .append("anotherField", new BsonInt32(100));

        byte[] bsonData = serializeDocument(doc);
        PartialParser parser = new PartialParser("topLevel", "nested");

        // Act
        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertEquals(2, result.size());
        assertEquals("topValue", result.get("topLevel"));

        @SuppressWarnings("unchecked")
        Map<String, Object> parsedNested = (Map<String, Object>) result.get("nested");
        assertNotNull(parsedNested);
        assertEquals("nestedValue", parsedNested.get("nestedField"));
    }

    @Test
    public void testParseArray() {
        // Arrange
        BsonArray array = new BsonArray();
        array.add(new BsonInt32(1));
        array.add(new BsonInt32(2));
        array.add(new BsonInt32(3));

        BsonDocument doc = new BsonDocument()
            .append("name", new BsonString("Test"))
            .append("numbers", array);

        byte[] bsonData = serializeDocument(doc);
        PartialParser parser = new PartialParser("numbers");

        // Act
        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertEquals(1, result.size());
        Object arrayResult = result.get("numbers");
        assertNotNull(arrayResult);
        assertTrue(arrayResult instanceof java.util.List);
    }

    // ==================== 边界情况测试 ====================

    @Test
    public void testParseEmptyDocument() {
        // Arrange
        BsonDocument doc = new BsonDocument();
        byte[] bsonData = serializeDocument(doc);
        PartialParser parser = new PartialParser("field1");

        // Act
        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertEquals(0, result.size());
    }

    @Test
    public void testParseWithInvalidBsonData() {
        // Arrange
        byte[] invalidData = new byte[]{0x01, 0x02};
        PartialParser parser = new PartialParser("field1");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            parser.parse(invalidData);
        });
    }

    @Test
    public void testParseWithNullBsonData() {
        // Arrange
        PartialParser parser = new PartialParser("field1");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            parser.parse(null);
        });
    }

    @Test
    public void testParseWithEmptyBsonData() {
        // Arrange
        byte[] emptyData = new byte[0];
        PartialParser parser = new PartialParser("field1");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            parser.parse(emptyData);
        });
    }

    @Test
    public void testParseLargeDocument() {
        // Arrange - 创建包含 100 个字段的文档
        BsonDocument doc = new BsonDocument();
        for (int i = 0; i < 100; i++) {
            doc.append("field" + i, new BsonString("value" + i));
        }

        byte[] bsonData = serializeDocument(doc);
        PartialParser parser = new PartialParser("field5", "field50", "field99");

        // Act
        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertEquals(3, result.size());
        assertEquals("value5", result.get("field5"));
        assertEquals("value50", result.get("field50"));
        assertEquals("value99", result.get("field99"));
    }

    @Test
    public void testParseWithDuplicateFieldNames() {
        // Arrange - BSON 允许重复字段名（后者覆盖前者）
        BsonDocument doc = new BsonDocument()
            .append("field", new BsonString("first"))
            .append("other", new BsonString("value"))
            .append("field", new BsonString("second"));

        byte[] bsonData = serializeDocument(doc);
        PartialParser parser = new PartialParser("field");

        // Act
        Map<String, Object> result = parser.parse(bsonData);

        // Assert - 应该保留最后一个值
        assertEquals(1, result.size());
        // 注意：实际行为取决于 BsonDocument 的实现
        // 如果 BsonDocument 不允许重复，则只会有一个 "field"
    }

    @Test
    public void testParseWithSpecialCharactersInFieldName() {
        // Arrange
        BsonDocument doc = new BsonDocument()
            .append("field_name", new BsonString("underscore"))
            .append("field-name", new BsonString("dash"))
            .append("field.name", new BsonString("dot"))
            .append("字段名", new BsonString("chinese"));

        byte[] bsonData = serializeDocument(doc);
        PartialParser parser = new PartialParser("field_name", "field.name", "字段名");

        // Act
        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertEquals(3, result.size());
        assertEquals("underscore", result.get("field_name"));
        assertEquals("dot", result.get("field.name"));
        assertEquals("chinese", result.get("字段名"));
    }

    @Test
    public void testGetTargetFieldCount() {
        // Arrange
        PartialParser parser = new PartialParser("field1", "field2", "field3");

        // Act
        int count = parser.getTargetFieldCount();

        // Assert
        assertEquals(3, count);
    }

    // ==================== 性能相关测试 ====================

    @Test
    public void testEarlyExitPerformance() {
        // Arrange - 创建一个大文档，目标字段在前面
        BsonDocument doc = new BsonDocument();
        doc.append("target1", new BsonString("value1"));
        doc.append("target2", new BsonString("value2"));
        // 添加大量其他字段
        for (int i = 0; i < 1000; i++) {
            doc.append("other" + i, new BsonString("value" + i));
        }

        byte[] bsonData = serializeDocument(doc);

        PartialParser parserWithEarlyExit = new PartialParser("target1", "target2");
        parserWithEarlyExit.setEarlyExit(true);

        PartialParser parserWithoutEarlyExit = new PartialParser("target1", "target2");
        parserWithoutEarlyExit.setEarlyExit(false);

        // Act
        long startWithEarlyExit = System.nanoTime();
        Map<String, Object> resultWithEarlyExit = parserWithEarlyExit.parse(bsonData);
        long timeWithEarlyExit = System.nanoTime() - startWithEarlyExit;

        long startWithoutEarlyExit = System.nanoTime();
        Map<String, Object> resultWithoutEarlyExit = parserWithoutEarlyExit.parse(bsonData);
        long timeWithoutEarlyExit = System.nanoTime() - startWithoutEarlyExit;

        // Assert
        assertEquals(2, resultWithEarlyExit.size());
        assertEquals(2, resultWithoutEarlyExit.size());

        // 提前退出应该更快（至少理论上）
        // 注意：由于 JIT 编译等因素，单次测试可能不稳定
        System.out.println("Early exit time: " + timeWithEarlyExit + " ns");
        System.out.println("No early exit time: " + timeWithoutEarlyExit + " ns");
    }

    // ==================== 嵌套文档和数组转换测试 (覆盖convertDocumentToMap和convertArrayToList的递归分支) ====================

    @Test
    public void testParseWithNestedDocument() {
        // Arrange - 创建包含嵌套文档的BSON
        BsonDocument innerDoc = new BsonDocument()
            .append("innerField", new BsonString("innerValue"));
        BsonDocument doc = new BsonDocument()
            .append("name", new BsonString("John"))
            .append("nested", innerDoc);

        byte[] bsonData = serializeDocument(doc);
        PartialParser parser = new PartialParser("name", "nested");

        // Act
        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("nested"));
        Object nestedValue = result.get("nested");
        assertTrue(nestedValue instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> nestedMap = (Map<String, Object>) nestedValue;
        assertEquals("innerValue", nestedMap.get("innerField"));
    }

    @Test
    public void testConvertDocumentToMapWithPrimitiveFields() {
        // Arrange - 创建包含嵌套文档的BSON，嵌套文档中包含基本类型字段
        // 这将测试convertDocumentToMap中value instanceof检查的"否"分支
        // 即：当嵌套文档的字段值不是BsonDocument也不是BsonArray时
        BsonDocument innerDoc = new BsonDocument()
            .append("primitiveString", new BsonString("primitiveValue"))  // 基本类型，不需要递归
            .append("primitiveInt", new BsonInt32(42))  // 基本类型，不需要递归
            .append("primitiveBoolean", new BsonBoolean(true));  // 基本类型，不需要递归

        BsonDocument doc = new BsonDocument()
            .append("nestedDoc", innerDoc);

        byte[] bsonData = serializeDocument(doc);
        PartialParser parser = new PartialParser("nestedDoc");

        // Act
        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("nestedDoc"));
        Object nestedValue = result.get("nestedDoc");
        assertTrue(nestedValue instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> nestedMap = (Map<String, Object>) nestedValue;
        // 这些基本类型字段在convertDocumentToMap中走的是"否"分支
        assertEquals("primitiveValue", nestedMap.get("primitiveString"));
        assertEquals(42, nestedMap.get("primitiveInt"));
        assertEquals(true, nestedMap.get("primitiveBoolean"));
    }

    @Test
    public void testConvertArrayToListWithPrimitiveElements() {
        // Arrange - 创建包含数组的BSON，数组中包含基本类型元素
        // 这将测试convertArrayToList中value instanceof检查的"否"分支
        // 即：当数组元素不是BsonDocument也不是BsonArray时
        BsonArray array = new BsonArray(Arrays.asList(
            new BsonString("element1"),  // 基本类型
            new BsonInt32(100),  // 基本类型
            new BsonBoolean(false)  // 基本类型
        ));

        BsonDocument doc = new BsonDocument()
            .append("primitiveArray", array);

        byte[] bsonData = serializeDocument(doc);
        PartialParser parser = new PartialParser("primitiveArray");

        // Act
        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("primitiveArray"));
        Object arrayValue = result.get("primitiveArray");
        assertTrue(arrayValue instanceof java.util.List);
        @SuppressWarnings("unchecked")
        java.util.List<Object> list = (java.util.List<Object>) arrayValue;
        // 这些基本类型元素在convertArrayToList中走的是"否"分支
        assertEquals(3, list.size());
        assertEquals("element1", list.get(0));
        assertEquals(100, list.get(1));
        assertEquals(false, list.get(2));
    }

    @Test
    public void testParseWithNestedArray() {
        // Arrange - 创建包含嵌套数组的BSON
        BsonArray innerArray = new BsonArray(Arrays.asList(
            new BsonInt32(1),
            new BsonInt32(2),
            new BsonInt32(3)
        ));
        BsonDocument doc = new BsonDocument()
            .append("name", new BsonString("John"))
            .append("numbers", innerArray);

        byte[] bsonData = serializeDocument(doc);
        PartialParser parser = new PartialParser("name", "numbers");

        // Act
        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("numbers"));
        Object numbersValue = result.get("numbers");
        assertTrue(numbersValue instanceof java.util.List);
        @SuppressWarnings("unchecked")
        java.util.List<Object> numbersList = (java.util.List<Object>) numbersValue;
        assertEquals(3, numbersList.size());
        assertEquals(1, numbersList.get(0));
        assertEquals(2, numbersList.get(1));
        assertEquals(3, numbersList.get(2));
    }

    @Test
    public void testParseWithArrayContainingDocuments() {
        // Arrange - 创建包含文档的数组
        BsonDocument innerDoc1 = new BsonDocument().append("id", new BsonInt32(1));
        BsonDocument innerDoc2 = new BsonDocument().append("id", new BsonInt32(2));
        BsonArray arrayWithDocs = new BsonArray(Arrays.asList(innerDoc1, innerDoc2));

        BsonDocument doc = new BsonDocument()
            .append("items", arrayWithDocs);

        byte[] bsonData = serializeDocument(doc);
        PartialParser parser = new PartialParser("items");

        // Act
        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("items"));
        Object itemsValue = result.get("items");
        assertTrue(itemsValue instanceof java.util.List);
        @SuppressWarnings("unchecked")
        java.util.List<Object> itemsList = (java.util.List<Object>) itemsValue;
        assertEquals(2, itemsList.size());
        assertTrue(itemsList.get(0) instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> firstItem = (Map<String, Object>) itemsList.get(0);
        assertEquals(1, firstItem.get("id"));
    }

    @Test
    public void testParseWithDocumentContainingArrayOfArrays() {
        // Arrange - 测试数组中包含嵌套数组
        BsonArray innerArray1 = new BsonArray(Arrays.asList(new BsonInt32(1), new BsonInt32(2)));
        BsonArray innerArray2 = new BsonArray(Arrays.asList(new BsonInt32(3), new BsonInt32(4)));
        BsonArray outerArray = new BsonArray(Arrays.asList(innerArray1, innerArray2));

        BsonDocument doc = new BsonDocument()
            .append("matrix", outerArray);

        byte[] bsonData = serializeDocument(doc);
        PartialParser parser = new PartialParser("matrix");

        // Act
        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("matrix"));
        Object matrixValue = result.get("matrix");
        assertTrue(matrixValue instanceof java.util.List);
        @SuppressWarnings("unchecked")
        java.util.List<Object> matrixList = (java.util.List<Object>) matrixValue;
        assertEquals(2, matrixList.size());
        assertTrue(matrixList.get(0) instanceof java.util.List);
        @SuppressWarnings("unchecked")
        java.util.List<Object> firstRow = (java.util.List<Object>) matrixList.get(0);
        assertEquals(2, firstRow.size());
        assertEquals(1, firstRow.get(0));
        assertEquals(2, firstRow.get(1));
    }

    @Test
    public void testConvertDocumentToMapWithNestedDocumentField() {
        // Test to cover Line 207 true branch: nested document contains another nested document
        // This ensures convertDocumentToMap recursively converts nested documents
        BsonDocument deeplyNestedDoc = new BsonDocument()
            .append("deepValue", new BsonString("deep"));

        BsonDocument nestedDoc = new BsonDocument()
            .append("level2", deeplyNestedDoc)  // This is a BsonDocument!
            .append("primitiveField", new BsonInt32(42));

        BsonDocument doc = new BsonDocument()
            .append("level1", nestedDoc);

        byte[] bsonData = serializeDocument(doc);
        PartialParser parser = new PartialParser("level1");

        // Act
        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("level1"));
        @SuppressWarnings("unchecked")
        Map<String, Object> level1Map = (Map<String, Object>) result.get("level1");
        assertTrue(level1Map.containsKey("level2"));
        @SuppressWarnings("unchecked")
        Map<String, Object> level2Map = (Map<String, Object>) level1Map.get("level2");
        assertEquals("deep", level2Map.get("deepValue"));
    }

    @Test
    public void testConvertDocumentToMapWithNestedArrayField() {
        // Test to cover Line 209 true branch: nested document contains an array
        // This ensures convertDocumentToMap recursively converts nested arrays
        BsonArray nestedArray = new BsonArray(Arrays.asList(
            new BsonInt32(1),
            new BsonInt32(2),
            new BsonInt32(3)
        ));

        BsonDocument nestedDoc = new BsonDocument()
            .append("arrayField", nestedArray)  // This is a BsonArray!
            .append("primitiveField", new BsonString("test"));

        BsonDocument doc = new BsonDocument()
            .append("outer", nestedDoc);

        byte[] bsonData = serializeDocument(doc);
        PartialParser parser = new PartialParser("outer");

        // Act
        Map<String, Object> result = parser.parse(bsonData);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("outer"));
        @SuppressWarnings("unchecked")
        Map<String, Object> outerMap = (Map<String, Object>) result.get("outer");
        assertTrue(outerMap.containsKey("arrayField"));
        @SuppressWarnings("unchecked")
        java.util.List<Object> arrayList = (java.util.List<Object>) outerMap.get("arrayField");
        assertEquals(3, arrayList.size());
        assertEquals(1, arrayList.get(0));
        assertEquals(2, arrayList.get(1));
        assertEquals(3, arrayList.get(2));
    }

    // ==================== 辅助方法 ====================

    private byte[] serializeDocument(BsonDocument doc) {
        BasicOutputBuffer buffer = new BasicOutputBuffer();
        BsonBinaryWriter writer = new BsonBinaryWriter(buffer);
        new org.bson.codecs.BsonDocumentCodec().encode(
            writer, doc, org.bson.codecs.EncoderContext.builder().build()
        );
        return buffer.toByteArray();
    }
}
