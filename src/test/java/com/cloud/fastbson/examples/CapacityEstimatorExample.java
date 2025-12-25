package com.cloud.fastbson.examples;

import com.cloud.fastbson.FastBson;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.util.CapacityEstimator;

/**
 * Examples demonstrating CapacityEstimator usage for performance tuning.
 *
 * <p>CapacityEstimator allows you to tune pre-allocation heuristics for your
 * specific business scenarios, improving performance by reducing HashMap/ArrayList
 * rehashing overhead.
 *
 * @author FastBSON
 * @since Phase 3.5
 */
public class CapacityEstimatorExample {

    /**
     * Example 1: Using default capacity estimator (recommended for most cases).
     */
    public static void example1_DefaultEstimator() {
        // Default estimator is automatically used
        // No configuration needed - works well for general BSON documents

        byte[] bsonData = getBsonData();
        BsonDocument doc = FastBson.parse(bsonData);

        // Default heuristics:
        // - Document fields: docLength / 20
        // - Array elements: docLength / 15
        // - Min capacity: 4
        // - Load factor: 0.75
    }

    /**
     * Example 2: Tuning for dense documents (many small fields).
     *
     * <p>Use case: Configuration objects, metadata documents with many boolean/int32 fields.
     */
    public static void example2_DenseDocuments() {
        // Create estimator for dense documents
        CapacityEstimator dense = CapacityEstimator.builder()
            .documentBytesPerField(10)  // Small fields (bool, int32) average 10 bytes
            .arrayBytesPerElement(8)    // Small array elements average 8 bytes
            .minCapacity(16)            // Expect at least 16 fields/elements
            .build();

        // Apply globally
        FastBson.setCapacityEstimator(dense);

        // Now all parsing uses optimized estimation
        byte[] bsonData = getBsonData();
        BsonDocument doc = FastBson.parse(bsonData);

        // Example scenario:
        // Document: { "enabled": true, "maxRetries": 5, "timeout": 30, ... } (50 fields)
        // Before: HashMap allocates for 25 fields, rehashes once
        // After:  HashMap allocates for 50 fields, no rehashing
    }

    /**
     * Example 3: Tuning for sparse documents (few large fields).
     *
     * <p>Use case: Content documents with large strings, blog posts, product descriptions.
     */
    public static void example3_SparseDocuments() {
        // Create estimator for sparse documents
        CapacityEstimator sparse = CapacityEstimator.builder()
            .documentBytesPerField(50)  // Large fields (strings, nested) average 50 bytes
            .minCapacity(2)             // May have as few as 2 fields
            .build();

        FastBson.setCapacityEstimator(sparse);

        byte[] bsonData = getBsonData();
        BsonDocument doc = FastBson.parse(bsonData);

        // Example scenario:
        // Document: { "title": "...", "content": "..." } (2 large strings)
        // Before: Over-allocates capacity for 10 fields
        // After:  Allocates capacity for 2 fields, saves memory
    }

    /**
     * Example 4: Tuning for time-series data (arrays of primitives).
     *
     * <p>Use case: Sensor data, metrics, measurements with int32/double arrays.
     */
    public static void example4_TimeSeriesData() {
        // Create estimator for time-series arrays
        CapacityEstimator timeSeries = CapacityEstimator.builder()
            .arrayBytesPerElement(8)    // Int32/Double primitives are ~8 bytes
            .minCapacity(100)           // Time-series arrays typically have 100+ elements
            .documentBytesPerField(15)  // Few fields per document
            .build();

        FastBson.setCapacityEstimator(timeSeries);

        byte[] bsonData = getBsonData();
        BsonDocument doc = FastBson.parse(bsonData);

        // Example scenario:
        // Document: { "sensorId": 123, "values": [1.5, 2.3, ...] } (1000 doubles)
        // Before: ArrayList resizes multiple times (4 -> 8 -> 16 -> ... -> 1024)
        // After:  ArrayList allocates 1000 capacity immediately
    }

