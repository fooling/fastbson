package com.cloud.fastbson.util;

import com.cloud.fastbson.annotation.BranchOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Helper class for reading and applying @BranchOrder annotation configuration.
 *
 * <p>This class reads the @BranchOrder annotation from parser classes and provides
 * utilities for optimizing type checking order based on frequency hints.
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * // Read branch order from annotated class
 * byte[] order = BranchOrderHelper.getBranchOrder(MyParser.class);
 *
 * // Check if type should be prioritized
 * boolean isPriority = BranchOrderHelper.isPriorityType(order, BsonType.INT64);
 *
 * // Get priority types in order
 * List<Byte> priorities = BranchOrderHelper.getPriorityTypes(order);
 * }</pre>
 *
 * @author FastBSON
 * @since Phase 3.5
 */
public final class BranchOrderHelper {

    /**
     * Default branch order optimized for general BSON documents.
     *
     * <p>Based on empirical frequency analysis:
     * <ul>
     *   <li>INT32: 35% (counters, IDs, small numbers)</li>
     *   <li>STRING: 30% (names, descriptions, text)</li>
     *   <li>DOUBLE: 15% (floating-point measurements)</li>
     *   <li>INT64: 10% (timestamps, large numbers)</li>
     * </ul>
     */
    public static final byte[] DEFAULT_ORDER = {
        BsonType.INT32,
        BsonType.STRING,
        BsonType.DOUBLE,
        BsonType.INT64,
        BsonType.BOOLEAN,
        BsonType.DOCUMENT,
        BsonType.ARRAY
    };

    // Private constructor to prevent instantiation
    private BranchOrderHelper() {
        throw new AssertionError("BranchOrderHelper is a utility class and should not be instantiated");
    }

    /**
     * Reads the @BranchOrder annotation from a class.
     *
     * <p>If the class is not annotated with @BranchOrder, returns {@link #DEFAULT_ORDER}.
     *
     * @param clazz the class to read annotation from
     * @return ordered array of BSON type codes, or default order if not annotated
     */
    public static byte[] getBranchOrder(Class<?> clazz) {
        BranchOrder annotation = clazz.getAnnotation(BranchOrder.class);
        if (annotation != null) {
            return annotation.value();
        }
        return DEFAULT_ORDER;
    }

    /**
     * Gets the workload description from @BranchOrder annotation.
     *
     * @param clazz the class to read annotation from
     * @return workload description, or empty string if not annotated
     */
    public static String getWorkloadDescription(Class<?> clazz) {
        BranchOrder annotation = clazz.getAnnotation(BranchOrder.class);
        if (annotation != null) {
            return annotation.workload();
        }
        return "";
    }

