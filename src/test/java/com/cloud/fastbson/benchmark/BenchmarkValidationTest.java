package com.cloud.fastbson.benchmark;

import com.cloud.fastbson.FastBson;
import com.cloud.fastbson.document.BsonDocument;
import org.bson.BsonBinaryReader;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.DecoderContext;
import org.bson.io.ByteBufferBsonInput;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 Benchmark 代码的功能正确性
 */
public class BenchmarkValidationTest {

    @Test
    public void testSmallDocumentParsing() {
        byte[] bsonData = BsonTestDataGenerator.generateDocument(10);

        // FastBSON 解析 (使用零装箱API)
        BsonDocument fastbsonResult = FastBson.parse(bsonData);

        // MongoDB BSON 解析
        BsonBinaryReader mongoReader = new BsonBinaryReader(new ByteBufferBsonInput(
            new org.bson.ByteBufNIO(ByteBuffer.wrap(bsonData))));
        BsonDocumentCodec codec = new BsonDocumentCodec();
        org.bson.BsonDocument mongoResult = codec.decode(mongoReader, DecoderContext.builder().build());
        mongoReader.close();

        // 验证
        assertNotNull(fastbsonResult);
        assertNotNull(mongoResult);
        assertEquals(10, fastbsonResult.size());
        assertEquals(10, mongoResult.size());
    }

    @Test
    public void testMediumDocumentParsing() {
        byte[] bsonData = BsonTestDataGenerator.generateDocument(50);

        BsonDocument result = FastBson.parse(bsonData);

        assertNotNull(result);
        assertEquals(50, result.size());
    }

    @Test
    public void testLargeDocumentParsing() {
        byte[] bsonData = BsonTestDataGenerator.generateDocument(100);

        BsonDocument result = FastBson.parse(bsonData);

        assertNotNull(result);
        assertEquals(100, result.size());
    }

    @Test
    public void testDataGeneratorCreatesValidBson() {
        for (int fieldCount : new int[]{1, 5, 10, 50, 100}) {
            byte[] bsonData = BsonTestDataGenerator.generateDocument(fieldCount);

            assertNotNull(bsonData);
            assertTrue(bsonData.length > 4, "BSON data should be at least 5 bytes (4 for length + 1 for terminator)");

            // 验证可以被两个库解析
            BsonDocument fastbsonResult = FastBson.parse(bsonData);

            BsonBinaryReader mongoReader = new BsonBinaryReader(new ByteBufferBsonInput(
                new org.bson.ByteBufNIO(ByteBuffer.wrap(bsonData))));
            BsonDocumentCodec codec = new BsonDocumentCodec();
            org.bson.BsonDocument mongoResult = codec.decode(mongoReader, DecoderContext.builder().build());
            mongoReader.close();

            assertEquals(fieldCount, fastbsonResult.size());
            assertEquals(fieldCount, mongoResult.size());
        }
    }

    @Test
    public void testPerformanceComparison() {
        byte[] bsonData = BsonTestDataGenerator.generateDocument(50);

        // 预热
        for (int i = 0; i < 100; i++) {
            FastBson.parse(bsonData);
        }

        // FastBSON 性能测试 (零装箱API)
        long fastbsonStart = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            BsonDocument doc = FastBson.parse(bsonData);
            // 访问一些字段来确保真实使用场景
            int size = doc.size();
        }
        long fastbsonTime = System.nanoTime() - fastbsonStart;

        // MongoDB BSON 性能测试
        long mongoStart = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
                new org.bson.ByteBufNIO(ByteBuffer.wrap(bsonData))));
            BsonDocumentCodec codec = new BsonDocumentCodec();
            org.bson.BsonDocument doc = codec.decode(reader, DecoderContext.builder().build());
            // 访问一些字段来确保真实使用场景
            int size = doc.size();
            reader.close();
        }
        long mongoTime = System.nanoTime() - mongoStart;

        System.out.println("FastBSON: " + (fastbsonTime / 1_000_000) + " ms");
        System.out.println("MongoDB BSON: " + (mongoTime / 1_000_000) + " ms");
        System.out.println("Speedup: " + String.format("%.2fx", (double) mongoTime / fastbsonTime));

        // 验证 FastBSON 至少不会比 MongoDB 慢太多（允许在 3x 范围内）
        assertTrue(fastbsonTime < mongoTime * 3,
            "FastBSON should not be more than 3x slower than MongoDB BSON");
    }
}
