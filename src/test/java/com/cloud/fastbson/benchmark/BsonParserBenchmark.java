package com.cloud.fastbson.benchmark;

import com.cloud.fastbson.handler.TypeHandler;
import com.cloud.fastbson.reader.BsonReader;
import org.bson.BsonBinaryReader;
import org.bson.BsonDocument;
import org.bson.BsonWriter;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.DecoderContext;
import org.bson.io.BasicOutputBuffer;
import org.bson.io.ByteBufferBsonInput;
import org.openjdk.jmh.annotations.*;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 性能基准测试：FastBSON vs org.mongodb.bson
 *
 * 测试场景：
 * 1. 小文档（< 1KB）- 10个字段
 * 2. 中等文档（1-10KB）- 50个字段
 * 3. 大文档（> 10KB）- 100个字段
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class BsonParserBenchmark {

    /**
     * 小文档测试数据（10个字段）
     */
    @State(Scope.Thread)
    public static class SmallDocumentState {
        public byte[] bsonData;

        @Setup(Level.Trial)
        public void setup() {
            bsonData = BsonTestDataGenerator.generateDocument(10);
        }
    }

    /**
     * 中等文档测试数据（50个字段）
     */
    @State(Scope.Thread)
    public static class MediumDocumentState {
        public byte[] bsonData;

        @Setup(Level.Trial)
        public void setup() {
            bsonData = BsonTestDataGenerator.generateDocument(50);
        }
    }

    /**
     * 大文档测试数据（100个字段）
     */
    @State(Scope.Thread)
    public static class LargeDocumentState {
        public byte[] bsonData;

        @Setup(Level.Trial)
        public void setup() {
            bsonData = BsonTestDataGenerator.generateDocument(100);
        }
    }

    // ==================== 小文档测试 ====================

    @Benchmark
    public Map<String, Object> fastbson_small(SmallDocumentState state) {
        BsonReader reader = new BsonReader(state.bsonData);
        TypeHandler handler = new TypeHandler();
        return handler.parseDocument(reader);
    }

    @Benchmark
    public BsonDocument mongodb_small(SmallDocumentState state) {
        BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
            new org.bson.ByteBufNIO(ByteBuffer.wrap(state.bsonData))));
        BsonDocumentCodec codec = new BsonDocumentCodec();
        BsonDocument result = codec.decode(reader, DecoderContext.builder().build());
        reader.close();
        return result;
    }

    // ==================== 中等文档测试 ====================

    @Benchmark
    public Map<String, Object> fastbson_medium(MediumDocumentState state) {
        BsonReader reader = new BsonReader(state.bsonData);
        TypeHandler handler = new TypeHandler();
        return handler.parseDocument(reader);
    }

    @Benchmark
    public BsonDocument mongodb_medium(MediumDocumentState state) {
        BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
            new org.bson.ByteBufNIO(ByteBuffer.wrap(state.bsonData))));
        BsonDocumentCodec codec = new BsonDocumentCodec();
        BsonDocument result = codec.decode(reader, DecoderContext.builder().build());
        reader.close();
        return result;
    }

    // ==================== 大文档测试 ====================

    @Benchmark
    public Map<String, Object> fastbson_large(LargeDocumentState state) {
        BsonReader reader = new BsonReader(state.bsonData);
        TypeHandler handler = new TypeHandler();
        return handler.parseDocument(reader);
    }

    @Benchmark
    public BsonDocument mongodb_large(LargeDocumentState state) {
        BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
            new org.bson.ByteBufNIO(ByteBuffer.wrap(state.bsonData))));
        BsonDocumentCodec codec = new BsonDocumentCodec();
        BsonDocument result = codec.decode(reader, DecoderContext.builder().build());
        reader.close();
        return result;
    }

    /**
     * 运行基准测试
     */
    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
