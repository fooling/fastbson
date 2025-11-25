package com.cloud.fastbson.performance;

import com.cloud.fastbson.handler.TypeHandler;
import com.cloud.fastbson.parser.PartialParser;
import com.cloud.fastbson.reader.BsonReader;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.bson.io.BasicOutputBuffer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 提前退出性能验证测试
 *
 * <p>测试目标：
 * <ul>
 *   <li>验证提前退出机制能带来性能提升</li>
 *   <li>测试不同字段位置对性能的影响</li>
 *   <li>对比完整解析 vs 部分解析 vs 提前退出</li>
 * </ul>
 *
 * @author FastBSON
 * @since 1.0.0
 */
public class EarlyExitPerformanceTest {

    private static final int ITERATIONS = 10000;
    private static final int FIELD_COUNT = 100;

    // ==================== 测试场景 1: 目标字段在文档前部 ====================

    @Test
    public void testEarlyExit_TargetFieldsAtFront() {
        // Arrange - 创建文档，目标字段在前 5 个
        BsonDocument doc = createLargeDocument(FIELD_COUNT);
        byte[] bsonData = serializeDocument(doc);

        // 目标字段：field0, field1（在前部）
        PartialParser parserWithEarlyExit = new PartialParser("field0", "field1");
        parserWithEarlyExit.setEarlyExit(true);

        PartialParser parserWithoutEarlyExit = new PartialParser("field0", "field1");
        parserWithoutEarlyExit.setEarlyExit(false);

        // Warmup
        for (int i = 0; i < 100; i++) {
            parserWithEarlyExit.parse(bsonData);
            parserWithoutEarlyExit.parse(bsonData);
        }

        // Act - 测试提前退出
        long startWithEarlyExit = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            Map<String, Object> result = parserWithEarlyExit.parse(bsonData);
            assertEquals(2, result.size());
        }
        long timeWithEarlyExit = System.nanoTime() - startWithEarlyExit;

