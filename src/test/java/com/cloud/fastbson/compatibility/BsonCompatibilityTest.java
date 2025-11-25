package com.cloud.fastbson.compatibility;

import com.cloud.fastbson.handler.TypeHandler;
import com.cloud.fastbson.reader.BsonReader;
import com.cloud.fastbson.types.BinaryData;
import com.cloud.fastbson.types.DBPointer;
import com.cloud.fastbson.types.JavaScriptWithScope;
import com.cloud.fastbson.types.MaxKey;
import com.cloud.fastbson.types.MinKey;
import com.cloud.fastbson.types.RegexValue;
import com.cloud.fastbson.types.Timestamp;
import org.bson.*;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.EncoderContext;
import org.bson.io.BasicOutputBuffer;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 端到端兼容性测试：使用 MongoDB BSON 库作为参照
 *
 * <p>测试流程：
 * 1. 使用 org.bson.BsonDocument 构造测试数据（类似 mongo shell 的 JSON 格式）
 * 2. 将 BsonDocument 序列化为 BSON 二进制 byte[]
 * 3. 使用 FastBSON 解析该 byte[]
 * 4. 逐字段对比 FastBSON 和 MongoDB BSON 的解析结果
 * 5. 验证所有 21 种 BSON 类型的兼容性
 */
public class BsonCompatibilityTest {

    private final TypeHandler handler = new TypeHandler();

    /**
     * 将 BsonDocument 序列化为 BSON 二进制
     */
    private byte[] serializeBsonDocument(BsonDocument doc) {
        BasicOutputBuffer buffer = new BasicOutputBuffer();
        BsonBinaryWriter writer = new BsonBinaryWriter(buffer);
        new BsonDocumentCodec().encode(writer, doc, EncoderContext.builder().build());
        return buffer.toByteArray();
    }

    @Test
    public void testBasicTypes() {
        // 构造测试文档（类似 mongo shell 格式）
        BsonDocument doc = new BsonDocument()
            .append("stringField", new BsonString("Hello, FastBSON"))
            .append("int32Field", new BsonInt32(42))
            .append("int64Field", new BsonInt64(9223372036854775807L))
            .append("doubleField", new BsonDouble(3.14159))
            .append("booleanTrue", new BsonBoolean(true))
            .append("booleanFalse", new BsonBoolean(false))
            .append("nullField", new BsonNull());

        // 序列化为 BSON
        byte[] bsonData = serializeBsonDocument(doc);

        // 使用 FastBSON 解析
        BsonReader reader = new BsonReader(bsonData);
        Map<String, Object> result = handler.parseDocument(reader);

        // 验证结果
        assertEquals("Hello, FastBSON", result.get("stringField"));
        assertEquals(42, result.get("int32Field"));
        assertEquals(9223372036854775807L, result.get("int64Field"));
        assertEquals(3.14159, (Double) result.get("doubleField"), 0.00001);
        assertEquals(true, result.get("booleanTrue"));
        assertEquals(false, result.get("booleanFalse"));
        assertNull(result.get("nullField"));
    }

    @Test
    public void testDateTimeAndObjectId() {
        Date now = new Date(1700000000000L); // 固定时间戳便于测试
        ObjectId objectId = new ObjectId("507f1f77bcf86cd799439011");

        BsonDocument doc = new BsonDocument()
            .append("dateTime", new BsonDateTime(now.getTime()))
            .append("objectId", new BsonObjectId(objectId));

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        Map<String, Object> result = handler.parseDocument(reader);

        // 验证 DateTime (FastBSON 返回 Long 以提高性能)
        Long parsedTimestamp = (Long) result.get("dateTime");
        assertEquals(now.getTime(), parsedTimestamp.longValue());

        // 验证 ObjectId（FastBSON 返回 hex 字符串）
        String parsedObjectId = (String) result.get("objectId");
        assertEquals(objectId.toHexString(), parsedObjectId);
    }

    @Test
    public void testBinaryData() {
        byte[] testData = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05};
        BsonBinary binary = new BsonBinary((byte) 0x00, testData);

        BsonDocument doc = new BsonDocument()
            .append("binaryField", binary);

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        Map<String, Object> result = handler.parseDocument(reader);

