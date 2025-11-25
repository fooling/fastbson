package com.cloud.fastbson.handler;

import com.cloud.fastbson.exception.InvalidBsonTypeException;
import com.cloud.fastbson.handler.parsers.ArrayParser;
import com.cloud.fastbson.handler.parsers.BinaryParser;
import com.cloud.fastbson.handler.parsers.BooleanParser;
import com.cloud.fastbson.handler.parsers.DBPointerParser;
import com.cloud.fastbson.handler.parsers.DateTimeParser;
import com.cloud.fastbson.handler.parsers.Decimal128Parser;
import com.cloud.fastbson.handler.parsers.DocumentParser;
import com.cloud.fastbson.handler.parsers.DoubleParser;
import com.cloud.fastbson.handler.parsers.Int32Parser;
import com.cloud.fastbson.handler.parsers.Int64Parser;
import com.cloud.fastbson.handler.parsers.JavaScriptWithScopeParser;
import com.cloud.fastbson.handler.parsers.MaxKeyParser;
import com.cloud.fastbson.handler.parsers.MinKeyParser;
import com.cloud.fastbson.handler.parsers.NullParser;
import com.cloud.fastbson.handler.parsers.ObjectIdParser;
import com.cloud.fastbson.handler.parsers.RegexParser;
import com.cloud.fastbson.handler.parsers.StringParser;
import com.cloud.fastbson.handler.parsers.TimestampParser;
import com.cloud.fastbson.reader.BsonReader;
import com.cloud.fastbson.util.BsonType;

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

        // Complex nested types: enum singleton parsers (Phase 2.11)
        // These parsers need access to TypeHandler for recursive parsing
        DocumentParser.INSTANCE.setHandler(INSTANCE);
        ArrayParser.INSTANCE.setHandler(INSTANCE);
        JavaScriptWithScopeParser.INSTANCE.setHandler(INSTANCE);

        PARSERS[BsonType.DOCUMENT & 0xFF] = DocumentParser.INSTANCE;
        PARSERS[BsonType.ARRAY & 0xFF] = ArrayParser.INSTANCE;
        PARSERS[BsonType.JAVASCRIPT_WITH_SCOPE & 0xFF] = JavaScriptWithScopeParser.INSTANCE;
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

    /**
     * Parses an embedded document.
     * Public method for external use (e.g., PartialParser).
     *
     * <p>This method is retained for backward compatibility and external use.
     * Internal parsing uses DocumentParser.
     */
    public Map<String, Object> parseDocument(BsonReader reader) {
        return (Map<String, Object>) DocumentParser.INSTANCE.parse(reader);
    }
}
