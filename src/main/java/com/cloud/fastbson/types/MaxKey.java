package com.cloud.fastbson.types;

/**
 * Represents BSON MaxKey.
 *
 * <p>MaxKey is a special BSON type that always compares greater than any other BSON value.
 * Uses singleton pattern to avoid allocations.
 */
public class MaxKey {
    public static final MaxKey INSTANCE = new MaxKey();

    private MaxKey() {
        // Private constructor for singleton
    }

    @Override
    public String toString() {
        return "MaxKey";
    }
}
