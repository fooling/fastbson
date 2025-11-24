package com.cloud.fastbson.util;

/**
 * BSON type constants according to MongoDB 3.4 specification.
 *
 * <p>Each constant represents a BSON type byte value used in BSON documents.
 *
 * @see <a href="http://bsonspec.org/spec.html">BSON Specification</a>
 */
public final class BsonType {

    // Prevent instantiation
    private BsonType() {
        throw new AssertionError("Cannot instantiate BsonType");
    }

    /**
     * Double (64-bit IEEE 754 floating point)
     */
    public static final byte DOUBLE = 0x01;

    /**
     * UTF-8 string
     */
    public static final byte STRING = 0x02;

    /**
     * Embedded document
     */
    public static final byte DOCUMENT = 0x03;

    /**
     * Array
     */
    public static final byte ARRAY = 0x04;

    /**
     * Binary data
     */
    public static final byte BINARY = 0x05;

    /**
     * Undefined (deprecated)
     */
    public static final byte UNDEFINED = 0x06;

    /**
     * ObjectId (12 bytes)
     */
    public static final byte OBJECT_ID = 0x07;

    /**
     * Boolean
     */
    public static final byte BOOLEAN = 0x08;

    /**
     * UTC datetime (int64 milliseconds since epoch)
     */
    public static final byte DATE_TIME = 0x09;

    /**
     * Null value
     */
    public static final byte NULL = 0x0A;

    /**
     * Regular expression
     */
    public static final byte REGEX = 0x0B;

    /**
     * DBPointer (deprecated)
     */
    public static final byte DB_POINTER = 0x0C;

    /**
     * JavaScript code
     */
    public static final byte JAVASCRIPT = 0x0D;

    /**
     * Symbol (deprecated)
     */
    public static final byte SYMBOL = 0x0E;

    /**
     * JavaScript code with scope
     */
    public static final byte JAVASCRIPT_WITH_SCOPE = 0x0F;

    /**
     * 32-bit integer
     */
    public static final byte INT32 = 0x10;

    /**
     * Timestamp
     */
    public static final byte TIMESTAMP = 0x11;

    /**
     * 64-bit integer
     */
    public static final byte INT64 = 0x12;

    /**
     * 128-bit decimal floating point (MongoDB 3.4+)
     */
    public static final byte DECIMAL128 = 0x13;

    /**
     * Min key
     */
    public static final byte MIN_KEY = (byte) 0xFF;

    /**
     * Max key
     */
    public static final byte MAX_KEY = 0x7F;

    /**
     * End of document marker
     */
    public static final byte END_OF_DOCUMENT = 0x00;

    /**
     * Checks if the given type byte is a valid BSON type.
     *
     * @param type the type byte to check
     * @return true if the type is valid, false otherwise
     */
    public static boolean isValidType(byte type) {
        return (type >= 0x01 && type <= 0x13) ||
               type == MIN_KEY ||
               type == MAX_KEY ||
               type == END_OF_DOCUMENT;
    }

    /**
     * Returns a human-readable name for the given BSON type.
     *
     * @param type the BSON type byte
     * @return the type name
     */
    public static String getTypeName(byte type) {
        switch (type) {
            case DOUBLE: return "double";
            case STRING: return "string";
            case DOCUMENT: return "document";
            case ARRAY: return "array";
            case BINARY: return "binary";
            case UNDEFINED: return "undefined";
            case OBJECT_ID: return "objectId";
            case BOOLEAN: return "boolean";
            case DATE_TIME: return "dateTime";
            case NULL: return "null";
            case REGEX: return "regex";
            case DB_POINTER: return "dbPointer";
            case JAVASCRIPT: return "javascript";
            case SYMBOL: return "symbol";
            case JAVASCRIPT_WITH_SCOPE: return "javascriptWithScope";
            case INT32: return "int32";
            case TIMESTAMP: return "timestamp";
            case INT64: return "int64";
            case DECIMAL128: return "decimal128";
            case MIN_KEY: return "minKey";
            case MAX_KEY: return "maxKey";
            case END_OF_DOCUMENT: return "endOfDocument";
            default: return "unknown(0x" + Integer.toHexString(type & 0xFF) + ")";
        }
    }

    /**
     * Checks if the type has a fixed length value.
     *
     * @param type the BSON type byte
     * @return true if the type has fixed length, false otherwise
     */
    public static boolean isFixedLength(byte type) {
        switch (type) {
            case DOUBLE:
            case OBJECT_ID:
            case BOOLEAN:
            case DATE_TIME:
            case NULL:
            case INT32:
            case TIMESTAMP:
            case INT64:
            case MIN_KEY:
            case MAX_KEY:
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns the fixed length in bytes for fixed-length types.
     * Returns -1 for variable-length types.
     *
     * @param type the BSON type byte
     * @return the length in bytes, or -1 if variable length
     */
    public static int getFixedLength(byte type) {
        switch (type) {
            case DOUBLE: return 8;
            case OBJECT_ID: return 12;
            case BOOLEAN: return 1;
            case DATE_TIME: return 8;
            case NULL: return 0;
            case INT32: return 4;
            case TIMESTAMP: return 8;
            case INT64: return 8;
            case MIN_KEY: return 0;
            case MAX_KEY: return 0;
            default: return -1;
        }
    }
}
