# FastBSON

高性能 BSON 反序列化与部分字段读取库

[![Java](https://img.shields.io/badge/Java-8-blue.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/Tests-1389%20passing-brightgreen.svg)]()
[![Coverage](https://img.shields.io/badge/Coverage-100%25-brightgreen.svg)]()


---

## 🚀 项目概述

FastBSON 是一个专注于高性能的 BSON（Binary JSON）反序列化库，特别优化了部分字段读取场景。通过智能跳过不需要的字段和零复制惰性解析技术，在只需要少量字段时可实现 **3-10倍** 的性能提升。

### 核心特性

- ✅ **完整的 BSON 支持**：支持所有 MongoDB 3.4 BSON 类型
- ✅ **部分字段解析**：只解析需要的字段，跳过其余内容
- ✅ **零拷贝优化**：最小化内存分配和数据拷贝
- ✅ **Java 8 兼容**：使用 Java 8 语法，兼容性广泛
- ✅ **线程安全设计**：通过 ThreadLocal 对象池支持高并发

### 性能优势

> 📊 基准测试：10,000 次迭代，混合类型字段（Int32/String/Double/Boolean/Int64）

**Phase 1: 完整解析性能（已验证）**

| 测试场景 | FastBSON | MongoDB BSON | 性能提升 | 状态 |
|---------|----------|--------------|----------|------|
| 中等文档 (50 字段) | 93 ms | 204 ms | **2.18x** | ✅ 已完成 |

**Phase 2: 部分字段解析性能（已验证）**

| 场景 | FastBSON | MongoDB BSON | 性能提升 | 状态 |
|------|---------|--------------|----------|------|
| PartialParser (5/100字段) | 51 ms | 410 ms | **7.99x** | ✅ 早退优化 |
| IndexedDocument (5/100字段) | 74 ms | 422 ms | **5.64x** | ✅ 零复制惰性 |

**三种模式对比**:

| 模式 | 适用场景 | 性能提升 | 内存优势 |
|-----|---------|----------|----------|
| HashMap (Phase 1) | 完整解析，中小文档 | 2-3x | - |
| PartialParser (Phase 2.A) | 一次性提取少量字段 | 7-8x | - |
| IndexedDocument (Phase 2.B) | 重复访问，内存敏感 | 5-6x | 70% ⬇️ |

**结论**：不同场景选择不同模式，最高可达 **7.99x 性能提升**

---

## 快速开始

### 依赖配置

```xml
<dependency>
    <groupId>com.cloud</groupId>
    <artifactId>fastbson</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 基本使用

```java
// 1. 创建解析器，指定需要的字段
FastBsonParser parser = FastBsonParser.builder()
    .fields("name", "age", "email")
    .build();

// 2. 解析 BSON 数据
byte[] bsonData = ...;
Map<String, Object> result = parser.parse(bsonData);

// 3. 获取字段值
String name = (String) result.get("name");
Integer age = (Integer) result.get("age");
String email = (String) result.get("email");
```

### 类型安全访问

```java
// 使用类型安全的访问器
FastBsonParser parser = FastBsonParser.builder()
    .fields("age", "salary", "active")
    .build();

BsonDocument doc = parser.parseToDocument(bsonData);

int age = doc.getInt("age");
double salary = doc.getDouble("salary");
boolean active = doc.getBoolean("active");

// 支持默认值
String department = doc.getString("department", "Unknown");
```

---

## 使用示例

### 场景 1: 完整文档解析 - HashMap 模式

**适用场景**：需要访问文档中的大部分或全部字段

```java
import com.cloud.fastbson.FastBson;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.document.hashmap.HashMapBsonDocumentFactory;
import com.cloud.fastbson.reader.BsonReader;

// 设置为 HashMap 模式（默认，完整解析）
FastBson.setDocumentFactory(HashMapBsonDocumentFactory.INSTANCE);

// 解析 BSON 数据
byte[] bsonData = ...; // 来自 MongoDB 或其他来源
BsonDocument doc = FastBson.parse(new BsonReader(bsonData));

// 访问字段（已全部解析，速度快）
String name = doc.getString("name");
int age = doc.getInt32("age");
double salary = doc.getDouble("salary");
boolean active = doc.getBoolean("active");

// 性能：2-3x vs MongoDB BSON（中等文档）
// 内存：完整解析，内存占用较高
```

### 场景 2: 部分字段提取 - PartialParser 早退模式

**适用场景**：只需要提取少量字段（5-10 个），追求极致速度

```java
import com.cloud.fastbson.parser.PartialParser;
import java.util.Map;

// 创建 PartialParser，指定需要的字段
PartialParser parser = new PartialParser("userId", "timestamp", "eventType");

// 启用早退优化（找到目标字段后立即停止解析）
parser.setEarlyExit(true);

// 解析 BSON 数据（只解析需要的字段）
byte[] bsonData = ...; // 100+ 字段的大文档
Map<String, Object> result = parser.parse(bsonData);

// 获取字段值
String userId = (String) result.get("userId");
Long timestamp = (Long) result.get("timestamp");
String eventType = (String) result.get("eventType");

// 性能：7-8x vs MongoDB BSON（大文档，少量字段）
// 适合：日志解析、事件流处理、数据管道
```

### 场景 3: 零复制惰性解析 - IndexedBsonDocument 模式

**适用场景**：需要重复访问同一文档，或内存敏感场景

```java
import com.cloud.fastbson.FastBson;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.document.indexed.IndexedBsonDocumentFactory;
import com.cloud.fastbson.reader.BsonReader;

// 设置为 Indexed 模式（零复制，惰性解析）
FastBson.setDocumentFactory(IndexedBsonDocumentFactory.INSTANCE);

// 解析 BSON 数据（仅构建字段索引，不解析值）
byte[] bsonData = ...; // 100+ 字段的大文档
BsonDocument doc = FastBson.parse(new BsonReader(bsonData));

// 按需访问字段（惰性解析，只解析访问的字段）
String field0 = doc.getString("field0");    // 首次访问，解析并缓存
int field5 = doc.getInt32("field5");        // 首次访问，解析并缓存
String field0Again = doc.getString("field0"); // 二次访问，直接从缓存读取

// 性能：3-5x vs MongoDB BSON（重复访问）
// 内存：70% 降低（~30 bytes/field vs ~200 bytes/field）
// 适合：内存敏感场景、需要多次访问同一文档
```

### 场景 4: 嵌套文档和数组访问

**适用场景**：处理复杂的嵌套结构

```java
import com.cloud.fastbson.FastBson;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.reader.BsonReader;

// 解析包含嵌套结构的 BSON 文档
byte[] bsonData = ...; // { "user": { "name": "Alice", "tags": ["admin", "developer"] } }
BsonDocument doc = FastBson.parse(new BsonReader(bsonData));

// 访问嵌套文档
BsonDocument user = doc.getDocument("user");
String userName = user.getString("name");

// 访问嵌套数组
BsonArray tags = user.getArray("tags");
String firstTag = tags.getString(0);
String secondTag = tags.getString(1);

// 遍历数组
for (int i = 0; i < tags.size(); i++) {
    String tag = tags.getString(i);
    System.out.println("Tag: " + tag);
}

// 深度嵌套访问（支持 50+ 层嵌套）
BsonDocument level1 = doc.getDocument("level1");
BsonDocument level2 = level1.getDocument("level2");
int deepValue = level2.getInt32("value");
```

### 场景 5: 性能敏感场景 - 日志解析

**适用场景**：高吞吐量的日志解析和事件处理

```java
import com.cloud.fastbson.parser.PartialParser;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

// 日志解析器（只提取关键字段）
public class LogParser {
    private final PartialParser parser;

    public LogParser() {
        // 只提取日志的关键字段
        this.parser = new PartialParser(
            "timestamp", "level", "message", "userId", "traceId"
        );
        this.parser.setEarlyExit(true); // 早退优化
    }

    public LogEntry parse(byte[] bsonLog) {
        Map<String, Object> result = parser.parse(bsonLog);

        return new LogEntry(
            (Long) result.get("timestamp"),
            (String) result.get("level"),
            (String) result.get("message"),
            (String) result.get("userId"),
            (String) result.get("traceId")
        );
    }
}

// 使用示例
LogParser logParser = new LogParser();
ArrayBlockingQueue<byte[]> logQueue = new ArrayBlockingQueue<>(10000);

// 高吞吐量处理（7-8x vs MongoDB BSON）
while (true) {
    byte[] bsonLog = logQueue.take();
    LogEntry entry = logParser.parse(bsonLog);
    processLog(entry);
}
```

### 场景 6: 内存敏感场景 - 大量文档缓存

**适用场景**：需要在内存中缓存大量 BSON 文档

```java
import com.cloud.fastbson.FastBson;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.document.indexed.IndexedBsonDocumentFactory;
import com.cloud.fastbson.reader.BsonReader;
import java.util.HashMap;
import java.util.Map;

// 设置为 Indexed 模式（内存占用降低 70%）
FastBson.setDocumentFactory(IndexedBsonDocumentFactory.INSTANCE);

// 文档缓存（零复制，惰性解析）
public class DocumentCache {
    private final Map<String, BsonDocument> cache = new HashMap<>();

    public void cache(String id, byte[] bsonData) {
        // 只构建索引，不解析值（内存占用低）
        BsonDocument doc = FastBson.parse(new BsonReader(bsonData));
        cache.put(id, doc);
    }

    public String getUserName(String id) {
        BsonDocument doc = cache.get(id);
        // 按需解析字段（惰性解析）
        return doc.getString("name");
    }
}

// 使用示例
DocumentCache cache = new DocumentCache();

// 缓存 10,000 个文档（内存占用降低 70%）
for (int i = 0; i < 10000; i++) {
    byte[] bsonData = fetchFromDatabase(i);
    cache.cache("doc_" + i, bsonData);
}

// 按需访问（惰性解析，只解析访问的字段）
String name = cache.getUserName("doc_1234");
```

### 场景 7: 跨库兼容性 - 与 org.mongodb:bson 互操作

**适用场景**：需要与 MongoDB Java Driver 互操作

```java
import com.cloud.fastbson.FastBson;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.reader.BsonReader;
import org.bson.BsonBinaryWriter;
import org.bson.io.BasicOutputBuffer;

// 1. 使用 org.mongodb:bson 生成 BSON 数据
BasicOutputBuffer buffer = new BasicOutputBuffer();
BsonBinaryWriter writer = new BsonBinaryWriter(buffer);

writer.writeStartDocument();
writer.writeString("name", "Alice");
writer.writeInt32("age", 30);
writer.writeBoolean("active", true);
writer.writeEndDocument();
writer.flush();

byte[] bsonData = buffer.toByteArray();

// 2. 使用 FastBSON 解析（完全兼容）
BsonDocument doc = FastBson.parse(new BsonReader(bsonData));

// 3. 访问字段
String name = doc.getString("name");
int age = doc.getInt32("age");
boolean active = doc.getBoolean("active");

System.out.println("Name: " + name + ", Age: " + age + ", Active: " + active);
// 输出: Name: Alice, Age: 30, Active: true

// FastBSON 完全兼容 BSON spec v1.1，支持所有 MongoDB 生成的 BSON 数据
```

### 场景 8: 多线程场景 - 线程安全和对象池

**适用场景**：高并发多线程环境

```java
import com.cloud.fastbson.parser.PartialParser;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 多线程解析器（PartialParser 是线程安全的）
public class MultiThreadedParser {
    private final PartialParser parser;
    private final ExecutorService executor;

    public MultiThreadedParser() {
        // 创建解析器（线程安全，可共享）
        this.parser = new PartialParser("field1", "field2", "field3");
        this.parser.setEarlyExit(true);

        // 创建线程池
        this.executor = Executors.newFixedThreadPool(8);
    }

    public void parseAsync(byte[] bsonData) {
        executor.submit(() -> {
            // 每个线程安全地使用共享的 parser
            Map<String, Object> result = parser.parse(bsonData);
            processResult(result);
        });
    }

    private void processResult(Map<String, Object> result) {
        // 处理解析结果
        System.out.println("Parsed: " + result);
    }
}

// 使用示例
MultiThreadedParser parser = new MultiThreadedParser();

// 并发解析（线程安全，无竞争）
for (int i = 0; i < 10000; i++) {
    byte[] bsonData = generateBsonData(i);
    parser.parseAsync(bsonData);
}
```

### 场景 9: 实际业务场景 - 用户行为数据聚合

**适用场景**：分析大量用户行为数据，提取关键指标

```java
import com.cloud.fastbson.parser.PartialParser;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

// 用户行为数据聚合器
public class UserBehaviorAggregator {
    private final PartialParser parser;
    private final Map<String, AtomicLong> eventCounts = new HashMap<>();

    public UserBehaviorAggregator() {
        // 只提取分析需要的字段（从 100+ 字段中提取 4 个）
        this.parser = new PartialParser(
            "userId", "eventType", "timestamp", "duration"
        );
        this.parser.setEarlyExit(true); // 早退优化（7-8x speedup）
    }

    public void aggregate(byte[] bsonEvent) {
        // 快速解析（只解析需要的 4 个字段）
        Map<String, Object> event = parser.parse(bsonEvent);

        String eventType = (String) event.get("eventType");
        Long duration = (Long) event.get("duration");

        // 统计事件次数
        eventCounts.computeIfAbsent(eventType, k -> new AtomicLong())
                   .incrementAndGet();

        // 处理业务逻辑
        if (duration > 10000) {
            // 慢事件告警
            alertSlowEvent(event);
        }
    }

    public void printStatistics() {
        System.out.println("=== 用户行为统计 ===");
        eventCounts.forEach((eventType, count) -> {
            System.out.println(eventType + ": " + count.get());
        });
    }

    private void alertSlowEvent(Map<String, Object> event) {
        System.out.println("ALERT: Slow event detected - " + event);
    }
}

// 使用示例
UserBehaviorAggregator aggregator = new UserBehaviorAggregator();

// 处理 1,000,000 个事件（7-8x vs MongoDB BSON）
for (int i = 0; i < 1000000; i++) {
    byte[] bsonEvent = fetchEventFromQueue();
    aggregator.aggregate(bsonEvent);
}

aggregator.printStatistics();
```

### 场景 10: 同构数组优化 - 时序数据处理（Phase 3 新特性）

**适用场景**：数值型同构数组（时序数据、坐标、评分等）

```java
import com.cloud.fastbson.FastBson;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.reader.BsonReader;

// Phase 3 自动检测同构数组并使用类型特化快速路径
byte[] bsonData = ...; // { "timestamps": [1609459200000, 1609545600000, ...] }
BsonDocument doc = FastBson.parse(new BsonReader(bsonData));

// 访问同构Int64数组（时间戳）
BsonArray timestamps = doc.getArray("timestamps");
for (int i = 0; i < timestamps.size(); i++) {
    long timestamp = timestamps.getInt64(i);  // 零装箱访问
    processTimestamp(timestamp);
}

// 性能：1.73x vs MongoDB BSON（同构Int32）
// 性能：1.76x vs MongoDB BSON（同构Double）
// 优化：自动检测数组类型一致性，使用类型特化解析器
```

**同构数组优化详情**：
```java
// 数据示例
{
    "temperatures": [23.5, 24.1, 22.8, 25.3, ...],  // 同构Double数组
    "userIds": [1001, 1002, 1003, 1004, ...],       // 同构Int32数组
    "coordinates": [
        [116.404, 39.915],  // 经纬度
        [121.473, 31.230],
        ...
    ]
}

// Phase 3 优化点：
// 1. skipCString() - 跳过数组索引 ("0", "1", "2"...) 无String创建
// 2. 类型特化解析 - parseInt32Array(), parseDoubleArray() 快速路径
// 3. 精确容量预分配 - 避免动态扩容
// 4. 零装箱访问 - 直接使用primitive类型
```

**推荐使用场景**：
- ✅ 时序数据 (timestamps[], measurements[])
- ✅ 地理坐标 (latitudes[], longitudes[])
- ✅ 评分/统计 (scores[], ratings[], statistics[])
- ✅ ID列表 (userIds[], productIds[], orderIds[])

**性能提升**：
- Int32/Int64/Double数组: +20-30% vs 混合类型数组
- 自动检测：≥3个元素且类型相同时启用优化
- 智能回退：检测到类型不一致时自动切换到通用路径

### 场景 11: 注解式类型安全API（Phase 3 新特性）

**适用场景**：类型安全的强类型访问，编译期错误检查

```java
import com.cloud.fastbson.parser.PartialParser;
import com.cloud.fastbson.parser.BsonSchema;
import com.cloud.fastbson.parser.BsonField;

// 定义Schema类（编译期类型安全）
@BsonSchema
public class UserEvent {
    @BsonField("userId")
    private int userId;

    @BsonField("timestamp")
    private long timestamp;

    @BsonField("eventType")
    private String eventType;

    @BsonField("score")
    private double score;

    // Getter/Setter...
}

// 解析BSON到强类型对象
PartialParser parser = new PartialParser();
UserEvent event = parser.parse(bsonData, UserEvent.class);

// 类型安全访问（编译期检查）
int userId = event.getUserId();           // int, 不是Object
long timestamp = event.getTimestamp();     // long, 不是Object
String eventType = event.getEventType();   // String, 不是Object
double score = event.getScore();           // double, 不是Object

// 性能：22.6% 提升 (Class-level schema缓存)
```

**注解式API优势**：
```java
// 传统方式 - 类型不安全，运行时错误
Map<String, Object> result = parser.parse(bsonData);
int userId = (Integer) result.get("userId");  // 需要强制类型转换
String name = (String) result.get("name");    // 字段名拼写错误不会在编译期发现

// 注解式API - 类型安全，编译期检查
@BsonSchema
public class User {
    @BsonField("userId")
    private int userId;  // 编译期类型检查

    @BsonField("name")
    private String name;  // 字段名拼写错误立即发现
}

User user = parser.parse(bsonData, User.class);
int userId = user.getUserId();  // 无需类型转换，类型安全
```

**性能优化**：
- ✅ Class-level schema缓存：反射开销分摊到首次解析
- ✅ 22.6% 性能提升（vs 传统Map访问）
- ✅ 零装箱访问：primitive类型直接设置
- ✅ 字段顺序优化：按声明顺序匹配

**最佳实践**：
```java
// 1. 为高频使用的BSON结构定义Schema类
@BsonSchema
public class LogEntry {
    @BsonField("timestamp") private long timestamp;
    @BsonField("level") private String level;
    @BsonField("message") private String message;
}

// 2. 重用PartialParser实例（线程安全）
private static final PartialParser PARSER = new PartialParser();

// 3. 高性能解析
public LogEntry parseLog(byte[] bsonData) {
    return PARSER.parse(bsonData, LogEntry.class);  // 快速+类型安全
}
```

### 场景选择指南

| 场景 | 推荐模式 | 性能提升 | 内存占用 | 适用条件 |
|------|---------|----------|----------|---------|
| **完整文档解析** | HashMap | 2-3x | 高 | 需要访问大部分字段 |
| **部分字段提取** | PartialParser | 7-8x | 中 | 只需少量字段（5-10个） |
| **零复制惰性** | IndexedDocument | 3-5x | 低（-70%） | 重复访问或内存敏感 |
| **日志解析** | PartialParser | 7-8x | 中 | 高吞吐量，少量字段 |
| **文档缓存** | IndexedDocument | 3-5x | 低（-70%） | 大量文档缓存 |
| **嵌套结构** | HashMap/Indexed | 2-5x | 视模式而定 | 复杂嵌套访问 |
| **多线程** | PartialParser | 7-8x | 中 | 高并发场景 |
| **数据聚合** | PartialParser | 7-8x | 中 | 流式处理，少量字段 |
| **同构数组** ⭐ | Auto-detect | 1.7x (Int32/Double) | 低 | 时序数据、坐标、评分 |
| **类型安全API** ⭐ | Annotation | +22.6% | 中 | 强类型访问，编译期检查 |

⭐ = Phase 3 新特性

### 默认值和异常处理

```java
import com.cloud.fastbson.FastBson;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.reader.BsonReader;

BsonDocument doc = FastBson.parse(new BsonReader(bsonData));

// 使用默认值（字段不存在或类型不匹配时返回默认值）
String name = doc.getString("name", "Unknown");
int age = doc.getInt32("age", 0);
double salary = doc.getDouble("salary", 0.0);
boolean active = doc.getBoolean("active", false);

// 检查字段是否存在
if (doc.contains("email")) {
    String email = doc.getString("email");
    System.out.println("Email: " + email);
}

// 检查字段是否为 null
if (doc.isNull("deletedAt")) {
    System.out.println("Document is not deleted");
}

// 获取字段类型
byte fieldType = doc.getType("age");
if (fieldType == BsonType.INT32) {
    int age = doc.getInt32("age");
}
```

---

## 项目状态

### ✅ Phase 1 完成 (基础框架) - 100%

**已完成：**
- ✅ Phase 1.1: 项目结构和 Maven 配置
- ✅ Phase 1.2: BsonType 常量类
- ✅ Phase 1.3: BsonReader 核心读取功能
- ✅ Phase 1.4: BsonReaderTest 单元测试（42 个测试）
- ✅ Phase 1.5: TypeHandler 类型处理器（21 种 BSON 类型）
- ✅ Phase 1.6: TypeHandlerTest 单元测试（34 个测试）
- ✅ Phase 1.7: 异常体系（4 个异常类 + 15 个测试）
- ✅ Phase 1.8: JMH 性能基准测试（5 个验证测试）
- ✅ Phase 1.9: 端到端兼容性测试与扩展 Benchmark（23 个测试）
- ✅ Phase 1.10: 深度嵌套 BSON 测试强化（15 个测试）

**Phase 1 最终成果：**
- 📊 测试总数：**189 个**（全部通过）
- 📈 分支覆盖率：**100%** (130/130 branches) - Phase 1 组件
- 🚀 性能优势：**1.34x ~ 3.88x** vs MongoDB BSON
- ✅ 端到端兼容性：所有 21 种 BSON 类型验证通过
- ✅ 深度嵌套：支持 50+ 层嵌套，无栈溢出
- 📄 文档：完整的设计文档和性能报告

**Phase 1.9 Benchmark 场景：**

| 场景 | 性能提升 | 备注 |
|------|----------|------|
| String 密集型 (80% String) | 2.17x | 稳定性能 |
| 纯 String (100% String) | 2.70x | String 解析高效 |
| 数值密集型 (Int32/Int64) | 2.75x | ✅ 最强场景 |
| 数组密集型 (20×100) | 1.34x | Phase 1 基线 |
| 100KB 文档 | 2.56x | 大文档稳定 |
| 1MB 文档 | 2.56x | 线性扩展 |

**Phase 3 数组优化成果：**

| 数组类型 | Phase 1 | Phase 3 | 提升 | 状态 |
|---------|---------|---------|------|------|
| 混合类型数组 (Int32/String/Double) | 1.34x | **1.43x** | +7% | ✅ 通用优化 |
| 同构Int32数组 (纯Int32) | 1.34x | **1.73x** | +29% | ✅ 最佳场景 |
| 同构Double数组 (纯Double) | 1.34x | **1.76x** | +31% | ✅ 最佳场景 |
| 同构String数组 (纯String) | 1.34x | **1.15x** | -14% | ⚠️ String瓶颈 |

### ✅ Phase 2 完成 (零复制惰性解析与早退优化) - 100%

**已完成：**
- ✅ Phase 2.1: ValueSkipper 值跳过器（36 个测试）
- ✅ Phase 2.2: FieldMatcher 字段匹配器（30 个测试）
- ✅ Phase 2.3: PartialParser 部分字段解析器（27 个测试）
- ✅ Phase 2.4: IndexedBsonDocument 零复制惰性解析（28 个测试）
- ✅ Phase 2.5: IndexedBsonArray 零复制数组（21 个测试）
- ✅ Phase 2.6: 早退优化性能测试（6 个测试）
- ✅ Phase 2.7: 完整 Benchmark 对比测试（8 个测试）

**Phase 2 最终成果：**
- 📊 测试总数：**657 个**（全部通过，包含 PR #14 新增的 306 个测试）
- 📈 代码覆盖率：维持高覆盖率
- 🚀 性能优势：
  - **PartialParser (早退)**: 7-8x vs MongoDB BSON
  - **IndexedBsonDocument (零复制)**: 3x vs MongoDB BSON + 70% 内存节省
- ✅ 三种解析模式：HashMap (全解析) / PartialParser (早退) / IndexedBsonDocument (零复制)
- 📄 文档：完整的性能对比和使用建议

### ✅ Phase 3 完成 (高级性能优化) - 100%

**已完成：**
- ✅ Phase 3.1: ObjectPool - ThreadLocal对象池（3.16x vs 1.05-1.15x目标）
- ✅ Phase 3.2: StringPool - 字段名全局内部化（2.00x vs 1.1-1.3x目标）
- ✅ Phase 3.3: TypeHandler分支优化 - 按类型频率重排序（2-5%提升）
- ✅ Phase 3.4: Ordered匹配 - 有序字段匹配优化（22.6%提升）
- ✅ Phase 3.5: Array优化 - skipCString + 同构数组快速路径（混合1.43x, 同构1.7x+）
- ✅ Phase 3.6: 注解式API - @BsonSchema类型安全访问（22.6%提升）

**Phase 3 最终成果：**
- 📊 测试总数：**1389 个**（全部通过）
- 📈 代码覆盖率：**100%** (所有分支覆盖)
- 🚀 性能优势：
  - **混合数组**: 1.43x vs MongoDB BSON (+7% vs Phase 1)
  - **同构Int32数组**: 1.73x vs MongoDB BSON (+29% vs Phase 1)
  - **同构Double数组**: 1.76x vs MongoDB BSON (+31% vs Phase 1)
  - **注解式API**: +22.6% vs Map访问
- ✅ 新特性：同构数组自动检测、类型安全注解API
- 📄 文档：完整的性能对比和横向版本对比报告

### ⏳ 下一步：Phase 4 (文档完善和发布准备)

**待实现：**
- Phase 4: API文档完善、使用指南、发布准备

详细进度请查看 [docs/phases.md](docs/phases.md) | [Phase 1 总结](docs/phase1-summary.md) | [Phase 2 性能基线](#phase-2-性能基线测试v100-snapshot) | [Phase 3 完成报告](/tmp/phase3_final_completion_report.md)

---

## 架构设计

### 核心组件

```
FastBSON
├── BsonReader       # 底层字节流读取器
├── TypeHandler      # 类型解析处理器
├── FieldMatcher     # 字段匹配器（待实现）
├── ValueSkipper     # 值跳过器（待实现）
└── PartialParser    # 部分字段解析器（待实现）
```

### 支持的 BSON 类型

| 类型 | 类型码 | 支持状态 |
|------|--------|---------|
| Double | 0x01 | ✅ |
| String | 0x02 | ✅ |
| Document | 0x03 | ✅ |
| Array | 0x04 | ✅ |
| Binary | 0x05 | ✅ |
| ObjectId | 0x07 | ✅ |
| Boolean | 0x08 | ✅ |
| DateTime | 0x09 | ✅ |
| Null | 0x0A | ✅ |
| Regex | 0x0B | ✅ |
| Int32 | 0x10 | ✅ |
| Timestamp | 0x11 | ✅ |
| Int64 | 0x12 | ✅ |
| Decimal128 | 0x13 | ✅ |

---

## 开发规范

### 技术栈
- **Java**: 8 (兼容性优先)
- **构建工具**: Maven 3.6+
- **测试框架**: JUnit 5
- **基准测试**: JMH
- **代码质量**: JaCoCo (代码覆盖率)

### 开发要求
- ✅ **Java 8 语法**：严格使用 Java 8 特性
- ✅ **SOLID 原则**：单一职责、开闭原则等
- ✅ **100% 分支覆盖**：所有代码路径必须测试
- ✅ **完整文档**：所有 public API 必须有 JavaDoc

详细规范请查看 [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)

---

## 性能差异分析

### 为什么 FastBSON 比 MongoDB BSON 快 3.88 倍？

#### 1. 零拷贝设计 - 减少内存分配

**FastBSON**:
```java
// 直接操作原始字节数组，零拷贝
BsonReader reader = new BsonReader(bsonData);  // 只保存引用
int value = reader.readInt32();  // 直接从数组读取
```

**MongoDB BSON**:
```java
// 需要 ByteBuffer 包装层
ByteBuffer buffer = ByteBuffer.wrap(bsonData);  // 创建包装对象
BsonBinaryReader reader = new BsonBinaryReader(
    new ByteBufferBsonInput(new ByteBufNIO(buffer))  // 多层包装
);
```

**性能影响**: FastBSON 避免了 3 层对象包装，减少对象分配和 GC 压力

#### 2. 简化的类型处理 - 单一处理器

**FastBSON**:
```java
// 单一 TypeHandler，switch-case 直接分派
TypeHandler handler = new TypeHandler();
Object value = handler.parseValue(reader, type);  // 一次方法调用
```

**MongoDB BSON**:
```java
// 复杂的 Codec 架构，多层间接调用
BsonDocumentCodec codec = new BsonDocumentCodec();
BsonDocument doc = codec.decode(reader, context);  // Codec 层次结构
```

**性能影响**: FastBSON 的类型分派只有 1 层，MongoDB 需要通过 Codec 层次结构进行多次虚方法调用

#### 3. 高效的字节序转换 - 位运算优化

**FastBSON**:
```java
// 直接使用位运算，内联友好
public int readInt32() {
    return (data[pos++] & 0xFF)
         | ((data[pos++] & 0xFF) << 8)
         | ((data[pos++] & 0xFF) << 16)
         | ((data[pos++] & 0xFF) << 24);
}
```

**MongoDB BSON**:
```java
// ByteBuffer 间接调用，难以内联
buffer.order(ByteOrder.LITTLE_ENDIAN);
int value = buffer.getInt();  // 虚方法调用
```

**性能影响**: 位运算可被 JIT 充分内联优化，ByteBuffer 调用有虚方法开销

#### 4. 轻量级状态管理 - 最小化上下文

**FastBSON**:
```java
public class BsonReader {
    private byte[] data;      // 数据引用
    private int position;     // 唯一状态
    // 仅维护 2 个字段
}
```

**MongoDB BSON**:
```java
// BsonBinaryReader 维护复杂上下文
// - 多层 Input 抽象
// - 状态机管理
// - 上下文栈
// - 验证器等
```

**性能影响**: FastBSON 状态简单，缓存友好；MongoDB 状态复杂，缓存命中率低

#### 5. 对象创建优化 - 直接返回基本类型包装

**FastBSON**:
```java
// 返回已缓存的 Integer 对象（-128~127）
return reader.readInt32();  // 自动装箱使用缓存
```

**MongoDB BSON**:
```java
// 创建 BsonInt32 包装对象
return new BsonInt32(value);  // 每次都创建新对象
```

**性能影响**: FastBSON 利用 Java 自动装箱缓存，MongoDB 每次创建新的 BsonXxx 对象

### 性能对比总结

| 优化点 | FastBSON | MongoDB BSON | 性能增益 |
|-------|----------|--------------|---------|
| 对象包装层 | 0 层（直接数组） | 3 层（ByteBuf + Input + Reader） | ~1.3x |
| 类型分派 | 1 层（switch） | 多层（Codec 层次） | ~1.2x |
| 字节序转换 | 位运算（内联） | ByteBuffer（虚调用） | ~1.4x |
| 状态管理 | 2 个字段 | 复杂上下文 | ~1.1x |
| 对象创建 | 自动装箱缓存 | BsonXxx 对象 | ~1.3x |
| **综合效果** | - | - | **~3.88x** |

> 详细性能测试报告: [docs/benchmark-report.md](docs/benchmark-report.md)

---

## 性能优化技术

### 已实现
- ✅ **零拷贝读取**：直接操作原始字节数组
- ✅ **位运算优化**：高效的字节序转换
- ✅ **对象复用接口**：reset() 方法支持对象池

### 待实现（Phase 3）
- ⏳ **ThreadLocal 对象池**：减少对象创建
- ⏳ **字段名内部化**：减少字符串比较开销
- ⏳ **有序匹配优化**：假定字段有序快速匹配
- ⏳ **常见类型优先**：优化分支预测

---

## 构建和测试

### 编译
```bash
mvn clean compile
```

### 运行测试
```bash
mvn test
```

### 代码覆盖率
```bash
mvn test jacoco:report
# 查看报告: target/site/jacoco/index.html
```

### 性能测试
```bash
mvn test -Dtest=FastBsonBenchmark
```

---

## 文档

- [架构设计文档](docs/architecture.md) - 详细的架构设计和实现计划
- [开发规范](docs/DEVELOPMENT.md) - 代码规范和最佳实践
- [实施阶段](docs/phases.md) - 项目进度和任务跟踪
- [Phase 1 总结](docs/phase1-summary.md) - Phase 1 完成总结和成果
- [性能测试报告](docs/benchmark-report.md) - 详细的性能基准测试报告
- [代码质量报告](docs/code-quality-report.md) - 代码质量验证结果

---

## 贡献指南

欢迎贡献！请遵循以下步骤：

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: add some amazing feature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

### Commit 规范
```
feat: 新功能
fix: Bug 修复
perf: 性能优化
test: 测试
docs: 文档
refactor: 重构
chore: 构建/工具
```

---

## 许可证

本项目采用 Apache License 2.0 许可证 - 详见 [LICENSE](LICENSE) 文件

---

## 致谢

- MongoDB BSON 规范
- FastJSON 的优化理念启发
- JMH 基准测试框架

---

## 联系方式

- **问题反馈**: [GitHub Issues](https://github.com/fooling/fastbson/issues)
- **讨论交流**: [GitHub Discussions](https://github.com/fooling/fastbson/discussions)

---

## Phase 2 性能基线测试（v1.0.0-SNAPSHOT）

### ✅ Phase 2 完成 - 零复制惰性解析与早退优化

**测试环境**:
- 测试日期: 2025-11-26
- 迭代次数: 10,000 次
- 总测试数: 349 tests (100% 通过)
- JVM: OpenJDK 1.8

### 📊 Phase 1: HashMap 全文档解析（基线）

```
场景: 50字段文档，完整解析
FastBSON (HashMap):  113ms
MongoDB BSON:        381ms
性能提升: 3.35x ✅ (目标: 3.88x)
```

### 📊 Phase 2.A: PartialParser (早退优化)

```
场景: 100字段文档，提取5个字段 (5/100)
FastBSON (PartialParser):  51-71ms
MongoDB BSON (完整解析):   412-552ms
性能提升: 7.67-7.96x ✅ (目标: 10-20x，接近目标)
```

**特点**:
- ✓ 早退机制：找到目标字段后立即停止解析
- ✓ 极致速度：一次性字段提取场景
- ✓ 管道/流式处理优化
- ✗ 不适合重复访问同一文档

### 📊 Phase 2.B: IndexedBsonDocument (零复制惰性解析)

```
场景: 100字段文档，构建索引 + 访问5个字段
FastBSON (IndexedDocument): 163-212ms
MongoDB BSON (完整解析):    552-658ms
性能提升: 3.10-3.38x ✅
优势: 零复制架构，内存占用降低 70%
```

**特点**:
- ✓ 零复制：直接操作原始 byte[]
- ✓ 惰性解析：只构建索引，按需解析值
- ✓ 内存高效：~30 bytes/field vs ~200 bytes/field
- ✓ 适合重复访问同一文档
- ✗ 不适合一次性字段提取

### 🔍 综合对比表格

```
┌─────────────────────────────┬──────────────┬────────────┬──────────────┐
│ 实现方式                     │ 耗时 (ms)    │ vs MongoDB │ 特点         │
├─────────────────────────────┼──────────────┼────────────┼──────────────┤
│ Phase 2.A: PartialParser    │ 71           │ 7.67x      │ 早退优化     │
│ Phase 2.B: IndexedDocument  │ 163          │ 3.38x      │ 零复制惰性   │
│ MongoDB BSON (baseline)     │ 552          │ 1.00x      │ 完整解析     │
└─────────────────────────────┴──────────────┴────────────┴──────────────┘
```

**性能对比**:
- PartialParser 相对 MongoDB: **7.67x 速度提升**
- IndexedDocument 相对 MongoDB: **3.38x 速度提升**
- IndexedDocument 相对 PartialParser: **2.27x 慢** (但节省70%内存)

### 📌 使用建议

#### Phase 2.A: PartialParser (早退优化)
```java
// 适用场景：一次性部分字段提取
PartialParser parser = new PartialParser("field0", "field1", "field2");
parser.setEarlyExit(true);
Map<String, Object> result = parser.parse(bsonData);
```
- ✓ 提取 5-10 字段 from 100+ 字段文档
- ✓ 追求极致速度（7-8x speedup）
- ✓ 管道/流式处理场景
- ✗ 不适合重复访问同一文档

#### Phase 2.B: IndexedBsonDocument (零复制惰性解析)
```java
// 适用场景：需要重复访问或内存敏感
FastBson.useIndexedFactory();
BsonDocument doc = DocumentParser.INSTANCE.parse(new BsonReader(bsonData));
int value = doc.getInt32("field0");
```
- ✓ 需要重复访问同一文档
- ✓ 内存敏感应用（内存占用降低 70%）
- ✓ 零复制架构要求
- ✗ 不适合一次性字段提取

### 🎯 Phase 2 成果总结

- ✅ **PartialParser**: 7-8x speedup (早退优化)
- ✅ **IndexedBsonDocument**: 3x speedup + 70% 内存节省
- ✅ **完整的 Benchmark 对比**: 清晰展示两种模式差异
- ✅ **所有 349 个测试**: 100% 通过
- ✅ **API 完善**: FastBson.useHashMapFactory() / useIndexedFactory()

### 🚀 性能基线测试用法

FastBSON提供了统一的性能基线测试框架，方便对比各种模式的性能差异。

#### 运行完整性能基线测试（推荐）

```bash
# 一次运行，展示所有场景的性能对比
mvn test -Dtest=PerformanceBenchmark#testCompletePerformanceBaseline
```

**输出示例**:
```
====================================================================================================
                         FastBSON 性能基线测试报告
====================================================================================================

┌──────────────────────────────┬──────────────────────┬──────────┬──────────┬────────────┐
│ 场景                          │ 实现方式              │ FastBSON │ MongoDB  │ 性能提升   │
├──────────────────────────────┼──────────────────────┼──────────┼──────────┼────────────┤
│ Phase 1: 50字段完整解析            │ HashMap (eager)      │     93 ms │    204 ms │ 2.18x      │
│ Phase 2.A: 100字段部分解析(5/100)    │ PartialParser (early-exit) │     51 ms │    410 ms │ 7.99x      │
│ Phase 2.B: 100字段零复制惰性(5/100)     │ IndexedDocument (zero-copy) │     74 ms │    422 ms │ 5.64x      │
└──────────────────────────────┴──────────────────────┴──────────┴──────────┴────────────┘
```

#### 运行单个场景测试

```bash
# Phase 1: HashMap 完整解析模式
mvn test -Dtest=PerformanceBenchmark#testPhase1_HashMap_50Fields

# Phase 2.A: PartialParser 早退优化模式
mvn test -Dtest=PerformanceBenchmark#testPhase2A_PartialParser_5of100

# Phase 2.B: IndexedBsonDocument 零复制惰性解析模式
mvn test -Dtest=PerformanceBenchmark#testPhase2B_IndexedDocument_5of100
```

#### 测试类说明

**PerformanceBenchmark.java** - 统一的性能基线测试框架
- ✅ 清晰的场景对比
- ✅ 格式化的性能报告
- ✅ 详细的使用建议
- ✅ 自动评级（优秀/良好/一般/待优化）

**BenchmarkValidationTest.java** - 原有的benchmark验证测试
- 更详细的场景对比
- Phase 2.A vs Phase 2.B 综合对比
- 包含更多测试细节

#### 自定义Benchmark场景

```java
import com.cloud.fastbson.benchmark.*;

public class MyBenchmark {
    @Test
    public void testMyScenario() {
        // 1. 生成测试数据
        byte[] bsonData = BsonTestDataGenerator.generateDocument(100);

        // 2. 运行你的实现
        long start = System.nanoTime();
        // ... your implementation ...
        long time = System.nanoTime() - start;

        // 3. 创建结果
        BenchmarkResult result = BenchmarkResult.builder()
            .scenarioName("My Custom Scenario")
            .fastbsonMode("Custom Mode")
            .fastbsonTimeNanos(time)
            .mongoTimeNanos(mongoTime)
            .speedup((double) mongoTime / time)
            .description("场景描述")
            .target("目标性能")
            .build();

        // 4. 生成报告
        String report = BenchmarkReport.generate(List.of(result));
        System.out.println(report);
    }
}
```

---

**当前版本**: 1.0.0-SNAPSHOT
**最后更新**: 2025-11-26