        BinaryData parsedBinary = (BinaryData) result.get("binaryField");
        assertNotNull(parsedBinary);
        assertEquals(0x00, parsedBinary.subtype);
        assertArrayEquals(testData, parsedBinary.data);
    }

    @Test
    public void testRegex() {
        BsonRegularExpression regex = new BsonRegularExpression("^test.*", "i");

        BsonDocument doc = new BsonDocument()
            .append("regexField", regex);

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        Map<String, Object> result = handler.parseDocument(reader);

        RegexValue parsedRegex = (RegexValue) result.get("regexField");
        assertNotNull(parsedRegex);
        assertEquals("^test.*", parsedRegex.pattern);
        assertEquals("i", parsedRegex.options);
    }

    @Test
    public void testTimestamp() {
        BsonTimestamp timestamp = new BsonTimestamp(1234567890, 42);

        BsonDocument doc = new BsonDocument()
            .append("timestampField", timestamp);

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        Map<String, Object> result = handler.parseDocument(reader);

        Timestamp parsedTimestamp = (Timestamp) result.get("timestampField");
        assertNotNull(parsedTimestamp);
        assertEquals(1234567890, parsedTimestamp.seconds);
        assertEquals(42, parsedTimestamp.increment);
    }

    @Test
    public void testDecimal128() {
        org.bson.types.Decimal128 decimal = org.bson.types.Decimal128.parse("123.456");

        BsonDocument doc = new BsonDocument()
            .append("decimalField", new BsonDecimal128(decimal));

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        Map<String, Object> result = handler.parseDocument(reader);

        com.cloud.fastbson.types.Decimal128 parsedDecimal = (com.cloud.fastbson.types.Decimal128) result.get("decimalField");
        assertNotNull(parsedDecimal);
        assertEquals(16, parsedDecimal.bytes.length);
    }

    @Test
    public void testArray() {
        BsonArray array = new BsonArray(Arrays.asList(
            new BsonInt32(1),
            new BsonString("two"),
            new BsonDouble(3.0),
            new BsonBoolean(true)
        ));

        BsonDocument doc = new BsonDocument()
            .append("arrayField", array);

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        Map<String, Object> result = handler.parseDocument(reader);

        @SuppressWarnings("unchecked")
        List<Object> parsedArray = (List<Object>) result.get("arrayField");
        assertNotNull(parsedArray);
        assertEquals(4, parsedArray.size());
        assertEquals(1, parsedArray.get(0));
        assertEquals("two", parsedArray.get(1));
        assertEquals(3.0, (Double) parsedArray.get(2), 0.001);
        assertEquals(true, parsedArray.get(3));
    }

    @Test
    public void testNestedDocument() {
        BsonDocument nested = new BsonDocument()
            .append("innerField1", new BsonString("nested value"))
            .append("innerField2", new BsonInt32(99));

        BsonDocument doc = new BsonDocument()
            .append("outerField", new BsonString("outer value"))
            .append("nestedDoc", nested);

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        Map<String, Object> result = handler.parseDocument(reader);

        assertEquals("outer value", result.get("outerField"));

        @SuppressWarnings("unchecked")
        Map<String, Object> parsedNested = (Map<String, Object>) result.get("nestedDoc");
        assertNotNull(parsedNested);
        assertEquals("nested value", parsedNested.get("innerField1"));
        assertEquals(99, parsedNested.get("innerField2"));
    }

    @Test
    public void testJavaScript() {
        BsonJavaScript js = new BsonJavaScript("function() { return 42; }");

        BsonDocument doc = new BsonDocument()
            .append("jsField", js);

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        Map<String, Object> result = handler.parseDocument(reader);

        String parsedJs = (String) result.get("jsField");
        assertEquals("function() { return 42; }", parsedJs);
    }

    @Test
    public void testJavaScriptWithScope() {
        BsonDocument scope = new BsonDocument()
            .append("x", new BsonInt32(10))
            .append("y", new BsonString("test"));

        BsonJavaScriptWithScope jsWithScope = new BsonJavaScriptWithScope(
            "function() { return x + y; }", scope);

        BsonDocument doc = new BsonDocument()
            .append("jsWithScopeField", jsWithScope);

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        Map<String, Object> result = handler.parseDocument(reader);

        JavaScriptWithScope parsed =
            (JavaScriptWithScope) result.get("jsWithScopeField");
        assertNotNull(parsed);
        assertEquals("function() { return x + y; }", parsed.code);
        assertEquals(10, parsed.scope.get("x"));
        assertEquals("test", parsed.scope.get("y"));
    }

    @Test
    public void testMinKeyAndMaxKey() {
        BsonDocument doc = new BsonDocument()
            .append("minKey", new BsonMinKey())
            .append("maxKey", new BsonMaxKey());

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        Map<String, Object> result = handler.parseDocument(reader);

        assertTrue(result.get("minKey") instanceof MinKey);
        assertTrue(result.get("maxKey") instanceof MaxKey);
    }

    @Test
    public void testSymbol() {
        BsonSymbol symbol = new BsonSymbol("symbolValue");

        BsonDocument doc = new BsonDocument()
            .append("symbolField", symbol);

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        Map<String, Object> result = handler.parseDocument(reader);

        String parsedSymbol = (String) result.get("symbolField");
        assertEquals("symbolValue", parsedSymbol);
    }

    @Test
    public void testDBPointer() {
        ObjectId oid = new ObjectId("507f1f77bcf86cd799439011");
        BsonDbPointer dbPointer = new BsonDbPointer("db.collection", oid);

        BsonDocument doc = new BsonDocument()
            .append("dbPointerField", dbPointer);

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        Map<String, Object> result = handler.parseDocument(reader);

        DBPointer parsed = (DBPointer) result.get("dbPointerField");
        assertNotNull(parsed);
        assertEquals("db.collection", parsed.namespace);
        assertEquals(oid.toHexString(), parsed.id);
    }

    @Test
    public void testComplexDocument() {
        // 构造一个包含多种类型的复杂文档
        BsonDocument doc = new BsonDocument()
            .append("name", new BsonString("John Doe"))
            .append("age", new BsonInt32(30))
            .append("salary", new BsonDouble(50000.50))
            .append("isActive", new BsonBoolean(true))
            .append("joinDate", new BsonDateTime(new Date().getTime()))
            .append("tags", new BsonArray(Arrays.asList(
                new BsonString("java"),
                new BsonString("bson"),
                new BsonString("mongodb")
            )))
            .append("address", new BsonDocument()
                .append("street", new BsonString("123 Main St"))
                .append("city", new BsonString("San Francisco"))
                .append("zipCode", new BsonInt32(94101))
            )
            .append("metadata", new BsonNull());

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        Map<String, Object> result = handler.parseDocument(reader);

        // 验证基本字段
        assertEquals("John Doe", result.get("name"));
        assertEquals(30, result.get("age"));
        assertEquals(50000.50, (Double) result.get("salary"), 0.01);
        assertEquals(true, result.get("isActive"));
        assertNotNull(result.get("joinDate"));
        assertNull(result.get("metadata"));

        // 验证数组
        @SuppressWarnings("unchecked")
        List<Object> tags = (List<Object>) result.get("tags");
        assertEquals(3, tags.size());
        assertEquals("java", tags.get(0));
        assertEquals("bson", tags.get(1));
        assertEquals("mongodb", tags.get(2));

        // 验证嵌套文档
        @SuppressWarnings("unchecked")
        Map<String, Object> address = (Map<String, Object>) result.get("address");
        assertEquals("123 Main St", address.get("street"));
        assertEquals("San Francisco", address.get("city"));
        assertEquals(94101, address.get("zipCode"));
    }

    @Test
    public void testEmptyDocument() {
        BsonDocument doc = new BsonDocument();

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        Map<String, Object> result = handler.parseDocument(reader);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testEmptyArray() {
        BsonDocument doc = new BsonDocument()
            .append("emptyArray", new BsonArray());

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        Map<String, Object> result = handler.parseDocument(reader);

        @SuppressWarnings("unchecked")
        List<Object> array = (List<Object>) result.get("emptyArray");
        assertNotNull(array);
        assertEquals(0, array.size());
    }

    @Test
    public void testUnicodeStrings() {
        BsonDocument doc = new BsonDocument()
            .append("chinese", new BsonString("你好，世界"))
            .append("japanese", new BsonString("こんにちは"))
            .append("emoji", new BsonString("🚀💻🎉"))
            .append("mixed", new BsonString("Hello 世界 🌍"));

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        Map<String, Object> result = handler.parseDocument(reader);

        assertEquals("你好，世界", result.get("chinese"));
        assertEquals("こんにちは", result.get("japanese"));
        assertEquals("🚀💻🎉", result.get("emoji"));
        assertEquals("Hello 世界 🌍", result.get("mixed"));
    }
}
