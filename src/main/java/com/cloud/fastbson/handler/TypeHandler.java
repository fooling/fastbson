package com.cloud.fastbson.handler;

import com.cloud.fastbson.exception.InvalidBsonTypeException;
import com.cloud.fastbson.handler.parsers.BinaryParser;
import com.cloud.fastbson.handler.parsers.BooleanParser;
import com.cloud.fastbson.handler.parsers.DBPointerParser;
import com.cloud.fastbson.handler.parsers.DateTimeParser;
import com.cloud.fastbson.handler.parsers.Decimal128Parser;
import com.cloud.fastbson.handler.parsers.DoubleParser;
import com.cloud.fastbson.handler.parsers.Int32Parser;
import com.cloud.fastbson.handler.parsers.Int64Parser;
import com.cloud.fastbson.handler.parsers.MaxKeyParser;
import com.cloud.fastbson.handler.parsers.MinKeyParser;
import com.cloud.fastbson.handler.parsers.NullParser;
import com.cloud.fastbson.handler.parsers.ObjectIdParser;
import com.cloud.fastbson.handler.parsers.RegexParser;
import com.cloud.fastbson.handler.parsers.StringParser;
import com.cloud.fastbson.handler.parsers.TimestampParser;
import com.cloud.fastbson.reader.BsonReader;
import com.cloud.fastbson.util.BsonType;

import java.util.ArrayList;
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
 * Each BSON type is handled by an independent Parser class.
 */
public class TypeHandler {

    /**
     * Singleton instance for internal use (reduces GC pressure).
     */
    private static final TypeHandler INSTANCE = new TypeHandler();

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
     * Registers independent Parser classes for each BSON type.
     */
    private static void initializeParsers() {
        // Simple types: enum singleton parsers (Phase 2.9)
        PARSERS[BsonType.DOUBLE & 0xFF] = DoubleParser.INSTANCE;
        PARSERS[BsonType.INT32 & 0xFF] = Int32Parser.INSTANCE;
        PARSERS[BsonType.INT64 & 0xFF] = Int64Parser.INSTANCE;
        PARSERS[BsonType.STRING & 0xFF] = StringParser.INSTANCE;
        PARSERS[BsonType.BOOLEAN & 0xFF] = BooleanParser.INSTANCE;
        PARSERS[BsonType.JAVASCRIPT & 0xFF] = StringParser.INSTANCE;  // JavaScript is also string
        PARSERS[BsonType.SYMBOL & 0xFF] = StringParser.INSTANCE;      // Symbol is also string

        // Medium complexity types: enum singleton parsers (Phase 2.10)
        PARSERS[BsonType.DATE_TIME & 0xFF] = DateTimeParser.INSTANCE;
        PARSERS[BsonType.OBJECT_ID & 0xFF] = ObjectIdParser.INSTANCE;
        PARSERS[BsonType.NULL & 0xFF] = NullParser.INSTANCE;
        PARSERS[BsonType.UNDEFINED & 0xFF] = NullParser.INSTANCE;     // Undefined is also null
        PARSERS[BsonType.MIN_KEY & 0xFF] = MinKeyParser.INSTANCE;
        PARSERS[BsonType.MAX_KEY & 0xFF] = MaxKeyParser.INSTANCE;
        PARSERS[BsonType.BINARY & 0xFF] = BinaryParser.INSTANCE;
        PARSERS[BsonType.REGEX & 0xFF] = RegexParser.INSTANCE;
        PARSERS[BsonType.DB_POINTER & 0xFF] = DBPointerParser.INSTANCE;
        PARSERS[BsonType.TIMESTAMP & 0xFF] = TimestampParser.INSTANCE;
        PARSERS[BsonType.DECIMAL128 & 0xFF] = Decimal128Parser.INSTANCE;

        // Complex nested types: static method references (TODO: Phase 2.11)
        PARSERS[BsonType.DOCUMENT & 0xFF] = TypeHandler::parseDocumentStatic;
        PARSERS[BsonType.ARRAY & 0xFF] = TypeHandler::parseArrayStatic;
        PARSERS[BsonType.JAVASCRIPT_WITH_SCOPE & 0xFF] = TypeHandler::parseJavaScriptWithScopeStatic;
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
     * Parses JavaScript code with scope (int32 total_length + string code + document scope).
     * Static method for use in lookup table.
     */
    private static JavaScriptWithScope parseJavaScriptWithScopeStatic(BsonReader reader) {
        int totalLength = reader.readInt32();
        String code = reader.readString();
        Map<String, Object> scope = INSTANCE.parseDocument(reader);
        return new JavaScriptWithScope(code, scope);
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
     * Uses singleton pattern to avoid allocations.
     */
    public static class MinKey {
        public static final MinKey INSTANCE = new MinKey();

        private MinKey() {
            // Private constructor for singleton
        }

        @Override
        public String toString() {
            return "MinKey";
        }
    }

    /**
     * Represents BSON MaxKey.
     * Uses singleton pattern to avoid allocations.
     */
    public static class MaxKey {
        public static final MaxKey INSTANCE = new MaxKey();

        private MaxKey() {
            // Private constructor for singleton
        }

        @Override
        public String toString() {
            return "MaxKey";
        }
    }
}
