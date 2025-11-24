# FastBSON 性能基准测试

## 概述

本模块使用 JMH (Java Microbenchmark Harness) 对 FastBSON 与官方 `org.mongodb:bson` 进行性能对比测试。

## 测试场景

### 1. 完整文档解析对比

- **小文档** (10个字段, < 1KB)
- **中等文档** (50个字段, 1-10KB)
- **大文档** (100个字段, > 10KB)

### 2. 测试指标

- **吞吐量** (ops/sec) - 每秒可处理的文档数量
- **延迟** (ns/op) - 单次解析的平均耗时

## 运行基准测试

### 方式1: 使用 Maven 编译后运行

```bash
# 编译测试代码
mvn test-compile

# 运行 benchmark
java -cp target/test-classes:target/classes:~/.m2/repository/org/openjdk/jmh/jmh-core/1.37/jmh-core-1.37.jar:~/.m2/repository/org/mongodb/bson/4.11.0/bson-4.11.0.jar com.cloud.fastbson.benchmark.BsonParserBenchmark
```

### 方式2: 使用 JMH Maven 插件

在 pom.xml 中添加 jmh-maven-plugin 后运行：

```bash
mvn test -Dtest=BsonParserBenchmark
```

### 方式3: 直接运行 main 方法

```bash
mvn exec:java -Dexec.mainClass="com.cloud.fastbson.benchmark.BsonParserBenchmark" \
  -Dexec.classpathScope=test
```

## 查看结果

测试完成后会输出类似以下格式的结果：

```
Benchmark                                Mode  Cnt      Score      Error  Units
BsonParserBenchmark.fastbson_small      thrpt    5  10000.123 ±  100.456  ops/s
BsonParserBenchmark.mongodb_small       thrpt    5   8000.789 ±   80.123  ops/s
BsonParserBenchmark.fastbson_medium     thrpt    5   5000.456 ±   50.789  ops/s
BsonParserBenchmark.mongodb_medium      thrpt    5   4000.123 ±   40.456  ops/s
BsonParserBenchmark.fastbson_large      thrpt    5   2000.789 ±   20.123  ops/s
BsonParserBenchmark.mongodb_large       thrpt    5   1500.456 ±   15.789  ops/s
```

## 测试数据生成

`BsonTestDataGenerator` 使用官方 `org.mongodb:bson` 库生成标准的 BSON 测试数据，确保测试的公平性。

支持生成：
- 简单文档（混合类型字段）
- 嵌套文档
- 包含数组的文档

## 性能优化方向

基于 benchmark 结果，可以识别性能瓶颈：

1. **字段解析** - 是否需要优化特定类型的解析
2. **内存分配** - 是否有不必要的对象创建
3. **字符串处理** - 字段名和字符串值的处理效率

## Phase 2 目标

建立性能基线，为后续 Phase 3（部分字段解析）和 Phase 4（性能优化）提供对比依据。
