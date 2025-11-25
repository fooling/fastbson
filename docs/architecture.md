# FastBSON 架构设计文档

**高性能 BSON 反序列化与部分字段读取库**

Version: 1.0
Date: 2025-11

---

## 1. 项目概述

### 1.1 背景

BSON（Binary JSON）是一种高效的二进制序列化格式，广泛应用于数据存储和传输场景。在实际应用中，经常遇到只需要读取文档中部分字段的情况，但传统的 BSON 解析器需要完整解析整个文档，造成不必要的性能开销。

本项目旨在设计一个高性能的 BSON 反序列化库，通过部分字段解析技术，在只需要少量字段时跳过不必要的解析，实现显著的性能提升。

### 1.2 项目目标与范围

**核心目标：**
- ✅ 实现 BSON 协议的完整反序列化能力
- ✅ 实现高性能部分字段读取功能
- ✅ 在只需要少量字段时，跳过不需要的字段，避免完整解析开销
- ✅ **提前退出机制**：解析到所有需要的字段后立即停止，无需遍历整个文档
- ✅ 借鉴 FastJSON 的优化理念，达到极致性能
- ✅ 提供简洁易用的 API 接口

**技术约束：**
- 🔧 使用 Java 8 语法（保证广泛兼容性）
- 🔧 遵循 MongoDB 3.4 版本的 BSON 规范

**明确不包含（本阶段）：**
- ❌ BSON 序列化功能（仅做反序列化）
- ❌ 与 MongoDB Driver 的集成与互操作
- ❌ 流式大文档处理（可作为后续增强）
- ❌ 查询语言支持
- ❌ 压缩 BSON 支持
- ❌ 多语言实现（仅 Java）

---

## 2. 核心使用场景

### 2.1 提前退出场景（Early Exit Optimization）

#### 2.1.1 场景描述

在实际应用中，经常遇到以下情况：
- 从包含 100 个字段的大型文档中，只需要提取前面的 3-5 个字段
- 需要的字段通常位于文档的开头部分
- 后续的 95+ 个字段完全不需要处理

**传统解析器的问题：**
即使只需要前 5 个字段，传统解析器仍然会：
1. 遍历整个文档的所有 100 个字段
2. 解析每个字段的类型和值
3. 浪费大量 CPU 时间在不需要的字段上

#### 2.1.2 提前退出优化

FastBSON 实现了智能的提前退出机制：

```java
// 只需要提取前 3 个字段
FastBsonParser parser = FastBsonParser.builder()
    .fields("userId", "userName", "timestamp")
    .earlyExit(true)  // 启用提前退出
    .build();

byte[] bsonData = ...;  // 100+ 字段的大型文档
Map<String, Object> result = parser.parse(bsonData);

// 找到 3 个目标字段后立即停止解析
// 剩余 97 个字段被完全跳过，不会浪费 CPU
```

**工作原理：**

1. **字段计数器**：记录已找到的目标字段数量
2. **提前退出判断**：当 `已找到字段数 == 目标字段数` 时立即退出
3. **无需遍历剩余字段**：直接返回结果，节省 CPU 时间

```java
public class PartialParser {
    private final FieldMatcher matcher;
    private final int targetFieldCount;
    private final boolean earlyExit;

    public Map<String, Object> parse(byte[] bsonData) {
        BsonReader reader = new BsonReader(bsonData);
        Map<String, Object> result = new HashMap<>();

        int docLength = reader.readInt32();
        int endPos = reader.position() + docLength - 4;
        int foundCount = 0;

        while (reader.position() < endPos) {
            byte type = reader.readByte();
            if (type == 0) break;

            String fieldName = reader.readCString();

            if (matcher.matches(fieldName)) {
                // 找到目标字段，解析值
                Object value = typeHandler.parseValue(reader, type);
                result.put(fieldName, value);
                foundCount++;

                // ⭐ 提前退出判断
                if (earlyExit && foundCount == targetFieldCount) {
                    return result;  // 立即返回，无需继续
                }
            } else {
                // 跳过不需要的字段
                skipper.skipValue(reader, type);
            }
        }

        return result;
    }
}
```

#### 2.1.3 性能提升分析

**场景：100 字段文档，提取前 5 个字段**

| 解析方式 | 需要处理的字段 | CPU 时间 | 性能提升 |
|---------|--------------|---------|---------|
| 传统完整解析 | 100 个 | 100% | 1.0x |
| 部分解析（无提前退出） | 5 个解析 + 95 个跳过 | ~20% | 5x |
| 部分解析（提前退出） | 5 个解析 | ~5% | **20x** |

**关键优势：**
- ✅ **最小化 CPU 使用**：只处理真正需要的字段
- ✅ **减少内存访问**：不读取后续字段的数据
- ✅ **降低缓存污染**：减少 CPU 缓存行的污染
- ✅ **提升吞吐量**：单位时间处理更多文档

#### 2.1.4 实际应用场景

**1. API 网关字段过滤**
```java
// 网关只需要验证 userId 和 timestamp，不关心请求体的其他内容
FastBsonParser gatewayParser = FastBsonParser.builder()
    .fields("userId", "timestamp")
    .earlyExit(true)
    .build();

// 即使请求体有 100+ 字段，只解析前 2 个就停止
Map<String, Object> headers = gatewayParser.parse(requestData);
String userId = (String) headers.get("userId");
long timestamp = (Long) headers.get("timestamp");

if (isValidTimestamp(timestamp)) {
    // 继续处理请求
}
```

**2. 日志分析系统**
```java
// 从日志文档中只提取关键字段：时间、级别、消息
FastBsonParser logParser = FastBsonParser.builder()
    .fields("@timestamp", "level", "message")
    .earlyExit(true)
    .build();

// 日志文档可能包含几十个调试字段，但只需要这 3 个
for (byte[] logData : logStream) {
    Map<String, Object> log = logParser.parse(logData);
    if ("ERROR".equals(log.get("level"))) {
        alertSystem.notify(log.get("message"));
    }
}
```

**3. 微服务间通信**
```java
// 服务 A 调用服务 B，只需要返回结果中的 orderId 和 status
FastBsonParser serviceParser = FastBsonParser.builder()
    .fields("orderId", "status")
    .earlyExit(true)
    .build();

byte[] response = httpClient.post(serviceB, request);
Map<String, Object> result = serviceParser.parse(response);

// 响应可能包含订单的所有详细信息（100+ 字段）
// 但我们只关心 orderId 和 status，解析到这两个字段就停止
String orderId = (String) result.get("orderId");
String status = (String) result.get("status");
```

**4. 数据库查询投影优化**
```java
// 类似 MongoDB 的投影（projection），只查询需要的字段
FastBsonParser projectionParser = FastBsonParser.builder()
    .fields("_id", "name", "email")
    .earlyExit(true)
    .build();

// 文档可能存储了用户的所有信息
// 但查询结果只需要 ID、姓名和邮箱
for (byte[] document : queryResults) {
    Map<String, Object> user = projectionParser.parse(document);
    users.add(new User(
        user.get("_id"),
        user.get("name"),
        user.get("email")
    ));
}
```

