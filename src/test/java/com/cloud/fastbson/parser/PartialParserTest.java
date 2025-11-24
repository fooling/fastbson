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
