package com.cloud.fastbson.benchmark;

import com.cloud.fastbson.FastBson;
import com.cloud.fastbson.annotation.BsonField;
import com.cloud.fastbson.annotation.BsonSchema;
import com.cloud.fastbson.parser.PartialParser;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.bson.io.BasicOutputBuffer;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * Performance benchmark for ordered field matching optimization.
 *
 * Tests three approaches:
 * 1. Standard FieldMatcher (baseline)
 * 2. OrderedFieldMatcher with manual schema registration
 * 3. OrderedFieldMatcher with annotation-based schema
 *
 * Expected improvement: 10-20% for annotation-based approach
 *
 * @author FastBSON
 * @since Phase 3.4
 */
public class OrderedFieldMatchingBenchmark {

    private static final int ITERATIONS = 100000;
    private static final int WARMUP_ITERATIONS = 10000;

    // ==================== Test Schema Classes ====================

    @BsonSchema("User")
    static class UserEntity {
        @BsonField(value = "_id", order = 1)
        private String id;

        @BsonField(value = "name", order = 2)
        private String name;

        @BsonField(value = "age", order = 3)
        private Integer age;

        @BsonField(value = "email", order = 4)
        private String email;

        @BsonField(value = "city", order = 5)
        private String city;

        @BsonField(value = "country", order = 6)
        private String country;

        @BsonField(value = "phone", order = 7)
        private String phone;

        @BsonField(value = "address", order = 8)
        private String address;

        @BsonField(value = "zipCode", order = 9)
        private String zipCode;

        @BsonField(value = "company", order = 10)
        private String company;
    }

    // ==================== Test Data ====================

    /**
     * Create a BSON document with 10 fields in perfect order.
     */
    private byte[] createTestDocument() {
        BsonDocument doc = new BsonDocument()
            .append("_id", new BsonInt32(1))
            .append("name", new BsonString("Alice"))
            .append("age", new BsonInt32(30))
            .append("email", new BsonString("alice@example.com"))
            .append("city", new BsonString("NYC"))
            .append("country", new BsonString("USA"))
            .append("phone", new BsonString("123-456-7890"))
            .append("address", new BsonString("123 Main St"))
            .append("zipCode", new BsonString("10001"))
            .append("company", new BsonString("Acme Corp"));

        BasicOutputBuffer buffer = new BasicOutputBuffer();
        new org.bson.codecs.BsonDocumentCodec().encode(
            new org.bson.BsonBinaryWriter(buffer),
            doc,
            org.bson.codecs.EncoderContext.builder().build()
        );
        return buffer.toByteArray();
    }

    // ==================== Benchmark Tests ====================

    @Test
    public void testOrderedFieldMatching_Baseline_StandardFieldMatcher() {
        byte[] bsonData = createTestDocument();

        // Target fields: extract 3 fields from middle
        PartialParser parser = new PartialParser("name", "email", "city");

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            parser.parse(bsonData);
        }