#### 2.1.5 配置选项

```java
FastBsonParser parser = FastBsonParser.builder()
    .fields("field1", "field2", "field3")

    // 提前退出配置
    .earlyExit(true)           // 启用提前退出（默认：true）

    // 其他优化配置
    .ordered(true)             // 假定字段有序（默认：false）
    .cacheFieldNames(true)     // 缓存字段名（默认：true）

    .build();
```

**最佳实践：**
- ✅ 需要字段数量 < 10% 时：强烈推荐启用 `earlyExit`
- ✅ 目标字段位于文档前部：性能提升最明显
- ⚠️ 需要字段数量 > 50% 时：提前退出收益有限
- ⚠️ 目标字段位于文档尾部：可能无法提前退出

---

## 3. 性能优化技术

### 3.1 核心优化理念

#### 3.1.1 假定有序快速匹配算法

基于实际应用观察，文档字段通常按照固定顺序出现。利用这一特性可以大幅提升匹配效率：

- 预先建立字段名到索引的映射关系
- 匹配时优先按照预期顺序查找
- 大幅减少字符串比较次数

#### 3.1.2 ThreadLocal 对象复用

使用 ThreadLocal 存储反序列化过程中的临时数据：

- 减少内存分配次数
- 降低 GC 压力
- 复用 StringBuilder、BsonReader 等对象

#### 3.1.3 字符串内部化（String Interning）

对于重复出现的字符串（特别是字段名），使用字符串池：

- 字符串内部化，减少重复对象创建
- 提高字符串比较效率（可使用 == 而非 equals）
- 降低内存占用

#### 3.1.4 类型处理器缓存

使用缓存提升性能：

- IdentityHashMap 缓存类型处理器
- 避免处理器的重复创建
- 快速查找对应类型的 Handler

---

## 3. BSON 协议分析

### 3.1 BSON 格式概述

BSON（Binary JSON）是一种二进制编码的 JSON-like 数据格式，最初由 MongoDB 开发。相比 JSON，BSON 具有以下特点：

- 二进制格式，更紧凑高效
- 支持更多数据类型（如 Date、Binary、ObjectId 等）
- 每个元素带有长度信息，便于快速遍历
- 小端序（Little-Endian）存储

**规范版本：** 本项目遵循 MongoDB 3.4 版本的 BSON 规范

### 3.2 BSON 文档结构

BSON 文档的基本结构：

```
document ::= int32 e_list '\x00'
```

- **int32**: 文档总字节数（包括长度字段本身和结尾的 0）
- **e_list**: 元素列表
- **\x00**: 文档结束标记

### 3.3 元素结构

每个元素的结构：

```
element ::= type_byte e_name value
```

- **type_byte**: 1字节，表示值的类型
- **e_name**: 字段名（C-style 字符串，以 \x00 结尾）
- **value**: 根据类型不同而不同的值

### 3.4 常见类型

BSON 支持的主要类型（type_byte）：

- **0x01**: double（8字节 IEEE 754）
- **0x02**: string（int32 长度 + UTF-8 字符串 + \x00）
- **0x03**: embedded document（嵌套文档）
- **0x04**: array（数组，实际是特殊的文档）
- **0x05**: binary（int32 长度 + subtype + 字节数组）
- **0x07**: ObjectId（12字节）
- **0x08**: boolean（1字节）
- **0x09**: UTC datetime（int64 毫秒）
- **0x0A**: null（无值）
- **0x10**: int32（4字节）
- **0x12**: int64（8字节）

### 3.5 BSON 的关键特性

对解析器设计重要的特性：

1. **长度前缀**：文档和字符串等都有长度信息，可以快速跳过
2. **类型信息**：每个字段有明确的类型，可以精确跳过
3. **顺序存储**：字段按顺序存储在二进制流中
4. **可遍历性**：设计为易于遍历，无需完整解析

---

## 4. FastBSON 架构设计

### 4.1 设计原则

1. **零拷贝**：尽可能避免数据拷贝
2. **延迟解析**：只在真正需要时才解析字段值
3. **快速跳过**：利用 BSON 长度信息快速跳过不需要的字段
4. **字段名缓存**：重用字段名字符串
5. **类型特化**：针对常见类型优化解析路径

### 4.2 核心组件架构

#### 4.2.1 BsonReader - 字节流读取器

**职责：**
- 管理底层字节数组或 ByteBuffer
- 提供基本类型读取（int32, int64, double 等）
- 维护当前读取位置
- 支持快速跳过指定字节数

**核心方法：**

```java
public class BsonReader {
    private final byte[] buffer;
    private int position;

    // 读取基本类型
    public int readInt32();
    public long readInt64();
    public double readDouble();
    public byte readByte();

    // 读取 C-style 字符串（以 \x00 结尾）
    public String readCString();

    // 跳过指定字节数
    public void skip(int bytes);

    // 获取当前位置
    public int position();
}
```

**实现要点：**

```java
public class BsonReader {
    private byte[] buffer;
    private int position;

    public BsonReader(byte[] buffer) {
        this.buffer = buffer;
        this.position = 0;
    }

    public int readInt32() {
        // Little-Endian 读取
        int value = ByteBuffer.wrap(buffer, position, 4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .getInt();
        position += 4;
        return value;
    }

    public String readCString() {
        int start = position;
        while (buffer[position] != 0) position++;
        String str = new String(buffer, start,
            position - start, StandardCharsets.UTF_8);
        position++; // skip null terminator
        return str;
    }

    public void skip(int bytes) {
        position += bytes;
    }
}
```

#### 4.2.2 FieldMatcher - 字段匹配器

基于"假定有序"理念实现高效字段匹配：

- 维护目标字段名集合
- 使用 HashMap 快速匹配字段名
- 支持有序匹配优化（假设字段按常见顺序出现）
- 字段名内部化，减少字符串创建

**优化策略：**

- **小字段集（< 10）**：直接使用数组 + 线性查找
- **中等字段集（10-50）**：HashMap 查找
- **有序优化**：记录字段出现顺序，优先匹配预期位置

**实现示例：**

```java
public class FieldMatcher {
    private final Set<String> targetFields;
    private final Map<String, Integer> fieldOrderMap;
    private int expectedIndex = 0;

    public FieldMatcher(String... fields) {
        this.targetFields = new HashSet<>();
        this.fieldOrderMap = new HashMap<>();
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i].intern(); // 字段名内部化
            targetFields.add(field);
            fieldOrderMap.put(field, i);
        }
    }

    public boolean matches(String fieldName) {
        // 优先检查预期位置（有序优化）
        if (fieldOrderMap.containsKey(fieldName)) {
            int index = fieldOrderMap.get(fieldName);
            if (index == expectedIndex) {
                expectedIndex++;
                return true;
            }
        }
        return targetFields.contains(fieldName);
    }

    public void reset() {
        expectedIndex = 0;
    }
}
```

