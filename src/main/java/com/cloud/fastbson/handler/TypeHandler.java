package com.cloud.fastbson.handler;

import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.document.BsonDocumentFactory;
import com.cloud.fastbson.document.fast.FastBsonDocumentFactory;
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
import com.cloud.fastbson.util.CapacityEstimator;

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

    /**
     * Document factory for creating BsonDocument and BsonArray instances.
     * Uses FastBsonDocumentFactory (fastutil-based, zero-boxing).
     */
    private static BsonDocumentFactory documentFactory = FastBsonDocumentFactory.INSTANCE;

    /**
     * Capacity estimator for document and array pre-allocation.
     * Uses default heuristics optimized for general BSON documents.
     *
     * @since Phase 3.5
     */
    private static CapacityEstimator capacityEstimator = CapacityEstimator.defaults();

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

        // Phase 2.13: Inject factory for zero-boxing architecture
        DocumentParser.INSTANCE.setFactory(documentFactory);
        ArrayParser.INSTANCE.setFactory(documentFactory);

        // Phase 3.5: Inject capacity estimator for pre-allocation optimization
        DocumentParser.INSTANCE.setCapacityEstimator(capacityEstimator);
        ArrayParser.INSTANCE.setCapacityEstimator(capacityEstimator);

        PARSERS[BsonType.DOCUMENT & 0xFF] = DocumentParser.INSTANCE;
        PARSERS[BsonType.ARRAY & 0xFF] = ArrayParser.INSTANCE;
        PARSERS[BsonType.JAVASCRIPT_WITH_SCOPE & 0xFF] = JavaScriptWithScopeParser.INSTANCE;
    }

    /**
     * Sets the document factory for creating BsonDocument and BsonArray instances.
     *
     * <p>Uses FastBsonDocumentFactory (fastutil-based, zero-boxing).
     *
     * @param factory the BsonDocumentFactory to use for creating documents/arrays
     */
    public static void setDocumentFactory(BsonDocumentFactory factory) {
        documentFactory = factory;
        // Re-inject factory into parsers
        DocumentParser.INSTANCE.setFactory(documentFactory);
        ArrayParser.INSTANCE.setFactory(documentFactory);
    }

    /**
     * Gets the current document factory.
     *
     * @return the current BsonDocumentFactory
     */
    public static BsonDocumentFactory getDocumentFactory() {
        return documentFactory;
    }

    /**
     * Sets the capacity estimator for document and array pre-allocation.
     *
     * <p>Capacity estimation is used to pre-allocate the right size for HashMaps and ArrayLists,
     * avoiding costly rehashing/resizing operations during parsing.
     *
     * <p>Default estimator uses heuristics optimized for general BSON documents.
     * Can be customized for specific business scenarios.
     *
     * @param estimator the CapacityEstimator to use
     * @since Phase 3.5
     */
    public static void setCapacityEstimator(CapacityEstimator estimator) {
        capacityEstimator = estimator;
        // Re-inject estimator into parsers
        DocumentParser.INSTANCE.setCapacityEstimator(capacityEstimator);
        ArrayParser.INSTANCE.setCapacityEstimator(capacityEstimator);
    }

    /**
     * Gets the current capacity estimator.
     *
     * @return the current CapacityEstimator
     * @since Phase 3.5
     */
    public static CapacityEstimator getCapacityEstimator() {
        return capacityEstimator;
    }

    /**
     * Parses a BSON value based on its type byte (high-performance API).
     *
     * <p>Uses lookup table for O(1) dispatch instead of switch-case.
     *
     * <p>Returns native types without boxing conversion:
     * <ul>
     *   <li>Documents: BsonDocument (zero-copy)</li>
     *   <li>Arrays: BsonArray (zero-copy)</li>
     *   <li>Primitives: int, long, double, boolean (no boxing)</li>
     *   <li>Strings: String</li>
     * </ul>
     *
     * @param reader the BsonReader positioned at the value
     * @param type the BSON type byte
     * @return the parsed value (native types, no legacy conversion)
     * @throws InvalidBsonTypeException if the type is unsupported
     */
    public Object getParsedValue(BsonReader reader, byte type) {
        BsonTypeParser parser = PARSERS[type & 0xFF];
        if (parser != null) {
            return parser.parse(reader);
        }
        throw new InvalidBsonTypeException(type);
    }

}
