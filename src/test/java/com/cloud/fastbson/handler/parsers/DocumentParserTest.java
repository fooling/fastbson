package com.cloud.fastbson.handler.parsers;

import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.document.BsonDocumentFactory;
import com.cloud.fastbson.document.hashmap.HashMapBsonDocumentFactory;
import com.cloud.fastbson.handler.TypeHandler;
import com.cloud.fastbson.reader.BsonReader;
import com.cloud.fastbson.util.BsonType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for DocumentParser covering HashMap optimization path.
 * This test covers parseDirectHashMap and parseValueDirect methods.
 */
class DocumentParserTest {

    private DocumentParser parser;
    private BsonDocumentFactory factory;

    @BeforeAll
    static void initTypeHandler() {
        // Trigger TypeHandler static initialization
        TypeHandler.getDocumentFactory();
    }

    @BeforeEach
    void setUp() {
        parser = DocumentParser.INSTANCE;
        // Use HashMapBsonDocumentFactory to trigger parseDirectHashMap path
        factory = HashMapBsonDocumentFactory.INSTANCE;
        parser.setFactory(factory);
        ArrayParser.INSTANCE.setFactory(factory);
    }

    /**
     * Helper class to build BSON document bytes.
     */
    private static class DocumentBuilder {
        private ByteArrayOutputStream baos = new ByteArrayOutputStream();

        DocumentBuilder putInt32(String fieldName, int value) throws IOException {
            baos.write(BsonType.INT32);
            baos.write(fieldName.getBytes("UTF-8"));
            baos.write(0x00);

            baos.write(value & 0xFF);
            baos.write((value >> 8) & 0xFF);
            baos.write((value >> 16) & 0xFF);
            baos.write((value >> 24) & 0xFF);
            return this;
        }

        DocumentBuilder putInt64(String fieldName, long value) throws IOException {
            baos.write(BsonType.INT64);
            baos.write(fieldName.getBytes("UTF-8"));
            baos.write(0x00);

            for (int i = 0; i < 8; i++) {
                baos.write((int) ((value >> (i * 8)) & 0xFF));
            }
            return this;
        }

        DocumentBuilder putDouble(String fieldName, double value) throws IOException {
            baos.write(BsonType.DOUBLE);
            baos.write(fieldName.getBytes("UTF-8"));
            baos.write(0x00);

            long bits = Double.doubleToLongBits(value);
            for (int i = 0; i < 8; i++) {
                baos.write((int) ((bits >> (i * 8)) & 0xFF));
            }
            return this;
        }

        DocumentBuilder putString(String fieldName, String value) throws IOException {
            baos.write(BsonType.STRING);
            baos.write(fieldName.getBytes("UTF-8"));
            baos.write(0x00);

            byte[] strBytes = value.getBytes("UTF-8");
            int strLength = strBytes.length + 1;

            baos.write(strLength & 0xFF);
            baos.write((strLength >> 8) & 0xFF);
            baos.write((strLength >> 16) & 0xFF);
            baos.write((strLength >> 24) & 0xFF);

            baos.write(strBytes);
            baos.write(0x00);
            return this;
        }

        DocumentBuilder putBoolean(String fieldName, boolean value) throws IOException {
            baos.write(BsonType.BOOLEAN);
            baos.write(fieldName.getBytes("UTF-8"));
            baos.write(0x00);
            baos.write(value ? 0x01 : 0x00);
            return this;
        }

        DocumentBuilder putNull(String fieldName) throws IOException {
            baos.write(BsonType.NULL);
            baos.write(fieldName.getBytes("UTF-8"));
            baos.write(0x00);
            return this;
        }

        DocumentBuilder putObjectId(String fieldName, byte[] objectIdBytes) throws IOException {
            if (objectIdBytes.length != 12) {
                throw new IllegalArgumentException("ObjectId must be 12 bytes");
            }
            baos.write(BsonType.OBJECT_ID);
            baos.write(fieldName.getBytes("UTF-8"));
            baos.write(0x00);
            baos.write(objectIdBytes);
            return this;
        }

