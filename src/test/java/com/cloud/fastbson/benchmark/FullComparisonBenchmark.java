package com.cloud.fastbson.benchmark;

import com.cloud.fastbson.FastBson;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.handler.parsers.DocumentParser;
import com.cloud.fastbson.parser.PartialParser;
import com.cloud.fastbson.reader.BsonReader;
import org.bson.BsonBinaryReader;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.DecoderContext;
import org.bson.io.ByteBufferBsonInput;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 全场景对比 Benchmark
 *
 * 统一预热次数和测试次数，确保公平对比
 */
public class FullComparisonBenchmark {

    private static final int WARMUP = 1000;
    private static final int ITERATIONS = 10000;

    @Test
    public void runFullComparison() {
        System.out.println("\n========================================");
        System.out.println("       FastBSON 全场景性能对比");
        System.out.println("========================================");
        System.out.println("预热次数: " + WARMUP);
        System.out.println("测试次数: " + ITERATIONS);
        System.out.println("========================================\n");

        // 场景1: 纯解析（不访问字段）
        runScenario1_ParseOnly();

        // 场景2: 解析 + 访问全部字段
        runScenario2_ParseAndAccessAll();

        // 场景3: 解析 + 访问10%字段
        runScenario3_ParseAndAccess10Percent();

        // 场景4: 解析 + 访问1个字段
        runScenario4_ParseAndAccess1Field();

        // 场景5: PartialParser 早退优化
        runScenario5_PartialParserEarlyExit();

        // 场景6: 批量解析 + 访问
        runScenario6_BatchParseAndAccess();

        // 场景7: HashMap模式完整解析
        runScenario7_HashMapMode();

        System.out.println("========================================");
    }

    private void runScenario1_ParseOnly() {
        System.out.println("【场景1】纯解析（50字段，不访问）");
        byte[] bsonData = BsonTestDataGenerator.generateDocument(50);

        // 预热
        for (int i = 0; i < WARMUP; i++) {
            FastBson.parse(bsonData);
        }
        for (int i = 0; i < WARMUP; i++) {
            parseMongoDB(bsonData);
        }

        // FastBSON
        long fastStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            FastBson.parse(bsonData);
        }
        long fastTime = System.nanoTime() - fastStart;

