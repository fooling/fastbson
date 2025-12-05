package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.document.BsonArrayBuilder;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.document.BsonDocumentFactory;
import com.cloud.fastbson.document.fast.FastBsonDocumentFactory;
import com.cloud.fastbson.handler.TypeHandler;
import com.cloud.fastbson.reader.BsonReader;
import com.cloud.fastbson.util.BsonType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for ArrayParser covering all BSON type branches.
 */
class ArrayParserTest {

    private ArrayParser parser;
    private BsonDocumentFactory factory;

    @BeforeAll
    static void initTypeHandler() {
        // Trigger TypeHandler static initialization which sets up handlers
        TypeHandler.getDocumentFactory();
    }

    @BeforeEach
    void setUp() {
        parser = ArrayParser.INSTANCE;
        factory = FastBsonDocumentFactory.INSTANCE;
        parser.setFactory(factory);
        // DocumentParser also needs factory for nested document parsing
        DocumentParser.INSTANCE.setFactory(factory);
    }

    /**
     * Helper method to create BSON array bytes with specific types.
     */
    private byte[] createBsonArray(ArrayBuilder builder) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] elements = builder.build();

        // Calculate total length (4 bytes for length + elements + 1 byte terminator)
        int totalLength = 4 + elements.length + 1;

        // Write length as little-endian int32
        baos.write(totalLength & 0xFF);
        baos.write((totalLength >> 8) & 0xFF);
        baos.write((totalLength >> 16) & 0xFF);
        baos.write((totalLength >> 24) & 0xFF);

        // Write elements
        baos.write(elements);

        // Write terminator
        baos.write(0x00);

        return baos.toByteArray();
    }

    /**
     * Helper class to build array elements.
     */
    private static class ArrayBuilder {
        private ByteArrayOutputStream baos = new ByteArrayOutputStream();
        private int index = 0;

        ArrayBuilder addInt32(int value) throws IOException {
            baos.write(BsonType.INT32);
            baos.write(String.valueOf(index++).getBytes());
            baos.write(0x00); // C-string terminator

            // Write int32 as little-endian
            baos.write(value & 0xFF);
            baos.write((value >> 8) & 0xFF);
            baos.write((value >> 16) & 0xFF);
            baos.write((value >> 24) & 0xFF);
            return this;
        }

        ArrayBuilder addInt64(long value) throws IOException {
            baos.write(BsonType.INT64);
            baos.write(String.valueOf(index++).getBytes());
            baos.write(0x00);

            // Write int64 as little-endian
            for (int i = 0; i < 8; i++) {
                baos.write((int) ((value >> (i * 8)) & 0xFF));
            }
            return this;
        }

        ArrayBuilder addDouble(double value) throws IOException {
            baos.write(BsonType.DOUBLE);
            baos.write(String.valueOf(index++).getBytes());
            baos.write(0x00);

            long bits = Double.doubleToLongBits(value);
            for (int i = 0; i < 8; i++) {
                baos.write((int) ((bits >> (i * 8)) & 0xFF));
            }
            return this;
        }

        ArrayBuilder addBoolean(boolean value) throws IOException {
            baos.write(BsonType.BOOLEAN);
            baos.write(String.valueOf(index++).getBytes());
            baos.write(0x00);
            baos.write(value ? 0x01 : 0x00);
            return this;
        }

        ArrayBuilder addString(String value) throws IOException {
            baos.write(BsonType.STRING);
            baos.write(String.valueOf(index++).getBytes());
            baos.write(0x00);

            byte[] strBytes = value.getBytes("UTF-8");
            int strLength = strBytes.length + 1; // +1 for null terminator

            // Write string length
            baos.write(strLength & 0xFF);
            baos.write((strLength >> 8) & 0xFF);
            baos.write((strLength >> 16) & 0xFF);
            baos.write((strLength >> 24) & 0xFF);

            // Write string content
            baos.write(strBytes);
            baos.write(0x00); // String null terminator
            return this;
        }

        ArrayBuilder addObjectId(byte[] objectIdBytes) throws IOException {
            if (objectIdBytes.length != 12) {
                throw new IllegalArgumentException("ObjectId must be 12 bytes");
            }
            baos.write(BsonType.OBJECT_ID);
            baos.write(String.valueOf(index++).getBytes());
            baos.write(0x00);
            baos.write(objectIdBytes);
            return this;
        }

        ArrayBuilder addDateTime(long timestamp) throws IOException {
            baos.write(BsonType.DATE_TIME);
            baos.write(String.valueOf(index++).getBytes());
            baos.write(0x00);

            // Write int64 timestamp as little-endian
            for (int i = 0; i < 8; i++) {
                baos.write((int) ((timestamp >> (i * 8)) & 0xFF));
            }
            return this;
        }

        ArrayBuilder addNull() throws IOException {
            baos.write(BsonType.NULL);
            baos.write(String.valueOf(index++).getBytes());
            baos.write(0x00);
            // NULL type has no value bytes
            return this;
        }

        ArrayBuilder addBinary(byte subtype, byte[] data) throws IOException {
            baos.write(BsonType.BINARY);
            baos.write(String.valueOf(index++).getBytes());
            baos.write(0x00);

            // Write binary length
            int length = data.length;
            baos.write(length & 0xFF);
            baos.write((length >> 8) & 0xFF);
            baos.write((length >> 16) & 0xFF);
            baos.write((length >> 24) & 0xFF);

            // Write subtype
            baos.write(subtype);

            // Write data
            baos.write(data);
            return this;
        }

        ArrayBuilder addDocument(byte[] docBytes) throws IOException {
            baos.write(BsonType.DOCUMENT);
            baos.write(String.valueOf(index++).getBytes());
            baos.write(0x00);
            baos.write(docBytes);
            return this;
        }

        ArrayBuilder addArray(byte[] arrayBytes) throws IOException {
            baos.write(BsonType.ARRAY);
            baos.write(String.valueOf(index++).getBytes());
            baos.write(0x00);
            baos.write(arrayBytes);
            return this;
        }

        ArrayBuilder addRegex(String pattern, String options) throws IOException {
            baos.write(BsonType.REGEX);
            baos.write(String.valueOf(index++).getBytes());
            baos.write(0x00);
            // Regex is stored as two C-strings: pattern and options
            baos.write(pattern.getBytes("UTF-8"));
            baos.write(0x00);
            baos.write(options.getBytes("UTF-8"));
            baos.write(0x00);
            return this;
        }

        byte[] build() {
            return baos.toByteArray();
        }
    }

    @Test
    void testParseArrayWithInt32() throws IOException {
        byte[] bsonArray = createBsonArray(
            new ArrayBuilder()
                .addInt32(42)
                .addInt32(100)
        );

        BsonReader reader = new BsonReader(bsonArray);
        BsonArray result = (BsonArray) parser.parse(reader);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(42, result.getInt32(0));
        assertEquals(100, result.getInt32(1));
    }

    @Test
    void testParseArrayWithInt64() throws IOException {
        byte[] bsonArray = createBsonArray(
            new ArrayBuilder()
                .addInt64(123456789012345L)
        );

        BsonReader reader = new BsonReader(bsonArray);
        BsonArray result = (BsonArray) parser.parse(reader);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(123456789012345L, result.getInt64(0));
    }

    @Test
    void testParseArrayWithDouble() throws IOException {
        byte[] bsonArray = createBsonArray(
            new ArrayBuilder()
                .addDouble(3.14159)
        );

        BsonReader reader = new BsonReader(bsonArray);
        BsonArray result = (BsonArray) parser.parse(reader);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(3.14159, result.getDouble(0), 0.00001);
    }

    @Test
    void testParseArrayWithBoolean() throws IOException {
        byte[] bsonArray = createBsonArray(
            new ArrayBuilder()
                .addBoolean(true)
                .addBoolean(false)
        );

        BsonReader reader = new BsonReader(bsonArray);
        BsonArray result = (BsonArray) parser.parse(reader);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.getBoolean(0));
        assertFalse(result.getBoolean(1));
    }

    @Test
    void testParseArrayWithString() throws IOException {
        byte[] bsonArray = createBsonArray(
            new ArrayBuilder()
                .addString("hello")
                .addString("world")
        );

        BsonReader reader = new BsonReader(bsonArray);
        BsonArray result = (BsonArray) parser.parse(reader);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("hello", result.getString(0));
        assertEquals("world", result.getString(1));
    }

    @Test
    void testParseArrayWithObjectId() throws IOException {
        byte[] objectId = new byte[12];
        for (int i = 0; i < 12; i++) {
            objectId[i] = (byte) i;
        }

        byte[] bsonArray = createBsonArray(
            new ArrayBuilder()
                .addObjectId(objectId)
        );

        BsonReader reader = new BsonReader(bsonArray);
        BsonArray result = (BsonArray) parser.parse(reader);

        assertNotNull(result);
        assertEquals(1, result.size());
        String expectedHex = "000102030405060708090a0b";
        assertEquals(expectedHex, result.get(0));
    }

    @Test
    void testParseArrayWithDateTime() throws IOException {
        long timestamp = 1638360000000L;

        byte[] bsonArray = createBsonArray(
            new ArrayBuilder()
                .addDateTime(timestamp)
        );

        BsonReader reader = new BsonReader(bsonArray);
        BsonArray result = (BsonArray) parser.parse(reader);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(timestamp, result.get(0));
    }

    @Test
    void testParseArrayWithNull() throws IOException {
        byte[] bsonArray = createBsonArray(
            new ArrayBuilder()
                .addNull()
        );

        BsonReader reader = new BsonReader(bsonArray);
        BsonArray result = (BsonArray) parser.parse(reader);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0));
    }

    @Test
    void testParseArrayWithBinary() throws IOException {
        byte[] binaryData = new byte[] { 0x01, 0x02, 0x03, 0x04 };

        byte[] bsonArray = createBsonArray(
            new ArrayBuilder()
                .addBinary((byte) 0x00, binaryData)
        );

        BsonReader reader = new BsonReader(bsonArray);
        BsonArray result = (BsonArray) parser.parse(reader);

        assertNotNull(result);
        assertEquals(1, result.size());
        Object binary = result.get(0);
        assertNotNull(binary);
        // Binary is stored as BinaryData object or byte array
        assertTrue(binary.toString().contains("BinaryData") || binary instanceof byte[]);
    }

    @Test
    void testParseArrayWithNestedDocument() throws IOException {
        // Create nested document: {"key": "value"}
        ByteArrayOutputStream docBaos = new ByteArrayOutputStream();
        byte[] keyBytes = "key".getBytes("UTF-8");
        byte[] valueBytes = "value".getBytes("UTF-8");
        int valueLength = valueBytes.length + 1;

        // Element: type + field name + value
        ByteArrayOutputStream elemBaos = new ByteArrayOutputStream();
        elemBaos.write(BsonType.STRING);
        elemBaos.write(keyBytes);
        elemBaos.write(0x00);
        elemBaos.write(valueLength & 0xFF);
        elemBaos.write((valueLength >> 8) & 0xFF);
        elemBaos.write((valueLength >> 16) & 0xFF);
        elemBaos.write((valueLength >> 24) & 0xFF);
        elemBaos.write(valueBytes);
        elemBaos.write(0x00);

        byte[] elements = elemBaos.toByteArray();
        int docLength = 4 + elements.length + 1;

        docBaos.write(docLength & 0xFF);
        docBaos.write((docLength >> 8) & 0xFF);
        docBaos.write((docLength >> 16) & 0xFF);
        docBaos.write((docLength >> 24) & 0xFF);
        docBaos.write(elements);
        docBaos.write(0x00);

        byte[] docBytes = docBaos.toByteArray();

        byte[] bsonArray = createBsonArray(
            new ArrayBuilder()
                .addDocument(docBytes)
        );

        BsonReader reader = new BsonReader(bsonArray);
        BsonArray result = (BsonArray) parser.parse(reader);

        assertNotNull(result);
        assertEquals(1, result.size());
        BsonDocument nestedDoc = result.getDocument(0);
        assertNotNull(nestedDoc);
        assertEquals("value", nestedDoc.getString("key"));
    }

    @Test
    void testParseArrayWithNestedArray() throws IOException {
        // Create nested array with int32 values
        byte[] nestedArrayBytes = createBsonArray(
            new ArrayBuilder()
                .addInt32(1)
                .addInt32(2)
        );

        byte[] bsonArray = createBsonArray(
            new ArrayBuilder()
                .addArray(nestedArrayBytes)
        );

        BsonReader reader = new BsonReader(bsonArray);
        BsonArray result = (BsonArray) parser.parse(reader);

        assertNotNull(result);
        assertEquals(1, result.size());
        BsonArray nestedArray = result.getArray(0);
        assertNotNull(nestedArray);
        assertEquals(2, nestedArray.size());
        assertEquals(1, nestedArray.getInt32(0));
        assertEquals(2, nestedArray.getInt32(1));
    }

    @Test
    void testGetValueSize() {
        // Create a simple array with known length
        byte[] arrayBytes = new byte[] {
            0x10, 0x00, 0x00, 0x00,  // length = 16 bytes
            0x10, 0x30, 0x00,        // int32 field "0"
            0x2A, 0x00, 0x00, 0x00,  // value = 42
            0x00                     // terminator
        };

        int size = parser.getValueSize(arrayBytes, 0);
        assertEquals(16, size);
    }

    @Test
    void testParseArrayWithMixedTypes() throws IOException {
        byte[] bsonArray = createBsonArray(
            new ArrayBuilder()
                .addInt32(42)
                .addString("test")
                .addBoolean(true)
                .addDouble(3.14)
                .addNull()
        );

        BsonReader reader = new BsonReader(bsonArray);
        BsonArray result = (BsonArray) parser.parse(reader);

        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals(42, result.getInt32(0));
        assertEquals("test", result.getString(1));
        assertTrue(result.getBoolean(2));
        assertEquals(3.14, result.getDouble(3), 0.001);
        assertNull(result.get(4));
    }

    @Test
    void testParseArrayWithRegex() throws IOException {
        // Test the default case in switch statement (complex types handled by TypeHandler)
        byte[] bsonArray = createBsonArray(
            new ArrayBuilder()
                .addRegex("^test.*", "i")
        );

        BsonReader reader = new BsonReader(bsonArray);
        BsonArray result = (BsonArray) parser.parse(reader);

        // The default case currently doesn't add the value to the array
        // This is documented as "暂时跳过不支持的类型" (temporarily skip unsupported types)
        assertNotNull(result);
        // Since the default case doesn't add to builder, size should be 0
        assertEquals(0, result.size());
    }
}
