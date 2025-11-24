package com.cloud.fastbson.handler;

import com.cloud.fastbson.exception.InvalidBsonTypeException;
import com.cloud.fastbson.reader.BsonReader;
import com.cloud.fastbson.util.BsonType;
import com.cloud.fastbson.util.BsonUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles parsing of all BSON types.
 *
 * <p>Provides type-specific parsing methods for all BSON value types
 * according to the MongoDB 3.4 BSON specification.
 */
public class TypeHandler {

    /**
     * Parses a BSON value based on its type byte.
     *
     * @param reader the BsonReader positioned at the value
     * @param type the BSON type byte
     * @return the parsed value
     * @throws InvalidBsonTypeException if the type is unsupported
     */
    public Object parseValue(BsonReader reader, byte type) {
        switch (type) {
            case BsonType.DOUBLE:
                return parseDouble(reader);

            case BsonType.STRING:
                return parseString(reader);

            case BsonType.DOCUMENT:
                return parseDocument(reader);

            case BsonType.ARRAY:
                return parseArray(reader);

            case BsonType.BINARY:
                return parseBinary(reader);

            case BsonType.UNDEFINED:
                return null; // Deprecated, treat as null

            case BsonType.OBJECT_ID:
                return parseObjectId(reader);

            case BsonType.BOOLEAN:
                return parseBoolean(reader);

            case BsonType.DATE_TIME:
                return parseDateTime(reader);

            case BsonType.NULL:
                return null;

            case BsonType.REGEX:
                return parseRegex(reader);

            case BsonType.DB_POINTER:
                return parseDBPointer(reader);

            case BsonType.JAVASCRIPT:
                return parseJavaScript(reader);

            case BsonType.SYMBOL:
                return parseSymbol(reader);

            case BsonType.JAVASCRIPT_WITH_SCOPE:
                return parseJavaScriptWithScope(reader);

            case BsonType.INT32:
                return parseInt32(reader);

            case BsonType.TIMESTAMP:
                return parseTimestamp(reader);

            case BsonType.INT64:
                return parseInt64(reader);

            case BsonType.DECIMAL128:
                return parseDecimal128(reader);

            case BsonType.MIN_KEY:
                return new MinKey();

            case BsonType.MAX_KEY:
                return new MaxKey();

            default:
                throw new InvalidBsonTypeException(type);
        }
    }

    /**
     * Parses a double value (8 bytes IEEE 754).
     */
    private Double parseDouble(BsonReader reader) {
        return reader.readDouble();
    }

    /**
     * Parses a string value (int32 length + UTF-8 + null terminator).
     */
    private String parseString(BsonReader reader) {
        return reader.readString();
    }

    /**
     * Parses an embedded document.
     */
    private Map<String, Object> parseDocument(BsonReader reader) {
        int docLength = reader.readInt32();
        int endPosition = reader.position() + docLength - 4;

        Map<String, Object> document = new HashMap<String, Object>();

        while (reader.position() < endPosition) {
            byte type = reader.readByte();
            if (type == BsonType.END_OF_DOCUMENT) {
                break;
            }

            String fieldName = reader.readCString();
            Object value = parseValue(reader, type);
            document.put(fieldName, value);
        }

        return document;
    }

    /**
     * Parses an array (same as document, but keys are "0", "1", "2", etc.).
     */
    private List<Object> parseArray(BsonReader reader) {
        Map<String, Object> arrayDoc = parseDocument(reader);
        List<Object> array = new ArrayList<Object>();

        // Arrays in BSON use string indices "0", "1", "2"...
        for (int i = 0; i < arrayDoc.size(); i++) {
            String key = String.valueOf(i);
            if (arrayDoc.containsKey(key)) {
                array.add(arrayDoc.get(key));
            } else {
                break;
            }
        }

        return array;
    }

    /**
     * Parses binary data (int32 length + subtype + bytes).
     */
    private BinaryData parseBinary(BsonReader reader) {
        int length = reader.readInt32();
        byte subtype = reader.readByte();
        byte[] data = reader.readBytes(length);
        return new BinaryData(subtype, data);
    }

    /**
     * Parses an ObjectId (12 bytes).
     */
    private String parseObjectId(BsonReader reader) {
        byte[] bytes = reader.readBytes(12);
        return BsonUtils.bytesToHex(bytes);
    }

    /**
     * Parses a boolean value (1 byte: 0x00 or 0x01).
     */
    private Boolean parseBoolean(BsonReader reader) {
        return reader.readByte() != 0;
    }

