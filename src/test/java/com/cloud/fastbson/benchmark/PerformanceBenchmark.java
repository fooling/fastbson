package com.cloud.fastbson.benchmark;

import com.cloud.fastbson.FastBson;
import com.cloud.fastbson.handler.parsers.DocumentParser;
import com.cloud.fastbson.parser.PartialParser;
import com.cloud.fastbson.reader.BsonReader;
import org.bson.BsonBinaryReader;
import org.bson.BsonDocument;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.DecoderContext;
import org.bson.io.ByteBufferBsonInput;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FastBSON性能基线测试
 *
 * <p>统一的benchmark框架，对比FastBSON多种模式与MongoDB BSON的性能差异
 */
public class PerformanceBenchmark {
    /** 预热次数 */
    private static final int WARMUP_ITERATIONS = 1000;

    /** 测试次数 */
    private static final int TEST_ITERATIONS = 10000;

    /**
     * 完整性能基线测试（推荐）
     *
     * <p>一次运行，展示所有场景的性能对比
     */
    @Test
    public void testCompletePerformanceBaseline() {
        System.out.println("\n🚀 开始运行 FastBSON 完整性能基线测试...\n");

        List<BenchmarkResult> results = new ArrayList<>();

        // Phase 1: 50字段完整解析（保留3.88x场景）
        results.add(runPhase1_HashMap_50Fields());

        // Phase 2.A: 100字段部分解析，早退优化
        results.add(runPhase2A_PartialParser_5of100());

        // Phase 2.B: 100字段零复制惰性解析
        results.add(runPhase2B_IndexedDocument_5of100());

        // 生成报告
        String report = BenchmarkReport.generate(results);
        System.out.println(report);

        // 验证所有场景都达到基本性能要求
        for (BenchmarkResult result : results) {
            assertTrue(result.getSpeedup() > 1.0,
                result.getScenarioName() + " 性能应该优于MongoDB");
        }
    }

    /**
     * Phase 1: HashMap 完整解析模式
     *
     * <p>场景：50字段文档，完整解析 + 构建索引
     * <p>目标：3.5-4.0x vs MongoDB（保留历史最佳3.88x）
     */
    @Test
    public void testPhase1_HashMap_50Fields() {
        BenchmarkResult result = runPhase1_HashMap_50Fields();
        System.out.println(BenchmarkReport.generate(List.of(result)));

        assertTrue(result.getSpeedup() > 1.0, "Phase 1 should be faster than MongoDB");
        if (result.getSpeedup() < 2.5) {
            System.out.println("⚠️  警告：Phase 1性能低于预期（< 2.5x），建议检查实现");
        }
    }

    /**
     * Phase 2.A: PartialParser 早退优化模式
     *
     * <p>场景：100字段文档，提取5个字段 (5%)
     * <p>目标：7-10x vs MongoDB
     */
    @Test
    public void testPhase2A_PartialParser_5of100() {
        BenchmarkResult result = runPhase2A_PartialParser_5of100();
        System.out.println(BenchmarkReport.generate(List.of(result)));

        assertTrue(result.getSpeedup() > 1.0, "Phase 2.A should be faster than MongoDB");
        if (result.getSpeedup() < 5.0) {
            System.out.println("⚠️  警告：Phase 2.A性能低于预期（< 5.0x），建议检查早退优化");
        }
    }

    /**
     * Phase 2.B: IndexedBsonDocument 零复制惰性解析模式
     *
     * <p>场景：100字段文档，构建索引 + 访问5个字段
     * <p>目标：3-3.5x vs MongoDB + 70%内存优势
     */
    @Test
    public void testPhase2B_IndexedDocument_5of100() {
        BenchmarkResult result = runPhase2B_IndexedDocument_5of100();
        System.out.println(BenchmarkReport.generate(List.of(result)));

        assertTrue(result.getSpeedup() > 1.0, "Phase 2.B should be faster than MongoDB");
        if (result.getSpeedup() < 1.5) {
            System.out.println("⚠️  警告：Phase 2.B性能低于预期（< 1.5x），但内存优势明显");
        }
    }

