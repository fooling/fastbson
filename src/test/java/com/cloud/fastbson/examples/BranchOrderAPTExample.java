package com.cloud.fastbson.examples;

import com.cloud.fastbson.annotation.BranchOrder;
import com.cloud.fastbson.processor.TypeDispatcher;
import com.cloud.fastbson.util.BsonType;

/**
 * Example demonstrating how to use the @BranchOrder annotation with APT
 * (Annotation Processing Tool) for compile-time code generation.
 *
 * <h2>How APT Works</h2>
 * <ol>
 *   <li>You annotate a class/interface with @BranchOrder specifying type priority</li>
 *   <li>During compilation, BranchOrderProcessor reads the annotation</li>
 *   <li>Processor generates an optimized TypeDispatcher implementation</li>
 *   <li>The generated code has switch/if-else ordered by your type priority</li>
 * </ol>
 *
 * <h2>Setup in Your Project</h2>
 * <pre>{@code
 * <!-- In your pom.xml -->
 * <dependencies>
 *     <dependency>
 *         <groupId>com.cloud</groupId>
 *         <artifactId>fastbson</artifactId>
 *         <version>1.0.0-SNAPSHOT</version>
 *     </dependency>
 * </dependencies>
 *
 * <build>
 *     <plugins>
 *         <plugin>
 *             <groupId>org.apache.maven.plugins</groupId>
 *             <artifactId>maven-compiler-plugin</artifactId>
 *             <version>3.11.0</version>
 *             <configuration>
 *                 <annotationProcessorPaths>
 *                     <path>
 *                         <groupId>com.cloud</groupId>
 *                         <artifactId>fastbson</artifactId>
 *                         <version>1.0.0-SNAPSHOT</version>
 *                     </path>
 *                 </annotationProcessorPaths>
 *             </configuration>
 *         </plugin>
 *     </plugins>
 * </build>
 * }</pre>
 *
 * <h2>Generated Code Location</h2>
 * <p>Generated files are placed in {@code target/generated-sources/annotations/}
 *
 * <h2>Example Generated Code</h2>
 * <p>For TimeSeriesDispatcher below, the processor generates:
 * <pre>{@code
 * public final class TimeSeriesDispatcherImpl implements TypeDispatcher {
 *     @Override
 *     public Object dispatch(BsonReader reader, byte type) {
 *         // Priority types first (from @BranchOrder)
 *         if (type == BsonType.INT64) {
 *             return Long.valueOf(reader.readInt64());
 *         } else if (type == BsonType.DOUBLE) {
 *             return Double.valueOf(reader.readDouble());
 *         } else if (type == BsonType.STRING) {
 *             return reader.readString();
 *         }
 *         // ... remaining types in default order
 *     }
 * }
 * }</pre>
 *
 * @see com.cloud.fastbson.annotation.BranchOrder
 * @see com.cloud.fastbson.processor.BranchOrderProcessor
 * @see com.cloud.fastbson.processor.TypeDispatcher
 */
public class BranchOrderAPTExample {

    // ==========================================
    // Example 1: Time-Series Data Dispatcher
    // ==========================================
    /**
     * Optimized for time-series data where:
     * - 60% of values are INT64 (timestamps)
     * - 30% are DOUBLE (sensor readings)
     * - 10% are STRING (metadata)
     *
     * <p>After compilation, use the generated class:
     * <pre>{@code
     * TypeDispatcher dispatcher = new TimeSeriesDispatcherImpl();
     * Object value = dispatcher.dispatch(reader, type);
     * }</pre>
     */
    @BranchOrder(value = {BsonType.INT64, BsonType.DOUBLE, BsonType.STRING},
                 workload = "Time-series sensor data")
    public static abstract class TimeSeriesDispatcher implements TypeDispatcher {}

    // ==========================================
    // Example 2: Web API Dispatcher
    // ==========================================
    /**
     * Optimized for web API responses where:
     * - 50% of values are STRING
     * - 25% are INT32 (IDs, counts)
     * - 15% are BOOLEAN (flags)
     * - 10% are DOCUMENT (nested objects)
     */
    @BranchOrder(value = {BsonType.STRING, BsonType.INT32, BsonType.BOOLEAN, BsonType.DOCUMENT},
                 workload = "Web API JSON-like responses")
    public static abstract class WebApiDispatcher implements TypeDispatcher {}

    // ==========================================
    // Example 3: Analytics Dispatcher
    // ==========================================
    /**
     * Optimized for analytics/metrics data where:
     * - 45% are DOUBLE (metrics, percentages)
     * - 35% are INT32 (counters, counts)
     * - 15% are INT64 (large numbers)
     * - 5% are STRING (labels)
     */
    @BranchOrder(value = {BsonType.DOUBLE, BsonType.INT32, BsonType.INT64, BsonType.STRING},
                 workload = "Analytics and metrics data")
    public static abstract class AnalyticsDispatcher implements TypeDispatcher {}

    // ==========================================
    // Example 4: Minimal Dispatcher (Interface)
    // ==========================================
    /**
     * You can also annotate interfaces instead of abstract classes.
     */
    @BranchOrder(value = {BsonType.INT64}, workload = "Counter-only data")
    public interface CounterDispatcher extends TypeDispatcher {}

    // ==========================================
    // Usage Examples
    // ==========================================

    /**
     * Demonstrates how to use a generated dispatcher.
     *
     * <p>Note: This example uses mock data. In real usage, the generated
     * implementations would be available after compilation.
     */
    public static void main(String[] args) {
        System.out.println("=== BranchOrder APT Example ===\n");

        // After compilation, the following classes are generated:
        // - TimeSeriesDispatcherImpl
        // - WebApiDispatcherImpl
        // - AnalyticsDispatcherImpl
        // - CounterDispatcherImpl

        System.out.println("Generated classes are placed in:");
        System.out.println("  target/generated-sources/annotations/\n");

        System.out.println("Example usage:");
        System.out.println("  TypeDispatcher dispatcher = new TimeSeriesDispatcherImpl();");
        System.out.println("  Object value = dispatcher.dispatch(reader, type);");
        System.out.println();

        System.out.println("The generated dispatch method checks types in priority order:");
        System.out.println("  1. INT64 (first - 60% of data)");
        System.out.println("  2. DOUBLE (second - 30% of data)");
        System.out.println("  3. STRING (third - 10% of data)");
        System.out.println("  4. ... remaining types in default order");
        System.out.println();

        System.out.println("This optimizes CPU branch prediction by checking");
        System.out.println("most frequent types first, typically improving");
        System.out.println("performance by 2-5% for type-skewed workloads.");
    }
}