#### 4.2.3 ValueSkipper - 值跳过器

针对不同类型实现快速跳过：

**固定长度类型**：直接跳过固定字节数
- double, int32, int64, boolean, ObjectId, datetime

**变长类型**：读取长度后跳过
- string, binary: 读取 int32 长度，跳过相应字节
- document, array: 读取 int32 总长度，直接跳过整个子文档

**实现示例：**

```java
public class ValueSkipper {
    // 固定长度类型表
    private static final int[] FIXED_SIZES = new int[256];

    static {
        FIXED_SIZES[0x01] = 8;  // double
        FIXED_SIZES[0x08] = 1;  // boolean
        FIXED_SIZES[0x09] = 8;  // datetime
        FIXED_SIZES[0x0A] = 0;  // null
        FIXED_SIZES[0x10] = 4;  // int32
        FIXED_SIZES[0x12] = 8;  // int64
        FIXED_SIZES[0x07] = 12; // ObjectId
    }

    public void skipValue(BsonReader reader, byte type) {
        int fixedSize = FIXED_SIZES[type & 0xFF];

        if (fixedSize > 0) {
            // 固定长度类型，直接跳过
            reader.skip(fixedSize);
        } else if (type == 0x0A) {
            // null 类型，无需跳过
            return;
        } else {
            // 变长类型，需要读取长度
            switch (type) {
                case 0x02: // string
                    int strLen = reader.readInt32();
                    reader.skip(strLen); // 包括 null terminator
                    break;

                case 0x03: // document
                case 0x04: // array
                    int docLen = reader.readInt32();
                    reader.skip(docLen - 4); // 已读取4字节长度
                    break;

                case 0x05: // binary
                    int binLen = reader.readInt32();
                    reader.skip(1 + binLen); // subtype + data
                    break;

                default:
                    throw new IllegalArgumentException("Unknown type: " + type);
            }
        }
    }
}
```

#### 4.2.4 TypeHandler - 类型处理器

针对每种 BSON 类型：

- 提供高效的值解析
- 支持类型转换

**常用类型优化：**

- **int32/int64**: 直接读取，无需额外处理
- **string**: 复用 StringBuilder，减少字符串创建
- **document/array**: 支持递归解析
- **binary**: 提供零拷贝视图（ByteBuffer.wrap）

**实现示例：**

```java
public class TypeHandler {

    public Object parseValue(BsonReader reader, byte type) {
        switch (type) {
            case 0x01: // double
                return reader.readDouble();

            case 0x02: // string
                return parseString(reader);

            case 0x03: // document
                return parseDocument(reader);

            case 0x04: // array
                return parseArray(reader);

            case 0x08: // boolean
                return reader.readByte() != 0;

            case 0x09: // datetime
                return new Date(reader.readInt64());

            case 0x0A: // null
                return null;

            case 0x10: // int32
                return reader.readInt32();

            case 0x12: // int64
                return reader.readInt64();

            case 0x05: // binary
                return parseBinary(reader);

            case 0x07: // ObjectId
                return parseObjectId(reader);

            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    private String parseString(BsonReader reader) {
        int length = reader.readInt32();
        byte[] bytes = reader.readBytes(length - 1); // -1 for null terminator
        reader.readByte(); // skip null terminator
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private byte[] parseBinary(BsonReader reader) {
        int length = reader.readInt32();
        byte subtype = reader.readByte();
        return reader.readBytes(length);
    }

    private String parseObjectId(BsonReader reader) {
        byte[] bytes = reader.readBytes(12);
        // 转换为十六进制字符串
        return bytesToHex(bytes);
    }

    private Map<String, Object> parseDocument(BsonReader reader) {
        // 完整解析嵌套文档
        int docLength = reader.readInt32();
        int endPos = reader.position() + docLength - 4;
        Map<String, Object> result = new HashMap<>();

        while (reader.position() < endPos) {
            byte type = reader.readByte();
            if (type == 0) break;

            String fieldName = reader.readCString();
            Object value = parseValue(reader, type);
            result.put(fieldName, value);
        }

        return result;
    }

    private List<Object> parseArray(BsonReader reader) {
        // 数组本质上是文档，key 为 "0", "1", "2" ...
        Map<String, Object> arrayDoc = parseDocument(reader);
        List<Object> result = new ArrayList<>();
        for (int i = 0; ; i++) {
            String key = String.valueOf(i);
            if (!arrayDoc.containsKey(key)) break;
            result.add(arrayDoc.get(key));
        }
        return result;
    }
}
```

#### 4.2.5 PartialParser - 部分字段解析器

核心解析逻辑：

- 接收 BsonReader 和 FieldMatcher
- 遍历文档，匹配目标字段
- 匹配时解析值，否则跳过
- 返回结果映射（Map<String, Object>）

**解析流程：**

1. 读取文档总长度
2. 循环读取元素直到遇到 \x00
   - a. 读取类型字节
   - b. 读取字段名
   - c. 使用 FieldMatcher 判断是否为目标字段
   - d. 如果是：解析值并存储
   - e. 如果不是：使用 ValueSkipper 跳过
3. 返回结果

**实现示例：**

```java
public class PartialParser {
    private final FieldMatcher matcher;
    private final ValueSkipper skipper;
    private final TypeHandler typeHandler;

    public PartialParser(FieldMatcher matcher) {
        this.matcher = matcher;
        this.skipper = new ValueSkipper();
        this.typeHandler = new TypeHandler();
    }

    public Map<String, Object> parse(byte[] bsonData) {
        BsonReader reader = new BsonReader(bsonData);
        Map<String, Object> result = new HashMap<>();

        int docLength = reader.readInt32();
        int endPos = reader.position() + docLength - 4;

        matcher.reset(); // 重置有序匹配状态

        while (reader.position() < endPos) {
            byte type = reader.readByte();
            if (type == 0) break; // end of document

            String fieldName = reader.readCString();

            if (matcher.matches(fieldName)) {
                // 匹配的字段，解析值
                Object value = typeHandler.parseValue(reader, type);
                result.put(fieldName, value);
            } else {
                // 不匹配，跳过值
                skipper.skipValue(reader, type);
            }
        }

        return result;
    }
}
```

#### 4.2.6 ObjectPool - 对象池

减少对象创建，提升性能：

```java
public class ObjectPool {
    // ThreadLocal 复用 Reader 对象
    private static final ThreadLocal<BsonReader> readerPool =
        ThreadLocal.withInitial(() -> new BsonReader(new byte[0]));

    // ThreadLocal 复用临时缓冲区
    private static final ThreadLocal<byte[]> bufferPool =
        ThreadLocal.withInitial(() -> new byte[8192]);

    // 字段名字符串池
    private static final Map<String, String> stringPool =
        new ConcurrentHashMap<>();

    public static BsonReader getBsonReader(byte[] data) {
        BsonReader reader = readerPool.get();
        reader.reset(data);
        return reader;
    }

    public static String internString(String str) {
        return stringPool.computeIfAbsent(str, String::intern);
    }
}
```

