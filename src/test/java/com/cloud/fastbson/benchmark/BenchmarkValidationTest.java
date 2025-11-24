package com.cloud.fastbson.benchmark;

import com.cloud.fastbson.handler.TypeHandler;
import com.cloud.fastbson.reader.BsonReader;
import org.bson.BsonBinaryReader;
import org.bson.BsonDocument;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.DecoderContext;
import org.bson.io.ByteBufferBsonInput;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 Benchmark 代码的功能正确性
 */
public class BenchmarkValidationTest {

    @Test
    public void testSmallDocumentParsing() {
        byte[] bsonData = BsonTestDataGenerator.generateDocument(10);

        // FastBSON 解析
        BsonReader reader = new BsonReader(bsonData);
        TypeHandler handler = new TypeHandler();
        Map<String, Object> fastbsonResult = handler.parseDocument(reader);

        // MongoDB BSON 解析
        BsonBinaryReader mongoReader = new BsonBinaryReader(new ByteBufferBsonInput(
            new org.bson.ByteBufNIO(ByteBuffer.wrap(bsonData))));
        BsonDocumentCodec codec = new BsonDocumentCodec();
        BsonDocument mongoResult = codec.decode(mongoReader, DecoderContext.builder().build());
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

        BsonReader reader = new BsonReader(bsonData);
        TypeHandler handler = new TypeHandler();
        Map<String, Object> result = handler.parseDocument(reader);

        assertNotNull(result);
        assertEquals(50, result.size());
    }

    @Test
    public void testLargeDocumentParsing() {
        byte[] bsonData = BsonTestDataGenerator.generateDocument(100);

        BsonReader reader = new BsonReader(bsonData);
        TypeHandler handler = new TypeHandler();
        Map<String, Object> result = handler.parseDocument(reader);

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
            BsonReader fastbsonReader = new BsonReader(bsonData);
            TypeHandler handler = new TypeHandler();
            Map<String, Object> fastbsonResult = handler.parseDocument(fastbsonReader);

            BsonBinaryReader mongoReader = new BsonBinaryReader(new ByteBufferBsonInput(
                new org.bson.ByteBufNIO(ByteBuffer.wrap(bsonData))));
            BsonDocumentCodec codec = new BsonDocumentCodec();
            BsonDocument mongoResult = codec.decode(mongoReader, DecoderContext.builder().build());
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
            BsonReader reader = new BsonReader(bsonData);
            TypeHandler handler = new TypeHandler();
            handler.parseDocument(reader);
        }

        // FastBSON 性能测试
        long fastbsonStart = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            BsonReader reader = new BsonReader(bsonData);
            TypeHandler handler = new TypeHandler();
            handler.parseDocument(reader);
        }
        long fastbsonTime = System.nanoTime() - fastbsonStart;

        // MongoDB BSON 性能测试
        long mongoStart = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
                new org.bson.ByteBufNIO(ByteBuffer.wrap(bsonData))));
            BsonDocumentCodec codec = new BsonDocumentCodec();
            codec.decode(reader, DecoderContext.builder().build());
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