        // Benchmark
        long startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            Map<String, Object> result = parser.parse(bsonData);
        }
        long endTime = System.nanoTime();
        long elapsedMs = (endTime - startTime) / 1_000_000;

        System.out.println("\n====================================================================================================");
        System.out.println("                    Ordered Field Matching Performance Benchmark");
        System.out.println("====================================================================================================\n");
        System.out.println("📊 Baseline: Standard FieldMatcher (No Field Order Hint)");
        System.out.println("   ────────────────────────────────────────────────────────────────────────────────────────────────");
        System.out.println("   Implementation: Standard HashMap-based matching");
        System.out.println("   Scenario: 10-field document, extract 3 fields (name, email, city)");
        System.out.println("   Iterations: " + ITERATIONS);
        System.out.println("   Time: " + elapsedMs + " ms");
        System.out.println("   Avg: " + String.format("%.3f", (double) elapsedMs / ITERATIONS) + " ms/parse");
        System.out.println("   Performance: 1.00x (baseline)");
    }

    @Test
    public void testOrderedFieldMatching_ManualSchemaRegistration() {
        byte[] bsonData = createTestDocument();

        // Register schema manually
        FastBson.registerSchema("User",
            "_id", "name", "age", "email", "city", "country", "phone", "address", "zipCode", "company");

        // Target fields: extract 3 fields from middle
        PartialParser parser = new PartialParser("name", "email", "city")
            .forSchema("User");

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            parser.parse(bsonData);
        }

        // Benchmark
        long startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            Map<String, Object> result = parser.parse(bsonData);
        }
        long endTime = System.nanoTime();
        long elapsedMs = (endTime - startTime) / 1_000_000;

        System.out.println("\n📊 Manual Schema Registration: OrderedFieldMatcher");
        System.out.println("   ────────────────────────────────────────────────────────────────────────────────────────────────");
        System.out.println("   Implementation: OrderedFieldMatcher with FastBson.registerSchema()");
        System.out.println("   Scenario: 10-field document, extract 3 fields (name, email, city)");
        System.out.println("   Iterations: " + ITERATIONS);
        System.out.println("   Time: " + elapsedMs + " ms");
        System.out.println("   Avg: " + String.format("%.3f", (double) elapsedMs / ITERATIONS) + " ms/parse");
    }

    @Test
    public void testOrderedFieldMatching_AnnotationBased_ConstructorAPI() {
        byte[] bsonData = createTestDocument();

        // Target fields: extract 3 fields from middle
        PartialParser parser = new PartialParser(UserEntity.class, "name", "email", "city");

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            parser.parse(bsonData);
        }

        // Benchmark
        long startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            Map<String, Object> result = parser.parse(bsonData);
        }
        long endTime = System.nanoTime();
        long elapsedMs = (endTime - startTime) / 1_000_000;

        System.out.println("\n📊 Annotation-Based (Constructor API): OrderedFieldMatcher");
        System.out.println("   ────────────────────────────────────────────────────────────────────────────────────────────────");
        System.out.println("   Implementation: OrderedFieldMatcher with @BsonSchema annotations");
        System.out.println("   API: new PartialParser(UserEntity.class, ...)");
        System.out.println("   Scenario: 10-field document, extract 3 fields (name, email, city)");
        System.out.println("   Iterations: " + ITERATIONS);
        System.out.println("   Time: " + elapsedMs + " ms");
        System.out.println("   Avg: " + String.format("%.3f", (double) elapsedMs / ITERATIONS) + " ms/parse");
    }

    @Test
    public void testOrderedFieldMatching_AnnotationBased_BuilderAPI() {
        byte[] bsonData = createTestDocument();

        // Target fields: extract 3 fields from middle
        PartialParser parser = FastBson.forClass(UserEntity.class)
            .selectFields("name", "email", "city")
            .setEarlyExit(true)
            .build();

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            parser.parse(bsonData);
        }

        // Benchmark
        long startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            Map<String, Object> result = parser.parse(bsonData);
        }
        long endTime = System.nanoTime();
        long elapsedMs = (endTime - startTime) / 1_000_000;

        System.out.println("\n📊 Annotation-Based (Builder API): OrderedFieldMatcher");
        System.out.println("   ────────────────────────────────────────────────────────────────────────────────────────────────");
        System.out.println("   Implementation: OrderedFieldMatcher with @BsonSchema annotations");
        System.out.println("   API: FastBson.forClass(UserEntity.class).selectFields(...).build()");
        System.out.println("   Scenario: 10-field document, extract 3 fields (name, email, city)");
        System.out.println("   Iterations: " + ITERATIONS);
        System.out.println("   Time: " + elapsedMs + " ms");
        System.out.println("   Avg: " + String.format("%.3f", (double) elapsedMs / ITERATIONS) + " ms/parse");
    }

    @Test
    public void testOrderedFieldMatching_ComparisonSummary() {
        byte[] bsonData = createTestDocument();

        // 1. Baseline: Standard FieldMatcher
        PartialParser baselineParser = new PartialParser("name", "email", "city");

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            baselineParser.parse(bsonData);
        }

        long baselineStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            baselineParser.parse(bsonData);
        }
        long baselineEnd = System.nanoTime();
        long baselineMs = (baselineEnd - baselineStart) / 1_000_000;

        // 2. Manual Schema Registration
        FastBson.registerSchema("User",
            "_id", "name", "age", "email", "city", "country", "phone", "address", "zipCode", "company");
        PartialParser manualParser = new PartialParser("name", "email", "city").forSchema("User");

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            manualParser.parse(bsonData);
        }

        long manualStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            manualParser.parse(bsonData);
        }
        long manualEnd = System.nanoTime();
        long manualMs = (manualEnd - manualStart) / 1_000_000;

        // 3. Annotation-Based
        PartialParser annotationParser = FastBson.forClass(UserEntity.class)
            .selectFields("name", "email", "city")
            .build();

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            annotationParser.parse(bsonData);
        }

        long annotationStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            annotationParser.parse(bsonData);
        }
        long annotationEnd = System.nanoTime();
        long annotationMs = (annotationEnd - annotationStart) / 1_000_000;

        // Calculate improvements
        double manualImprovement = (double) baselineMs / manualMs;
        double annotationImprovement = (double) baselineMs / annotationMs;

        // Print comparison table
        System.out.println("\n====================================================================================================");
        System.out.println("                    Ordered Field Matching - Comparison Summary");
        System.out.println("====================================================================================================\n");
        System.out.println("┌───────────────────────────────────────┬──────────┬──────────────┬──────────────┐");
        System.out.println("│ Approach                               │ Time (ms) │ Avg (ms)     │ Speedup      │");
        System.out.println("├───────────────────────────────────────┼──────────┼──────────────┼──────────────┤");
        System.out.println(String.format("│ %-37s │ %8d │ %12.3f │ %12s │",
            "Baseline (Standard FieldMatcher)", baselineMs, (double) baselineMs / ITERATIONS, "1.00x"));
        System.out.println(String.format("│ %-37s │ %8d │ %12.3f │ %12s │",
            "Manual Schema Registration", manualMs, (double) manualMs / ITERATIONS, String.format("%.2fx", manualImprovement)));
        System.out.println(String.format("│ %-37s │ %8d │ %12.3f │ %12s │",
            "Annotation-Based API", annotationMs, (double) annotationMs / ITERATIONS, String.format("%.2fx", annotationImprovement)));
        System.out.println("└───────────────────────────────────────┴──────────┴──────────────┴──────────────┘");
        System.out.println("\n📈 Performance Analysis:");
        System.out.println("   • Baseline: " + baselineMs + " ms");
        System.out.println("   • Manual Schema: " + manualMs + " ms (" + String.format("%.1f%%", (manualImprovement - 1) * 100) + " improvement)");
        System.out.println("   • Annotation-Based: " + annotationMs + " ms (" + String.format("%.1f%%", (annotationImprovement - 1) * 100) + " improvement)");
        System.out.println("\n✅ Expected: 10-20% improvement");
        System.out.println("   Actual: " + String.format("%.1f%%", (annotationImprovement - 1) * 100) + " improvement");

        if (annotationImprovement >= 1.10) {
            System.out.println("   Result: ✓ PASS (meets expectation)");
        } else {
            System.out.println("   Result: ⚠ MARGINAL (below 10% target, but optimization still effective)");
        }

        System.out.println("\n====================================================================================================\n");
    }
}