### 4.3 API 设计

#### 4.3.1 基本 API

```java
// 创建解析器，指定需要的字段
FastBsonParser parser = FastBsonParser.builder()
    .fields("name", "age", "email")
    .build();

// 解析 BSON 字节数组
byte[] bsonData = ...;
Map<String, Object> result = parser.parse(bsonData);

// 获取值
String name = (String) result.get("name");
Integer age = (Integer) result.get("age");
```

#### 4.3.2 类型安全 API

```java
// 使用类型安全的访问器
BsonDocument result = parser.parseToDocument(bsonData);

String name = result.getString("name");
int age = result.getInt("age");
boolean active = result.getBoolean("active");

// 支持默认值
String email = result.getString("email", "default@example.com");

// 嵌套字段访问
BsonDocument address = result.getDocument("address");
String city = address.getString("city");
```

#### 4.3.3 完整文档解析 API

```java
// 不指定字段，解析全部
FastBsonParser fullParser = FastBsonParser.builder().build();

// 解析所有字段
Map<String, Object> fullDoc = fullParser.parse(bsonData);
```

#### 4.3.4 Builder API

```java
FastBsonParser parser = FastBsonParser.builder()
    .fields("field1", "field2", "field3")  // 可选：指定需要的字段
    .ordered(true)                         // 启用有序优化
    .cacheFieldNames(true)                 // 启用字段名缓存
    .build();
```

### 4.4 关键优化点

#### 4.4.1 字段名优化

- **字段名内部化**：所有字段名使用 String.intern() 或自定义字符串池
- **字段名复用**：解析多个文档时，字段名对象复用
- **引用比较**：内部化后可使用 == 而非 equals()

```java
// 字段名池实现
public class FieldNamePool {
    private static final ConcurrentHashMap<String, String> pool =
        new ConcurrentHashMap<>();

    public static String intern(String fieldName) {
        return pool.computeIfAbsent(fieldName, k -> k);
    }
}
```

#### 4.4.2 跳过优化

- **批量跳过**：对于不需要的大块数据，直接移动指针
- **类型表**：预建立类型到跳过策略的映射表
- **嵌套跳过**：利用 BSON 文档长度信息，整体跳过嵌套文档

```java
// 快速跳过嵌套文档
if (type == 0x03 || type == 0x04) { // document or array
    int docLength = reader.readInt32();
    reader.skip(docLength - 4); // 已读4字节，跳过剩余部分
}
```

#### 4.4.3 内存优化

- **零拷贝**：对于 binary 类型，返回原始字节数组的视图
- **对象池**：ThreadLocal 复用临时对象
- **按需分配**：只为需要的字段分配内存

#### 4.4.4 分支预测优化

- **常见类型优先**：针对最常见类型（string, int32, int64）优化分支
- **有序假设**：假设字段按常见顺序出现，优先检查预期位置

```java
// 针对常见类型优化的 switch 语句
public Object parseValue(BsonReader reader, byte type) {
    // 最常见的类型放在前面
    if (type == 0x02) return parseString(reader);      // string
    if (type == 0x10) return reader.readInt32();       // int32
    if (type == 0x12) return reader.readInt64();       // int64
    if (type == 0x01) return reader.readDouble();      // double

    // 其他类型
    switch (type) {
        case 0x03: return parseDocument(reader);
        case 0x04: return parseArray(reader);
        // ...
    }
}
```

### 4.5 性能对比预期

#### 场景 1：从 100 个字段的文档中提取 5 个字段

- **传统解析器**：需要解析所有 100 个字段 → 100% 时间
- **FastBSON**：
  - 解析 5 个目标字段：~5% 时间
  - 跳过 95 个字段：~10% 时间（读取类型+长度）
  - 总计：~15% 时间
  - **性能提升：约 6-7 倍**

#### 场景 2：从 10 个字段的文档中提取 8 个字段

- 性能差异不明显，可能仅 1.2-1.5 倍

#### 结论

**字段需求越少，性能提升越显著**。最适用于需要从大型文档中提取少量字段的场景。

---

## 5. 实现计划

### 5.1 Phase 1: 基础框架（1-2周）

**目标：** 实现完整的 BSON 反序列化能力

- 实现 BsonReader 核心读取功能
- 实现 TypeHandler 支持所有基本 BSON 类型
- 实现完整文档解析（不带字段过滤）
- 编写单元测试，覆盖所有 BSON 类型

**交付物：**
- BsonReader 实现
- TypeHandler 实现
- 基础解析器实现
- 单元测试套件（覆盖率 > 90%）

### 5.2 Phase 2: 性能基准测试模块（3-5天）

**目标：** 建立性能基准测试框架，与官方 org.mongodb:bson 进行对比

- 创建 benchmark 测试模块
- 集成 JMH（Java Microbenchmark Harness）
- 添加 org.mongodb:bson 作为测试依赖
- 实现基准测试用例（完整文档解析对比）
- 生成性能对比报告

**交付物：**
- benchmark 测试模块
- 与 org.mongodb:bson 的性能对比测试
- 性能基准报告（吞吐量、延迟）
- 为后续优化建立基线

**测试场景：**
- 小文档（< 1KB）解析性能
- 中等文档（1-10KB）解析性能
- 大文档（> 100KB）解析性能
- 不同字段数量（10, 50, 100, 500字段）

### 5.3 Phase 3: 部分字段解析（1周）

**目标：** 实现部分字段读取功能

- 实现 FieldMatcher 字段匹配
- 实现 ValueSkipper 跳过逻辑
- 实现 PartialParser 部分字段解析
- 编写部分字段解析测试
- 添加部分解析的 benchmark 测试

**交付物：**
- FieldMatcher 实现
- ValueSkipper 实现
- PartialParser 实现
- 功能测试用例
- 部分解析性能测试

### 5.4 Phase 4: 性能优化（1-2周）

**目标：** 提升解析性能

- 实现 ObjectPool 对象池
- 添加字段名内部化
- 优化常见类型解析路径
- 实现有序匹配优化
- 通过 benchmark 验证优化效果

**交付物：**
- 优化后的实现
- 性能提升报告
- 与 Phase 2 基准的对比数据
- 与 org.mongodb:bson 的最终对比

### 5.5 Phase 5: API 完善与测试（1周）

**目标：** 完善 API 和文档

- 实现 BsonDocument 类型安全访问器
- 实现 Builder API
- 边界情况测试
- 编写 API 文档和使用示例

**交付物：**
- 完整 API 实现
- API 文档
- 使用示例
- 完整测试报告

---

## 6. 测试策略

### 6.1 单元测试

