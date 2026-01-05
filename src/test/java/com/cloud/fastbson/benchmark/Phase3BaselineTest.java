package com.cloud.fastbson.benchmark;

import com.cloud.fastbson.FastBson;
import com.cloud.fastbson.document.BsonDocument;
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
 * Phase 3 Baseline Test - works on both v0.0.2 and current version.
 */
public class Phase3BaselineTest {

    private static final int WARMUP = 100;
    private static final int ITERATIONS = 1000;

    @Test
    public void testPhase3AllScenarios() {
        System.out.println("\n=== Phase 3 Test (Current Version) ===\n");

        // Phase 3.1: StringPool - batch parsing same structure documents
        testPhase3_1_StringPool();

        // Phase 3.2: ObjectPool - high throughput continuous parsing
        testPhase3_2_ObjectPool();

        // Phase 3.3: HashMap capacity - fixed structure documents
        testPhase3_3_HashMapCapacity();
    }

    private void testPhase3_1_StringPool() {
        System.out.println("Phase 3.1: StringPool - 批量解析相同结构文档");

        byte[] singleDoc = BsonTestDataGenerator.generateDocument(50);
        List<byte[]> documents = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            documents.add(singleDoc);
        }

        // Warmup FastBSON
        for (int i = 0; i < WARMUP; i++) {
            for (byte[] doc : documents) {
                FastBson.parse(doc);
            }
        }

        // FastBSON test
        long fastStart = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++) {
            for (byte[] doc : documents) {
                FastBson.parse(doc);
            }
        }
        long fastTime = System.currentTimeMillis() - fastStart;

        // Warmup MongoDB
        for (int i = 0; i < WARMUP; i++) {
            for (byte[] doc : documents) {
                parseMongoDB(doc);
            }
        }

        // MongoDB test
        long mongoStart = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++) {
            for (byte[] doc : documents) {
                parseMongoDB(doc);
            }
        }
        long mongoTime = System.currentTimeMillis() - mongoStart;

        double speedup = (double) mongoTime / fastTime;
        System.out.println("  FastBSON: " + fastTime + " ms");
        System.out.println("  MongoDB:  " + mongoTime + " ms");
        System.out.println("  Speedup:  " + String.format("%.2fx", speedup));
        System.out.println();
    }

    private void testPhase3_2_ObjectPool() {
        System.out.println("Phase 3.2: ObjectPool - 高吞吐量连续解析");

        List<byte[]> documents = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            documents.add(BsonTestDataGenerator.generateDocument(20));
        }

        // Warmup FastBSON
        for (int i = 0; i < WARMUP * 10; i++) {
            FastBson.parse(documents.get(i % documents.size()));
        }

        // FastBSON test
        long fastStart = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS * 10; i++) {
            FastBson.parse(documents.get(i % documents.size()));
        }
        long fastTime = System.currentTimeMillis() - fastStart;

        // Warmup MongoDB
        for (int i = 0; i < WARMUP * 10; i++) {
            parseMongoDB(documents.get(i % documents.size()));
        }

        // MongoDB test
        long mongoStart = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS * 10; i++) {
            parseMongoDB(documents.get(i % documents.size()));
        }
        long mongoTime = System.currentTimeMillis() - mongoStart;

        double speedup = (double) mongoTime / fastTime;
        System.out.println("  FastBSON: " + fastTime + " ms");
        System.out.println("  MongoDB:  " + mongoTime + " ms");
        System.out.println("  Speedup:  " + String.format("%.2fx", speedup));
        System.out.println();
    }

    private void testPhase3_3_HashMapCapacity() {
        System.out.println("Phase 3.3: HashMap容量 - 固定结构文档");

        byte[] bsonData = BsonTestDataGenerator.generateDocument(50);

        // Warmup FastBSON
        for (int i = 0; i < WARMUP * 10; i++) {
            FastBson.parse(bsonData);
        }

        // FastBSON test
        long fastStart = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS * 10; i++) {
            FastBson.parse(bsonData);
        }
        long fastTime = System.currentTimeMillis() - fastStart;

        // Warmup MongoDB
        for (int i = 0; i < WARMUP * 10; i++) {
            parseMongoDB(bsonData);
        }

        // MongoDB test
        long mongoStart = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS * 10; i++) {
            parseMongoDB(bsonData);
        }
        long mongoTime = System.currentTimeMillis() - mongoStart;

        double speedup = (double) mongoTime / fastTime;
        System.out.println("  FastBSON: " + fastTime + " ms");
        System.out.println("  MongoDB:  " + mongoTime + " ms");
        System.out.println("  Speedup:  " + String.format("%.2fx", speedup));
        System.out.println();
    }

    private org.bson.BsonDocument parseMongoDB(byte[] data) {
        BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
            new org.bson.ByteBufNIO(ByteBuffer.wrap(data))));
        BsonDocumentCodec codec = new BsonDocumentCodec();
        org.bson.BsonDocument doc = codec.decode(reader, DecoderContext.builder().build());
        reader.close();
        return doc;
    }
}
