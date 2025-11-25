package com.cloud.fastbson.types;

/**
 * Represents BSON MinKey.
 *
 * <p>MinKey is a special BSON type that always compares less than any other BSON value.
 * Uses singleton pattern to avoid allocations.
 */
public class MinKey {
    public static final MinKey INSTANCE = new MinKey();

    private MinKey() {
        // Private constructor for singleton
    }

    @Override
    public String toString() {
        return "MinKey";
    }
}