        DocumentBuilder putDateTime(String fieldName, long timestamp) throws IOException {
            baos.write(BsonType.DATE_TIME);
            baos.write(fieldName.getBytes("UTF-8"));
            baos.write(0x00);

            for (int i = 0; i < 8; i++) {
                baos.write((int) ((timestamp >> (i * 8)) & 0xFF));
            }
            return this;
        }

        DocumentBuilder putBinary(String fieldName, byte subtype, byte[] data) throws IOException {
            baos.write(BsonType.BINARY);
            baos.write(fieldName.getBytes("UTF-8"));
            baos.write(0x00);

            int length = data.length;
            baos.write(length & 0xFF);
            baos.write((length >> 8) & 0xFF);
            baos.write((length >> 16) & 0xFF);
            baos.write((length >> 24) & 0xFF);

            baos.write(subtype);
            baos.write(data);
            return this;
        }

        DocumentBuilder putRegex(String fieldName, String pattern, String options) throws IOException {
            baos.write(BsonType.REGEX);
            baos.write(fieldName.getBytes("UTF-8"));
            baos.write(0x00);
            baos.write(pattern.getBytes("UTF-8"));
            baos.write(0x00);
            baos.write(options.getBytes("UTF-8"));
            baos.write(0x00);
            return this;
        }

        DocumentBuilder putTimestamp(String fieldName, long value) throws IOException {
            baos.write(BsonType.TIMESTAMP);
            baos.write(fieldName.getBytes("UTF-8"));
            baos.write(0x00);

            for (int i = 0; i < 8; i++) {
                baos.write((int) ((value >> (i * 8)) & 0xFF));
            }
            return this;
        }

        DocumentBuilder putMinKey(String fieldName) throws IOException {
            baos.write(BsonType.MIN_KEY);
            baos.write(fieldName.getBytes("UTF-8"));
            baos.write(0x00);
            return this;
        }

        DocumentBuilder putMaxKey(String fieldName) throws IOException {
            baos.write(BsonType.MAX_KEY);
            baos.write(fieldName.getBytes("UTF-8"));
            baos.write(0x00);
            return this;
        }

        DocumentBuilder putDecimal128(String fieldName, byte[] decimal128Bytes) throws IOException {
            if (decimal128Bytes.length != 16) {
                throw new IllegalArgumentException("Decimal128 must be 16 bytes");
            }
            baos.write(BsonType.DECIMAL128);
            baos.write(fieldName.getBytes("UTF-8"));
            baos.write(0x00);
            baos.write(decimal128Bytes);
            return this;
        }

        DocumentBuilder putDocument(String fieldName, byte[] docBytes) throws IOException {
            baos.write(BsonType.DOCUMENT);
            baos.write(fieldName.getBytes("UTF-8"));
            baos.write(0x00);
            baos.write(docBytes);
            return this;
        }

        DocumentBuilder putArray(String fieldName, byte[] arrayBytes) throws IOException {
            baos.write(BsonType.ARRAY);
            baos.write(fieldName.getBytes("UTF-8"));
            baos.write(0x00);
            baos.write(arrayBytes);
            return this;
        }

        DocumentBuilder putDBPointer(String fieldName, String namespace, byte[] objectId) throws IOException {
            if (objectId.length != 12) {
                throw new IllegalArgumentException("ObjectId must be 12 bytes");
            }
            baos.write(BsonType.DB_POINTER);
            baos.write(fieldName.getBytes("UTF-8"));
            baos.write(0x00);

            // Write namespace as string
            byte[] nsBytes = namespace.getBytes("UTF-8");
            int nsLength = nsBytes.length + 1;
            baos.write(nsLength & 0xFF);
            baos.write((nsLength >> 8) & 0xFF);
            baos.write((nsLength >> 16) & 0xFF);
            baos.write((nsLength >> 24) & 0xFF);
            baos.write(nsBytes);
            baos.write(0x00);

            // Write objectId
            baos.write(objectId);
            return this;
        }