        // MongoDB
        long mongoStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            parseMongoDB(bsonData);
        }
        long mongoTime = System.nanoTime() - mongoStart;

        printResult(fastTime, mongoTime);
    }

    private void runScenario2_ParseAndAccessAll() {
        System.out.println("【场景2】解析 + 访问全部50字段");
        byte[] bsonData = BsonTestDataGenerator.generateDocument(50);

        // 预热
        for (int i = 0; i < WARMUP; i++) {
            BsonDocument doc = FastBson.parse(bsonData);
            for (int j = 0; j < 50; j++) doc.get("field" + j);
        }
        for (int i = 0; i < WARMUP; i++) {
            org.bson.BsonDocument doc = parseMongoDB(bsonData);
            for (int j = 0; j < 50; j++) doc.get("field" + j);
        }

        // FastBSON
        long fastStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            BsonDocument doc = FastBson.parse(bsonData);
            for (int j = 0; j < 50; j++) doc.get("field" + j);
        }
        long fastTime = System.nanoTime() - fastStart;

        // MongoDB
        long mongoStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            org.bson.BsonDocument doc = parseMongoDB(bsonData);
            for (int j = 0; j < 50; j++) doc.get("field" + j);
        }
        long mongoTime = System.nanoTime() - mongoStart;

        printResult(fastTime, mongoTime);
    }

    private void runScenario3_ParseAndAccess10Percent() {
        System.out.println("【场景3】解析 + 访问5字段（10%）");
        byte[] bsonData = BsonTestDataGenerator.generateDocument(50);

        // 预热
        for (int i = 0; i < WARMUP; i++) {
            BsonDocument doc = FastBson.parse(bsonData);
            doc.get("field0"); doc.get("field10"); doc.get("field20");
            doc.get("field30"); doc.get("field40");
        }
        for (int i = 0; i < WARMUP; i++) {
            org.bson.BsonDocument doc = parseMongoDB(bsonData);
            doc.get("field0"); doc.get("field10"); doc.get("field20");
            doc.get("field30"); doc.get("field40");
        }

        // FastBSON
        long fastStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            BsonDocument doc = FastBson.parse(bsonData);
            doc.get("field0"); doc.get("field10"); doc.get("field20");
            doc.get("field30"); doc.get("field40");
        }
        long fastTime = System.nanoTime() - fastStart;

        // MongoDB
        long mongoStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            org.bson.BsonDocument doc = parseMongoDB(bsonData);
            doc.get("field0"); doc.get("field10"); doc.get("field20");
            doc.get("field30"); doc.get("field40");
        }
        long mongoTime = System.nanoTime() - mongoStart;

        printResult(fastTime, mongoTime);
    }

    private void runScenario4_ParseAndAccess1Field() {
        System.out.println("【场景4】解析 + 访问1字段");
        byte[] bsonData = BsonTestDataGenerator.generateDocument(50);

        // 预热
        for (int i = 0; i < WARMUP; i++) {
            BsonDocument doc = FastBson.parse(bsonData);
            doc.get("field25");
        }
        for (int i = 0; i < WARMUP; i++) {
            org.bson.BsonDocument doc = parseMongoDB(bsonData);
            doc.get("field25");
        }

        // FastBSON
        long fastStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            BsonDocument doc = FastBson.parse(bsonData);
            doc.get("field25");
        }
        long fastTime = System.nanoTime() - fastStart;

        // MongoDB
        long mongoStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            org.bson.BsonDocument doc = parseMongoDB(bsonData);
            doc.get("field25");
        }
        long mongoTime = System.nanoTime() - mongoStart;

        printResult(fastTime, mongoTime);
    }

    private void runScenario5_PartialParserEarlyExit() {
        System.out.println("【场景5】PartialParser早退（100字段取5个）");
        byte[] bsonData = BsonTestDataGenerator.generateDocument(100);
        PartialParser parser = new PartialParser("field0", "field10", "field20", "field30", "field40");
        parser.setEarlyExit(true);

        // 预热
        for (int i = 0; i < WARMUP; i++) {
            parser.parse(bsonData);
        }
        for (int i = 0; i < WARMUP; i++) {
            org.bson.BsonDocument doc = parseMongoDB(bsonData);
            doc.get("field0"); doc.get("field10"); doc.get("field20");
            doc.get("field30"); doc.get("field40");
        }

        // FastBSON
        long fastStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            parser.parse(bsonData);
        }
        long fastTime = System.nanoTime() - fastStart;

        // MongoDB
        long mongoStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            org.bson.BsonDocument doc = parseMongoDB(bsonData);
            doc.get("field0"); doc.get("field10"); doc.get("field20");
            doc.get("field30"); doc.get("field40");
        }
        long mongoTime = System.nanoTime() - mongoStart;

        printResult(fastTime, mongoTime);
    }

    private void runScenario6_BatchParseAndAccess() {
        System.out.println("【场景6】批量1000条 + 每条访问3字段");
        List<byte[]> documents = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            documents.add(BsonTestDataGenerator.generateDocument(20));
        }

        // 预热
        for (int w = 0; w < 10; w++) {
            for (byte[] data : documents) {
                BsonDocument doc = FastBson.parse(data);
                doc.get("field0"); doc.get("field5"); doc.get("field10");
            }
        }
        for (int w = 0; w < 10; w++) {
            for (byte[] data : documents) {
                org.bson.BsonDocument doc = parseMongoDB(data);
                doc.get("field0"); doc.get("field5"); doc.get("field10");
            }
        }

        // FastBSON
        long fastStart = System.nanoTime();
        for (int iter = 0; iter < 100; iter++) {
            for (byte[] data : documents) {
                BsonDocument doc = FastBson.parse(data);
                doc.get("field0"); doc.get("field5"); doc.get("field10");
            }
        }
        long fastTime = System.nanoTime() - fastStart;

        // MongoDB
        long mongoStart = System.nanoTime();
        for (int iter = 0; iter < 100; iter++) {
            for (byte[] data : documents) {
                org.bson.BsonDocument doc = parseMongoDB(data);
                doc.get("field0"); doc.get("field5"); doc.get("field10");
            }
        }
        long mongoTime = System.nanoTime() - mongoStart;

        printResult(fastTime, mongoTime);
    }

    private void runScenario7_HashMapMode() {
        System.out.println("【场景7】HashMap模式完整解析 + 访问全部");
        byte[] bsonData = BsonTestDataGenerator.generateDocument(50);

        FastBson.useHashMapFactory();

        // 预热
        for (int i = 0; i < WARMUP; i++) {
            BsonDocument doc = (BsonDocument) DocumentParser.INSTANCE.parse(new BsonReader(bsonData));
            for (int j = 0; j < 50; j++) doc.get("field" + j);
        }

        // FastBSON
        long fastStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            BsonDocument doc = (BsonDocument) DocumentParser.INSTANCE.parse(new BsonReader(bsonData));
            for (int j = 0; j < 50; j++) doc.get("field" + j);
        }
        long fastTime = System.nanoTime() - fastStart;

        FastBson.useFastFactory();

        // MongoDB (预热已在前面场景完成)
        for (int i = 0; i < WARMUP; i++) {
            org.bson.BsonDocument doc = parseMongoDB(bsonData);
            for (int j = 0; j < 50; j++) doc.get("field" + j);
        }

        long mongoStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            org.bson.BsonDocument doc = parseMongoDB(bsonData);
            for (int j = 0; j < 50; j++) doc.get("field" + j);
        }
        long mongoTime = System.nanoTime() - mongoStart;

        printResult(fastTime, mongoTime);
    }

    private org.bson.BsonDocument parseMongoDB(byte[] data) {
        BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
            new org.bson.ByteBufNIO(ByteBuffer.wrap(data))));
        BsonDocumentCodec codec = new BsonDocumentCodec();
        org.bson.BsonDocument doc = codec.decode(reader, DecoderContext.builder().build());
        reader.close();
        return doc;
    }

    private void printResult(long fastTime, long mongoTime) {
        long fastMs = fastTime / 1_000_000;
        long mongoMs = mongoTime / 1_000_000;
        double speedup = (double) mongoTime / fastTime;
        System.out.println("  FastBSON: " + fastMs + " ms");
        System.out.println("  MongoDB:  " + mongoMs + " ms");
        System.out.println("  Speedup:  " + String.format("%.2fx", speedup));
        System.out.println();
    }
}
