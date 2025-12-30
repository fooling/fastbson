package com.cloud.fastbson.benchmark;

import com.cloud.fastbson.FastBson;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.handler.parsers.DocumentParser;
import com.cloud.fastbson.reader.BsonReader;
import org.bson.BsonBinaryReader;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.DecoderContext;
import org.bson.io.ByteBufferBsonInput;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 真实场景 Benchmark - 测试实际解析+访问字段的性能
 */
public class RealWorldBenchmark {

    private static final int WARMUP = 100;
    private static final int ITERATIONS = 10000;

    @Test
    public void testRealWorldScenarios() {
        System.out.println("\n=== 真实场景性能测试 ===\n");

        // 场景1: 解析后访问所有字段 (急切解析 vs 惰性解析)
        testScenario1_ParseAndAccessAllFields();

        // 场景2: 解析后只访问部分字段 (惰性解析优势场景)
        testScenario2_ParseAndAccessPartialFields();

        // 场景3: HashMap急切解析模式 (Phase 3目标场景)
        testScenario3_HashMapEagerParsing();

        // 场景4: 批量解析+访问 (真实数据库查询场景)
        testScenario4_BatchParseAndAccess();
    }

    /**
     * 场景1: 解析 + 访问所有字段
     * 这是惰性解析的"最差场景"，因为每个字段都要解析
     */
    private void testScenario1_ParseAndAccessAllFields() {
        System.out.println("场景1: 解析 + 访问所有50个字段");

        byte[] bsonData = BsonTestDataGenerator.generateDocument(50);

        // 预热 FastBSON (Indexed模式)
        for (int i = 0; i < WARMUP; i++) {
            BsonDocument doc = FastBson.parse(bsonData);
            accessAllFields(doc, 50);
        }

        // FastBSON测试
        long fastStart = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++) {
            BsonDocument doc = FastBson.parse(bsonData);
            accessAllFields(doc, 50);
        }
        long fastTime = System.currentTimeMillis() - fastStart;

        // 预热 MongoDB
        for (int i = 0; i < WARMUP; i++) {
            org.bson.BsonDocument doc = parseMongoDB(bsonData);
            accessAllFieldsMongo(doc, 50);
        }

