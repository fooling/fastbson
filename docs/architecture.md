# FastBSON 架构设计文档

**高性能 BSON 反序列化与部分字段读取库**

Version: 1.0
Date: 2024-11

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

## 2. 性能优化技术

### 2.1 核心优化理念

#### 2.1.1 假定有序快速匹配算法

基于实际应用观察，文档字段通常按照固定顺序出现。利用这一特性可以大幅提升匹配效率：

- 预先建立字段名到索引的映射关系
- 匹配时优先按照预期顺序查找
- 大幅减少字符串比较次数

#### 2.1.2 ThreadLocal 对象复用

使用 ThreadLocal 存储反序列化过程中的临时数据：

- 减少内存分配次数
- 降低 GC 压力
- 复用 StringBuilder、BsonReader 等对象

#### 2.1.3 字符串内部化（String Interning）

对于重复出现的字符串（特别是字段名），使用字符串池：

- 字符串内部化，减少重复对象创建
- 提高字符串比较效率（可使用 == 而非 equals）
- 降低内存占用

#### 2.1.4 类型处理器缓存

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

### 5.2 Phase 2: 部分字段解析（1周）

**目标：** 实现部分字段读取功能

- 实现 FieldMatcher 字段匹配
- 实现 ValueSkipper 跳过逻辑
- 实现 PartialParser 部分字段解析
- 编写部分字段解析测试

**交付物：**
- FieldMatcher 实现
- ValueSkipper 实现
- PartialParser 实现
- 功能测试用例

### 5.3 Phase 3: 性能优化（1-2周）

**目标：** 提升解析性能

- 实现 ObjectPool 对象池
- 添加字段名内部化
- 优化常见类型解析路径
- 实现有序匹配优化
- 添加性能基准测试

**交付物：**
- 优化后的实现
- 性能测试报告
- 与传统解析器的对比数据

### 5.4 Phase 4: API 完善与测试（1周）

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

## 10. 总结

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
*最后更新: 2024-11*