        DocumentBuilder putJavaScriptWithScope(String fieldName, String code, byte[] scopeDoc) throws IOException {
            baos.write(BsonType.JAVASCRIPT_WITH_SCOPE);
            baos.write(fieldName.getBytes("UTF-8"));
            baos.write(0x00);

            // Calculate total length (int32 + string + document)
            byte[] codeBytes = code.getBytes("UTF-8");
            int codeLength = codeBytes.length + 1;
            int stringSize = 4 + codeLength;
            int totalLength = 4 + stringSize + scopeDoc.length;

            // Write total length
            baos.write(totalLength & 0xFF);
            baos.write((totalLength >> 8) & 0xFF);
            baos.write((totalLength >> 16) & 0xFF);
            baos.write((totalLength >> 24) & 0xFF);

            // Write code string
            baos.write(codeLength & 0xFF);
            baos.write((codeLength >> 8) & 0xFF);
            baos.write((codeLength >> 16) & 0xFF);
            baos.write((codeLength >> 24) & 0xFF);
            baos.write(codeBytes);
            baos.write(0x00);

            // Write scope document
            baos.write(scopeDoc);
            return this;
        }

        DocumentBuilder putInvalidType(String fieldName) throws IOException {
            baos.write((byte) 0x99);  // Invalid BSON type
            baos.write(fieldName.getBytes("UTF-8"));
            baos.write(0x00);
            return this;
        }

        byte[] build() throws IOException {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] elements = baos.toByteArray();

            int totalLength = 4 + elements.length + 1;

            result.write(totalLength & 0xFF);
            result.write((totalLength >> 8) & 0xFF);
            result.write((totalLength >> 16) & 0xFF);
            result.write((totalLength >> 24) & 0xFF);

            result.write(elements);
            result.write(0x00);