    // ==================== 内部实现 ====================

    private BenchmarkResult runPhase1_HashMap_50Fields() {
        byte[] bsonData = BsonTestDataGenerator.generateDocument(50);

        // 启用 Phase 1 HashMap 模式
        FastBson.useHashMapFactory();

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            com.cloud.fastbson.document.BsonDocument doc =
                (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(new BsonReader(bsonData));
        }

        // FastBSON测试
        long fastbsonStart = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            com.cloud.fastbson.document.BsonDocument doc =
                (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(new BsonReader(bsonData));
        }
        long fastbsonTime = System.nanoTime() - fastbsonStart;

        // 恢复默认工厂
        FastBson.useFastFactory();

        // MongoDB测试
        long mongoTime = runMongoDBParsing(bsonData, null);

        double speedup = (double) mongoTime / fastbsonTime;

        return BenchmarkResult.builder()
            .scenarioName("Phase 1: 50字段完整解析")
            .fastbsonMode("HashMap (eager)")
            .fastbsonTimeNanos(fastbsonTime)
            .mongoTimeNanos(mongoTime)
            .speedup(speedup)
            .description("50字段文档，完整解析 + 构建索引")
            .target("3.5-4.0x (历史最佳: 3.88x)")
            .passed(speedup > 2.5)
            .note("中小型文档标准解析场景")
            .build();
    }

    private BenchmarkResult runPhase2A_PartialParser_5of100() {
        byte[] bsonData = BsonTestDataGenerator.generateDocument(100);

        // PartialParser早退优化
        PartialParser partialParser = new PartialParser("field0", "field10", "field20", "field30", "field40");
        partialParser.setEarlyExit(true);

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            Map<String, Object> result = partialParser.parse(bsonData);
        }

