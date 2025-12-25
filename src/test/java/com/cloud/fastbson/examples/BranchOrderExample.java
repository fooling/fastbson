package com.cloud.fastbson.examples;

import com.cloud.fastbson.annotation.BranchOrder;
import com.cloud.fastbson.util.BsonType;
import com.cloud.fastbson.util.BranchOrderHelper;

/**
 * Examples demonstrating @BranchOrder annotation for CPU branch prediction optimization.
 *
 * <p>The @BranchOrder annotation allows you to customize the order in which BSON types
 * are checked during parsing, optimizing CPU branch prediction for your specific workload.
 *
 * <h2>When to Use @BranchOrder</h2>
 * <ul>
 *   <li><b>Use it</b> when your data has skewed type distribution (>40% of one type)</li>
 *   <li><b>Use it</b> when profiling shows branch misprediction overhead</li>
 *   <li><b>Don't use it</b> for uniform type distribution (default order is optimal)</li>
 *   <li><b>Don't use it</b> unless you've measured actual performance impact</li>
 * </ul>
 *
 * <h2>Performance Impact</h2>
 * <ul>
 *   <li>Typical improvement: 2-5% for skewed workloads</li>
 *   <li>Best case: 8-10% for extreme skew (80%+ single type)</li>
 *   <li>No impact: 0% for uniform distribution or well-predicted branches</li>
 * </ul>
 *
 * @author FastBSON
 * @since Phase 3.5
 */
public class BranchOrderExample {

    // ========================================================================
    // Example 1: Time-Series Data (Timestamps + Measurements)
    // ========================================================================

    /**
     * Time-series workload with 60% INT64 timestamps and 30% DOUBLE values.
     *
     * <p><b>Type Distribution:</b>
     * <ul>
     *   <li>INT64 (timestamps): 60%</li>
     *   <li>DOUBLE (sensor values): 30%</li>
     *   <li>STRING (sensor IDs): 5%</li>
     *   <li>INT32 (status codes): 5%</li>
     * </ul>
     *
     * <p><b>Performance Gain:</b> ~3-5% compared to default order
     */
    @BranchOrder({
        BsonType.INT64,    // Check timestamps first (60% hit rate)
        BsonType.DOUBLE,   // Then sensor values (30% hit rate)
        BsonType.STRING,   // Then IDs (5% hit rate)
        BsonType.INT32     // Then status (5% hit rate)
    })
    static class TimeSeriesParser {
        // Implementation would use this branch order in parsing logic
    }

    public static void example1_TimeSeriesData() {
        // Read the configured branch order
        byte[] order = BranchOrderHelper.getBranchOrder(TimeSeriesParser.class);

        System.out.println("Time-Series Branch Order:");
        System.out.println("  " + BranchOrderHelper.format(order));
        System.out.println();

        // Output: INT64 → DOUBLE → STRING → INT32

        // In parsing code, check types in this order:
        // 1. First check INT64 (60% of fields match here)
        // 2. Then check DOUBLE (30% of remaining fields)
        // 3. Then check STRING (5%)
        // 4. Finally check INT32 (5%)
    }

    // ========================================================================
    // Example 2: Web API Documents (String-Heavy)
    // ========================================================================

    /**
     * Web API workload with 50% STRING fields and 25% INT32 IDs.
     *
     * <p><b>Type Distribution:</b>
     * <ul>
     *   <li>STRING (names, descriptions): 50%</li>
     *   <li>INT32 (IDs, counts): 25%</li>
     *   <li>BOOLEAN (flags): 15%</li>
     *   <li>DOUBLE (ratings): 10%</li>
     * </ul>
     *
     * <p><b>Performance Gain:</b> ~4-6% compared to default order
     */
    @BranchOrder(
        value = {
            BsonType.STRING,   // Check strings first (50% hit rate)
            BsonType.INT32,    // Then integers (25% hit rate)
            BsonType.BOOLEAN,  // Then booleans (15% hit rate)
            BsonType.DOUBLE    // Finally doubles (10% hit rate)
        },
        workload = "Web API documents with text-heavy content"
    )
    static class WebApiParser {
    }

    public static void example2_WebApiDocuments() {
        byte[] order = BranchOrderHelper.getBranchOrder(WebApiParser.class);
        String workload = BranchOrderHelper.getWorkloadDescription(WebApiParser.class);

        System.out.println("Web API Branch Order:");
        System.out.println("  " + BranchOrderHelper.format(order));
        System.out.println("  Workload: " + workload);
        System.out.println();

        // Output:
        // STRING → INT32 → BOOLEAN → DOUBLE
        // Workload: Web API documents with text-heavy content
    }

