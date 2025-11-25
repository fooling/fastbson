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
 *
 * <p>Uses Strategy Pattern with lookup table for O(1) type dispatch.
 * Uses singleton pattern to reduce GC pressure.
 */
public class TypeHandler {

    /**
     * Singleton instance for internal use (reduces GC pressure).
     */
    private static final TypeHandler INSTANCE = new TypeHandler();

    /**
     * Functional interface for BSON type parsing strategy.
     */
    @FunctionalInterface
    interface BsonTypeParser {
        /**
         * Parses a BSON value from the reader.
         *
         * @param reader the BsonReader positioned at the value
         * @return the parsed value
         */
        Object parse(BsonReader reader);
    }

    /**
     * Lookup table for O(1) type dispatch.
     * Index is BSON type byte (as unsigned int 0-255).
     */
    private static final BsonTypeParser[] PARSERS = new BsonTypeParser[256];

    static {
        // Initialize lookup table with type-specific parsers
        initializeParsers();
    }

    /**
     * Initializes the parser lookup table.
     */
    private static void initializeParsers() {
        // Simple types: method references
        PARSERS[BsonType.DOUBLE & 0xFF] = BsonReader::readDouble;
        PARSERS[BsonType.INT32 & 0xFF] = BsonReader::readInt32;
        PARSERS[BsonType.INT64 & 0xFF] = BsonReader::readInt64;
        PARSERS[BsonType.STRING & 0xFF] = BsonReader::readString;
        PARSERS[BsonType.JAVASCRIPT & 0xFF] = BsonReader::readString;
        PARSERS[BsonType.SYMBOL & 0xFF] = BsonReader::readString;

        // Types requiring simple transformation: lambdas
        PARSERS[BsonType.BOOLEAN & 0xFF] = (BsonReader reader) -> reader.readByte() != 0;
        PARSERS[BsonType.DATE_TIME & 0xFF] = (BsonReader reader) -> new Date(reader.readInt64());
        PARSERS[BsonType.OBJECT_ID & 0xFF] = (BsonReader reader) -> BsonUtils.bytesToHex(reader.readBytes(12));

        // Null types
        PARSERS[BsonType.NULL & 0xFF] = (BsonReader reader) -> null;
        PARSERS[BsonType.UNDEFINED & 0xFF] = (BsonReader reader) -> null;

        // Complex types: static method references
        PARSERS[BsonType.DOCUMENT & 0xFF] = TypeHandler::parseDocumentStatic;
        PARSERS[BsonType.ARRAY & 0xFF] = TypeHandler::parseArrayStatic;
        PARSERS[BsonType.BINARY & 0xFF] = TypeHandler::parseBinaryStatic;
        PARSERS[BsonType.REGEX & 0xFF] = TypeHandler::parseRegexStatic;
        PARSERS[BsonType.DB_POINTER & 0xFF] = TypeHandler::parseDBPointerStatic;
        PARSERS[BsonType.JAVASCRIPT_WITH_SCOPE & 0xFF] = TypeHandler::parseJavaScriptWithScopeStatic;
        PARSERS[BsonType.TIMESTAMP & 0xFF] = TypeHandler::parseTimestampStatic;
        PARSERS[BsonType.DECIMAL128 & 0xFF] = TypeHandler::parseDecimal128Static;
        PARSERS[BsonType.MIN_KEY & 0xFF] = (BsonReader reader) -> new MinKey();
        PARSERS[BsonType.MAX_KEY & 0xFF] = (BsonReader reader) -> new MaxKey();
    }

    /**
     * Parses a BSON value based on its type byte.
     *
     * <p>Uses lookup table for O(1) dispatch instead of switch-case.
     *
     * @param reader the BsonReader positioned at the value
     * @param type the BSON type byte
     * @return the parsed value
     * @throws InvalidBsonTypeException if the type is unsupported
     */
    public Object parseValue(BsonReader reader, byte type) {
        BsonTypeParser parser = PARSERS[type & 0xFF];
        if (parser != null) {
            return parser.parse(reader);
        }
        throw new InvalidBsonTypeException(type);
    }

    // ==================== Complex Type Parsers (Static Methods) ====================

    /**
     * Parses an embedded document.
     * Static method for use in lookup table.
     */
    private static Map<String, Object> parseDocumentStatic(BsonReader reader) {
        return INSTANCE.parseDocument(reader);
    }

    /**
     * Parses an embedded document.
     * Public method for external use (e.g., PartialParser).
     */
    public Map<String, Object> parseDocument(BsonReader reader) {
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
     * Static method for use in lookup table.
     */
    private static List<Object> parseArrayStatic(BsonReader reader) {
        // Read array as document first
        int docLength = reader.readInt32();
        int endPosition = reader.position() + docLength - 4;

        Map<String, Object> arrayDoc = new HashMap<String, Object>();

        while (reader.position() < endPosition) {
            byte type = reader.readByte();
            if (type == BsonType.END_OF_DOCUMENT) {
                break;
            }

            String fieldName = reader.readCString();
            Object value = INSTANCE.parseValue(reader, type);
            arrayDoc.put(fieldName, value);
        }

        // Convert to list (arrays use string indices "0", "1", "2"...)
        List<Object> array = new ArrayList<Object>();
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
     * Static method for use in lookup table.
     */
    private static BinaryData parseBinaryStatic(BsonReader reader) {
        int length = reader.readInt32();
        byte subtype = reader.readByte();
        byte[] data = reader.readBytes(length);
        return new BinaryData(subtype, data);
    }

    /**
     * Parses a regular expression (cstring pattern + cstring options).
     * Static method for use in lookup table.
     */
    private static RegexValue parseRegexStatic(BsonReader reader) {
        String pattern = reader.readCString();
        String options = reader.readCString();
        return new RegexValue(pattern, options);
    }

    /**
     * Parses a DBPointer (deprecated: string + 12-byte ObjectId).
     * Static method for use in lookup table.
     */
    private static DBPointer parseDBPointerStatic(BsonReader reader) {
        String namespace = reader.readString();
        byte[] id = reader.readBytes(12);
        return new DBPointer(namespace, BsonUtils.bytesToHex(id));
    }

    /**
     * Parses JavaScript code with scope (int32 total_length + string code + document scope).
     * Static method for use in lookup table.
     */
    private static JavaScriptWithScope parseJavaScriptWithScopeStatic(BsonReader reader) {
        int totalLength = reader.readInt32();
        String code = reader.readString();
        Map<String, Object> scope = INSTANCE.parseDocument(reader);
        return new JavaScriptWithScope(code, scope);
    }

    /**
     * Parses a timestamp (int64: increment in low 32 bits, seconds in high 32 bits).
     * Static method for use in lookup table.
     */
    private static Timestamp parseTimestampStatic(BsonReader reader) {
        long value = reader.readInt64();
        int increment = (int) (value & 0xFFFFFFFFL);
        int seconds = (int) ((value >> 32) & 0xFFFFFFFFL);
        return new Timestamp(seconds, increment);
    }

    /**
     * Parses a 128-bit decimal (16 bytes).
     * Note: Java doesn't have native 128-bit decimal, so we store as bytes.
     * Static method for use in lookup table.
     */
    private static Decimal128 parseDecimal128Static(BsonReader reader) {
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
