package com.cloud.fastbson.util;

import com.cloud.fastbson.reader.BsonReader;

import java.util.HashMap;

/**
 * ThreadLocal object pool for frequently allocated objects.
 *
 * <p>Reduces object allocation and GC pressure by reusing objects within the same thread.
 * This pool manages:
 * <ul>
 *   <li>BsonReader instances - for BSON parsing</li>
 *   <li>HashMap instances - for document results</li>
 *   <li>StringBuilder instances - for string building</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> Uses ThreadLocal, each thread has its own pool.
 * No synchronization needed.
 *
 * <p><b>Object Leakage Protection:</b> Returned Map instances are NOT pooled
 * (caller owns them). Only internal temporary objects are pooled.
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * BsonReader reader = ObjectPool.borrowReader(bsonData);
 * HashMap<String, Object> result = ObjectPool.borrowMap();
 * // ... use reader and result
 * // reader is auto-returned to pool on next borrow
 * // result is owned by caller, not returned to pool
 * }</pre>
 *
 * @since 0.0.3
 */
public final class ObjectPool {

    /**
     * BsonReader pool - one instance per thread.
     * Reset with new data on each borrow.
     */
    private static final ThreadLocal<BsonReader> READER_POOL =
        ThreadLocal.withInitial(() -> new BsonReader(new byte[0]));

    /**
     * HashMap pool for document results - one instance per thread.
     * Cleared on each borrow.
     * Initial capacity of 32 to reduce rehashing for typical documents.
     */
    private static final ThreadLocal<HashMap<String, Object>> MAP_POOL =
        ThreadLocal.withInitial(() -> new HashMap<String, Object>(32));

    /**
     * StringBuilder pool for string operations - one instance per thread.
     * Cleared on each borrow.
     * Initial capacity of 256 for typical string sizes.
     */
    private static final ThreadLocal<StringBuilder> STRING_BUILDER_POOL =
        ThreadLocal.withInitial(() -> new StringBuilder(256));

    /**
     * Private constructor to prevent instantiation.
     */
    private ObjectPool() {
        throw new AssertionError("Cannot instantiate ObjectPool utility class");
    }

    /**
     * Borrows a BsonReader from the pool, reset with new data.
     *
     * <p>The reader is automatically reset to the provided data,
     * so it's ready to use immediately.
     *
     * <p><b>Note:</b> The borrowed reader is NOT explicitly returned.
     * It will be reused on the next borrow call from the same thread.
     *
     * @param data the BSON data to parse (must not be null)
     * @return a BsonReader instance ready to parse the data
     * @throws IllegalArgumentException if data is null
     */
    public static BsonReader borrowReader(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        BsonReader reader = READER_POOL.get();
        reader.reset(data);
        return reader;
    }

    /**
     * Borrows a HashMap from the pool, cleared and ready to use.
     *
     * <p>The map is cleared before being returned, so it contains no entries.
     *
     * <p><b>Important:</b> The borrowed map should be used as a temporary
     * working buffer. If you need to return the map to the caller, create
     * a new map and copy the data, or use the map directly (it won't be
     * returned to the pool).
     *
     * @return an empty HashMap instance
     */
    public static HashMap<String, Object> borrowMap() {
        HashMap<String, Object> map = MAP_POOL.get();
        map.clear();
        return map;
    }

    /**
     * Borrows a StringBuilder from the pool, cleared and ready to use.
     *
     * <p>The builder is cleared before being returned (length set to 0).
     *
     * <p><b>Note:</b> After using the builder, call toString() to get the
     * result. The builder will be reused on the next borrow.
     *
     * @return an empty StringBuilder instance
     */
    public static StringBuilder borrowStringBuilder() {
        StringBuilder sb = STRING_BUILDER_POOL.get();
        sb.setLength(0);
        return sb;
    }

    /**
     * Clears all thread-local pools for the current thread.
     *
     * <p>This can be used for testing or to free memory when a thread
     * is no longer using the pool.
     *
     * <p><b>Warning:</b> After calling this, the next borrow will
     * create new objects.
     */
    public static void clearCurrentThreadPools() {
        READER_POOL.remove();
        MAP_POOL.remove();
        STRING_BUILDER_POOL.remove();
    }

    /**
     * Gets the BsonReader instance for the current thread without resetting.
     *
     * <p>This is primarily for testing to verify object reuse.
     *
     * @return the current thread's BsonReader instance
     */
    static BsonReader getCurrentThreadReader() {
        return READER_POOL.get();
    }

    /**
     * Gets the HashMap instance for the current thread without clearing.
     *
     * <p>This is primarily for testing to verify object reuse.
     *
     * @return the current thread's HashMap instance
     */
    static HashMap<String, Object> getCurrentThreadMap() {
        return MAP_POOL.get();
    }

    /**
     * Gets the StringBuilder instance for the current thread without clearing.
     *
     * <p>This is primarily for testing to verify object reuse.
     *
     * @return the current thread's StringBuilder instance
     */
    static StringBuilder getCurrentThreadStringBuilder() {
        return STRING_BUILDER_POOL.get();
    }
}