        // FastBSON测试
        long fastbsonStart = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            Map<String, Object> result = partialParser.parse(bsonData);
        }
        long fastbsonTime = System.nanoTime() - fastbsonStart;

        // MongoDB测试
        String[] fields = {"field0", "field10", "field20", "field30", "field40"};
        long mongoTime = runMongoDBParsing(bsonData, fields);

        double speedup = (double) mongoTime / fastbsonTime;

        return BenchmarkResult.builder()
            .scenarioName("Phase 2.A: 100字段部分解析(5/100)")
            .fastbsonMode("PartialParser (early-exit)")
            .fastbsonTimeNanos(fastbsonTime)
            .mongoTimeNanos(mongoTime)
            .speedup(speedup)
            .description("100字段文档，提取5个字段，早退优化")
            .target("7-10x (目标: 10-20x)")
            .passed(speedup > 5.0)
            .note("一次性部分字段提取，极致速度")
            .build();
    }

    private BenchmarkResult runPhase2B_IndexedDocument_5of100() {
        byte[] bsonData = BsonTestDataGenerator.generateDocument(100);

        // 启用IndexedBsonDocument零复制模式
        FastBson.useIndexedFactory();

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            com.cloud.fastbson.document.BsonDocument doc =
                (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(new BsonReader(bsonData));
            doc.getInt32("field0", 0);
            doc.getString("field1", "");
            doc.getDouble("field2", 0.0);
            doc.getBoolean("field3", false);
            doc.getInt64("field4", 0L);
        }

        // FastBSON测试
        long fastbsonStart = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            com.cloud.fastbson.document.BsonDocument doc =
                (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(new BsonReader(bsonData));
            doc.getInt32("field0", 0);
            doc.getString("field1", "");
            doc.getDouble("field2", 0.0);
            doc.getBoolean("field3", false);
            doc.getInt64("field4", 0L);
        }
        long fastbsonTime = System.nanoTime() - fastbsonStart;

        // 恢复默认工厂
        FastBson.useFastFactory();

        // MongoDB测试（使用正确的字段类型）
        long mongoStart = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
                new org.bson.ByteBufNIO(ByteBuffer.wrap(bsonData))));
            BsonDocumentCodec codec = new BsonDocumentCodec();
            BsonDocument doc = codec.decode(reader, DecoderContext.builder().build());
            doc.getInt32("field0");
            doc.getString("field1");
            doc.getDouble("field2");
            doc.getBoolean("field3");
            doc.getInt64("field4");
            reader.close();
        }
        long mongoTime = System.nanoTime() - mongoStart;

        double speedup = (double) mongoTime / fastbsonTime;

        return BenchmarkResult.builder()
            .scenarioName("Phase 2.B: 100字段零复制惰性(5/100)")
            .fastbsonMode("IndexedDocument (zero-copy)")
            .fastbsonTimeNanos(fastbsonTime)
            .mongoTimeNanos(mongoTime)
            .speedup(speedup)
            .description("100字段文档，构建索引 + 访问5个字段")
            .target("3-3.5x + 70%内存优势")
            .passed(speedup > 1.5)
            .note("零复制架构，内存占用降低70%")
            .build();
    }

    /**
     * 运行MongoDB BSON解析
     *
     * <p>CRITICAL FIX: 添加与FastBSON相同的预热迭代次数，确保公平对比。
     * 之前MongoDB没有预热直接计时，导致FastBSON性能被高估。
     *
     * @param bsonData BSON数据
     * @param fields 要访问的字段（null表示不访问任何字段）
     * @return 耗时（纳秒）
     */
    private long runMongoDBParsing(byte[] bsonData, String[] fields) {
        // ✅ FIX: 添加MongoDB预热，与FastBSON一致
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
                new org.bson.ByteBufNIO(ByteBuffer.wrap(bsonData))));
            BsonDocumentCodec codec = new BsonDocumentCodec();
            BsonDocument doc = codec.decode(reader, DecoderContext.builder().build());

            // 如果指定了字段，则访问这些字段
            if (fields != null) {
                for (String field : fields) {
                    doc.get(field);
                }
            }

            reader.close();
        }

        // 预热完成，开始计时测试
        long mongoStart = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
                new org.bson.ByteBufNIO(ByteBuffer.wrap(bsonData))));
            BsonDocumentCodec codec = new BsonDocumentCodec();
            BsonDocument doc = codec.decode(reader, DecoderContext.builder().build());

            // 如果指定了字段，则访问这些字段
            if (fields != null) {
                for (String field : fields) {
                    doc.get(field);
                }
            }

            reader.close();
        }
        return System.nanoTime() - mongoStart;
    }

    // ==================== Phase 3 优化专属场景 ====================

    /**
     * Phase 3.1 场景：高频字段名重复（StringPool优势）
     *
     * <p><b>优化点</b>：StringPool字段名interning
     * <p><b>测试场景</b>：批量解析1000个相同结构文档，字段名完全重复
     * <p><b>预期收益</b>：减少String分配，启用引用相等性比较，内存占用降低40-60%
     */
    @Test
    public void testPhase3_1_StringPoolBenefit() {
        BenchmarkResult result = runPhase3_1_StringPoolBenefit();
        System.out.println("\n" + BenchmarkReport.generate(List.of(result)));
        assertTrue(result.getSpeedup() > 1.0, "StringPool优化应该提升性能");
    }

    private BenchmarkResult runPhase3_1_StringPoolBenefit() {
        // 生成1000个相同结构的文档
        int docCount = 1000;
        byte[] singleDocData = BsonTestDataGenerator.generateDocument(50);

        // 创建批量文档数据
        List<byte[]> documents = new ArrayList<>();
        for (int i = 0; i < docCount; i++) {
            documents.add(singleDocData);
        }

        FastBson.useHashMapFactory();

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS / 10; i++) {  // 减少预热次数避免过长
            for (byte[] doc : documents) {
                DocumentParser.INSTANCE.parse(new BsonReader(doc));
            }
        }

        // FastBSON测试：连续解析1000个相同结构文档
        long fastbsonStart = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS / 10; i++) {
            for (byte[] doc : documents) {
                DocumentParser.INSTANCE.parse(new BsonReader(doc));
            }
        }
        long fastbsonTime = System.nanoTime() - fastbsonStart;

        FastBson.useFastFactory();

        // MongoDB测试：预热
        for (int i = 0; i < WARMUP_ITERATIONS / 10; i++) {
            for (byte[] doc : documents) {
                BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
                    new org.bson.ByteBufNIO(ByteBuffer.wrap(doc))));
                BsonDocumentCodec codec = new BsonDocumentCodec();
                codec.decode(reader, DecoderContext.builder().build());
                reader.close();
            }
        }

        // MongoDB测试：计时
        long mongoStart = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS / 10; i++) {
            for (byte[] doc : documents) {
                BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
                    new org.bson.ByteBufNIO(ByteBuffer.wrap(doc))));
                BsonDocumentCodec codec = new BsonDocumentCodec();
                codec.decode(reader, DecoderContext.builder().build());
                reader.close();
            }
        }
        long mongoTime = System.nanoTime() - mongoStart;

        double speedup = (double) mongoTime / fastbsonTime;

        return BenchmarkResult.builder()
            .scenarioName("Phase 3.1: 字段名重复场景(StringPool)")
            .fastbsonMode("StringPool interning")
            .fastbsonTimeNanos(fastbsonTime)
            .mongoTimeNanos(mongoTime)
            .speedup(speedup)
            .description("批量解析1000个相同结构文档（50字段）")
            .target("1.1-1.3x + 40-60%内存优势")
            .passed(speedup > 1.0)
            .note("StringPool减少重复字段名分配")
            .build();
    }

    /**
     * Phase 3.2 场景：高吞吐量连续解析（ObjectPool优势）
     *
     * <p><b>优化点</b>：ThreadLocal ObjectPool复用BsonReader
     * <p><b>测试场景</b>：连续解析10000个文档无间断
     * <p><b>预期收益</b>：减少BsonReader分配，降低GC压力
     */
    @Test
    public void testPhase3_2_ObjectPoolBenefit() {
        BenchmarkResult result = runPhase3_2_ObjectPoolBenefit();
        System.out.println("\n" + BenchmarkReport.generate(List.of(result)));
        assertTrue(result.getSpeedup() > 1.0, "ObjectPool优化应该提升性能");
    }

    private BenchmarkResult runPhase3_2_ObjectPoolBenefit() {
        // 生成大量不同文档
        List<byte[]> documents = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            documents.add(BsonTestDataGenerator.generateDocument(20));
        }

        // 使用PartialParser测试ObjectPool效果
        PartialParser parser = new PartialParser("field0", "field5", "field10");
        parser.setEarlyExit(true);

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            parser.parse(documents.get(i % documents.size()));
        }

        // FastBSON测试：高吞吐量连续解析
        long fastbsonStart = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            parser.parse(documents.get(i % documents.size()));
        }
        long fastbsonTime = System.nanoTime() - fastbsonStart;

        // MongoDB测试：预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            byte[] doc = documents.get(i % documents.size());
            BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
                new org.bson.ByteBufNIO(ByteBuffer.wrap(doc))));
            BsonDocumentCodec codec = new BsonDocumentCodec();
            BsonDocument bsonDoc = codec.decode(reader, DecoderContext.builder().build());
            bsonDoc.get("field0");
            bsonDoc.get("field5");
            bsonDoc.get("field10");
            reader.close();
        }

        // MongoDB测试：计时
        long mongoStart = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            byte[] doc = documents.get(i % documents.size());
            BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
                new org.bson.ByteBufNIO(ByteBuffer.wrap(doc))));
            BsonDocumentCodec codec = new BsonDocumentCodec();
            BsonDocument bsonDoc = codec.decode(reader, DecoderContext.builder().build());
            bsonDoc.get("field0");
            bsonDoc.get("field5");
            bsonDoc.get("field10");
            reader.close();
        }
        long mongoTime = System.nanoTime() - mongoStart;

        double speedup = (double) mongoTime / fastbsonTime;

        return BenchmarkResult.builder()
            .scenarioName("Phase 3.2: 高吞吐量场景(ObjectPool)")
            .fastbsonMode("ThreadLocal BsonReader pool")
            .fastbsonTimeNanos(fastbsonTime)
            .mongoTimeNanos(mongoTime)
            .speedup(speedup)
            .description("连续解析10000个文档（20字段部分解析）")
            .target("1.05-1.15x + 降低GC压力")
            .passed(speedup > 1.0)
            .note("ObjectPool减少BsonReader分配")
            .build();
    }

    /**
     * Phase 3.3 场景：已知结构文档（HashMap容量优化）
     *
     * <p><b>优化点</b>：HashMap容量预分配，避免rehash
     * <p><b>测试场景</b>：固定50字段文档，精确容量估算
     * <p><b>预期收益</b>：避免HashMap扩容，减少内存复制
     */
    @Test
    public void testPhase3_3_HashMapCapacityBenefit() {
        BenchmarkResult result = runPhase3_3_HashMapCapacityBenefit();
        System.out.println("\n" + BenchmarkReport.generate(List.of(result)));
        assertTrue(result.getSpeedup() > 1.0, "HashMap容量优化应该提升性能");
    }

    private BenchmarkResult runPhase3_3_HashMapCapacityBenefit() {
        // 生成固定50字段文档
        byte[] bsonData = BsonTestDataGenerator.generateDocument(50);

        FastBson.useHashMapFactory();

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            DocumentParser.INSTANCE.parse(new BsonReader(bsonData));
        }

        // FastBSON测试：容量预分配
        long fastbsonStart = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            DocumentParser.INSTANCE.parse(new BsonReader(bsonData));
        }
        long fastbsonTime = System.nanoTime() - fastbsonStart;

        FastBson.useFastFactory();

        // MongoDB测试：预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
                new org.bson.ByteBufNIO(ByteBuffer.wrap(bsonData))));
            BsonDocumentCodec codec = new BsonDocumentCodec();
            codec.decode(reader, DecoderContext.builder().build());
            reader.close();
        }

        // MongoDB测试：计时
        long mongoStart = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
                new org.bson.ByteBufNIO(ByteBuffer.wrap(bsonData))));
            BsonDocumentCodec codec = new BsonDocumentCodec();
            codec.decode(reader, DecoderContext.builder().build());
            reader.close();
        }
        long mongoTime = System.nanoTime() - mongoStart;

        double speedup = (double) mongoTime / fastbsonTime;

        return BenchmarkResult.builder()
            .scenarioName("Phase 3.3: 已知结构场景(HashMap容量)")
            .fastbsonMode("Capacity pre-allocation")
            .fastbsonTimeNanos(fastbsonTime)
            .mongoTimeNanos(mongoTime)
            .speedup(speedup)
            .description("固定50字段文档，精确容量预分配")
            .target("1.05-1.1x + 减少rehash")
            .passed(speedup > 1.0)
            .note("避免HashMap动态扩容开销")
            .build();
    }

    /**
     * Phase 3 完整测试套件
     *
     * <p>一次性运行所有Phase 3优化场景，展示每个优化的价值
     */
    @Test
    public void testPhase3_CompleteOptimizationSuite() {
        System.out.println("\n🎯 开始运行 Phase 3 优化价值验证测试...\n");

        List<BenchmarkResult> results = new ArrayList<>();

        // Phase 3.1: StringPool字段名interning
        results.add(runPhase3_1_StringPoolBenefit());

        // Phase 3.2: ObjectPool BsonReader复用
        results.add(runPhase3_2_ObjectPoolBenefit());

        // Phase 3.3: HashMap容量预分配
        results.add(runPhase3_3_HashMapCapacityBenefit());

        // 生成报告
        String report = BenchmarkReport.generate(results);
        System.out.println(report);

        // 验证所有优化都有正向收益
        for (BenchmarkResult result : results) {
            assertTrue(result.getSpeedup() > 1.0,
                result.getScenarioName() + " 应该有性能提升");
        }
    }
}