        // Act - 测试不提前退出
        long startWithoutEarlyExit = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            Map<String, Object> result = parserWithoutEarlyExit.parse(bsonData);
            assertEquals(2, result.size());
        }
        long timeWithoutEarlyExit = System.nanoTime() - startWithoutEarlyExit;

        // Assert & Report
        double speedup = (double) timeWithoutEarlyExit / timeWithEarlyExit;
        System.out.println("\n=== 场景 1: 目标字段在文档前部 ===");
        System.out.println("文档大小: " + FIELD_COUNT + " 字段");
        System.out.println("目标字段: 2 个 (field0, field1)");
        System.out.println("迭代次数: " + ITERATIONS);
        System.out.println("提前退出:   " + formatTime(timeWithEarlyExit));
        System.out.println("不提前退出: " + formatTime(timeWithoutEarlyExit));
        System.out.println("性能提升:   " + String.format("%.2fx", speedup));

        // 提前退出应该更快
        assertTrue(speedup > 1.0, "Early exit should be faster when target fields are at front");
    }

    // ==================== 测试场景 2: 目标字段在文档中部 ====================

    @Test
    public void testEarlyExit_TargetFieldsInMiddle() {
        // Arrange - 目标字段在中部
        BsonDocument doc = createLargeDocument(FIELD_COUNT);
        byte[] bsonData = serializeDocument(doc);

        // 目标字段：field45, field50（在中部）
        PartialParser parserWithEarlyExit = new PartialParser("field45", "field50");
        parserWithEarlyExit.setEarlyExit(true);

        PartialParser parserWithoutEarlyExit = new PartialParser("field45", "field50");
        parserWithoutEarlyExit.setEarlyExit(false);

        // Warmup
        for (int i = 0; i < 100; i++) {
            parserWithEarlyExit.parse(bsonData);
            parserWithoutEarlyExit.parse(bsonData);
        }

        // Act
        long startWithEarlyExit = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            Map<String, Object> result = parserWithEarlyExit.parse(bsonData);
            assertEquals(2, result.size());
        }
        long timeWithEarlyExit = System.nanoTime() - startWithEarlyExit;

        long startWithoutEarlyExit = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            Map<String, Object> result = parserWithoutEarlyExit.parse(bsonData);
            assertEquals(2, result.size());
        }
        long timeWithoutEarlyExit = System.nanoTime() - startWithoutEarlyExit;

        // Report
        double speedup = (double) timeWithoutEarlyExit / timeWithEarlyExit;
        System.out.println("\n=== 场景 2: 目标字段在文档中部 ===");
        System.out.println("文档大小: " + FIELD_COUNT + " 字段");
        System.out.println("目标字段: 2 个 (field45, field50)");
        System.out.println("迭代次数: " + ITERATIONS);
        System.out.println("提前退出:   " + formatTime(timeWithEarlyExit));
        System.out.println("不提前退出: " + formatTime(timeWithoutEarlyExit));
        System.out.println("性能提升:   " + String.format("%.2fx", speedup));

        assertTrue(speedup > 1.0, "Early exit should be faster even when target fields are in middle");
    }

    // ==================== 测试场景 3: 目标字段在文档尾部 ====================

    @Test
    public void testEarlyExit_TargetFieldsAtEnd() {
        // Arrange - 目标字段在尾部
        BsonDocument doc = createLargeDocument(FIELD_COUNT);
        byte[] bsonData = serializeDocument(doc);

        // 目标字段：field98, field99（在尾部）
        PartialParser parserWithEarlyExit = new PartialParser("field98", "field99");
        parserWithEarlyExit.setEarlyExit(true);

        PartialParser parserWithoutEarlyExit = new PartialParser("field98", "field99");
        parserWithoutEarlyExit.setEarlyExit(false);

        // Warmup
        for (int i = 0; i < 100; i++) {
            parserWithEarlyExit.parse(bsonData);
            parserWithoutEarlyExit.parse(bsonData);
        }

        // Act
        long startWithEarlyExit = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            Map<String, Object> result = parserWithEarlyExit.parse(bsonData);
            assertEquals(2, result.size());
        }
        long timeWithEarlyExit = System.nanoTime() - startWithEarlyExit;

        long startWithoutEarlyExit = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            Map<String, Object> result = parserWithoutEarlyExit.parse(bsonData);
            assertEquals(2, result.size());
        }
        long timeWithoutEarlyExit = System.nanoTime() - startWithoutEarlyExit;

        // Report
        double speedup = (double) timeWithoutEarlyExit / timeWithEarlyExit;
        System.out.println("\n=== 场景 3: 目标字段在文档尾部 ===");
        System.out.println("文档大小: " + FIELD_COUNT + " 字段");
        System.out.println("目标字段: 2 个 (field98, field99)");
        System.out.println("迭代次数: " + ITERATIONS);
        System.out.println("提前退出:   " + formatTime(timeWithEarlyExit));
        System.out.println("不提前退出: " + formatTime(timeWithoutEarlyExit));
        System.out.println("性能提升:   " + String.format("%.2fx", speedup));

        // 即使在尾部，提前退出也应该不会明显更慢（可能相近）
        // 注意：由于JIT编译、GC等因素，性能可能有波动，使用较宽松的阈值
        assertTrue(speedup >= 0.8, "Early exit should not be significantly slower");
    }

    // ==================== 测试场景 4: 部分字段解析 vs 完整解析 ====================

    @Test
    public void testPartialParsing_VsFullParsing() {
        // Arrange
        BsonDocument doc = createLargeDocument(FIELD_COUNT);
        byte[] bsonData = serializeDocument(doc);

        PartialParser partialParser = new PartialParser("field0", "field1", "field2");
        partialParser.setEarlyExit(true);

        TypeHandler fullParser = new TypeHandler();

        // Warmup
        for (int i = 0; i < 100; i++) {
            partialParser.parse(bsonData);
            BsonReader reader = new BsonReader(bsonData);
            fullParser.parseDocument(reader);
        }

        // Act - 部分解析（提前退出）
        long startPartial = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            Map<String, Object> result = partialParser.parse(bsonData);
            assertEquals(3, result.size());
        }
        long timePartial = System.nanoTime() - startPartial;

        // Act - 完整解析
        long startFull = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            BsonReader reader = new BsonReader(bsonData);
            Map<String, Object> result = fullParser.parseDocument(reader);
            assertEquals(FIELD_COUNT, result.size());
        }
        long timeFull = System.nanoTime() - startFull;

        // Report
        double speedup = (double) timeFull / timePartial;
        System.out.println("\n=== 场景 4: 部分解析 vs 完整解析 ===");
        System.out.println("文档大小: " + FIELD_COUNT + " 字段");
        System.out.println("目标字段: 3 个 (field0, field1, field2)");
        System.out.println("迭代次数: " + ITERATIONS);
        System.out.println("部分解析（提前退出）: " + formatTime(timePartial));
        System.out.println("完整解析:             " + formatTime(timeFull));
        System.out.println("性能提升:             " + String.format("%.2fx", speedup));

        // 部分解析应该明显快于完整解析
        assertTrue(speedup > 2.0, "Partial parsing with early exit should be significantly faster than full parsing");
    }

    // ==================== 测试场景 5: 不同目标字段数量 ====================

    @Test
    public void testEarlyExit_DifferentFieldCounts() {
        // Arrange
        BsonDocument doc = createLargeDocument(FIELD_COUNT);
        byte[] bsonData = serializeDocument(doc);

        // 测试 1 个、5 个、10 个目标字段
        int[] targetCounts = {1, 5, 10};

        System.out.println("\n=== 场景 5: 不同目标字段数量 ===");
        System.out.println("文档大小: " + FIELD_COUNT + " 字段");
        System.out.println("迭代次数: " + ITERATIONS);

        for (int count : targetCounts) {
            String[] targetFields = new String[count];
            for (int i = 0; i < count; i++) {
                targetFields[i] = "field" + i;
            }

            PartialParser parserWithEarlyExit = new PartialParser(targetFields);
            parserWithEarlyExit.setEarlyExit(true);

            PartialParser parserWithoutEarlyExit = new PartialParser(targetFields);
            parserWithoutEarlyExit.setEarlyExit(false);

            // Warmup
            for (int i = 0; i < 100; i++) {
                parserWithEarlyExit.parse(bsonData);
                parserWithoutEarlyExit.parse(bsonData);
            }

            // Test with early exit
            long startWithEarlyExit = System.nanoTime();
            for (int i = 0; i < ITERATIONS; i++) {
                parserWithEarlyExit.parse(bsonData);
            }
            long timeWithEarlyExit = System.nanoTime() - startWithEarlyExit;

            // Test without early exit
            long startWithoutEarlyExit = System.nanoTime();
            for (int i = 0; i < ITERATIONS; i++) {
                parserWithoutEarlyExit.parse(bsonData);
            }
            long timeWithoutEarlyExit = System.nanoTime() - startWithoutEarlyExit;

            double speedup = (double) timeWithoutEarlyExit / timeWithEarlyExit;
            System.out.println("\n目标字段数: " + count);
            System.out.println("  提前退出:   " + formatTime(timeWithEarlyExit));
            System.out.println("  不提前退出: " + formatTime(timeWithoutEarlyExit));
            System.out.println("  性能提升:   " + String.format("%.2fx", speedup));

            assertTrue(speedup > 1.0, "Early exit should be faster for " + count + " target fields");
        }
    }

    // ==================== 测试场景 6: 字段稀疏度影响 ====================

    @Test
    public void testEarlyExit_FieldSparsity() {
        System.out.println("\n=== 场景 6: 字段稀疏度影响 ===");
        System.out.println("迭代次数: " + ITERATIONS);

        // 测试不同的字段密度：5%, 10%, 20%
        int[] documentSizes = {20, 50, 100};

        for (int docSize : documentSizes) {
            BsonDocument doc = createLargeDocument(docSize);
            byte[] bsonData = serializeDocument(doc);

            // 固定提取 2 个字段
            PartialParser parser = new PartialParser("field0", "field1");
            parser.setEarlyExit(true);

            // Warmup
            for (int i = 0; i < 100; i++) {
                parser.parse(bsonData);
            }

            // Test
            long start = System.nanoTime();
            for (int i = 0; i < ITERATIONS; i++) {
                Map<String, Object> result = parser.parse(bsonData);
                assertEquals(2, result.size());
            }
            long time = System.nanoTime() - start;

            double percentage = (2.0 / docSize) * 100;
            System.out.println("\n文档大小: " + docSize + " 字段");
            System.out.println("  提取比例: " + String.format("%.1f%%", percentage) + " (2/" + docSize + ")");
            System.out.println("  解析时间: " + formatTime(time));
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建包含指定数量字段的大文档
     */
    private BsonDocument createLargeDocument(int fieldCount) {
        BsonDocument doc = new BsonDocument();
        for (int i = 0; i < fieldCount; i++) {
            doc.append("field" + i, new BsonString("value" + i));
        }
        return doc;
    }

    /**
     * 序列化 BSON 文档
     */
    private byte[] serializeDocument(BsonDocument doc) {
        BasicOutputBuffer buffer = new BasicOutputBuffer();
        org.bson.BsonBinaryWriter writer = new org.bson.BsonBinaryWriter(buffer);
        new org.bson.codecs.BsonDocumentCodec().encode(
            writer, doc, org.bson.codecs.EncoderContext.builder().build()
        );
        return buffer.toByteArray();
    }

    /**
     * 格式化时间（纳秒转毫秒）
     */
    private String formatTime(long nanos) {
        return String.format("%.2f ms", nanos / 1_000_000.0);
    }
}