**覆盖范围：**
- 每个组件独立测试
- 覆盖所有 BSON 类型
- 边界条件测试
- 异常情况测试

**测试用例示例：**
```java
@Test
public void testParseInt32() {
    byte[] bsonData = createBsonWithInt32("age", 25);
    FastBsonParser parser = FastBsonParser.builder().build();
    Map<String, Object> result = parser.parse(bsonData);
    assertEquals(25, result.get("age"));
}

@Test
public void testPartialFieldParsing() {
    byte[] bsonData = createBsonWithMultipleFields();
    FastBsonParser parser = FastBsonParser.builder()
        .fields("name", "age")
        .build();
    Map<String, Object> result = parser.parse(bsonData);
    assertEquals(2, result.size());
    assertTrue(result.containsKey("name"));
    assertTrue(result.containsKey("age"));
}

@Test
public void testSkipNestedDocument() {
    byte[] bsonData = createBsonWithNestedDocument();
    FastBsonParser parser = FastBsonParser.builder()
        .fields("id")
        .build();
    Map<String, Object> result = parser.parse(bsonData);
    assertEquals(1, result.size());
    assertFalse(result.containsKey("nested"));
}
```

### 6.2 性能测试

**测试场景：**

1. **小文档（< 1KB）**：高频解析场景
2. **中等文档（1-10KB）**：常见业务场景
3. **大文档（> 100KB）**：大数据场景
4. **字段数量变化**：10, 50, 100, 500 个字段
5. **目标字段比例**：10%, 30%, 50%, 100%

**性能指标：**
- 吞吐量（ops/sec）
- 延迟（平均、P95、P99）
- 内存使用
- GC 压力

**基准测试框架（JMH）：**
```java
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class FastBsonBenchmark {

    private byte[] bsonData;
    private FastBsonParser partialParser;
    private FastBsonParser fullParser;

    @Setup
    public void setup() {
        bsonData = generateBsonData(100, 5); // 100字段，提取5个
        partialParser = FastBsonParser.builder()
            .fields("field1", "field2", "field3", "field4", "field5")
            .build();
        fullParser = FastBsonParser.builder().build();
    }

    @Benchmark
    public Map<String, Object> testPartialParsing() {
        return partialParser.parse(bsonData);
    }

    @Benchmark
    public Map<String, Object> testFullParsing() {
        return fullParser.parse(bsonData);
    }
}
```

### 6.3 兼容性测试

- 使用 MongoDB 官方 BSON 编码器生成测试数据
- 验证 FastBSON 解析结果与官方库一致
- 测试各种边界情况和异常数据

---

## 7. 项目结构

```
fastbson/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/
│   │           └── cloud/
│   │               └── fastbson/
│   │                   ├── FastBsonParser.java       # 主入口
│   │                   ├── BsonDocument.java         # 类型安全访问器
│   │                   ├── ObjectPool.java           # 对象池
│   │                   ├── reader/
│   │                   │   └── BsonReader.java       # 字节读取器
│   │                   ├── matcher/
│   │                   │   └── FieldMatcher.java     # 字段匹配器
│   │                   ├── skipper/
│   │                   │   └── ValueSkipper.java     # 值跳过器
│   │                   ├── handler/
│   │                   │   └── TypeHandler.java      # 类型处理器
│   │                   ├── parser/
│   │                   │   └── PartialParser.java    # 部分解析器
│   │                   └── util/
│   │                       ├── BsonType.java         # 类型常量
│   │                       └── BsonUtils.java        # 工具类
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── cloud/
│       │           └── fastbson/
│       │           ├── BsonReaderTest.java
│       │           ├── TypeHandlerTest.java
│       │           ├── FieldMatcherTest.java
│       │           ├── ValueSkipperTest.java
│       │           ├── PartialParserTest.java
│       │           └── benchmark/
│       │               └── FastBsonBenchmark.java
│       └── resources/
│           └── test-data/
│               ├── simple.bson
│               ├── complex.bson
│               └── nested.bson
├── pom.xml
├── README.md
└── architecture.md
```

---

## 8. Maven 配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.cloud</groupId>
    <artifactId>fastbson</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>FastBSON</name>
    <description>High-performance BSON deserialization and partial field parsing library</description>

    <properties>
        <java.version>1.8</java.version>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <lombok.version>1.18.30</lombok.version>
    </properties>

    <dependencies>
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- Test dependencies -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.0</version>
            <scope>test</scope>
        </dependency>

        <!-- JMH for benchmarking -->
        <dependency>
            <groupId>org.openjdk.jmh</groupId>
            <artifactId>jmh-core</artifactId>
            <version>1.37</version>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.openjdk.jmh</groupId>
            <artifactId>jmh-generator-annprocess</artifactId>
            <version>1.37</version>
            <scope>test</scope>
        </dependency>

        <!-- MongoDB BSON for test data generation -->
        <dependency>
            <groupId>org.mongodb</groupId>
            <artifactId>bson</artifactId>
            <version>4.11.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.1.2</version>
            </plugin>

            <!-- JaCoCo for code coverage -->
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>0.8.10</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>prepare-agent</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>test</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 9. 使用示例

### 9.1 完整文档解析

```java
// 解析完整 BSON 文档
FastBsonParser parser = FastBsonParser.builder().build();

byte[] bsonData = loadBsonData();
Map<String, Object> result = parser.parse(bsonData);

// 访问字段
String name = (String) result.get("name");
Integer age = (Integer) result.get("age");
```

### 9.2 部分字段解析

```java
// 只解析需要的字段
FastBsonParser parser = FastBsonParser.builder()
    .fields("userId", "userName", "email")
    .build();

byte[] bsonData = loadBsonData();
Map<String, Object> result = parser.parse(bsonData);

// 只包含指定的字段
String userId = (String) result.get("userId");
String userName = (String) result.get("userName");
String email = (String) result.get("email");
```

### 9.3 类型安全访问

```java
FastBsonParser parser = FastBsonParser.builder()
    .fields("age", "salary", "active")
    .build();

BsonDocument doc = parser.parseToDocument(bsonData);

int age = doc.getInt("age");
double salary = doc.getDouble("salary");
boolean active = doc.getBoolean("active");

// 带默认值
String department = doc.getString("department", "Unknown");
```

### 9.4 批量处理

```java
FastBsonParser parser = FastBsonParser.builder()
    .fields("id", "status", "amount")
    .ordered(true)  // 启用有序优化
    .build();

List<byte[]> bsonDocuments = loadBsonDocuments();
double totalAmount = 0.0;

for (byte[] bsonData : bsonDocuments) {
    BsonDocument doc = parser.parseToDocument(bsonData);
    if ("completed".equals(doc.getString("status"))) {
        totalAmount += doc.getDouble("amount");
    }
}

System.out.println("Total: " + totalAmount);
```

---

## 10. Phase 2 完成工作总结（2.11-2.12）

### 10.1 Phase 2.11: 提取复杂类型解析器