    // ========================================================================
    // Example 3: Analytics/Metrics (Numeric-Heavy)
    // ========================================================================

    /**
     * Analytics workload with 70% numeric types (DOUBLE + INT32 + INT64).
     *
     * <p><b>Type Distribution:</b>
     * <ul>
     *   <li>DOUBLE (metrics): 45%</li>
     *   <li>INT32 (counters): 20%</li>
     *   <li>INT64 (large numbers): 15%</li>
     *   <li>STRING (labels): 20%</li>
     * </ul>
     *
     * <p><b>Performance Gain:</b> ~3-4% compared to default order
     */
    @BranchOrder({
        BsonType.DOUBLE,   // Metrics first (45%)
        BsonType.INT32,    // Counters second (20%)
        BsonType.STRING,   // Labels third (20%)
        BsonType.INT64     // Large numbers last (15%)
    })
    static class AnalyticsParser {
    }

    public static void example3_AnalyticsMetrics() {
        byte[] order = BranchOrderHelper.getBranchOrder(AnalyticsParser.class);

        System.out.println("Analytics Branch Order:");
        System.out.println("  " + BranchOrderHelper.format(order));
        System.out.println();
    }

    // ========================================================================
    // Example 4: E-Commerce Orders (Mixed Types)
    // ========================================================================

    /**
     * E-commerce workload with balanced type distribution.
     *
     * <p><b>Type Distribution:</b>
     * <ul>
     *   <li>INT32 (product IDs, quantities): 30%</li>
     *   <li>DOUBLE (prices, totals): 25%</li>
     *   <li>STRING (product names): 25%</li>
     *   <li>BOOLEAN (flags): 10%</li>
     *   <li>INT64 (timestamps): 10%</li>
     * </ul>
     *
     * <p><b>Note:</b> For relatively balanced distributions like this,
     * the default order may be sufficient. Only use custom order if
     * profiling shows branch misprediction overhead.
     */
    @BranchOrder({
        BsonType.INT32,    // Product IDs first (30%)
        BsonType.DOUBLE,   // Prices second (25%)
        BsonType.STRING,   // Product names third (25%)
        BsonType.BOOLEAN,  // Flags fourth (10%)
        BsonType.INT64     // Timestamps last (10%)
    })
    static class ECommerceParser {
    }

    public static void example4_ECommerceOrders() {
        byte[] order = BranchOrderHelper.getBranchOrder(ECommerceParser.class);

        System.out.println("E-Commerce Branch Order:");
        System.out.println("  " + BranchOrderHelper.format(order));
        System.out.println("  Note: For balanced distributions, custom order may not help much");
        System.out.println();
    }

    // ========================================================================
    // Example 5: How to Determine Optimal Order
    // ========================================================================

    /**
     * Step-by-step guide to finding the optimal branch order for your workload.
     */
    public static void example5_HowToDetermineOrder() {
        System.out.println("=== How to Determine Optimal Branch Order ===\n");

        System.out.println("Step 1: Profile Your Data");
        System.out.println("  - Sample 10,000+ documents from your production data");
        System.out.println("  - Count BSON type frequencies for each field");
        System.out.println("  - Calculate percentage distribution\n");

        System.out.println("Step 2: Identify Skew");
        System.out.println("  - If top type > 40%: Custom order will likely help");
        System.out.println("  - If top type < 30%: Default order is probably fine");
        System.out.println("  - If distribution is uniform: Skip custom order\n");

        System.out.println("Step 3: Create Custom Order");
        System.out.println("  - Sort types by frequency (highest first)");
        System.out.println("  - Include top 3-5 types in @BranchOrder");
        System.out.println("  - Remaining types use default order\n");

        System.out.println("Step 4: Benchmark");
        System.out.println("  - Run before/after benchmarks with real data");
        System.out.println("  - Measure actual performance impact");
        System.out.println("  - Expected gain: 2-5% for skewed workloads\n");

        System.out.println("Step 5: Validate");
        System.out.println("  - Run full test suite to ensure correctness");
        System.out.println("  - Monitor production metrics after deployment");
        System.out.println();
    }

    // ========================================================================
    // Example 6: Using BranchOrderHelper Utilities
    // ========================================================================

