package com.cloud.fastbson.util;

import com.cloud.fastbson.FastBson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Schema learner for automatic field order detection and caching.
 *
 * <p>This class enables zero-configuration field order optimization by
 * automatically learning field order from the first document and caching
 * it for subsequent parses.
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * // Enable auto-learning in PartialParser
 * PartialParser parser = new PartialParser("name", "email")
 *     .withSchemaId("User")
 *     .enableAutoLearn();
 *
 * // First parse: learns field order
 * parser.parse(userData1);
 * // Internally: SchemaLearner records ["_id", "name", "age", "email", ...]
 *
 * // Subsequent parses: uses cached order automatically
 * parser.parse(userData2);  // Optimized!
 * parser.parse(userData3);  // Optimized!
 * }</pre>
 *
 * <p><b>Learning phases:</b>
 * <ul>
 *   <li><b>Observation:</b> Record field names in order during first parse</li>
 *   <li><b>Finalization:</b> Convert to array and register to global schema registry</li>
 *   <li><b>Application:</b> Use registered schema for optimized parsing</li>
 * </ul>
 *
 * <p>Thread-safe for concurrent learning and retrieval.
 *
 * @author FastBSON
 * @since Phase 3.4
 */
public final class SchemaLearner {

    /**
     * Global registry of learned schemas.
     *
     * <p>Maps schema ID to LearnedSchema object.
     * Thread-safe for concurrent access.
     */
    private static final Map<String, LearnedSchema> LEARNED_SCHEMAS =
        new ConcurrentHashMap<String, LearnedSchema>();

    /**
     * Represents a schema being learned or already learned.
     */
    static class LearnedSchema {
        /**
         * Observed fields during learning phase (mutable).
         */
        List<String> observedFields;

        /**
         * Finalized field order after learning (immutable).
         */
        String[] fieldOrder;

        /**
         * Whether learning is complete.
         */
        boolean isLearned;

        LearnedSchema() {
            this.observedFields = new ArrayList<String>();
            this.fieldOrder = null;
            this.isLearned = false;
        }
    }

    /**
     * Observes a field during learning phase.
     *
     * <p>This method should be called for each field encountered during
     * the first parse of a document with auto-learning enabled.
     *
     * <p>If learning is already complete for this schema, this method
     * does nothing (idempotent).
     *
     * @param schemaId Schema identifier
     * @param fieldName Field name encountered
     */
    public static void observeField(String schemaId, String fieldName) {
        if (schemaId == null || fieldName == null) {
            return;
        }

        LearnedSchema schema = LEARNED_SCHEMAS.get(schemaId);

        // If schema doesn't exist, create it
        if (schema == null) {
            LearnedSchema newSchema = new LearnedSchema();
            schema = LEARNED_SCHEMAS.putIfAbsent(schemaId, newSchema);
            if (schema == null) {
                schema = newSchema; // Use the one we just created
            }
        }

        // Only record if still in learning phase
        synchronized (schema) {
            if (!schema.isLearned) {
                schema.observedFields.add(fieldName);
            }
        }
    }

    /**
     * Finishes learning for a schema and registers it to global schema registry.
     *
     * <p>This method:
     * <ol>
     *   <li>Converts observed fields to immutable array</li>
     *   <li>Marks schema as learned</li>
     *   <li>Registers to FastBson.SCHEMA_REGISTRY for global use</li>
     *   <li>Releases observation list to free memory</li>
     * </ol>
     *
     * <p>If learning is already complete, this method does nothing (idempotent).
     *
     * @param schemaId Schema identifier
     */
    public static void finishLearning(String schemaId) {
        if (schemaId == null) {
            return;
        }

        LearnedSchema schema = LEARNED_SCHEMAS.get(schemaId);
        if (schema == null) {
            return; // No schema to finish
        }

        synchronized (schema) {
            if (!schema.isLearned) {
                // Convert observed fields to array
                schema.fieldOrder = schema.observedFields.toArray(new String[0]);
                schema.isLearned = true;
                schema.observedFields = null; // Release memory

                // Register to global schema registry
                if (schema.fieldOrder.length > 0) {
                    FastBson.registerSchema(schemaId, schema.fieldOrder);
                }
            }
        }
    }

    /**
     * Gets the learned field order for a schema.
     *
     * @param schemaId Schema identifier
     * @return Learned field order, or null if schema not learned yet
     */
    public static String[] getLearnedFieldOrder(String schemaId) {
        if (schemaId == null) {
            return null;
        }

        LearnedSchema schema = LEARNED_SCHEMAS.get(schemaId);
        if (schema == null) {
            return null;
        }

        synchronized (schema) {
            return schema.isLearned ? schema.fieldOrder : null;
        }
    }

    /**
     * Checks if a schema has finished learning.
     *
     * @param schemaId Schema identifier
     * @return true if schema is learned, false otherwise
     */
    public static boolean isLearned(String schemaId) {
        if (schemaId == null) {
            return false;
        }

        LearnedSchema schema = LEARNED_SCHEMAS.get(schemaId);
        if (schema == null) {
            return false;
        }

        synchronized (schema) {
            return schema.isLearned;
        }
    }

    /**
     * Checks if a schema is currently in learning phase.
     *
     * @param schemaId Schema identifier
     * @return true if schema exists and is learning, false otherwise
     */
    public static boolean isLearning(String schemaId) {
        if (schemaId == null) {
            return false;
        }

        LearnedSchema schema = LEARNED_SCHEMAS.get(schemaId);
        if (schema == null) {
            return false;
        }

        synchronized (schema) {
            return !schema.isLearned && schema.observedFields != null;
        }
    }

    /**
     * Clears a specific learned schema.
     *
     * <p>Useful for resetting learning when schema changes.
     *
     * @param schemaId Schema identifier
     */
    public static void clearSchema(String schemaId) {
        if (schemaId != null) {
            LEARNED_SCHEMAS.remove(schemaId);
        }
    }

    /**
     * Clears all learned schemas.
     *
     * <p>Useful for testing or resetting learning state.
     */
    public static void clearAll() {
        LEARNED_SCHEMAS.clear();
    }

    /**
     * Gets the number of learned schemas.
     *
     * @return Number of schemas (including those in learning phase)
     */
    public static int getSchemaCount() {
        return LEARNED_SCHEMAS.size();
    }

    /**
     * Gets the number of completed learned schemas.
     *
     * @return Number of schemas that have finished learning
     */
    public static int getLearnedSchemaCount() {
        int count = 0;
        for (LearnedSchema schema : LEARNED_SCHEMAS.values()) {
            synchronized (schema) {
                if (schema.isLearned) {
                    count++;
                }
            }
        }
        return count;
    }

    // Private constructor to prevent instantiation
    private SchemaLearner() {
        throw new AssertionError("SchemaLearner is a utility class and should not be instantiated");
    }
}