            return result.toByteArray();
        }
    }

    @Test
    void testParseWithHashMapFactory_Int32() throws IOException {
        byte[] bsonDoc = new DocumentBuilder()
            .putInt32("age", 30)
            .build();

        BsonReader reader = new BsonReader(bsonDoc);
        BsonDocument result = (BsonDocument) parser.parse(reader);

        assertNotNull(result);
        assertEquals(30, result.getInt32("age"));
    }

    @Test
    void testParseWithHashMapFactory_Int64() throws IOException {
        byte[] bsonDoc = new DocumentBuilder()
            .putInt64("bignum", 9876543210L)
            .build();

        BsonReader reader = new BsonReader(bsonDoc);
        BsonDocument result = (BsonDocument) parser.parse(reader);

        assertNotNull(result);
        assertEquals(9876543210L, result.getInt64("bignum"));
    }

    @Test
    void testParseWithHashMapFactory_Double() throws IOException {
        byte[] bsonDoc = new DocumentBuilder()
            .putDouble("pi", 3.14159)
            .build();

        BsonReader reader = new BsonReader(bsonDoc);
        BsonDocument result = (BsonDocument) parser.parse(reader);

        assertNotNull(result);
        assertEquals(3.14159, result.getDouble("pi"), 0.00001);
    }

    @Test
    void testParseWithHashMapFactory_String() throws IOException {
        byte[] bsonDoc = new DocumentBuilder()
            .putString("name", "John")
            .build();

        BsonReader reader = new BsonReader(bsonDoc);
        BsonDocument result = (BsonDocument) parser.parse(reader);

        assertNotNull(result);
        assertEquals("John", result.getString("name"));
    }

    @Test
    void testParseWithHashMapFactory_Boolean() throws IOException {
        byte[] bsonDoc = new DocumentBuilder()
            .putBoolean("active", true)
            .build();

        BsonReader reader = new BsonReader(bsonDoc);
        BsonDocument result = (BsonDocument) parser.parse(reader);

        assertNotNull(result);
        assertTrue(result.getBoolean("active"));
    }

    @Test
    void testParseWithHashMapFactory_Null() throws IOException {
        byte[] bsonDoc = new DocumentBuilder()
            .putNull("empty")
            .build();

        BsonReader reader = new BsonReader(bsonDoc);
        BsonDocument result = (BsonDocument) parser.parse(reader);

        assertNotNull(result);
        assertTrue(result.isNull("empty"));
    }

    @Test
    void testParseWithHashMapFactory_ObjectId() throws IOException {
        byte[] objectId = new byte[12];
        for (int i = 0; i < 12; i++) {
            objectId[i] = (byte) i;
        }

        byte[] bsonDoc = new DocumentBuilder()
            .putObjectId("_id", objectId)
            .build();

        BsonReader reader = new BsonReader(bsonDoc);
        BsonDocument result = (BsonDocument) parser.parse(reader);

        assertNotNull(result);
        assertEquals("000102030405060708090a0b", result.getObjectId("_id"));
    }

    @Test
    void testParseWithHashMapFactory_DateTime() throws IOException {
        long timestamp = 1638360000000L;

        byte[] bsonDoc = new DocumentBuilder()
            .putDateTime("created", timestamp)
            .build();

        BsonReader reader = new BsonReader(bsonDoc);
        BsonDocument result = (BsonDocument) parser.parse(reader);

        assertNotNull(result);
        assertEquals(timestamp, result.getDateTime("created"));
    }

    @Test
    void testParseWithHashMapFactory_Binary() throws IOException {
        byte[] binaryData = new byte[] { 0x01, 0x02, 0x03, 0x04 };

        byte[] bsonDoc = new DocumentBuilder()
            .putBinary("data", (byte) 0x00, binaryData)
            .build();

        BsonReader reader = new BsonReader(bsonDoc);
        BsonDocument result = (BsonDocument) parser.parse(reader);

        assertNotNull(result);
        Object binary = result.get("data");
        assertNotNull(binary);
    }

    @Test
    void testParseWithHashMapFactory_Regex() throws IOException {
        byte[] bsonDoc = new DocumentBuilder()
            .putRegex("pattern", "^test.*", "i")
            .build();

        BsonReader reader = new BsonReader(bsonDoc);
        BsonDocument result = (BsonDocument) parser.parse(reader);

        assertNotNull(result);
        Object regex = result.get("pattern");
        assertNotNull(regex);
        assertEquals("^test.*/i", regex.toString());
    }

    @Test
    void testParseWithHashMapFactory_Timestamp() throws IOException {
        byte[] bsonDoc = new DocumentBuilder()
            .putTimestamp("ts", 1234567890L)
            .build();

        BsonReader reader = new BsonReader(bsonDoc);
        BsonDocument result = (BsonDocument) parser.parse(reader);

        assertNotNull(result);
        Object timestamp = result.get("ts");
        assertNotNull(timestamp);
        assertEquals(1234567890L, timestamp);
    }

    @Test
    void testParseWithHashMapFactory_MinKey() throws IOException {
        byte[] bsonDoc = new DocumentBuilder()
            .putMinKey("min")
            .build();

        BsonReader reader = new BsonReader(bsonDoc);
        BsonDocument result = (BsonDocument) parser.parse(reader);

        assertNotNull(result);
        Object minKey = result.get("min");
        assertNotNull(minKey);
        assertEquals("MinKey", minKey);
    }

    @Test
    void testParseWithHashMapFactory_MaxKey() throws IOException {
        byte[] bsonDoc = new DocumentBuilder()
            .putMaxKey("max")
            .build();

        BsonReader reader = new BsonReader(bsonDoc);
        BsonDocument result = (BsonDocument) parser.parse(reader);

        assertNotNull(result);
        Object maxKey = result.get("max");
        assertNotNull(maxKey);
        assertEquals("MaxKey", maxKey);
    }

    @Test
    void testParseWithHashMapFactory_Decimal128() throws IOException {
        byte[] decimal128 = new byte[16];
        for (int i = 0; i < 16; i++) {
            decimal128[i] = (byte) i;
        }

        byte[] bsonDoc = new DocumentBuilder()
            .putDecimal128("decimal", decimal128)
            .build();

        BsonReader reader = new BsonReader(bsonDoc);
        BsonDocument result = (BsonDocument) parser.parse(reader);

        assertNotNull(result);
        Object decimal = result.get("decimal");
        assertNotNull(decimal);
        assertTrue(decimal instanceof byte[]);
    }

    @Test
    void testParseWithHashMapFactory_NestedDocument() throws IOException {
        byte[] nestedDoc = new DocumentBuilder()
            .putString("key", "value")
            .build();

        byte[] bsonDoc = new DocumentBuilder()
            .putDocument("nested", nestedDoc)
            .build();

        BsonReader reader = new BsonReader(bsonDoc);
        BsonDocument result = (BsonDocument) parser.parse(reader);

        assertNotNull(result);
        BsonDocument nested = result.getDocument("nested");
        assertNotNull(nested);
        assertEquals("value", nested.getString("key"));
    }

    @Test
    void testParseWithHashMapFactory_EmptyDocument() throws IOException {
        byte[] bsonDoc = new DocumentBuilder().build();

        BsonReader reader = new BsonReader(bsonDoc);
        BsonDocument result = (BsonDocument) parser.parse(reader);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testGetValueSize() {
        byte[] docBytes = new byte[] {
            0x10, 0x00, 0x00, 0x00,  // length = 16 bytes
            0x10, 0x61, 0x00,        // int32 field "a"
            0x2A, 0x00, 0x00, 0x00,  // value = 42
            0x00                     // terminator
        };

        int size = parser.getValueSize(docBytes, 0);
        assertEquals(16, size);
    }

    @Test
    void testParseWithHashMapFactory_Array() throws IOException {
        // Create array with int32 values
        ByteArrayOutputStream arrayBaos = new ByteArrayOutputStream();
        arrayBaos.write(BsonType.INT32);
        arrayBaos.write('0');
        arrayBaos.write(0x00);
        arrayBaos.write(0x01);
        arrayBaos.write(0x00);
        arrayBaos.write(0x00);
        arrayBaos.write(0x00);

        byte[] arrayElements = arrayBaos.toByteArray();
        ByteArrayOutputStream arrayResult = new ByteArrayOutputStream();
        int arrayLen = 4 + arrayElements.length + 1;
        arrayResult.write(arrayLen & 0xFF);
        arrayResult.write((arrayLen >> 8) & 0xFF);
        arrayResult.write((arrayLen >> 16) & 0xFF);
        arrayResult.write((arrayLen >> 24) & 0xFF);
        arrayResult.write(arrayElements);
        arrayResult.write(0x00);

        byte[] bsonDoc = new DocumentBuilder()
            .putArray("items", arrayResult.toByteArray())
            .build();

        BsonReader reader = new BsonReader(bsonDoc);
        BsonDocument result = (BsonDocument) parser.parse(reader);

        assertNotNull(result);
        Object array = result.get("items");
        assertNotNull(array);
    }

    @Test
    void testParseWithHashMapFactory_DBPointer() throws IOException {
        byte[] objectId = new byte[12];
        for (int i = 0; i < 12; i++) {
            objectId[i] = (byte) i;
        }

        byte[] bsonDoc = new DocumentBuilder()
            .putDBPointer("ref", "db.collection", objectId)
            .build();

        BsonReader reader = new BsonReader(bsonDoc);
        BsonDocument result = (BsonDocument) parser.parse(reader);

        assertNotNull(result);
        Object dbPointer = result.get("ref");
        assertNotNull(dbPointer);
        assertTrue(dbPointer instanceof Object[]);
        Object[] parts = (Object[]) dbPointer;
        assertEquals(2, parts.length);
        assertEquals("db.collection", parts[0]);
        assertEquals("000102030405060708090a0b", parts[1]);
    }

    @Test
    void testParseWithHashMapFactory_JavaScriptWithScope() throws IOException {
        byte[] scopeDoc = new DocumentBuilder()
            .putInt32("x", 10)
            .build();

        byte[] bsonDoc = new DocumentBuilder()
            .putJavaScriptWithScope("js", "function() { return x; }", scopeDoc)
            .build();

        BsonReader reader = new BsonReader(bsonDoc);
        BsonDocument result = (BsonDocument) parser.parse(reader);

        assertNotNull(result);
        Object jsWithScope = result.get("js");
        assertNotNull(jsWithScope);
        assertTrue(jsWithScope instanceof Object[]);
        Object[] parts = (Object[]) jsWithScope;
        assertEquals(2, parts.length);
        assertEquals("function() { return x; }", parts[0]);
        assertTrue(parts[1] instanceof BsonDocument);
    }

    @Test
    void testParseWithHashMapFactory_InvalidType() throws IOException {
        byte[] bsonDoc = new DocumentBuilder()
            .putInvalidType("bad")
            .build();

        BsonReader reader = new BsonReader(bsonDoc);

        assertThrows(com.cloud.fastbson.exception.InvalidBsonTypeException.class, () -> {
            parser.parse(reader);
        });
    }
}