    public static void example6_UtilityMethods() {
        System.out.println("=== BranchOrderHelper Utility Methods ===\n");

        // Get branch order from annotated class
        byte[] order = BranchOrderHelper.getBranchOrder(TimeSeriesParser.class);
        System.out.println("Branch order: " + BranchOrderHelper.format(order));

        // Check if type is priority
        boolean isInt64Priority = BranchOrderHelper.isPriorityType(order, BsonType.INT64);
        System.out.println("Is INT64 priority? " + isInt64Priority);  // true

        // Get priority index
        int index = BranchOrderHelper.getPriorityIndex(order, BsonType.DOUBLE);
        System.out.println("DOUBLE priority index: " + index);  // 1 (second)

        // Validate configuration
        try {
            BranchOrderHelper.validate(order);
            System.out.println("Configuration is valid");
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid configuration: " + e.getMessage());
        }

        // Format for logging
        String formatted = BranchOrderHelper.format(order);
        System.out.println("Formatted: " + formatted);
        System.out.println();
    }

    // ========================================================================
    // Example 7: Default Order vs Custom Order Comparison
    // ========================================================================

    public static void example7_DefaultVsCustom() {
        System.out.println("=== Default Order vs Custom Order ===\n");

        // Default order (optimized for general documents)
        byte[] defaultOrder = BranchOrderHelper.DEFAULT_ORDER;
        System.out.println("Default Order (General Documents):");
        System.out.println("  " + BranchOrderHelper.format(defaultOrder));
        System.out.println("  - INT32: 35% (counters, IDs)");
        System.out.println("  - STRING: 30% (text fields)");
        System.out.println("  - DOUBLE: 15% (floats)");
        System.out.println("  - INT64: 10% (timestamps)");
        System.out.println();

        // Custom order for time-series
        byte[] timeSeriesOrder = BranchOrderHelper.getBranchOrder(TimeSeriesParser.class);
        System.out.println("Custom Order (Time-Series Data):");
        System.out.println("  " + BranchOrderHelper.format(timeSeriesOrder));
        System.out.println("  - INT64: 60% (timestamps dominate)");
        System.out.println("  - DOUBLE: 30% (measurements)");
        System.out.println("  - STRING: 5%");
        System.out.println("  - INT32: 5%");
        System.out.println();

        System.out.println("Expected Performance Impact:");
        System.out.println("  - Time-series with default order: Good baseline");
        System.out.println("  - Time-series with custom order:  +3-5% improvement");
        System.out.println();
    }

    // ========================================================================
    // Example 8: Validation and Error Handling
    // ========================================================================

    public static void example8_Validation() {
        System.out.println("=== Validation Examples ===\n");

        // Valid configuration
        byte[] valid = {BsonType.INT64, BsonType.DOUBLE, BsonType.STRING};
        try {
            BranchOrderHelper.validate(valid);
            System.out.println("✓ Valid configuration");
        } catch (IllegalArgumentException e) {
            System.out.println("✗ " + e.getMessage());
        }

        // Invalid: Duplicate types
        byte[] duplicate = {BsonType.INT64, BsonType.DOUBLE, BsonType.INT64};
        try {
            BranchOrderHelper.validate(duplicate);
            System.out.println("✓ Valid configuration");
        } catch (IllegalArgumentException e) {
            System.out.println("✗ Duplicate type error: " + e.getMessage());
        }

        // Invalid: Unknown type
        byte[] invalid = {BsonType.INT64, (byte) 0xFF, BsonType.STRING};
        try {
            BranchOrderHelper.validate(invalid);
            System.out.println("✓ Valid configuration");
        } catch (IllegalArgumentException e) {
            System.out.println("✗ Invalid type error: " + e.getMessage());
        }

        System.out.println();
    }

    // ========================================================================
    // Main Method - Run All Examples
    // ========================================================================

    public static void main(String[] args) {
        System.out.println("====================================");
        System.out.println("Branch Order Optimization Examples");
        System.out.println("====================================\n");

        example1_TimeSeriesData();
        example2_WebApiDocuments();
        example3_AnalyticsMetrics();
        example4_ECommerceOrders();
        example5_HowToDetermineOrder();
        example6_UtilityMethods();
        example7_DefaultVsCustom();
        example8_Validation();

        System.out.println("====================================");
        System.out.println("Key Takeaways:");
        System.out.println("  1. Profile your data first");
        System.out.println("  2. Only use custom order for skewed distributions");
        System.out.println("  3. Benchmark to verify actual impact (2-5% typical)");
        System.out.println("  4. Default order works well for general cases");
        System.out.println("====================================");
    }
}