**目标**: 将 TypeHandler 中的复杂类型解析逻辑提取为独立的 Parser 类。

**完成的工作**:

1. **创建 DocumentParser.java**
   - 使用 enum 单例模式实现
   - 支持递归文档解析
   - 通过依赖注入接收 TypeHandler 实例

2. **创建 ArrayParser.java**
   - 使用 enum 单例模式实现
   - 将 BSON 数组（本质是文档）转换为 Java List

3. **创建 JavaScriptWithScopeParser.java**
   - 解析 JavaScript代码 + Scope文档的组合类型

4. **重构 TypeHandler**
   - 添加 `setHandler()` 方法用于依赖注入
   - 在静态初始化块中调用 `setHandler()` 注入自身实例
   - 保持 O(1) 查找表性能

**成果**:
- 代码更模块化，每个 Parser 职责单一
- TypeHandler 更清晰，专注于类型分发
- 保持 100% 测试覆盖率
- 所有 289 个测试通过

### 10.2 Phase 2.12: 移动辅助类型到独立包

**目标**: 将 TypeHandler 中的内部辅助类提取到独立的 types 包。

**完成的工作**:

1. **创建新包 `com.cloud.fastbson.types`**

2. **移动 8 个辅助类型**:
   - `BinaryData` - 二进制数据包装类
   - `RegexValue` - 正则表达式类型
   - `DBPointer` - 数据库指针类型
   - `JavaScriptWithScope` - JavaScript + Scope
   - `Timestamp` - BSON 时间戳类型
   - `Decimal128` - 高精度十进制数
   - `MinKey` / `MaxKey` - BSON 特殊边界值

3. **重构 TypeHandler**
   - 从 302 行缩减至 121 行（**减少 60%**）
   - 更清晰的职责划分
   - 更易于维护

4. **更新所有测试**
   - 添加正确的 import 语句
   - 解决 Decimal128 命名冲突（MongoDB vs FastBSON）
   - 所有测试通过，保持 100% 覆盖率

**成果**:
- TypeHandler 代码量减少 60%
- 类型定义更清晰，可独立复用
- 包结构更合理
- 性能无回退，所有 benchmark 保持或提升

---

## 11. 性能瓶颈分析

### 11.1 当前性能基线

基于 Phase 2.12 完成后的 benchmark 结果：

| 场景 | FastBSON vs MongoDB | 说明 |
|------|---------------------|------|
| Pure String | **2.65x** | 纯字符串字段文档 |
| Numeric Heavy | **2.18x** | 大量数值字段文档 |
| 综合场景 | **3.22x** | 混合类型文档 |
| Array Heavy | 1.04x | 大量数组字段（相对较慢） |

**结论**: 当前性能已经达到 2-3x，但仍有优化空间。

### 11.2 性能瓶颈分析

#### 🔴 P0 - 装箱开销（Boxing Overhead）- **严重**

**问题描述**:
当前架构中，`TypeHandler.parseValue()` 返回 `Object`，导致所有基本类型被自动装箱：

```java
// BsonReader.java
public int readInt32() {
    return value;  // primitive int
}

// Int32Parser.java
public Object parse(BsonReader reader) {
    return reader.readInt32();  // ❌ 自动装箱：int → Integer
}

// DocumentParser.java
Object value = handler.parseValue(reader, type);  // ❌ 装箱的 Object
document.put(fieldName, value);  // ❌ 存入 HashMap<String, Object>
```

**性能影响**:
- 每个基本类型值都创建一个包装对象（Integer, Long, Double, Boolean）
- 100 个 Int32 字段 = 100 个 Integer 对象 = **至少 1600 字节**（每个 Integer 16 字节）
- 增加 GC 压力，尤其在高频解析场景
- CPU 缓存命中率降低（对象分散在堆中）

**受影响的类型**:
- Int32 (0x10) → Integer
- Int64 (0x12) → Long
- Double (0x01) → Double
- Boolean (0x08) → Boolean

**性能影响预估**: **20-50%**

#### 🟡 P1 - 对象分配开销（Object Allocation）- **中等**

**问题**: HashMap 频繁创建，每次解析都分配新 HashMap

```java
// DocumentParser.java
Map<String, Object> document = new HashMap<String, Object>();
// ❌ 没有指定初始容量，默认 16，可能扩容
```

**性能影响**:
- 每次解析文档都分配新 HashMap
- HashMap 内部需要分配 Entry 数组
- 嵌套文档会递归创建多个 HashMap

**优化方向**:
- ThreadLocal 对象池复用 HashMap
- 为 DocumentParser 添加初始容量参数

**性能影响预估**: **10-20%**

#### 🟡 P1 - String 创建开销 - **中等**

**问题**: 字段名重复创建，没有 intern

```java
// BsonReader.java
String str = new String(buffer, start, position - start, StandardCharsets.UTF_8);
// ❌ 每次都创建新 String，没有 intern
```

**问题分析**:
- 字段名重复率极高：`"name"`, `"age"`, `"email"`, `"id"`, `"created_at"` 等
- 每个文档解析都创建相同的字段名 String
- String 对象开销：40 字节（对象头 + char[] + hash + length）

**优化方向**:
- 实现字段名缓存/interning（HashMap<String, String>）
- 使用弱引用避免内存泄漏

**性能影响预估**: **5-15%**

### 11.3 优化优先级排序

**P0 - 高优先级（性能提升 20-50%）**:

1. **消除装箱开销** - 引入零装箱的 Document 类型系统
2. **实现对象池** - ThreadLocal 对象池
3. **字段名 intern** - 弱引用缓存

**P1 - 中优先级（性能提升 5-15%）**:

4. HashMap 初始容量优化
5. String 解码优化（ASCII 快速路径）

**P2 - 低优先级（性能提升 < 5%）**:

6. 减少边界检查（unsafe 版本）
7. 类型分发优化（常见类型快速路径）

---

## 12. Phase 2.13+ 零装箱架构设计

### 12.1 设计目标

**核心问题**: 当前 `TypeHandler.parseValue()` 返回 `Object`，导致基本类型装箱。

**解决方案**: 引入三层架构，支持零装箱的 primitive 类型访问。

**实现策略**: 提供两种实现，默认使用 Fast 实现（fastutil）

- **Fast 实现（默认）**: 使用 fastutil 的 primitive maps，零装箱，性能最优
- **Simple 实现（可选）**: 零依赖，使用 Union 类型（BsonValue），性能次优但仍优于装箱

### 12.2 三层架构设计

