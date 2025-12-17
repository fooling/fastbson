package com.cloud.fastbson.schema;

import com.cloud.fastbson.annotation.BsonField;
import com.cloud.fastbson.annotation.BsonSchema;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Schema introspector that extracts BSON field order from Java classes.
 *
 * <p>This class uses reflection to read @BsonField annotations and build
 * a cached field order array for optimization.
 *
 * <p><b>Caching:</b> Field order is extracted once per class and cached
 * in a namespace-level cache (ConcurrentHashMap keyed by Class).
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * @BsonSchema("User")
 * public class UserEntity {
 *     @BsonField(value = "_id", order = 1)
 *     private String id;
 *
 *     @BsonField(value = "name", order = 2)
 *     private String name;
 * }
 *
 * // Extract field order
 * String[] fieldOrder = SchemaIntrospector.getFieldOrder(UserEntity.class);
 * // Result: ["_id", "name"]
 * }</pre>
 *
 * @author FastBSON
 * @since Phase 3.4
 */
public final class SchemaIntrospector {

    /**
     * Namespace-level cache: Class -> Field order array.
     *
     * <p>Thread-safe for concurrent access.
     */
    private static final Map<Class<?>, String[]> CLASS_SCHEMA_CACHE =
        new ConcurrentHashMap<Class<?>, String[]>();

    /**
     * Gets the field order for a schema class.
     *
     * <p>The field order is extracted from @BsonField annotations and cached.
     * Subsequent calls for the same class return the cached result.
     *
     * @param schemaClass Schema class with @BsonField annotations
     * @return Field order array, or null if no @BsonField annotations found
     */
    public static String[] getFieldOrder(Class<?> schemaClass) {
        if (schemaClass == null) {
            return null;
        }
        return CLASS_SCHEMA_CACHE.computeIfAbsent(schemaClass,
            SchemaIntrospector::extractFieldOrder);
    }

    /**
     * Gets the schema name for a class.
     *
     * <p>Priority:
     * <ol>
     *   <li>@BsonSchema(value = "CustomName")</li>
     *   <li>Simple class name (e.g., "UserEntity" -> "UserEntity")</li>
     * </ol>
     *
     * @param schemaClass Schema class
     * @return Schema name
     */
    public static String getSchemaName(Class<?> schemaClass) {
        if (schemaClass == null) {
            return null;
        }

        // Check @BsonSchema annotation
        BsonSchema annotation = schemaClass.getAnnotation(BsonSchema.class);
        if (annotation != null && !annotation.value().isEmpty()) {
            return annotation.value();
        }

        // Default to simple class name
        return schemaClass.getSimpleName();
    }

    /**
     * Checks if a class has schema metadata (at least one @BsonField annotation).
     *
     * @param schemaClass Schema class
     * @return true if class has @BsonField annotations, false otherwise
     */
    public static boolean hasSchemaMetadata(Class<?> schemaClass) {
        if (schemaClass == null) {
            return false;
        }

        // Check if any field has @BsonField annotation
        Field[] fields = schemaClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(BsonField.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Clears the schema cache for a specific class.
     *
     * @param schemaClass Schema class to clear
     */
    public static void clearCache(Class<?> schemaClass) {
        if (schemaClass != null) {
            CLASS_SCHEMA_CACHE.remove(schemaClass);
        }
    }

    /**
     * Clears all cached schemas.
     */
    public static void clearAllCache() {
        CLASS_SCHEMA_CACHE.clear();
    }

    /**
     * Gets the number of cached schemas.
     *
     * @return Number of cached schemas
     */
    public static int getCacheSize() {
        return CLASS_SCHEMA_CACHE.size();
    }

    /**
     * Extracts field order from a class using reflection.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Find all fields with @BsonField annotation</li>
     *   <li>Sort by order value (ascending)</li>
     *   <li>Fields with order=-1 are placed last in declaration order</li>
     *   <li>Extract BSON field names</li>
     * </ol>
     *
     * @param schemaClass Schema class
     * @return Field order array, or empty array if no annotations found
     */
    private static String[] extractFieldOrder(Class<?> schemaClass) {
        // Collect all fields with @BsonField annotation
        List<FieldInfo> fieldInfos = new ArrayList<FieldInfo>();

        Field[] fields = schemaClass.getDeclaredFields();
        for (Field field : fields) {
            BsonField annotation = field.getAnnotation(BsonField.class);
            if (annotation != null) {
                String bsonFieldName = annotation.value().isEmpty()
                    ? field.getName()
                    : annotation.value();
                int order = annotation.order();

                fieldInfos.add(new FieldInfo(bsonFieldName, order));
            }
        }

        // If no @BsonField annotations found, return empty array
        if (fieldInfos.isEmpty()) {
            return new String[0];
        }

        // Sort by order (fields with order=-1 go last)
        fieldInfos.sort(new Comparator<FieldInfo>() {
            @Override
            public int compare(FieldInfo f1, FieldInfo f2) {
                // Fields with order=-1 go last
                if (f1.order == -1 && f2.order == -1) {
                    return 0; // Keep declaration order
                }
                if (f1.order == -1) {
                    return 1; // f1 after f2
                }
                if (f2.order == -1) {
                    return -1; // f2 after f1
                }
                // Both have explicit order, compare numerically
                return Integer.compare(f1.order, f2.order);
            }
        });

        // Extract field names
        String[] result = new String[fieldInfos.size()];
        for (int i = 0; i < fieldInfos.size(); i++) {
            result[i] = fieldInfos.get(i).name;
        }

        return result;
    }

    /**
     * Helper class to hold field info during sorting.
     */
    private static class FieldInfo {
        final String name;
        final int order;

        FieldInfo(String name, int order) {
            this.name = name;
            this.order = order;
        }
    }

    // Private constructor to prevent instantiation
    private SchemaIntrospector() {
        throw new AssertionError("SchemaIntrospector is a utility class and should not be instantiated");
    }
}
