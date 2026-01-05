package com.cloud.fastbson.benchmark;

import com.cloud.fastbson.FastBson;
import com.cloud.fastbson.document.BsonDocument;
import org.junit.jupiter.api.Test;

/**
 * 分析各阶段耗时，找出真正的瓶颈
 */
public class OptimizationAnalysis {

    private static final int ITERATIONS = 100000;

    @Test
    public void analyzeBottlenecks() {
        System.out.println("\n=== 瓶颈分析 ===\n");

        byte[] bsonData = BsonTestDataGenerator.generateDocument(50);

        // 预热
        for (int i = 0; i < 1000; i++) {
            BsonDocument doc = FastBson.parse(bsonData);
            doc.get("field0");
        }

        // 测试1: 纯解析（建索引）
        long parseStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            FastBson.parse(bsonData);
        }
        long parseTime = System.nanoTime() - parseStart;

        // 测试2: 解析 + 访问1个字段
        long access1Start = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            BsonDocument doc = FastBson.parse(bsonData);
            doc.get("field0");
        }
        long access1Time = System.nanoTime() - access1Start;

        // 测试3: 解析 + 访问5个字段
        long access5Start = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            BsonDocument doc = FastBson.parse(bsonData);
            doc.get("field0");
            doc.get("field10");
            doc.get("field20");
            doc.get("field30");
            doc.get("field40");
        }
        long access5Time = System.nanoTime() - access5Start;

        // 测试4: 解析 + 访问全部50字段
        long accessAllStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            BsonDocument doc = FastBson.parse(bsonData);
            for (int j = 0; j < 50; j++) {
                doc.get("field" + j);
            }
        }
        long accessAllTime = System.nanoTime() - accessAllStart;

        // 测试5: 重复访问同一字段（测试缓存效果）
        BsonDocument cachedDoc = FastBson.parse(bsonData);
        long cacheStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            cachedDoc.get("field25");  // 中间位置
        }
        long cacheTime = System.nanoTime() - cacheStart;

        // 输出分析
        System.out.println("测试次数: " + ITERATIONS);
        System.out.println();

        double parseNs = (double) parseTime / ITERATIONS;
        double access1Ns = (double) access1Time / ITERATIONS;
        double access5Ns = (double) access5Time / ITERATIONS;
        double accessAllNs = (double) accessAllTime / ITERATIONS;
        double cacheNs = (double) cacheTime / ITERATIONS;

        System.out.println("1. 纯解析（建索引）: " + String.format("%.0f ns", parseNs));
        System.out.println("2. 解析+访问1字段:  " + String.format("%.0f ns", access1Ns) +
                          " (字段访问: " + String.format("%.0f ns", access1Ns - parseNs) + ")");
        System.out.println("3. 解析+访问5字段:  " + String.format("%.0f ns", access5Ns) +
                          " (字段访问: " + String.format("%.0f ns", (access5Ns - parseNs) / 5) + "/字段)");
        System.out.println("4. 解析+访问50字段: " + String.format("%.0f ns", accessAllNs) +
                          " (字段访问: " + String.format("%.0f ns", (accessAllNs - parseNs) / 50) + "/字段)");
        System.out.println("5. 缓存访问:        " + String.format("%.0f ns", cacheNs) + "/次");

        System.out.println();
        System.out.println("=== 瓶颈分布 ===");
        System.out.println("解析占比(访问全部): " + String.format("%.1f%%", parseNs / accessAllNs * 100));
        System.out.println("字段访问占比:       " + String.format("%.1f%%", (accessAllNs - parseNs) / accessAllNs * 100));

        // 测试6: String.hashCode 开销
        String fieldName = "field25";
        long hashStart = System.nanoTime();
        int hash = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            hash += fieldName.hashCode();
        }
        long hashTime = System.nanoTime() - hashStart;
        System.out.println();
        System.out.println("String.hashCode: " + String.format("%.1f ns", (double) hashTime / ITERATIONS));

        // 测试7: "field" + i 字符串拼接开销
        long concatStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            String s = "field" + (i % 50);
        }
        long concatTime = System.nanoTime() - concatStart;
        System.out.println("字符串拼接 \"field\"+i: " + String.format("%.1f ns", (double) concatTime / ITERATIONS));
    }
}