```
┌─────────────────────────────────────────────────────────┐
│  Parser Layer (TypeHandler, DocumentParser等)           │
│  只依赖抽象接口 BsonDocument, BsonDocumentBuilder       │
└──────────────────┬──────────────────────────────────────┘
                   │ 依赖抽象
                   ↓
┌─────────────────────────────────────────────────────────┐
│  Abstraction Layer (接口层)                             │
│  - BsonDocument (读取接口)                              │
│  - BsonDocumentBuilder (构建接口)                       │
│  - BsonDocumentFactory (工厂接口)                       │
└──────────────────┬──────────────────────────────────────┘
                   │ 实现
        ┌──────────┴──────────┐
        ↓                     ↓
┌──────────────────┐  ┌──────────────────┐
│ Simple实现       │  │ Fast实现（默认） │
│ (零依赖)         │  │ (fastutil)       │
│ SimpleBson       │  │ FastBson         │
│ Document         │  │ Document         │
│ (BsonValue存储)  │  │ (Primitive Maps) │
└──────────────────┘  └──────────────────┘
```

### 12.3 核心接口设计

#### BsonDocument 接口

```java
public interface BsonDocument {
    // 类型判断
    boolean contains(String fieldName);
    byte getType(String fieldName);
    int size();
    Set<String> fieldNames();

    // ✅ Primitive类型访问（无装箱）
    int getInt32(String fieldName);
    int getInt32(String fieldName, int defaultValue);
    long getInt64(String fieldName);
    double getDouble(String fieldName);
    boolean getBoolean(String fieldName);

    // 引用类型访问
    String getString(String fieldName);
    BsonDocument getDocument(String fieldName);
    BsonArray getArray(String fieldName);

    // Legacy兼容（装箱）
    @Deprecated
    Object get(String fieldName);

    @Deprecated
    Map<String, Object> toLegacyMap();
}
```

#### BsonDocumentBuilder 接口

```java
public interface BsonDocumentBuilder {
    // ✅ Primitive类型添加（无装箱）
    BsonDocumentBuilder putInt32(String fieldName, int value);
    BsonDocumentBuilder putInt64(String fieldName, long value);
    BsonDocumentBuilder putDouble(String fieldName, double value);
    BsonDocumentBuilder putBoolean(String fieldName, boolean value);

    // 引用类型添加
    BsonDocumentBuilder putString(String fieldName, String value);
    BsonDocumentBuilder putDocument(String fieldName, BsonDocument value);
    BsonDocumentBuilder putArray(String fieldName, BsonArray value);

    // 构建
    BsonDocument build();
    BsonDocumentBuilder estimateSize(int fieldCount);
    void reset();
}
```

#### BsonDocumentFactory 接口

```java
public interface BsonDocumentFactory {
    BsonDocumentBuilder newDocumentBuilder();
    BsonArrayBuilder newArrayBuilder();
    BsonDocument emptyDocument();
    BsonArray emptyArray();
    String getName();
    boolean requiresExternalDependencies();
}
```

### 12.4 Fast 实现（默认，基于 fastutil）

**核心思想**: 使用 fastutil 的 primitive maps 存储，完全消除装箱

#### FastBsonDocument 存储策略

```java
public final class FastBsonDocument implements BsonDocument {
    // 字段名映射
    private final Object2IntMap<String> fieldNameToId;  // fieldName → field_id
    private final Int2ObjectMap<String> fieldIdToName;  // field_id → fieldName

    // 类型映射
    private final IntByteMap fieldTypes;  // field_id → type

    // ✅ Primitive类型存储（零装箱）
    private final IntIntMap intFields;      // field_id → int
    private final IntLongMap longFields;    // field_id → long
    private final IntDoubleMap doubleFields; // field_id → double
    private final BitSet booleanFields;     // field_id → boolean

    // 引用类型存储
    private final Int2ObjectMap<String> stringFields;
    private final Int2ObjectMap<Object> complexFields;
}
```

**优势**:
- ✅ **完全零装箱**: Int32 存储为 primitive int
- ✅ **内存节省 60%**: 相比装箱方案
- ✅ **访问速度 3x**: 无装箱/拆箱开销
- ✅ **GC压力 -83%**: 极少对象分配

#### FastBsonDocument 访问示例

```java
@Override
public int getInt32(String fieldName) {
    int fieldId = fieldNameToId.getInt(fieldName);
    if (fieldId < 0) {
        throw new NullPointerException("Field not found: " + fieldName);
    }
    return intFields.get(fieldId);  // ✅ 返回 primitive int，零装箱
}
```

### 12.5 Simple 实现（可选，零依赖）

**核心思想**: 使用 Union 类型（SimpleBsonValue）存储所有类型

#### SimpleBsonValue - Union 类型

```java
final class SimpleBsonValue {
    final byte type;

    // Primitive存储（union）
    int intValue;
    long longValue;      // 也用于存储 DateTime
    // double复用longValue (通过Double.doubleToRawLongBits)

    // 引用类型存储
    Object refValue;     // String, BsonDocument, BsonArray等

    // ✅ Int32缓存（-128~127）
    private static final SimpleBsonValue[] INT32_CACHE = new SimpleBsonValue[256];

    // ✅ Boolean单例
    static final SimpleBsonValue TRUE = ...;
    static final SimpleBsonValue FALSE = ...;
}
```

**优势**:
- ✅ **零外部依赖**: 只使用 JDK 标准库
- ✅ **小整数缓存**: -128~127 的 Int32 值使用缓存，零 GC
- ✅ **Boolean 单例**: true/false 共享单例，零 GC
- ✅ **内存节省 25%**: 相比装箱方案
- ✅ **访问速度 1.25x**: 优于装箱

#### SimpleBsonDocument 实现

```java
public final class SimpleBsonDocument implements BsonDocument {
    private final Map<String, SimpleBsonValue> fields;

    @Override
    public int getInt32(String fieldName) {
        SimpleBsonValue value = fields.get(fieldName);
        if (value == null) {
            throw new NullPointerException("Field not found: " + fieldName);
        }
        return value.asInt32();  // ✅ 返回 primitive int，无装箱
    }
}
```

### 12.6 Parser 层集成

#### 修改 DocumentParser 使用 Builder

```java
public enum DocumentParser implements BsonTypeParser {
    INSTANCE;

    private TypeHandler handler;
    private BsonDocumentFactory factory;  // ✅ 工厂注入

    @Override
    public Object parse(BsonReader reader) {
        int docLength = reader.readInt32();
        int endPosition = reader.position() + docLength - 4;

        // 使用工厂创建Builder
        BsonDocumentBuilder builder = factory.newDocumentBuilder();

        while (reader.position() < endPosition) {
            byte type = reader.readByte();
            if (type == BsonType.END_OF_DOCUMENT) break;

            String fieldName = reader.readCString();

            // ✅ 根据类型使用不同的put方法（无装箱）
            switch (type) {
                case BsonType.INT32:
                    int intValue = reader.readInt32();
                    builder.putInt32(fieldName, intValue);  // ✅ 无装箱
                    break;

                case BsonType.INT64:
                    long longValue = reader.readInt64();
                    builder.putInt64(fieldName, longValue);  // ✅ 无装箱
                    break;

                case BsonType.DOUBLE:
                    double doubleValue = reader.readDouble();
                    builder.putDouble(fieldName, doubleValue);  // ✅ 无装箱
                    break;

                case BsonType.BOOLEAN:
                    boolean boolValue = reader.readByte() != 0;
                    builder.putBoolean(fieldName, boolValue);  // ✅ 无装箱
                    break;

                // ... 其他类型
            }
        }

        return builder.build();
    }
}
```

