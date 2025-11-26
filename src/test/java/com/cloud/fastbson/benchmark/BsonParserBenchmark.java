package com.cloud.fastbson.benchmark;
import com.cloud.fastbson.handler.parsers.DocumentParser;

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

    /**
     * String 密集型文档（80% String 字段，50个字段）
     */
    @State(Scope.Thread)
    public static class StringHeavyDocumentState {
        public byte[] bsonData;

        @Setup(Level.Trial)
        public void setup() {
            bsonData = BsonTestDataGenerator.generateStringHeavyDocument(50);
        }
    }

    /**
     * 纯 String 文档（100% String 字段，50个字段）
     */
    @State(Scope.Thread)
    public static class PureStringDocumentState {
        public byte[] bsonData;

        @Setup(Level.Trial)
        public void setup() {
            bsonData = BsonTestDataGenerator.generatePureStringDocument(50);
        }
    }

    /**
     * 数值密集型文档（100% Int32/Int64，50个字段）
     */
    @State(Scope.Thread)
    public static class NumericHeavyDocumentState {
        public byte[] bsonData;

        @Setup(Level.Trial)
        public void setup() {
            bsonData = BsonTestDataGenerator.generateNumericHeavyDocument(50);
        }
    }

    /**
     * 数组密集型文档（20个数组，每个100个元素）
     */
    @State(Scope.Thread)
    public static class ArrayHeavyDocumentState {
        public byte[] bsonData;

        @Setup(Level.Trial)
        public void setup() {
            bsonData = BsonTestDataGenerator.generateArrayHeavyDocument(20, 100);
        }
    }

    /**
     * 100KB 文档
     */
    @State(Scope.Thread)
    public static class Document100KBState {
        public byte[] bsonData;

        @Setup(Level.Trial)
        public void setup() {
            bsonData = BsonTestDataGenerator.generate100KBDocument();
        }
    }

    /**
     * 1MB 文档
     */
    @State(Scope.Thread)
    public static class Document1MBState {
        public byte[] bsonData;

        @Setup(Level.Trial)
        public void setup() {
            bsonData = BsonTestDataGenerator.generate1MBDocument();
        }
    }

    // ==================== 小文档测试 ====================

    @Benchmark
    public com.cloud.fastbson.document.BsonDocument fastbson_small(SmallDocumentState state) {
        BsonReader reader = new BsonReader(state.bsonData);
        TypeHandler handler = new TypeHandler();
        return (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);
    }

    @Benchmark
    public org.bson.BsonDocument mongodb_small(SmallDocumentState state) {
        BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
            new org.bson.ByteBufNIO(ByteBuffer.wrap(state.bsonData))));
        BsonDocumentCodec codec = new BsonDocumentCodec();
        org.bson.BsonDocument result = codec.decode(reader, DecoderContext.builder().build());
        reader.close();
        return result;
    }

    // ==================== 中等文档测试 ====================

    @Benchmark
    public com.cloud.fastbson.document.BsonDocument fastbson_medium(MediumDocumentState state) {
        BsonReader reader = new BsonReader(state.bsonData);
        TypeHandler handler = new TypeHandler();
        return (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);
    }

    @Benchmark
    public org.bson.BsonDocument mongodb_medium(MediumDocumentState state) {
        BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
            new org.bson.ByteBufNIO(ByteBuffer.wrap(state.bsonData))));
        BsonDocumentCodec codec = new BsonDocumentCodec();
        org.bson.BsonDocument result = codec.decode(reader, DecoderContext.builder().build());
        reader.close();
        return result;
    }

    // ==================== 大文档测试 ====================

    @Benchmark
    public com.cloud.fastbson.document.BsonDocument fastbson_large(LargeDocumentState state) {
        BsonReader reader = new BsonReader(state.bsonData);
        TypeHandler handler = new TypeHandler();
        return (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);
    }

    @Benchmark
    public org.bson.BsonDocument mongodb_large(LargeDocumentState state) {
        BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
            new org.bson.ByteBufNIO(ByteBuffer.wrap(state.bsonData))));
        BsonDocumentCodec codec = new BsonDocumentCodec();
        org.bson.BsonDocument result = codec.decode(reader, DecoderContext.builder().build());
        reader.close();
        return result;
    }

    // ==================== String 密集型测试 ====================

    @Benchmark
    public com.cloud.fastbson.document.BsonDocument fastbson_stringHeavy(StringHeavyDocumentState state) {
        BsonReader reader = new BsonReader(state.bsonData);
        TypeHandler handler = new TypeHandler();
        return (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);
    }

    @Benchmark
    public org.bson.BsonDocument mongodb_stringHeavy(StringHeavyDocumentState state) {
        BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
            new org.bson.ByteBufNIO(ByteBuffer.wrap(state.bsonData))));
        BsonDocumentCodec codec = new BsonDocumentCodec();
        org.bson.BsonDocument result = codec.decode(reader, DecoderContext.builder().build());
        reader.close();
        return result;
    }

    // ==================== 纯 String 测试 ====================

    @Benchmark
    public com.cloud.fastbson.document.BsonDocument fastbson_pureString(PureStringDocumentState state) {
        BsonReader reader = new BsonReader(state.bsonData);
        TypeHandler handler = new TypeHandler();
        return (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);
    }

    @Benchmark
    public org.bson.BsonDocument mongodb_pureString(PureStringDocumentState state) {
        BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
            new org.bson.ByteBufNIO(ByteBuffer.wrap(state.bsonData))));
        BsonDocumentCodec codec = new BsonDocumentCodec();
        org.bson.BsonDocument result = codec.decode(reader, DecoderContext.builder().build());
        reader.close();
        return result;
    }

    // ==================== 数值密集型测试 ====================

    @Benchmark
    public com.cloud.fastbson.document.BsonDocument fastbson_numericHeavy(NumericHeavyDocumentState state) {
        BsonReader reader = new BsonReader(state.bsonData);
        TypeHandler handler = new TypeHandler();
        return (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);
    }

    @Benchmark
    public org.bson.BsonDocument mongodb_numericHeavy(NumericHeavyDocumentState state) {
        BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
            new org.bson.ByteBufNIO(ByteBuffer.wrap(state.bsonData))));
        BsonDocumentCodec codec = new BsonDocumentCodec();
        org.bson.BsonDocument result = codec.decode(reader, DecoderContext.builder().build());
        reader.close();
        return result;
    }

    // ==================== 数组密集型测试 ====================

    @Benchmark
    public com.cloud.fastbson.document.BsonDocument fastbson_arrayHeavy(ArrayHeavyDocumentState state) {
        BsonReader reader = new BsonReader(state.bsonData);
        TypeHandler handler = new TypeHandler();
        return (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);
    }

    @Benchmark
    public org.bson.BsonDocument mongodb_arrayHeavy(ArrayHeavyDocumentState state) {
        BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
            new org.bson.ByteBufNIO(ByteBuffer.wrap(state.bsonData))));
        BsonDocumentCodec codec = new BsonDocumentCodec();
        org.bson.BsonDocument result = codec.decode(reader, DecoderContext.builder().build());
        reader.close();
        return result;
    }

    // ==================== 100KB 文档测试 ====================

    @Benchmark
    public com.cloud.fastbson.document.BsonDocument fastbson_100KB(Document100KBState state) {
        BsonReader reader = new BsonReader(state.bsonData);
        TypeHandler handler = new TypeHandler();
        return (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);
    }

    @Benchmark
    public org.bson.BsonDocument mongodb_100KB(Document100KBState state) {
        BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
            new org.bson.ByteBufNIO(ByteBuffer.wrap(state.bsonData))));
        BsonDocumentCodec codec = new BsonDocumentCodec();
        org.bson.BsonDocument result = codec.decode(reader, DecoderContext.builder().build());
        reader.close();
        return result;
    }

    // ==================== 1MB 文档测试 ====================

    @Benchmark
    public com.cloud.fastbson.document.BsonDocument fastbson_1MB(Document1MBState state) {
        BsonReader reader = new BsonReader(state.bsonData);
        TypeHandler handler = new TypeHandler();
        return (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);
    }

    @Benchmark
    public org.bson.BsonDocument mongodb_1MB(Document1MBState state) {
        BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(
            new org.bson.ByteBufNIO(ByteBuffer.wrap(state.bsonData))));
        BsonDocumentCodec codec = new BsonDocumentCodec();
        org.bson.BsonDocument result = codec.decode(reader, DecoderContext.builder().build());
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