    /**
     * Example 5: Tuning load factor for memory vs collision trade-off.
     *
     * <p>Use case: Memory-constrained environments or collision-sensitive workloads.
     */
    public static void example5_LoadFactorTuning() {
        // Lower load factor: fewer collisions, more memory
        CapacityEstimator lowCollisions = CapacityEstimator.builder()
            .loadFactor(0.5)  // HashMap rehashes at 50% full (vs default 75%)
            .build();

        FastBson.setCapacityEstimator(lowCollisions);

        // Higher load factor: more memory-efficient, more collisions
        CapacityEstimator memoryEfficient = CapacityEstimator.builder()
            .loadFactor(0.9)  // HashMap rehashes at 90% full
            .build();

        // Note: Java's default 0.75 is generally optimal
        // Only tune if profiling shows specific bottlenecks
    }

    /**
     * Example 6: Resetting to default estimator.
     */
    public static void example6_ResetToDefault() {
        // After tuning, you can reset to default
        FastBson.useDefaultCapacityEstimator();

        // Or explicitly set default
        FastBson.setCapacityEstimator(CapacityEstimator.defaults());
    }

    /**
     * Example 7: Querying current estimator settings.
     */
    public static void example7_QuerySettings() {
        CapacityEstimator current = FastBson.getCapacityEstimator();

        int documentBytes = current.getDocumentBytesPerField();
        int arrayBytes = current.getArrayBytesPerElement();
        int minCap = current.getMinCapacity();
        double loadFactor = current.getLoadFactor();

        System.out.println("Current settings:");
        System.out.println("  Document bytes per field: " + documentBytes);
        System.out.println("  Array bytes per element: " + arrayBytes);
        System.out.println("  Min capacity: " + minCap);
        System.out.println("  Load factor: " + loadFactor);
    }

    /**
     * Example 8: Business-specific tuning based on profiling.
     *
     * <p>Recommended workflow:
     * <ol>
     *   <li>Start with default estimator</li>
     *   <li>Profile your application with real data</li>
     *   <li>Measure actual bytes-per-field and bytes-per-element</li>
     *   <li>Tune estimator based on measurements</li>
     *   <li>Benchmark before/after to verify improvement</li>
     * </ol>
     */
    public static void example8_ProfilingBasedTuning() {
        // Step 1: Profile your documents
        // Measure: Total document bytes / field count
        // Result: Your documents average 18 bytes per field

        // Step 2: Create tuned estimator
        CapacityEstimator tuned = CapacityEstimator.builder()
            .documentBytesPerField(18)  // Based on profiling
            .arrayBytesPerElement(12)   // Based on profiling
            .build();

        // Step 3: Apply and benchmark
        FastBson.setCapacityEstimator(tuned);

        // Step 4: Verify with your workload
        // Expect: Fewer HashMap rehashes, ~2-5% performance improvement
    }

    // Helper method to simulate BSON data
    private static byte[] getBsonData() {
        // Placeholder - in real usage, you'd have actual BSON data
        return new byte[1000];
    }

    public static void main(String[] args) {
        System.out.println("=== CapacityEstimator Examples ===\n");

        System.out.println("Example 1: Default estimator");
        example1_DefaultEstimator();

        System.out.println("\nExample 2: Dense documents (many small fields)");
        example2_DenseDocuments();

        System.out.println("\nExample 3: Sparse documents (few large fields)");
        example3_SparseDocuments();

        System.out.println("\nExample 4: Time-series data (arrays of primitives)");
        example4_TimeSeriesData();

        System.out.println("\nExample 5: Load factor tuning");
        example5_LoadFactorTuning();

        System.out.println("\nExample 6: Reset to default");
        example6_ResetToDefault();

        System.out.println("\nExample 7: Query current settings");
        example7_QuerySettings();

        System.out.println("\nExample 8: Profiling-based tuning");
        example8_ProfilingBasedTuning();
    }
}