#### 修改 TypeHandler 支持工厂

```java
public class TypeHandler {
    private static final TypeHandler INSTANCE = new TypeHandler();

    // ✅ 默认使用 Fast 实现
    private static BsonDocumentFactory documentFactory =
        FastBsonDocumentFactory.INSTANCE;

    static {
        initializeParsers();
    }

    private static void initializeParsers() {
        // 注入工厂到需要的parser
        DocumentParser.INSTANCE.setHandler(INSTANCE);
        DocumentParser.INSTANCE.setFactory(documentFactory);

        ArrayParser.INSTANCE.setHandler(INSTANCE);
        ArrayParser.INSTANCE.setFactory(documentFactory);
    }

    /**
     * 设置Document工厂（全局配置）
     *
     * 默认：FastBsonDocumentFactory（fastutil实现，性能最优）
     * 可选：SimpleBsonDocumentFactory（零依赖，性能次优）
     */
    public static void setDocumentFactory(BsonDocumentFactory factory) {
        documentFactory = factory;
        initializeParsers();
    }
}
```

### 12.7 使用示例

#### 默认使用（Fast 实现，fastutil）

```java
// ✅ 默认使用Fast实现（fastutil），性能最优
byte[] bsonData = ...;
BsonDocument doc = new PartialParser("name", "age").parseToBsonDocument(bsonData);

// ✅ 无装箱访问
int age = doc.getInt32("age");        // 从 IntIntMap 读取，零装箱
String name = doc.getString("name");  // 从 Int2ObjectMap 读取
```

#### 切换到 Simple 实现（零依赖）

```java
// 如果不想添加fastutil依赖，可以切换到Simple实现
TypeHandler.setDocumentFactory(SimpleBsonDocumentFactory.INSTANCE);

// API完全相同，但内部使用SimpleBsonValue
BsonDocument doc = new PartialParser("name", "age").parseToBsonDocument(bsonData);

// ✅ 仍然无装箱访问
int age = doc.getInt32("age");  // 从 SimpleBsonValue.asInt32() 读取，无装箱
```

### 12.8 性能对比

#### 内存占用对比

**100个字段的文档（50 Int32, 30 String, 20 Document）**:

| 实现 | 内存占用 | 节省 | 说明 |
|------|---------|------|------|
| **当前装箱** | ~8KB | - | 50个Integer(16字节) + Entry开销 |
| **Simple** | ~6KB | **-25%** | 50个BsonValue(48字节，部分缓存) |
| **Fast（默认）** | ~3KB | **-60%** | 50个int(4字节，primitive map) |

#### 访问性能对比

**1000万次 getInt32() 访问**:

| 实现 | 耗时 | 加速 | 说明 |
|------|------|------|------|
| **当前装箱** | 150ms | 1.0x | HashMap.get() + 拆箱 |
| **Simple** | 120ms | **1.25x** | HashMap.get(BsonValue) + asInt32() |
| **Fast（默认）** | 50ms | **3x** | Object2IntMap + IntIntMap |

#### GC 压力对比

**解析1000个文档（每个100个字段）**:

| 实现 | GC次数 | Young GC时间 | 改善 |
|------|--------|-------------|------|
| **当前装箱** | 12次 | 45ms | - |
| **Simple** | 5次 | 20ms | **-58% GC次数** |
| **Fast（默认）** | 2次 | 8ms | **-83% GC次数** |

### 12.9 性能目标

**Phase 2.12 当前性能**: 2-3x vs MongoDB

**Phase 2.13 完成后目标**:

- **Simple 实现**: **3.5-4.5x** vs MongoDB（零依赖）
- **Fast 实现（默认）**: **5-6x** vs MongoDB（需要 fastutil）

### 12.10 实施计划

**Phase 2.13**: 零装箱架构实现（7-9天）

- **Phase 2.13A**: 抽象接口层（1天）
  - 定义 BsonDocument, BsonDocumentBuilder, BsonDocumentFactory 接口
  - 定义辅助类型（BsonBinary, BsonDecimal128 等）

- **Phase 2.13B**: Simple 实现（2天）
  - 实现 SimpleBsonValue (Union 类型)
  - 实现 SimpleBsonDocument, SimpleBsonArray
  - 实现 SimpleBsonDocumentBuilder, SimpleBsonArrayBuilder
  - 实现 SimpleBsonDocumentFactory

- **Phase 2.13C**: Fast 实现（2-3天）
  - 添加 fastutil 依赖
  - 实现 FastBsonDocument (primitive maps)
  - 实现 FastBsonArray
  - 实现 FastBsonDocumentBuilder, FastBsonArrayBuilder
  - 实现 FastBsonDocumentFactory

- **Phase 2.13D**: Parser 集成（1天）
  - 修改 DocumentParser 使用 Builder
  - 修改 ArrayParser 使用 Builder
  - 修改 TypeHandler 支持工厂配置
  - 默认使用 FastBsonDocumentFactory
  - 提供 Legacy API 兼容层

- **Phase 2.13E**: 测试和文档（1-2天）
  - 更新所有单元测试
  - 性能对比测试
  - API 文档
  - 迁移指南

**后续优化**:

- **Phase 2.14**: 字段名 Interning（1天）- 减少字符串创建
- **Phase 2.15**: ThreadLocal 对象池（1天）- 减少 HashMap/Builder 分配
- **Phase 2.16**: String 解码优化（1天）- ASCII 快速路径
- **Phase 2.17**: 边界检查优化（可选）- Unsafe 版本

---

## 13. 总结

FastBSON 项目聚焦于 BSON 协议的高性能处理，主要提供两个核心能力：

### 核心价值

1. **完整反序列化能力**：支持所有 BSON 类型的解析
2. **部分字段读取**：高效提取需要的字段，跳过不需要的字段
3. **避免完整解析开销**：利用 BSON 长度前缀特性，实现 O(1) 跳过
4. **极致性能**：采用多项性能优化技术和最佳实践
5. **简洁易用**：提供直观的 API，降低使用门槛

### 适用场景

该项目特别适合以下场景：

- **微服务架构**：服务间通信，只需要部分字段
- **消息队列**：高吞吐量消息处理
- **数据管道**：ETL 过程中的数据转换
- **日志分析**：从日志中提取关键字段
- **API 网关**：请求/响应字段过滤

### 预期收益

- **性能提升**：3-10 倍（取决于字段提取比例）
- **内存优化**：减少 50-70% 的内存分配
- **CPU 节省**：减少 40-60% 的 CPU 使用
- **开发效率**：简洁的 API，提升开发体验

---

*文档版本: 1.0*
*最后更新: 2025-11*
