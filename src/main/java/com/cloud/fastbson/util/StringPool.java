package com.cloud.fastbson.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Global string interning pool for BSON field names.
 *
 * <p>This pool reduces memory usage and improves comparison performance
 * by ensuring that identical field names reference the same String object.
 * Interned strings can be compared using {@code ==} instead of {@code equals()},
 * which is significantly faster (O(1) reference comparison vs O(n) character comparison).
 *
 * <p><b>Thread-safe:</b> Uses {@link ConcurrentHashMap} for concurrent access
 * from multiple parsing threads.
 *
 * <p><b>Memory-safe:</b> Pool size can be monitored via {@link #getPoolSize()}
 * and cleared if needed via {@link #clear()}.
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * String fieldName = StringPool.intern("name");
 * String sameName = StringPool.intern("name");
 * assert fieldName == sameName;  // Same reference
 * }</pre>
 *
 * @since 0.0.3
 */
public final class StringPool {

    /**
     * Global string pool (thread-safe).
     * Initial capacity of 256 is based on typical BSON schema size.
     */
    private static final ConcurrentHashMap<String, String> POOL =
        new ConcurrentHashMap<String, String>(256);

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private StringPool() {
        throw new AssertionError("Cannot instantiate StringPool utility class");
    }

    /**
     * Interns a string in the global pool.
     *
     * <p>If the string already exists in the pool, returns the pooled instance.
     * Otherwise, adds the string to the pool and returns it.
     *
     * <p>This enables reference equality ({@code ==}) comparisons instead of
     * value equality ({@code equals()}), improving performance.
     *
     * <p><b>Performance:</b> O(1) average case (ConcurrentHashMap lookup)
     *
     * @param str the string to intern (must not be null)
     * @return the interned string (same reference for equal strings)
     * @throws IllegalArgumentException if str is null
     */
    public static String intern(String str) {
        if (str == null) {
            throw new IllegalArgumentException("Cannot intern null string");
        }

        // Try to get from pool first (fast path)
        String interned = POOL.get(str);
        if (interned != null) {
            return interned;
        }

        // Not in pool, add it (slow path)
        // Use putIfAbsent to handle race conditions in multi-threaded scenarios
        POOL.putIfAbsent(str, str);

        // Return the pooled instance (may be from putIfAbsent or another thread)
        return POOL.get(str);
    }

    /**
     * Returns the current pool size.
     *
     * <p>This can be used for monitoring memory usage and pool efficiency.
     * A growing pool size indicates new field names being encountered.
     *
     * @return number of interned strings in the pool
     */
    public static int getPoolSize() {
        return POOL.size();
    }

    /**
     * Clears the string pool, removing all interned strings.
     *
     * <p><b>Usage:</b> Primarily for testing or memory cleanup.
     *
     * <p><b>Warning:</b> Should not be called in production during active parsing,
     * as it will cause previously interned strings to no longer share references.
     * Use this method carefully and only when no parsing is in progress.
     */
    public static void clear() {
        POOL.clear();
    }
}
