package com.cloud.fastbson.benchmark;
import com.cloud.fastbson.handler.parsers.DocumentParser;

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
 * 扩展 Benchmark 场景验证测试
 *
 * <p>测试新增的 benchmark 场景并记录性能基线数据
 */
public class ExtendedBenchmarkValidationTest {

    private final TypeHandler handler = new TypeHandler();

    @Test
    public void testStringHeavyDocument() {
        byte[] bsonData = BsonTestDataGenerator.generateStringHeavyDocument(50);

        // 验证数据大小合理
        assertTrue(bsonData.length > 0);
        System.out.println("String Heavy Document size: " + bsonData.length + " bytes");

        // 测试 FastBSON
        long fastbsonStart = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            BsonReader reader = new BsonReader(bsonData);
            com.cloud.fastbson.document.BsonDocument result = (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);
            assertEquals(50, result.size());
        }
        long fastbsonTime = System.nanoTime() - fastbsonStart;

        // 测试 MongoDB BSON
        long mongoStart = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
                new org.bson.ByteBufNIO(ByteBuffer.wrap(bsonData))));
            BsonDocumentCodec codec = new BsonDocumentCodec();
            BsonDocument doc = codec.decode(reader, DecoderContext.builder().build());
            assertEquals(50, doc.size());
            reader.close();
        }
        long mongoTime = System.nanoTime() - mongoStart;

        System.out.println("String Heavy (80% String, 50 fields):");
        System.out.println("  FastBSON: " + (fastbsonTime / 1_000_000) + " ms");
        System.out.println("  MongoDB:  " + (mongoTime / 1_000_000) + " ms");
        System.out.println("  Speedup:  " + String.format("%.2fx", (double) mongoTime / fastbsonTime));
    }

    @Test
    public void testPureStringDocument() {
        byte[] bsonData = BsonTestDataGenerator.generatePureStringDocument(50);

        assertTrue(bsonData.length > 0);
        System.out.println("Pure String Document size: " + bsonData.length + " bytes");

        // 测试 FastBSON
        long fastbsonStart = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            BsonReader reader = new BsonReader(bsonData);
            com.cloud.fastbson.document.BsonDocument result = (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);
            assertEquals(50, result.size());
        }
        long fastbsonTime = System.nanoTime() - fastbsonStart;

        // 测试 MongoDB BSON
        long mongoStart = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
                new org.bson.ByteBufNIO(ByteBuffer.wrap(bsonData))));
            BsonDocumentCodec codec = new BsonDocumentCodec();
            BsonDocument doc = codec.decode(reader, DecoderContext.builder().build());
            assertEquals(50, doc.size());
            reader.close();
        }
        long mongoTime = System.nanoTime() - mongoStart;

        System.out.println("Pure String (100% String, 50 fields):");
        System.out.println("  FastBSON: " + (fastbsonTime / 1_000_000) + " ms");
        System.out.println("  MongoDB:  " + (mongoTime / 1_000_000) + " ms");
        System.out.println("  Speedup:  " + String.format("%.2fx", (double) mongoTime / fastbsonTime));
    }

    @Test
    public void testNumericHeavyDocument() {
        byte[] bsonData = BsonTestDataGenerator.generateNumericHeavyDocument(50);

        assertTrue(bsonData.length > 0);
        System.out.println("Numeric Heavy Document size: " + bsonData.length + " bytes");

        // 测试 FastBSON
        long fastbsonStart = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            BsonReader reader = new BsonReader(bsonData);
            com.cloud.fastbson.document.BsonDocument result = (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);
            assertEquals(50, result.size());
        }
        long fastbsonTime = System.nanoTime() - fastbsonStart;

        // 测试 MongoDB BSON
        long mongoStart = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
                new org.bson.ByteBufNIO(ByteBuffer.wrap(bsonData))));
            BsonDocumentCodec codec = new BsonDocumentCodec();
            BsonDocument doc = codec.decode(reader, DecoderContext.builder().build());
            assertEquals(50, doc.size());
            reader.close();
        }
        long mongoTime = System.nanoTime() - mongoStart;

        System.out.println("Numeric Heavy (100% Int32/Int64, 50 fields):");
        System.out.println("  FastBSON: " + (fastbsonTime / 1_000_000) + " ms");
        System.out.println("  MongoDB:  " + (mongoTime / 1_000_000) + " ms");
        System.out.println("  Speedup:  " + String.format("%.2fx", (double) mongoTime / fastbsonTime));
    }

    @Test
    public void testArrayHeavyDocument() {
        byte[] bsonData = BsonTestDataGenerator.generateArrayHeavyDocument(20, 100);

        assertTrue(bsonData.length > 0);
        System.out.println("Array Heavy Document size: " + bsonData.length + " bytes");

        // 测试 FastBSON
        long fastbsonStart = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            BsonReader reader = new BsonReader(bsonData);
            com.cloud.fastbson.document.BsonDocument result = (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);
            assertEquals(20, result.size());
        }
        long fastbsonTime = System.nanoTime() - fastbsonStart;

        // 测试 MongoDB BSON
        long mongoStart = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
                new org.bson.ByteBufNIO(ByteBuffer.wrap(bsonData))));
            BsonDocumentCodec codec = new BsonDocumentCodec();
            BsonDocument doc = codec.decode(reader, DecoderContext.builder().build());
            assertEquals(20, doc.size());
            reader.close();
        }
        long mongoTime = System.nanoTime() - mongoStart;

        System.out.println("Array Heavy - Mixed Types (20 arrays x 100 elements):");
        System.out.println("  FastBSON: " + (fastbsonTime / 1_000_000) + " ms");
        System.out.println("  MongoDB:  " + (mongoTime / 1_000_000) + " ms");
        System.out.println("  Speedup:  " + String.format("%.2fx", (double) mongoTime / fastbsonTime));
    }

    @Test
    public void testHomogeneousInt32ArrayDocument() {
        byte[] bsonData = BsonTestDataGenerator.generateHomogeneousInt32ArrayDocument(20, 100);

        assertTrue(bsonData.length > 0);
        System.out.println("\nHomogeneous Int32 Array Document size: " + bsonData.length + " bytes");

        // 测试 FastBSON
        long fastbsonStart = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            BsonReader reader = new BsonReader(bsonData);
            com.cloud.fastbson.document.BsonDocument result = (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);
            assertEquals(20, result.size());
        }
        long fastbsonTime = System.nanoTime() - fastbsonStart;

        // 测试 MongoDB BSON
        long mongoStart = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
                new org.bson.ByteBufNIO(ByteBuffer.wrap(bsonData))));
            BsonDocumentCodec codec = new BsonDocumentCodec();
            BsonDocument doc = codec.decode(reader, DecoderContext.builder().build());
            assertEquals(20, doc.size());
            reader.close();
        }
        long mongoTime = System.nanoTime() - mongoStart;

        System.out.println("Homogeneous Int32 Arrays (20 arrays x 100 Int32):");
        System.out.println("  FastBSON: " + (fastbsonTime / 1_000_000) + " ms");
        System.out.println("  MongoDB:  " + (mongoTime / 1_000_000) + " ms");
        System.out.println("  Speedup:  " + String.format("%.2fx", (double) mongoTime / fastbsonTime));
    }

    @Test
    public void testHomogeneousStringArrayDocument() {
        byte[] bsonData = BsonTestDataGenerator.generateHomogeneousStringArrayDocument(20, 100);

        assertTrue(bsonData.length > 0);
        System.out.println("\nHomogeneous String Array Document size: " + bsonData.length + " bytes");

        // 测试 FastBSON
        long fastbsonStart = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            BsonReader reader = new BsonReader(bsonData);
            com.cloud.fastbson.document.BsonDocument result = (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);
            assertEquals(20, result.size());
        }
        long fastbsonTime = System.nanoTime() - fastbsonStart;

        // 测试 MongoDB BSON
        long mongoStart = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
                new org.bson.ByteBufNIO(ByteBuffer.wrap(bsonData))));
            BsonDocumentCodec codec = new BsonDocumentCodec();
            BsonDocument doc = codec.decode(reader, DecoderContext.builder().build());
            assertEquals(20, doc.size());
            reader.close();
        }
        long mongoTime = System.nanoTime() - mongoStart;

        System.out.println("Homogeneous String Arrays (20 arrays x 100 String):");
        System.out.println("  FastBSON: " + (fastbsonTime / 1_000_000) + " ms");
        System.out.println("  MongoDB:  " + (mongoTime / 1_000_000) + " ms");
        System.out.println("  Speedup:  " + String.format("%.2fx", (double) mongoTime / fastbsonTime));
    }

    @Test
    public void testHomogeneousDoubleArrayDocument() {
        byte[] bsonData = BsonTestDataGenerator.generateHomogeneousDoubleArrayDocument(20, 100);

        assertTrue(bsonData.length > 0);
        System.out.println("\nHomogeneous Double Array Document size: " + bsonData.length + " bytes");

        // 测试 FastBSON
        long fastbsonStart = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            BsonReader reader = new BsonReader(bsonData);
            com.cloud.fastbson.document.BsonDocument result = (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);
            assertEquals(20, result.size());
        }
        long fastbsonTime = System.nanoTime() - fastbsonStart;

        // 测试 MongoDB BSON
        long mongoStart = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
                new org.bson.ByteBufNIO(ByteBuffer.wrap(bsonData))));
            BsonDocumentCodec codec = new BsonDocumentCodec();
            BsonDocument doc = codec.decode(reader, DecoderContext.builder().build());
            assertEquals(20, doc.size());
            reader.close();
        }
        long mongoTime = System.nanoTime() - mongoStart;

        System.out.println("Homogeneous Double Arrays (20 arrays x 100 Double):");
        System.out.println("  FastBSON: " + (fastbsonTime / 1_000_000) + " ms");
        System.out.println("  MongoDB:  " + (mongoTime / 1_000_000) + " ms");
        System.out.println("  Speedup:  " + String.format("%.2fx", (double) mongoTime / fastbsonTime));
    }

    @Test
    public void test100KBDocument() {
        byte[] bsonData = BsonTestDataGenerator.generate100KBDocument();

        assertTrue(bsonData.length >= 100 * 1024);
        System.out.println("100KB Document size: " + bsonData.length + " bytes (" +
            String.format("%.1f", bsonData.length / 1024.0) + " KB)");

        // 测试 FastBSON
        long fastbsonStart = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            BsonReader reader = new BsonReader(bsonData);
            com.cloud.fastbson.document.BsonDocument result = (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);
            assertTrue(result.size() > 0);
        }
        long fastbsonTime = System.nanoTime() - fastbsonStart;

        // 测试 MongoDB BSON
        long mongoStart = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
                new org.bson.ByteBufNIO(ByteBuffer.wrap(bsonData))));
            BsonDocumentCodec codec = new BsonDocumentCodec();
            BsonDocument doc = codec.decode(reader, DecoderContext.builder().build());
            assertTrue(doc.size() > 0);
            reader.close();
        }
        long mongoTime = System.nanoTime() - mongoStart;

        System.out.println("100KB Document:");
        System.out.println("  FastBSON: " + (fastbsonTime / 1_000_000) + " ms");
        System.out.println("  MongoDB:  " + (mongoTime / 1_000_000) + " ms");
        System.out.println("  Speedup:  " + String.format("%.2fx", (double) mongoTime / fastbsonTime));
    }

    @Test
    public void test1MBDocument() {
        byte[] bsonData = BsonTestDataGenerator.generate1MBDocument();

        assertTrue(bsonData.length >= 1024 * 1024);
        System.out.println("1MB Document size: " + bsonData.length + " bytes (" +
            String.format("%.2f", bsonData.length / (1024.0 * 1024.0)) + " MB)");

        // 测试 FastBSON（减少迭代次数）
        long fastbsonStart = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            BsonReader reader = new BsonReader(bsonData);
            com.cloud.fastbson.document.BsonDocument result = (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);
            assertTrue(result.size() > 0);
        }
        long fastbsonTime = System.nanoTime() - fastbsonStart;

        // 测试 MongoDB BSON
        long mongoStart = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
                new org.bson.ByteBufNIO(ByteBuffer.wrap(bsonData))));
            BsonDocumentCodec codec = new BsonDocumentCodec();
            BsonDocument doc = codec.decode(reader, DecoderContext.builder().build());
            assertTrue(doc.size() > 0);
            reader.close();
        }
        long mongoTime = System.nanoTime() - mongoStart;

        System.out.println("1MB Document:");
        System.out.println("  FastBSON: " + (fastbsonTime / 1_000_000) + " ms");
        System.out.println("  MongoDB:  " + (mongoTime / 1_000_000) + " ms");
        System.out.println("  Speedup:  " + String.format("%.2fx", (double) mongoTime / fastbsonTime));
    }
}
