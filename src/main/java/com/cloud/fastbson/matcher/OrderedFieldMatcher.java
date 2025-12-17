package com.cloud.fastbson.matcher;

import com.cloud.fastbson.util.StringPool;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Ordered field matcher that uses field order hints for O(1) matching optimization.
 *
 * <p>This matcher assumes documents have stable field order and uses position-based
 * matching for fast path, falling back to linear search when order doesn't match.
 *
 * <p><b>Performance:</b>
 * <ul>
 *   <li>Fast path (order matches): O(1) position check + reference equality</li>
 *   <li>Slow path (order mismatch): O(n) linear search (same as FieldMatcher)</li>
 *   <li>Expected improvement: 10-20% overall, 60-90% in matching stage</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * // Target fields: ["name", "email", "city"]
 * // Expected order: ["_id", "name", "age", "email", "phone", "city", "country"]
 * OrderedFieldMatcher matcher = new OrderedFieldMatcher(
 *     new String[]{"name", "email", "city"},
 *     new String[]{"_id", "name", "age", "email", "phone", "city", "country"}
 * );
 *
 * // During parsing:
 * // Position 0: "_id" -> not target, skip
 * // Position 1: "name" -> matches expected position, O(1) match!
 * // Position 2: "age" -> not target, skip
 * // Position 3: "email" -> matches expected position, O(1) match!
 * // ...
 * }</pre>
 *
 * <p><b>Fallback behavior:</b>
 * When field order doesn't match (e.g., extra fields, missing fields, reordered),
 * the matcher gracefully falls back to linear search with no errors.
 *
 * @author FastBSON
 * @since Phase 3.4
 */
public class OrderedFieldMatcher {

    /**
     * Target fields to extract (interned for reference equality).
     */
    private final String[] targetFields;

    /**
     * Expected field order in BSON documents (interned for reference equality).
     */
    private final String[] expectedFieldOrder;

    /**
     * Map from target field name to target field index.
     * Used for fast lookup during linear search fallback.
     */
    private final Map<String, Integer> targetFieldIndexMap;

    /**
     * Map from expected position to target field index.
     * Used for O(1) position-based matching.
     *
     * <p>Example:
     * <pre>
     * targetFields = ["name", "email", "city"]
     * expectedFieldOrder = ["_id", "name", "age", "email", "phone", "city", "country"]
     *
     * positionToTargetIndex:
     *   position 1 ("name") -> target index 0
     *   position 3 ("email") -> target index 1
     *   position 5 ("city") -> target index 2
     * </pre>
     */
    private final int[] positionToTargetIndex;

    /**
     * Current document position during parsing.
     * Reset to 0 at the start of each document parse.
     */
    private int currentPosition;

    /**
     * Index of next expected target field.
     * Used to check if we're still on the fast path.
     */
    private int nextTargetIndex;

    /**
     * Number of target fields.
     */
    private final int targetFieldCount;

    /**
     * Statistics: fast path hit count (for monitoring).
     */
    private int fastPathHits;

    /**
     * Statistics: slow path fallback count (for monitoring).
     */
    private int slowPathFallbacks;

    /**
     * Constructs an ordered field matcher with target fields and expected field order.
     *
     * @param targetFields Target fields to extract
     * @param expectedFieldOrder Expected field order in BSON documents
     * @throws IllegalArgumentException if targetFields or expectedFieldOrder is null/empty
     */
    public OrderedFieldMatcher(String[] targetFields, String[] expectedFieldOrder) {
        if (targetFields == null || targetFields.length == 0) {
            throw new IllegalArgumentException("Target fields cannot be null or empty");
        }
        if (expectedFieldOrder == null || expectedFieldOrder.length == 0) {
            throw new IllegalArgumentException("Expected field order cannot be null or empty");
        }

        this.targetFieldCount = targetFields.length;
        this.targetFields = new String[targetFieldCount];
        this.targetFieldIndexMap = new HashMap<String, Integer>((int)(targetFieldCount / 0.75) + 1);

        // Intern target fields and build index map
        for (int i = 0; i < targetFieldCount; i++) {
            String interned = StringPool.intern(targetFields[i]);
            this.targetFields[i] = interned;
            this.targetFieldIndexMap.put(interned, Integer.valueOf(i));
        }

        // Intern expected field order
        this.expectedFieldOrder = new String[expectedFieldOrder.length];
        for (int i = 0; i < expectedFieldOrder.length; i++) {
            this.expectedFieldOrder[i] = StringPool.intern(expectedFieldOrder[i]);
        }

        // Build position-to-target-index mapping
        this.positionToTargetIndex = new int[expectedFieldOrder.length];
        Arrays.fill(this.positionToTargetIndex, -1); // -1 means not a target field

        for (int pos = 0; pos < expectedFieldOrder.length; pos++) {
            Integer targetIdx = targetFieldIndexMap.get(this.expectedFieldOrder[pos]);
            if (targetIdx != null) {
                this.positionToTargetIndex[pos] = targetIdx.intValue();
            }
        }

        // Initialize parsing state
        this.currentPosition = 0;
        this.nextTargetIndex = 0;
        this.fastPathHits = 0;
        this.slowPathFallbacks = 0;
    }