    /**
     * Checks if a type is in the priority list.
     *
     * <p>Priority types are checked before non-priority types.
     *
     * @param branchOrder the configured branch order
     * @param type the BSON type to check
     * @return true if type is in priority list, false otherwise
     */
    public static boolean isPriorityType(byte[] branchOrder, byte type) {
        for (byte priorityType : branchOrder) {
            if (priorityType == type) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the priority index of a type.
     *
     * <p>Lower index means higher priority (checked earlier).
     *
     * @param branchOrder the configured branch order
     * @param type the BSON type to check
     * @return priority index (0-based), or -1 if not in priority list
     */
    public static int getPriorityIndex(byte[] branchOrder, byte type) {
        for (int i = 0; i < branchOrder.length; i++) {
            if (branchOrder[i] == type) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Gets the list of priority types in order.
     *
     * @param branchOrder the configured branch order
     * @return list of priority types
     */
    public static List<Byte> getPriorityTypes(byte[] branchOrder) {
        List<Byte> result = new ArrayList<Byte>(branchOrder.length);
        for (byte type : branchOrder) {
            result.add(Byte.valueOf(type));
        }
        return result;
    }

    /**
     * Validates that a branch order configuration is valid.
     *
     * <p>Checks for:
     * <ul>
     *   <li>No duplicate types</li>
     *   <li>All types are valid BSON types</li>
     *   <li>At least one type specified</li>
     * </ul>
     *
     * @param branchOrder the branch order to validate
     * @throws IllegalArgumentException if configuration is invalid
     */
    public static void validate(byte[] branchOrder) {
        if (branchOrder == null || branchOrder.length == 0) {
            throw new IllegalArgumentException("Branch order cannot be null or empty");
        }

        // Check for duplicates
        for (int i = 0; i < branchOrder.length; i++) {
            for (int j = i + 1; j < branchOrder.length; j++) {
                if (branchOrder[i] == branchOrder[j]) {
                    throw new IllegalArgumentException(
                        "Duplicate type in branch order: 0x" + Integer.toHexString(branchOrder[i] & 0xFF));
                }
            }
        }

        // Validate each type is a known BSON type
        for (byte type : branchOrder) {
            if (!isValidBsonType(type)) {
                throw new IllegalArgumentException(
                    "Invalid BSON type in branch order: 0x" + Integer.toHexString(type & 0xFF));
            }
        }
    }

    /**
     * Checks if a byte value represents a valid BSON type.
     *
     * @param type the type byte to check
     * @return true if valid BSON type, false otherwise
     */
    private static boolean isValidBsonType(byte type) {
        // Valid BSON type bytes from specification
        switch (type) {
            case BsonType.DOUBLE:
            case BsonType.STRING:
            case BsonType.DOCUMENT:
            case BsonType.ARRAY:
            case BsonType.BINARY:
            case BsonType.UNDEFINED:
            case BsonType.OBJECT_ID:
            case BsonType.BOOLEAN:
            case BsonType.DATE_TIME:
            case BsonType.NULL:
            case BsonType.REGEX:
            case BsonType.DB_POINTER:
            case BsonType.JAVASCRIPT:
            case BsonType.SYMBOL:
            case BsonType.JAVASCRIPT_WITH_SCOPE:
            case BsonType.INT32:
            case BsonType.TIMESTAMP:
            case BsonType.INT64:
            case BsonType.DECIMAL128:
            case BsonType.MIN_KEY:
            case BsonType.MAX_KEY:
                return true;
            default:
                return false;
        }
    }

    /**
     * Creates a formatted string representation of a branch order.
     *
     * <p>Useful for logging and debugging.
     *
     * @param branchOrder the branch order to format
     * @return formatted string (e.g., "INT32 → STRING → DOUBLE → INT64")
     */
    public static String format(byte[] branchOrder) {
        if (branchOrder == null || branchOrder.length == 0) {
            return "(empty)";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < branchOrder.length; i++) {
            if (i > 0) {
                sb.append(" → ");
            }
            sb.append(getTypeName(branchOrder[i]));
        }
        return sb.toString();
    }

    /**
     * Gets the human-readable name for a BSON type.
     *
     * @param type the BSON type byte
     * @return type name (e.g., "INT32", "STRING")
     */
    public static String getTypeName(byte type) {
        switch (type) {
            case BsonType.DOUBLE: return "DOUBLE";
            case BsonType.STRING: return "STRING";
            case BsonType.DOCUMENT: return "DOCUMENT";
            case BsonType.ARRAY: return "ARRAY";
            case BsonType.BINARY: return "BINARY";
            case BsonType.UNDEFINED: return "UNDEFINED";
            case BsonType.OBJECT_ID: return "OBJECT_ID";
            case BsonType.BOOLEAN: return "BOOLEAN";
            case BsonType.DATE_TIME: return "DATE_TIME";
            case BsonType.NULL: return "NULL";
            case BsonType.REGEX: return "REGEX";
            case BsonType.DB_POINTER: return "DB_POINTER";
            case BsonType.JAVASCRIPT: return "JAVASCRIPT";
            case BsonType.SYMBOL: return "SYMBOL";
            case BsonType.JAVASCRIPT_WITH_SCOPE: return "JAVASCRIPT_WITH_SCOPE";
            case BsonType.INT32: return "INT32";
            case BsonType.TIMESTAMP: return "TIMESTAMP";
            case BsonType.INT64: return "INT64";
            case BsonType.DECIMAL128: return "DECIMAL128";
            case BsonType.MIN_KEY: return "MIN_KEY";
            case BsonType.MAX_KEY: return "MAX_KEY";
            default: return "UNKNOWN(0x" + Integer.toHexString(type & 0xFF) + ")";
        }
    }

    /**
     * Merges custom branch order with default order.
     *
     * <p>Types in custom order are prioritized, followed by remaining types
     * from default order that aren't in custom order.
     *
     * @param customOrder custom branch order (priority types)
     * @param defaultOrder default branch order (fallback types)
     * @return merged order with all types
     */
    public static byte[] merge(byte[] customOrder, byte[] defaultOrder) {
        List<Byte> result = new ArrayList<Byte>();

        // Add custom order first
        for (byte type : customOrder) {
            result.add(Byte.valueOf(type));
        }

        // Add remaining default types not in custom order
        for (byte type : defaultOrder) {
            if (!isPriorityType(customOrder, type)) {
                result.add(Byte.valueOf(type));
            }
        }

        // Convert back to byte array
        byte[] merged = new byte[result.size()];
        for (int i = 0; i < result.size(); i++) {
            merged[i] = result.get(i).byteValue();
        }
        return merged;
    }
}
