package com.cloud.fastbson.compatibility;
import com.cloud.fastbson.handler.parsers.DocumentParser;

import com.cloud.fastbson.document.BsonDocument;
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
    private byte[] serializeBsonDocument(org.bson.BsonDocument doc) {
        BasicOutputBuffer buffer = new BasicOutputBuffer();
        BsonBinaryWriter writer = new BsonBinaryWriter(buffer);
        new org.bson.codecs.BsonDocumentCodec().encode(writer, doc, EncoderContext.builder().build());
        return buffer.toByteArray();
    }

    @Test
    public void testBasicTypes() {
        // 构造测试文档（类似 mongo shell 格式）
        org.bson.BsonDocument doc = new org.bson.BsonDocument()
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
        BsonDocument result = (BsonDocument) DocumentParser.INSTANCE.parse(reader);

        // 验证结果
        assertEquals("Hello, FastBSON", result.get("stringField"));
        assertEquals(42, result.getInt32("int32Field"));
        assertEquals(9223372036854775807L, result.getInt64("int64Field"));
        assertEquals(3.14159, result.getDouble("doubleField"), 0.00001);
        assertEquals(true, result.getBoolean("booleanTrue"));
        assertEquals(false, result.getBoolean("booleanFalse"));
        assertNull(result.get("nullField"));
    }

    @Test
    public void testDateTimeAndObjectId() {
        Date now = new Date(1700000000000L); // 固定时间戳便于测试
        ObjectId objectId = new ObjectId("507f1f77bcf86cd799439011");

        org.bson.BsonDocument doc = new org.bson.BsonDocument()
            .append("dateTime", new BsonDateTime(now.getTime()))
            .append("objectId", new BsonObjectId(objectId));

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        BsonDocument result = (BsonDocument) DocumentParser.INSTANCE.parse(reader);

        // 验证 DateTime (FastBSON 返回 Long 以提高性能)
        Long parsedTimestamp = Long.valueOf(result.getDateTime("dateTime"));
        assertEquals(now.getTime(), parsedTimestamp.longValue());

        // 验证 ObjectId（FastBSON 返回 hex 字符串）
        // getString() 可能不支持所有类型，直接用 get() 然后转换
        String parsedObjectId = (String) result.get("objectId");
        assertEquals(objectId.toHexString(), parsedObjectId);
    }

    @Test
    public void testBinaryData() {
        byte[] testData = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05};
        BsonBinary binary = new BsonBinary((byte) 0x00, testData);

        org.bson.BsonDocument doc = new org.bson.BsonDocument()
            .append("binaryField", binary);

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        BsonDocument result = (BsonDocument) DocumentParser.INSTANCE.parse(reader);

        BinaryData parsedBinary = (BinaryData) result.get("binaryField");
        assertNotNull(parsedBinary);
        assertEquals(0x00, parsedBinary.subtype);
        assertArrayEquals(testData, parsedBinary.data);
    }

    @Test
    public void testRegex() {
        BsonRegularExpression regex = new BsonRegularExpression("^test.*", "i");

        org.bson.BsonDocument doc = new org.bson.BsonDocument()
            .append("regexField", regex);

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        BsonDocument result = (BsonDocument) DocumentParser.INSTANCE.parse(reader);

        RegexValue parsedRegex = (RegexValue) result.get("regexField");
        assertNotNull(parsedRegex);
        assertEquals("^test.*", parsedRegex.pattern);
        assertEquals("i", parsedRegex.options);
    }

    @Test
    public void testTimestamp() {
        BsonTimestamp timestamp = new BsonTimestamp(1234567890, 42);

        org.bson.BsonDocument doc = new org.bson.BsonDocument()
            .append("timestampField", timestamp);

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        BsonDocument result = (BsonDocument) DocumentParser.INSTANCE.parse(reader);

        Timestamp parsedTimestamp = (Timestamp) result.get("timestampField");
        assertNotNull(parsedTimestamp);
        assertEquals(1234567890, parsedTimestamp.seconds);
        assertEquals(42, parsedTimestamp.increment);
    }

    @Test
    public void testDecimal128() {
        org.bson.types.Decimal128 decimal = org.bson.types.Decimal128.parse("123.456");

        org.bson.BsonDocument doc = new org.bson.BsonDocument()
            .append("decimalField", new BsonDecimal128(decimal));

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        BsonDocument result = (BsonDocument) DocumentParser.INSTANCE.parse(reader);

        com.cloud.fastbson.types.Decimal128 parsedDecimal = (com.cloud.fastbson.types.Decimal128) result.get("decimalField");
        assertNotNull(parsedDecimal);
        assertEquals(16, parsedDecimal.bytes.length);
    }

    @Test
    public void testArray() {
        org.bson.BsonArray bsonArray = new org.bson.BsonArray(Arrays.asList(
            new BsonInt32(1),
            new BsonString("two"),
            new BsonDouble(3.0),
            new BsonBoolean(true)
        ));

        org.bson.BsonDocument doc = new org.bson.BsonDocument()
            .append("arrayField", bsonArray);

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        BsonDocument result = (BsonDocument) DocumentParser.INSTANCE.parse(reader);

        com.cloud.fastbson.document.BsonArray parsedArray = result.getArray("arrayField");
        assertNotNull(parsedArray);
        assertEquals(4, parsedArray.size());
        assertEquals(1, parsedArray.get(0));
        assertEquals("two", parsedArray.get(1));
        assertEquals(3.0, (Double) parsedArray.get(2), 0.001);
        assertEquals(true, parsedArray.get(3));
    }

    @Test
    public void testNestedDocument() {
        org.bson.BsonDocument nested = new org.bson.BsonDocument()
            .append("innerField1", new BsonString("nested value"))
            .append("innerField2", new BsonInt32(99));

        org.bson.BsonDocument doc = new org.bson.BsonDocument()
            .append("outerField", new BsonString("outer value"))
            .append("nestedDoc", nested);

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        BsonDocument result = (BsonDocument) DocumentParser.INSTANCE.parse(reader);

        assertEquals("outer value", result.getString("outerField"));

        BsonDocument parsedNested = result.getDocument("nestedDoc");
        assertNotNull(parsedNested);
        assertEquals("nested value", parsedNested.getString("innerField1"));
        assertEquals(99, parsedNested.getInt32("innerField2"));
    }

    @Test
    public void testJavaScript() {
        BsonJavaScript js = new BsonJavaScript("function() { return 42; }");

        org.bson.BsonDocument doc = new org.bson.BsonDocument()
            .append("jsField", js);

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        BsonDocument result = (BsonDocument) DocumentParser.INSTANCE.parse(reader);

        String parsedJs = result.getString("jsField");
        assertEquals("function() { return 42; }", parsedJs);
    }

    @Test
    public void testJavaScriptWithScope() {
        org.bson.BsonDocument scope = new org.bson.BsonDocument()
            .append("x", new BsonInt32(10))
            .append("y", new BsonString("test"));

        BsonJavaScriptWithScope jsWithScope = new BsonJavaScriptWithScope(
            "function() { return x + y; }", scope);

        org.bson.BsonDocument doc = new org.bson.BsonDocument()
            .append("jsWithScopeField", jsWithScope);

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        BsonDocument result = (BsonDocument) DocumentParser.INSTANCE.parse(reader);

        JavaScriptWithScope parsed =
            (JavaScriptWithScope) result.get("jsWithScopeField");
        assertNotNull(parsed);
        assertEquals("function() { return x + y; }", parsed.code);
        assertEquals(10, parsed.scope.get("x"));
        assertEquals("test", parsed.scope.get("y"));
    }

    @Test
    public void testMinKeyAndMaxKey() {
        org.bson.BsonDocument doc = new org.bson.BsonDocument()
            .append("minKey", new BsonMinKey())
            .append("maxKey", new BsonMaxKey());

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        BsonDocument result = (BsonDocument) DocumentParser.INSTANCE.parse(reader);

        assertTrue(result.get("minKey") instanceof MinKey);
        assertTrue(result.get("maxKey") instanceof MaxKey);
    }

    @Test
    public void testSymbol() {
        BsonSymbol symbol = new BsonSymbol("symbolValue");

        org.bson.BsonDocument doc = new org.bson.BsonDocument()
            .append("symbolField", symbol);

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        BsonDocument result = (BsonDocument) DocumentParser.INSTANCE.parse(reader);

        String parsedSymbol = result.getString("symbolField");
        assertEquals("symbolValue", parsedSymbol);
    }

    @Test
    public void testDBPointer() {
        ObjectId oid = new ObjectId("507f1f77bcf86cd799439011");
        BsonDbPointer dbPointer = new BsonDbPointer("db.collection", oid);

        org.bson.BsonDocument doc = new org.bson.BsonDocument()
            .append("dbPointerField", dbPointer);

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        BsonDocument result = (BsonDocument) DocumentParser.INSTANCE.parse(reader);

        DBPointer parsed = (DBPointer) result.get("dbPointerField");
        assertNotNull(parsed);
        assertEquals("db.collection", parsed.namespace);
        assertEquals(oid.toHexString(), parsed.id);
    }

    @Test
    public void testComplexDocument() {
        // 构造一个包含多种类型的复杂文档
        org.bson.BsonDocument doc = new org.bson.BsonDocument()
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
            .append("address", new org.bson.BsonDocument()
                .append("street", new BsonString("123 Main St"))
                .append("city", new BsonString("San Francisco"))
                .append("zipCode", new BsonInt32(94101))
            )
            .append("metadata", new BsonNull());

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        BsonDocument result = (BsonDocument) DocumentParser.INSTANCE.parse(reader);

        // 验证基本字段
        assertEquals("John Doe", result.get("name"));
        assertEquals(30, result.get("age"));
        assertEquals(50000.50, result.getDouble("salary"), 0.01);
        assertEquals(true, result.get("isActive"));
        assertNotNull(result.get("joinDate"));
        assertNull(result.get("metadata"));

        // 验证数组
        com.cloud.fastbson.document.BsonArray tags = result.getArray("tags");
        assertEquals(3, tags.size());
        assertEquals("java", tags.getString(0));
        assertEquals("bson", tags.getString(1));
        assertEquals("mongodb", tags.getString(2));

        // 验证嵌套文档
        BsonDocument address = result.getDocument("address");
        assertEquals("123 Main St", address.getString("street"));
        assertEquals("San Francisco", address.getString("city"));
        assertEquals(94101, address.getInt32("zipCode"));
    }

    @Test
    public void testEmptyDocument() {
        org.bson.BsonDocument doc = new org.bson.BsonDocument();

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        BsonDocument result = (BsonDocument) DocumentParser.INSTANCE.parse(reader);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testEmptyArray() {
        org.bson.BsonDocument doc = new org.bson.BsonDocument()
            .append("emptyArray", new BsonArray());

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        BsonDocument result = (BsonDocument) DocumentParser.INSTANCE.parse(reader);

        com.cloud.fastbson.document.BsonArray array = result.getArray("emptyArray");
        assertNotNull(array);
        assertEquals(0, array.size());
    }

    @Test
    public void testUnicodeStrings() {
        org.bson.BsonDocument doc = new org.bson.BsonDocument()
            .append("chinese", new BsonString("你好，世界"))
            .append("japanese", new BsonString("こんにちは"))
            .append("emoji", new BsonString("🚀💻🎉"))
            .append("mixed", new BsonString("Hello 世界 🌍"));

        byte[] bsonData = serializeBsonDocument(doc);
        BsonReader reader = new BsonReader(bsonData);
        BsonDocument result = (BsonDocument) DocumentParser.INSTANCE.parse(reader);

        assertEquals("你好，世界", result.get("chinese"));
        assertEquals("こんにちは", result.get("japanese"));
        assertEquals("🚀💻🎉", result.get("emoji"));
        assertEquals("Hello 世界 🌍", result.get("mixed"));
    }
}