    /**
     * Constructs an ordered field matcher with target field set and expected field order.
     *
     * @param targetFields Target fields to extract
     * @param expectedFieldOrder Expected field order in BSON documents
     */
    public OrderedFieldMatcher(Set<String> targetFields, String[] expectedFieldOrder) {
        this(targetFields.toArray(new String[0]), expectedFieldOrder);
    }

    /**
     * Resets the matcher state for parsing a new document.
     *
     * <p><b>IMPORTANT:</b> Call this method before parsing each new document
     * to reset position tracking.
     */
    public void reset() {
        this.currentPosition = 0;
        this.nextTargetIndex = 0;
    }

    /**
     * Matches a field name and returns true if it's a target field.
     *
     * <p>This method uses position-based optimization:
     * <ol>
     *   <li>Check if current position is within expected order bounds</li>
     *   <li>If yes, check if field at this position is a target field</li>
     *   <li>If yes, use reference equality (==) for O(1) match</li>
     *   <li>If no match, fall back to linear search</li>
     * </ol>
     *
     * @param fieldName Field name from BSON document
     * @return true if field is a target field, false otherwise
     */
    public boolean matches(String fieldName) {
        if (fieldName == null) {
            return false;
        }

        // Intern field name for reference equality comparison
        String internedFieldName = StringPool.intern(fieldName);

        // Fast path: check if current position matches expected order
        if (currentPosition < expectedFieldOrder.length) {
            // Check if expected field at this position is a target field
            int targetIdx = positionToTargetIndex[currentPosition];

            if (targetIdx != -1) {
                // This position should have a target field
                // Check if actual field matches expected field using reference equality
                if (internedFieldName == expectedFieldOrder[currentPosition]) {
                    // Perfect match! O(1) fast path
                    currentPosition++;
                    nextTargetIndex = targetIdx + 1;
                    fastPathHits++;
                    return true;
                }
            } else {
                // This position is not a target field
                // Check if actual field matches expected field (still on ordered path)
                if (internedFieldName == expectedFieldOrder[currentPosition]) {
                    // Field order still matches, just not a target field
                    currentPosition++;
                    return false;
                }
            }
        }

        // Slow path: field order doesn't match, fall back to linear search
        currentPosition++; // Still increment position to track where we are
        slowPathFallbacks++;
        return linearSearch(internedFieldName);
    }

    /**
     * Linear search fallback when field order doesn't match.
     *
     * @param internedFieldName Interned field name
     * @return true if field is a target field, false otherwise
     */
    private boolean linearSearch(String internedFieldName) {
        // Use reference equality since all fields are interned
        for (String target : targetFields) {
            if (target == internedFieldName) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the number of target fields.
     *
     * @return Number of target fields
     */
    public int getTargetFieldCount() {
        return targetFieldCount;
    }

    /**
     * Gets fast path hit count (for monitoring).
     *
     * @return Number of times fast path was used
     */
    public int getFastPathHits() {
        return fastPathHits;
    }

    /**
     * Gets slow path fallback count (for monitoring).
     *
     * @return Number of times slow path was used
     */
    public int getSlowPathFallbacks() {
        return slowPathFallbacks;
    }

    /**
     * Gets fast path hit rate (for monitoring).
     *
     * @return Fast path hit rate (0.0 to 1.0)
     */
    public double getFastPathHitRate() {
        int total = fastPathHits + slowPathFallbacks;
        return total == 0 ? 0.0 : (double) fastPathHits / total;
    }

    /**
     * Resets statistics.
     */
    public void resetStatistics() {
        this.fastPathHits = 0;
        this.slowPathFallbacks = 0;
    }
}