        // MongoDB测试
        long mongoStart = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++) {
            org.bson.BsonDocument doc = parseMongoDB(bsonData);
            accessAllFieldsMongo(doc, 50);
        }
        long mongoTime = System.currentTimeMillis() - mongoStart;

        printResult("IndexedDocument + 访问全部", fastTime, mongoTime);
    }

    /**
     * 场景2: 解析 + 只访问5个字段 (10%)
     * 惰性解析的优势场景
     */
    private void testScenario2_ParseAndAccessPartialFields() {
        System.out.println("场景2: 解析 + 只访问5个字段 (10%)");

        byte[] bsonData = BsonTestDataGenerator.generateDocument(50);

        // 预热 FastBSON
        for (int i = 0; i < WARMUP; i++) {
            BsonDocument doc = FastBson.parse(bsonData);
            accessPartialFields(doc);
        }

        // FastBSON测试
        long fastStart = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++) {
            BsonDocument doc = FastBson.parse(bsonData);
            accessPartialFields(doc);
        }
        long fastTime = System.currentTimeMillis() - fastStart;

        // 预热 MongoDB
        for (int i = 0; i < WARMUP; i++) {
            org.bson.BsonDocument doc = parseMongoDB(bsonData);
            accessPartialFieldsMongo(doc);
        }

        // MongoDB测试
        long mongoStart = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++) {
            org.bson.BsonDocument doc = parseMongoDB(bsonData);
            accessPartialFieldsMongo(doc);
        }
        long mongoTime = System.currentTimeMillis() - mongoStart;

        printResult("IndexedDocument + 访问10%", fastTime, mongoTime);
    }

    /**
     * 场景3: HashMap急切解析模式
     * 这是 Phase 3 优化的目标场景
     */
    private void testScenario3_HashMapEagerParsing() {
        System.out.println("场景3: HashMap急切解析 + 访问全部");

        byte[] bsonData = BsonTestDataGenerator.generateDocument(50);

        // 切换到HashMap模式
        FastBson.useHashMapFactory();

        // 预热 FastBSON
        for (int i = 0; i < WARMUP; i++) {
            BsonDocument doc = (BsonDocument) DocumentParser.INSTANCE.parse(new BsonReader(bsonData));
            accessAllFields(doc, 50);
        }

        // FastBSON测试
        long fastStart = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++) {
            BsonDocument doc = (BsonDocument) DocumentParser.INSTANCE.parse(new BsonReader(bsonData));
            accessAllFields(doc, 50);
        }
        long fastTime = System.currentTimeMillis() - fastStart;

        // 恢复默认
        FastBson.useFastFactory();

        // 预热 MongoDB
        for (int i = 0; i < WARMUP; i++) {
            org.bson.BsonDocument doc = parseMongoDB(bsonData);
            accessAllFieldsMongo(doc, 50);
        }

        // MongoDB测试
        long mongoStart = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++) {
            org.bson.BsonDocument doc = parseMongoDB(bsonData);
            accessAllFieldsMongo(doc, 50);
        }
        long mongoTime = System.currentTimeMillis() - mongoStart;

        printResult("HashMap急切解析 + 访问全部", fastTime, mongoTime);
    }

    /**
     * 场景4: 批量解析 + 访问 (模拟数据库查询)
     * 1000条记录，每条访问3个字段
     */
    private void testScenario4_BatchParseAndAccess() {
        System.out.println("场景4: 批量1000条 + 每条访问3字段");

        // 生成1000个不同的文档
        List<byte[]> documents = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            documents.add(BsonTestDataGenerator.generateDocument(20));
        }

        // 预热 FastBSON
        for (int w = 0; w < 10; w++) {
            for (byte[] data : documents) {
                BsonDocument doc = FastBson.parse(data);
                doc.getInt32("field0", 0);
                doc.getString("field1", "");
                doc.getDouble("field2", 0.0);
            }
        }

        // FastBSON测试
        long fastStart = System.currentTimeMillis();
        for (int iter = 0; iter < 100; iter++) {
            for (byte[] data : documents) {
                BsonDocument doc = FastBson.parse(data);
                doc.getInt32("field0", 0);
                doc.getString("field1", "");
                doc.getDouble("field2", 0.0);
            }
        }
        long fastTime = System.currentTimeMillis() - fastStart;

        // 预热 MongoDB
        for (int w = 0; w < 10; w++) {
            for (byte[] data : documents) {
                org.bson.BsonDocument doc = parseMongoDB(data);
                doc.getInt32("field0");
                doc.getString("field1");
                doc.getDouble("field2");
            }
        }

        // MongoDB测试
        long mongoStart = System.currentTimeMillis();
        for (int iter = 0; iter < 100; iter++) {
            for (byte[] data : documents) {
                org.bson.BsonDocument doc = parseMongoDB(data);
                doc.getInt32("field0");
                doc.getString("field1");
                doc.getDouble("field2");
            }
        }
        long mongoTime = System.currentTimeMillis() - mongoStart;

        printResult("批量1000条×100次 + 访问3字段", fastTime, mongoTime);
    }

    // ==================== 辅助方法 ====================

    private void accessAllFields(BsonDocument doc, int fieldCount) {
        for (int i = 0; i < fieldCount; i++) {
            doc.get("field" + i);
        }
    }

    private void accessAllFieldsMongo(org.bson.BsonDocument doc, int fieldCount) {
        for (int i = 0; i < fieldCount; i++) {
            doc.get("field" + i);
        }
    }

    private void accessPartialFields(BsonDocument doc) {
        doc.getInt32("field0", 0);
        doc.getString("field10", "");
        doc.getDouble("field20", 0.0);
        doc.getBoolean("field30", false);
        doc.getInt64("field40", 0L);
    }

    private void accessPartialFieldsMongo(org.bson.BsonDocument doc) {
        doc.get("field0");
        doc.get("field10");
        doc.get("field20");
        doc.get("field30");
        doc.get("field40");
    }

    private org.bson.BsonDocument parseMongoDB(byte[] data) {
        BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
            new org.bson.ByteBufNIO(ByteBuffer.wrap(data))));
        BsonDocumentCodec codec = new BsonDocumentCodec();
        org.bson.BsonDocument doc = codec.decode(reader, DecoderContext.builder().build());
        reader.close();
        return doc;
    }

    private void printResult(String scenario, long fastTime, long mongoTime) {
        double speedup = (double) mongoTime / fastTime;
        System.out.println("  FastBSON: " + fastTime + " ms");
        System.out.println("  MongoDB:  " + mongoTime + " ms");
        System.out.println("  Speedup:  " + String.format("%.2fx", speedup));
        System.out.println();
    }
}