    /**
     * Parses a UTC datetime (int64 milliseconds since Unix epoch).
     */
    private Date parseDateTime(BsonReader reader) {
        long milliseconds = reader.readInt64();
        return new Date(milliseconds);
    }

    /**
     * Parses a regular expression (cstring pattern + cstring options).
     */
    private RegexValue parseRegex(BsonReader reader) {
        String pattern = reader.readCString();
        String options = reader.readCString();
        return new RegexValue(pattern, options);
    }

    /**
     * Parses a DBPointer (deprecated: string + 12-byte ObjectId).
     */
    private DBPointer parseDBPointer(BsonReader reader) {
        String namespace = reader.readString();
        byte[] id = reader.readBytes(12);
        return new DBPointer(namespace, BsonUtils.bytesToHex(id));
    }

    /**
     * Parses JavaScript code (string).
     */
    private String parseJavaScript(BsonReader reader) {
        return reader.readString();
    }

    /**
     * Parses a Symbol (deprecated: string).
     */
    private String parseSymbol(BsonReader reader) {
        return reader.readString();
    }

    /**
     * Parses JavaScript code with scope (int32 total_length + string code + document scope).
     */
    private JavaScriptWithScope parseJavaScriptWithScope(BsonReader reader) {
        int totalLength = reader.readInt32();
        String code = reader.readString();
        Map<String, Object> scope = parseDocument(reader);
        return new JavaScriptWithScope(code, scope);
    }

    /**
     * Parses a 32-bit integer.
     */
    private Integer parseInt32(BsonReader reader) {
        return reader.readInt32();
    }

    /**
     * Parses a timestamp (int64: increment in low 32 bits, seconds in high 32 bits).
     */
    private Timestamp parseTimestamp(BsonReader reader) {
        long value = reader.readInt64();
        int increment = (int) (value & 0xFFFFFFFFL);
        int seconds = (int) ((value >> 32) & 0xFFFFFFFFL);
        return new Timestamp(seconds, increment);
    }

    /**
     * Parses a 64-bit integer.
     */
    private Long parseInt64(BsonReader reader) {
        return reader.readInt64();
    }

    /**
     * Parses a 128-bit decimal (16 bytes).
     * Note: Java doesn't have native 128-bit decimal, so we store as bytes.
     */
    private Decimal128 parseDecimal128(BsonReader reader) {
        byte[] bytes = reader.readBytes(16);
        return new Decimal128(bytes);
    }

    // Helper classes for complex BSON types

    /**
     * Represents BSON binary data.
     */
    public static class BinaryData {
        public final byte subtype;
        public final byte[] data;

        public BinaryData(byte subtype, byte[] data) {
            this.subtype = subtype;
            this.data = data;
        }
    }

    /**
     * Represents a BSON regular expression.
     */
    public static class RegexValue {
        public final String pattern;
        public final String options;

        public RegexValue(String pattern, String options) {
            this.pattern = pattern;
            this.options = options;
        }
    }

    /**
     * Represents a BSON DBPointer (deprecated).
     */
    public static class DBPointer {
        public final String namespace;
        public final String id;

        public DBPointer(String namespace, String id) {
            this.namespace = namespace;
            this.id = id;
        }
    }

    /**
     * Represents BSON JavaScript code with scope.
     */
    public static class JavaScriptWithScope {
        public final String code;
        public final Map<String, Object> scope;

        public JavaScriptWithScope(String code, Map<String, Object> scope) {
            this.code = code;
            this.scope = scope;
        }
    }

    /**
     * Represents a BSON timestamp.
     */
    public static class Timestamp {
        public final int seconds;
        public final int increment;

        public Timestamp(int seconds, int increment) {
            this.seconds = seconds;
            this.increment = increment;
        }
    }

    /**
     * Represents a BSON Decimal128 value.
     */
    public static class Decimal128 {
        public final byte[] bytes;

        public Decimal128(byte[] bytes) {
            if (bytes == null || bytes.length != 16) {
                throw new IllegalArgumentException("Decimal128 must be exactly 16 bytes");
            }
            this.bytes = bytes;
        }
    }

    /**
     * Represents BSON MinKey.
     */
    public static class MinKey {
        @Override
        public String toString() {
            return "MinKey";
        }
    }

    /**
     * Represents BSON MaxKey.
     */
    public static class MaxKey {
        @Override
        public String toString() {
            return "MaxKey";
        }
    }
}
