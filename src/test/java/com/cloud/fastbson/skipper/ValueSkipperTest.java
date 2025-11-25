package com.cloud.fastbson.skipper;

import com.cloud.fastbson.reader.BsonReader;
import com.cloud.fastbson.util.BsonType;
import com.cloud.fastbson.exception.InvalidBsonTypeException;
import org.bson.*;
import org.bson.io.BasicOutputBuffer;
import org.bson.io.BsonOutput;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ValueSkipper 单元测试
 *
 * @author FastBSON
 * @since 1.0.0
 */
public class ValueSkipperTest {

    // ==================== 构造函数测试 ====================

    @Test
    public void testConstructorWithValidReader() {
        // Arrange
        byte[] data = new byte[]{0x01, 0x02, 0x03};
        BsonReader reader = new BsonReader(data);

        // Act
        ValueSkipper skipper = new ValueSkipper(reader);

        // Assert
        assertNotNull(skipper);
    }

    @Test
    public void testConstructorWithNullReader() {
        // Arrange & Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new ValueSkipper(null);
        });
    }

    // ==================== 固定长度类型测试 ====================

    @Test
    public void testSkipDouble() {
        // Arrange - Double 类型 (8 bytes)
        byte[] data = createBsonWithDouble(3.14159);
        BsonReader reader = new BsonReader(data);
        reader.skip(4); // 跳过文档长度
        byte type = reader.readByte();
        reader.readCString(); // 跳过字段名

        int positionBefore = reader.position();
        ValueSkipper skipper = new ValueSkipper(reader);

        // Act
        skipper.skipValue(type);

        // Assert
        assertEquals(8, reader.position() - positionBefore);
    }

    @Test
    public void testSkipInt32() {
        // Arrange - Int32 类型 (4 bytes)
        byte[] data = createBsonWithInt32(42);
        BsonReader reader = new BsonReader(data);
        reader.skip(4); // 跳过文档长度
        byte type = reader.readByte();
        reader.readCString(); // 跳过字段名

        int positionBefore = reader.position();
        ValueSkipper skipper = new ValueSkipper(reader);

        // Act
        skipper.skipValue(type);

        // Assert
        assertEquals(4, reader.position() - positionBefore);
    }

    @Test
    public void testSkipInt64() {
        // Arrange - Int64 类型 (8 bytes)
        byte[] data = createBsonWithInt64(9223372036854775807L);
        BsonReader reader = new BsonReader(data);
        reader.skip(4); // 跳过文档长度
        byte type = reader.readByte();
        reader.readCString(); // 跳过字段名

        int positionBefore = reader.position();
        ValueSkipper skipper = new ValueSkipper(reader);

        // Act
        skipper.skipValue(type);

        // Assert
        assertEquals(8, reader.position() - positionBefore);
    }

    @Test
    public void testSkipBoolean() {
        // Arrange - Boolean 类型 (1 byte)
        byte[] data = createBsonWithBoolean(true);
        BsonReader reader = new BsonReader(data);
        reader.skip(4); // 跳过文档长度
        byte type = reader.readByte();
        reader.readCString(); // 跳过字段名

        int positionBefore = reader.position();
        ValueSkipper skipper = new ValueSkipper(reader);

        // Act
        skipper.skipValue(type);

        // Assert
        assertEquals(1, reader.position() - positionBefore);
    }

    @Test
    public void testSkipDateTime() {
        // Arrange - DateTime 类型 (8 bytes)
        byte[] data = createBsonWithDateTime(new Date());
        BsonReader reader = new BsonReader(data);
        reader.skip(4); // 跳过文档长度
        byte type = reader.readByte();
        reader.readCString(); // 跳过字段名

        int positionBefore = reader.position();
        ValueSkipper skipper = new ValueSkipper(reader);

        // Act
        skipper.skipValue(type);

        // Assert
        assertEquals(8, reader.position() - positionBefore);
    }

    @Test
    public void testSkipObjectId() {
        // Arrange - ObjectId 类型 (12 bytes)
        byte[] data = createBsonWithObjectId(new ObjectId());
        BsonReader reader = new BsonReader(data);
        reader.skip(4); // 跳过文档长度
        byte type = reader.readByte();
        reader.readCString(); // 跳过字段名

        int positionBefore = reader.position();
        ValueSkipper skipper = new ValueSkipper(reader);

        // Act
        skipper.skipValue(type);

        // Assert
        assertEquals(12, reader.position() - positionBefore);
    }

    @Test
    public void testSkipTimestamp() {
        // Arrange - Timestamp 类型 (8 bytes)
        byte[] data = createBsonWithTimestamp(new BsonTimestamp(1234567890, 1));
        BsonReader reader = new BsonReader(data);
        reader.skip(4); // 跳过文档长度
        byte type = reader.readByte();
        reader.readCString(); // 跳过字段名

        int positionBefore = reader.position();
        ValueSkipper skipper = new ValueSkipper(reader);

        // Act
        skipper.skipValue(type);

        // Assert
        assertEquals(8, reader.position() - positionBefore);
    }

    @Test
    public void testSkipDecimal128() {
        // Arrange - Decimal128 类型 (16 bytes)
        byte[] data = createBsonWithDecimal128(Decimal128.parse("123.456"));
        BsonReader reader = new BsonReader(data);
        reader.skip(4); // 跳过文档长度
        byte type = reader.readByte();
        reader.readCString(); // 跳过字段名

        int positionBefore = reader.position();
        ValueSkipper skipper = new ValueSkipper(reader);

        // Act
        skipper.skipValue(type);

        // Assert
        assertEquals(16, reader.position() - positionBefore);
    }

    // ==================== 零长度类型测试 ====================

    @Test
    public void testSkipNull() {
        // Arrange - Null 类型 (0 bytes)
        byte[] data = createBsonWithNull();
        BsonReader reader = new BsonReader(data);
        reader.skip(4); // 跳过文档长度
        byte type = reader.readByte();
        reader.readCString(); // 跳过字段名

        int positionBefore = reader.position();
        ValueSkipper skipper = new ValueSkipper(reader);

        // Act
        skipper.skipValue(type);

        // Assert
        assertEquals(0, reader.position() - positionBefore);
    }

    @Test
    public void testSkipMinKey() {
        // Arrange - MinKey 类型 (0 bytes)
        byte[] data = createBsonWithMinKey();
        BsonReader reader = new BsonReader(data);
        reader.skip(4); // 跳过文档长度
        byte type = reader.readByte();
        reader.readCString(); // 跳过字段名

        int positionBefore = reader.position();
        ValueSkipper skipper = new ValueSkipper(reader);

        // Act
        skipper.skipValue(type);

        // Assert
        assertEquals(0, reader.position() - positionBefore);
    }

    @Test
    public void testSkipMaxKey() {
        // Arrange - MaxKey 类型 (0 bytes)
        byte[] data = createBsonWithMaxKey();
        BsonReader reader = new BsonReader(data);
        reader.skip(4); // 跳过文档长度
        byte type = reader.readByte();
        reader.readCString(); // 跳过字段名

        int positionBefore = reader.position();
        ValueSkipper skipper = new ValueSkipper(reader);

        // Act
        skipper.skipValue(type);

        // Assert
        assertEquals(0, reader.position() - positionBefore);
    }

    // ==================== 变长类型测试 ====================

    @Test
    public void testSkipString() {
        // Arrange - String 类型
        String testString = "Hello, FastBSON!";
        byte[] data = createBsonWithString(testString);
        BsonReader reader = new BsonReader(data);
        reader.skip(4); // 跳过文档长度
        byte type = reader.readByte();
        reader.readCString(); // 跳过字段名

        int positionBefore = reader.position();
        ValueSkipper skipper = new ValueSkipper(reader);

        // Act
        skipper.skipValue(type);

        // Assert
        // String 格式: int32(length) + UTF-8 bytes + 0x00
        int expectedLength = 4 + testString.getBytes().length + 1;
        assertEquals(expectedLength, reader.position() - positionBefore);
    }

    @Test
    public void testSkipBinary() {
        // Arrange - Binary 类型
        byte[] binaryData = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05};
        byte[] data = createBsonWithBinary(binaryData);
        BsonReader reader = new BsonReader(data);
        reader.skip(4); // 跳过文档长度
        byte type = reader.readByte();
        reader.readCString(); // 跳过字段名

        int positionBefore = reader.position();
        ValueSkipper skipper = new ValueSkipper(reader);

        // Act
        skipper.skipValue(type);

        // Assert
        // Binary 格式: int32(length) + subtype(1) + bytes
        int expectedLength = 4 + 1 + binaryData.length;
        assertEquals(expectedLength, reader.position() - positionBefore);
    }

    @Test
    public void testSkipDocument() {
        // Arrange - 嵌套 Document
        BsonDocument nestedDoc = new BsonDocument()
            .append("field1", new BsonString("value1"))
            .append("field2", new BsonInt32(42));
        byte[] data = createBsonWithDocument(nestedDoc);

        BsonReader reader = new BsonReader(data);
        reader.skip(4); // 跳过文档长度
        byte type = reader.readByte();
        reader.readCString(); // 跳过字段名

        int positionBefore = reader.position();
        ValueSkipper skipper = new ValueSkipper(reader);

        // Act
        skipper.skipValue(type);

        // Assert
        // 验证跳过后的位置是否正确（应该跳到文档结尾）
        byte[] nestedDocBytes = serializeDocument(nestedDoc);
        assertEquals(nestedDocBytes.length, reader.position() - positionBefore);
    }

    @Test
    public void testSkipArray() {
        // Arrange - Array 类型
        BsonArray array = new BsonArray();
        array.add(new BsonInt32(1));
        array.add(new BsonInt32(2));
        array.add(new BsonInt32(3));
        byte[] data = createBsonWithArray(array);

        BsonReader reader = new BsonReader(data);
        reader.skip(4); // 跳过文档长度
        byte type = reader.readByte();
        reader.readCString(); // 跳过字段名

        int positionBefore = reader.position();
        ValueSkipper skipper = new ValueSkipper(reader);

        // Act
        skipper.skipValue(type);

        // Assert
        // Array 格式与 Document 相同
        byte[] arrayBytes = serializeArray(array);
        assertEquals(arrayBytes.length, reader.position() - positionBefore);
    }

    @Test
    public void testSkipJavaScript() {
        // Arrange - JavaScript 类型
        String jsCode = "function() { return 42; }";
        byte[] data = createBsonWithJavaScript(jsCode);
        BsonReader reader = new BsonReader(data);
        reader.skip(4); // 跳过文档长度
        byte type = reader.readByte();
        reader.readCString(); // 跳过字段名

        int positionBefore = reader.position();
        ValueSkipper skipper = new ValueSkipper(reader);

        // Act
        skipper.skipValue(type);

        // Assert
        // JavaScript 格式与 String 相同
        int expectedLength = 4 + jsCode.getBytes().length + 1;
        assertEquals(expectedLength, reader.position() - positionBefore);
    }

    @Test
    public void testSkipJavaScriptWithScope() {
        // Arrange - JavaScriptWithScope 类型
        String jsCode = "function() { return x; }";
        BsonDocument scope = new BsonDocument().append("x", new BsonInt32(42));
        byte[] data = createBsonWithJavaScriptWithScope(jsCode, scope);

        BsonReader reader = new BsonReader(data);
        reader.skip(4); // 跳过文档长度
        byte type = reader.readByte();
        reader.readCString(); // 跳过字段名

        int positionBefore = reader.position();
        ValueSkipper skipper = new ValueSkipper(reader);

        // Act
        skipper.skipValue(type);

        // Assert
        // JavaScriptWithScope 格式: int32(total) + string(code) + document(scope)
        // 通过读取 total length 验证
        assertTrue(reader.position() > positionBefore);
    }

    // ==================== 特殊类型测试 ====================

    @Test
    public void testSkipRegex() {
        // Arrange - Regex 类型
        String pattern = "^[a-z]+$";
        String options = "i";
        byte[] data = createBsonWithRegex(pattern, options);

        BsonReader reader = new BsonReader(data);
        reader.skip(4); // 跳过文档长度
        byte type = reader.readByte();
        reader.readCString(); // 跳过字段名

        int positionBefore = reader.position();
        ValueSkipper skipper = new ValueSkipper(reader);

        // Act
        skipper.skipValue(type);

        // Assert
        // Regex 格式: pattern (C-string) + options (C-string)
        int expectedLength = pattern.length() + 1 + options.length() + 1;
        assertEquals(expectedLength, reader.position() - positionBefore);
    }

    @Test
    public void testSkipDBPointer() {
        // Arrange - DBPointer 类型
        String namespace = "db.collection";
        ObjectId oid = new ObjectId();
        byte[] data = createBsonWithDBPointer(namespace, oid);

        BsonReader reader = new BsonReader(data);
        reader.skip(4); // 跳过文档长度
        byte type = reader.readByte();
        reader.readCString(); // 跳过字段名

        int positionBefore = reader.position();
        ValueSkipper skipper = new ValueSkipper(reader);

        // Act
        skipper.skipValue(type);

        // Assert
        // DBPointer 格式: string + 12 bytes ObjectId
        int expectedLength = 4 + namespace.getBytes().length + 1 + 12;
        assertEquals(expectedLength, reader.position() - positionBefore);
    }

    @Test
    public void testSkipSymbol() {
        // Arrange - Symbol 类型（与 String 格式相同）
        String symbol = "mySymbol";
        byte[] data = createBsonWithSymbol(symbol);

        BsonReader reader = new BsonReader(data);
        reader.skip(4); // 跳过文档长度
        byte type = reader.readByte();
        reader.readCString(); // 跳过字段名

        int positionBefore = reader.position();
        ValueSkipper skipper = new ValueSkipper(reader);

        // Act
        skipper.skipValue(type);

        // Assert
        int expectedLength = 4 + symbol.getBytes().length + 1;
        assertEquals(expectedLength, reader.position() - positionBefore);
    }

    // ==================== 异常情况测试 ====================

    @Test
    public void testSkipInvalidType() {
        // Arrange
        byte[] data = new byte[]{0x00, 0x00, 0x00, 0x00};
        BsonReader reader = new BsonReader(data);
        ValueSkipper skipper = new ValueSkipper(reader);

        // Act & Assert
        assertThrows(InvalidBsonTypeException.class, () -> {
            skipper.skipValue((byte) 0x99); // 无效的类型码
        });
    }

    // ==================== 固定长度查找表测试 ====================

    @Test
    public void testGetFixedLength_Double() {
        assertEquals(8, ValueSkipper.getFixedLength(BsonType.DOUBLE));
    }

    @Test
    public void testGetFixedLength_Int32() {
        assertEquals(4, ValueSkipper.getFixedLength(BsonType.INT32));
    }

    @Test
    public void testGetFixedLength_Int64() {
        assertEquals(8, ValueSkipper.getFixedLength(BsonType.INT64));
    }

    @Test
    public void testGetFixedLength_Boolean() {
        assertEquals(1, ValueSkipper.getFixedLength(BsonType.BOOLEAN));
    }

    @Test
    public void testGetFixedLength_ObjectId() {
        assertEquals(12, ValueSkipper.getFixedLength(BsonType.OBJECT_ID));
    }

    @Test
    public void testGetFixedLength_DateTime() {
        assertEquals(8, ValueSkipper.getFixedLength(BsonType.DATE_TIME));
    }

    @Test
    public void testGetFixedLength_Timestamp() {
        assertEquals(8, ValueSkipper.getFixedLength(BsonType.TIMESTAMP));
    }

    @Test
    public void testGetFixedLength_Decimal128() {
        assertEquals(16, ValueSkipper.getFixedLength(BsonType.DECIMAL128));
    }

    @Test
    public void testGetFixedLength_Null() {
        assertEquals(0, ValueSkipper.getFixedLength(BsonType.NULL));
    }

    @Test
    public void testGetFixedLength_MinKey() {
        assertEquals(0, ValueSkipper.getFixedLength(BsonType.MIN_KEY));
    }

    @Test
    public void testGetFixedLength_MaxKey() {
        assertEquals(0, ValueSkipper.getFixedLength(BsonType.MAX_KEY));
    }

    @Test
    public void testGetFixedLength_String() {
        // String 是变长类型
        assertEquals(-1, ValueSkipper.getFixedLength(BsonType.STRING));
    }

    @Test
    public void testGetFixedLength_Document() {
        // Document 是变长类型
        assertEquals(-1, ValueSkipper.getFixedLength(BsonType.DOCUMENT));
    }

    // ==================== 辅助方法 ====================

    private byte[] createBsonWithDouble(double value) {
        BsonDocument doc = new BsonDocument().append("field", new BsonDouble(value));
        return serializeDocument(doc);
    }

    private byte[] createBsonWithInt32(int value) {
        BsonDocument doc = new BsonDocument().append("field", new BsonInt32(value));
        return serializeDocument(doc);
    }

    private byte[] createBsonWithInt64(long value) {
        BsonDocument doc = new BsonDocument().append("field", new BsonInt64(value));
        return serializeDocument(doc);
    }

    private byte[] createBsonWithBoolean(boolean value) {
        BsonDocument doc = new BsonDocument().append("field", new BsonBoolean(value));
        return serializeDocument(doc);
    }

    private byte[] createBsonWithDateTime(Date value) {
        BsonDocument doc = new BsonDocument().append("field", new BsonDateTime(value.getTime()));
        return serializeDocument(doc);
    }

    private byte[] createBsonWithObjectId(ObjectId value) {
        BsonDocument doc = new BsonDocument().append("field", new BsonObjectId(value));
        return serializeDocument(doc);
    }

    private byte[] createBsonWithTimestamp(BsonTimestamp value) {
        BsonDocument doc = new BsonDocument().append("field", value);
        return serializeDocument(doc);
    }

    private byte[] createBsonWithDecimal128(Decimal128 value) {
        BsonDocument doc = new BsonDocument().append("field", new BsonDecimal128(value));
        return serializeDocument(doc);
    }

    private byte[] createBsonWithNull() {
        BsonDocument doc = new BsonDocument().append("field", new BsonNull());
        return serializeDocument(doc);
    }

    private byte[] createBsonWithMinKey() {
        BsonDocument doc = new BsonDocument().append("field", new BsonMinKey());
        return serializeDocument(doc);
    }

    private byte[] createBsonWithMaxKey() {
        BsonDocument doc = new BsonDocument().append("field", new BsonMaxKey());
        return serializeDocument(doc);
    }

    private byte[] createBsonWithString(String value) {
        BsonDocument doc = new BsonDocument().append("field", new BsonString(value));
        return serializeDocument(doc);
    }

    private byte[] createBsonWithBinary(byte[] value) {
        BsonDocument doc = new BsonDocument().append("field",
            new BsonBinary(BsonBinarySubType.BINARY, value));
        return serializeDocument(doc);
    }

    private byte[] createBsonWithDocument(BsonDocument value) {
        BsonDocument doc = new BsonDocument().append("field", value);
        return serializeDocument(doc);
    }

    private byte[] createBsonWithArray(BsonArray value) {
        BsonDocument doc = new BsonDocument().append("field", value);
        return serializeDocument(doc);
    }

    private byte[] createBsonWithJavaScript(String code) {
        BsonDocument doc = new BsonDocument().append("field", new BsonJavaScript(code));
        return serializeDocument(doc);
    }

    private byte[] createBsonWithJavaScriptWithScope(String code, BsonDocument scope) {
        BsonDocument doc = new BsonDocument().append("field",
            new BsonJavaScriptWithScope(code, scope));
        return serializeDocument(doc);
    }

    private byte[] createBsonWithRegex(String pattern, String options) {
        BsonDocument doc = new BsonDocument().append("field",
            new BsonRegularExpression(pattern, options));
        return serializeDocument(doc);
    }

    private byte[] createBsonWithDBPointer(String namespace, ObjectId oid) {
        BsonDocument doc = new BsonDocument().append("field",
            new BsonDbPointer(namespace, oid));
        return serializeDocument(doc);
    }

    private byte[] createBsonWithSymbol(String symbol) {
        BsonDocument doc = new BsonDocument().append("field", new BsonSymbol(symbol));
        return serializeDocument(doc);
    }

    private byte[] serializeDocument(BsonDocument doc) {
        BasicOutputBuffer buffer = new BasicOutputBuffer();
        BsonBinaryWriter writer = new BsonBinaryWriter(buffer);
        new org.bson.codecs.BsonDocumentCodec().encode(writer,
            doc, org.bson.codecs.EncoderContext.builder().build());
        return buffer.toByteArray();
    }

    private byte[] serializeArray(BsonArray array) {
        // Array 在 BSON 中实际是一个特殊的 Document
        BsonDocument arrayDoc = new BsonDocument();
        for (int i = 0; i < array.size(); i++) {
            arrayDoc.append(String.valueOf(i), array.get(i));
        }
        return serializeDocument(arrayDoc);
    }
}
