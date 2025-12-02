# FastBSON Backlog

本文档记录 FastBSON 项目的待开发功能和未来规划。

最后更新：2025-12-02

---

## 优先级说明

- **P0 - 关键**: 对性能有重大影响，应优先实现
- **P1 - 高**: 有明显价值，应尽快实现
- **P2 - 中**: 有一定价值，可以计划实现
- **P3 - 低**: 可选功能，视情况实现

---

## Phase 3: 高级性能优化

### 概述

Phase 3 专注于进一步优化性能，目标是在当前基础上再提升 20-30%，并改善特定场景（如数组密集型文档）的性能。

**预计时间**: 2-3 周

**当前性能基线**: 2.18x ~ 7.99x vs MongoDB

**Phase 3 目标**: 3x ~ 10x vs MongoDB

---

### Task 3.1: 字段名 Interning 优化

**优先级**: P0 - 关键

**预计时间**: 1 天

**预期性能提升**: +5-15%

#### 问题描述

当前实现中，每次解析文档都会创建新的字段名 String 对象，即使字段名高度重复（如 "id", "name", "age", "created_at" 等）。

#### 解决方案

实现字段名缓存/interning：

```java
public class FieldNamePool {
    // 使用弱引用避免内存泄漏
    private static final Map<String, WeakReference<String>> pool =
        new ConcurrentHashMap<>();

    public static String intern(String fieldName) {
        WeakReference<String> ref = pool.get(fieldName);
        if (ref != null) {
            String cached = ref.get();
            if (cached != null) {
                return cached;
            }
        }
        pool.put(fieldName, new WeakReference<>(fieldName));
        return fieldName;
    }
}
```

#### 优化效果

- **内存节省**: 减少重复字段名的 String 对象创建
- **字符串比较优化**: intern 后可使用 `==` 而非 `equals()`
- **性能提升**: 预计 +5-15%

#### 实施步骤

1. 创建 `FieldNamePool` 类
2. 修改 `BsonReader.readCString()` 使用 intern
3. 测试字段名复用率
4. Benchmark 验证性能提升

---

### Task 3.2: ThreadLocal 对象池

**优先级**: P0 - 关键

**预计时间**: 1-2 天

**预期性能提升**: +10-20%

#### 问题描述

每次解析都创建新的 HashMap、BsonReader、StringBuilder 等对象，增加 GC 压力。

#### 解决方案

实现 ThreadLocal 对象池：

```java
public class ObjectPool {
    // BsonReader 池
    private static final ThreadLocal<BsonReader> readerPool =
        ThreadLocal.withInitial(() -> new BsonReader(new byte[0]));

    // HashMap 池
    private static final ThreadLocal<Map<String, Object>> mapPool =
        ThreadLocal.withInitial(HashMap::new);

    // StringBuilder 池
    private static final ThreadLocal<StringBuilder> sbPool =
        ThreadLocal.withInitial(() -> new StringBuilder(256));

    public static BsonReader getBsonReader(byte[] data) {
        BsonReader reader = readerPool.get();
        reader.reset(data);
        return reader;
    }

    public static Map<String, Object> getMap() {
        Map<String, Object> map = mapPool.get();
        map.clear();
        return map;
    }

    public static StringBuilder getStringBuilder() {
        StringBuilder sb = sbPool.get();
        sb.setLength(0);
        return sb;
    }
}
```

#### 优化效果

- **减少对象创建**: 复用临时对象
- **降低 GC 压力**: 减少 Young GC 次数
- **提升吞吐量**: 预计 +10-20%

#### 风险评估

- **对象外泄**: 需要确保池化对象不会逃逸到调用者
- **线程安全**: ThreadLocal 本身线程安全，但需要注意 reset/clear 调用

#### 实施步骤

1. 创建 `ObjectPool` 类
2. 实现 BsonReader 池化
3. 实现 HashMap 池化
4. 实现 StringBuilder 池化
5. 修改 Parser 使用对象池
6. 测试对象外泄保护
7. Benchmark 验证性能提升

---

### Task 3.3: HashMap 初始容量优化

**优先级**: P1 - 高

**预计时间**: 0.5 天

**预期性能提升**: +2-5%

#### 问题描述

当前 DocumentParser 创建 HashMap 时没有指定初始容量，默认 16，大文档可能需要多次扩容。

#### 解决方案

启发式容量估算：

```java
// 根据文档长度估算字段数量
int estimatedFieldCount = docLength / 20; // 平均每个字段 20 字节
int initialCapacity = (int) (estimatedFieldCount / 0.75f) + 1;
Map<String, Object> document = new HashMap<>(initialCapacity);
```

#### 优化效果

- **减少扩容**: 避免 HashMap rehash
- **减少内存分配**: 一次分配足够容量
- **性能提升**: 预计 +2-5%

#### 实施步骤

1. 分析实际文档的字段数量 vs 文档大小比例
2. 实现启发式估算公式
3. 修改 DocumentParser 使用初始容量
4. Benchmark 验证性能提升

---

### Task 3.4: 数组优化（针对数组密集型场景）

**优先级**: P1 - 高

**预计时间**: 2-3 天

**预期性能提升**: 数组场景 +100-200%

#### 问题描述

当前数组解析性能较弱（1.34x vs MongoDB），在数组密集型场景下表现不佳。

#### 根因分析

1. 数组解析需要转换 BSON 文档（索引为字符串）到 Java List
2. 需要多次字符串比较和转换（"0", "1", "2" ...）
3. IndexedBsonArray 可能在大数组场景下索引构建开销大

#### 解决方案

**方案 1：优化 ArrayParser**

```java
// 直接预分配 ArrayList，避免 Document → List 转换
public List<Object> parseArray(BsonReader reader) {
    int docLength = reader.readInt32();
    int endPosition = reader.position() + docLength - 4;

    // 预分配 ArrayList（估算元素数量）
    List<Object> array = new ArrayList<>(docLength / 10);

    int expectedIndex = 0;
    while (reader.position() < endPosition) {
        byte type = reader.readByte();
        if (type == 0) break;

        String indexStr = reader.readCString();

        // 假定数组元素按顺序出现（0, 1, 2, ...）
        if (indexStr.equals(String.valueOf(expectedIndex))) {
            Object value = handler.parseValue(reader, type);
            array.add(value);
            expectedIndex++;
        } else {
            // 非连续索引，回退到原始逻辑
            // ...
        }
    }

    return array;
}
```

**方案 2：IndexedBsonArray 快速路径**

```java
// 为连续数组提供快速路径
class IndexedBsonArray {
    private boolean isContiguous;  // 数组索引是否连续

    public Object get(int index) {
        if (isContiguous) {
            // 快速路径：直接访问 elements[index]
            return parseElement(elements[index]);
        } else {
            // 慢速路径：查找索引
            return parseElement(findElement(index));
        }
    }
}
```

#### 优化效果

- **数组场景性能**: 从 1.34x 提升到 **2.5-3x** vs MongoDB
- **内存优化**: 减少 Document 对象创建

#### 实施步骤

1. 分析数组解析瓶颈（profiling）
2. 实现方案 1（优化 ArrayParser）
3. 实现方案 2（IndexedBsonArray 快速路径）
4. 测试非连续索引数组的兼容性
5. Benchmark 验证性能提升

---

### Task 3.5: String 解码优化（ASCII 快速路径）

**优先级**: P2 - 中

**预计时间**: 1 天

**预期性能提升**: String 密集场景 +10-20%

#### 问题描述

当前 String 解码统一使用 `new String(bytes, UTF-8)`，对于纯 ASCII 字符串（占 90%）性能不够优化。

#### 解决方案

实现 ASCII 快速路径：

```java
public String parseString(BsonReader reader) {
    int length = reader.readInt32() - 1; // 排除 null terminator
    byte[] bytes = reader.readBytes(length);
    reader.skip(1); // skip null terminator

    // 快速检查是否为纯 ASCII
    boolean isAscii = true;
    for (byte b : bytes) {
        if (b < 0) { // 非 ASCII
            isAscii = false;
            break;
        }
    }

    if (isAscii) {
        // ASCII 快速路径：直接转换（性能 2x）
        return new String(bytes, 0, length, StandardCharsets.US_ASCII);
    } else {
        // UTF-8 慢速路径
        return new String(bytes, 0, length, StandardCharsets.UTF_8);
    }
}
```

#### 优化效果

- **ASCII 字符串**: 解码速度 +100%
- **String 密集场景**: 总体性能 +10-20%

#### 实施步骤

1. 实现 ASCII 检测和快速路径
2. 测试 UTF-8 字符串兼容性
3. Benchmark 验证性能提升

---

### Task 3.6: 常见类型分支优化

**优先级**: P2 - 中

**预计时间**: 0.5 天

**预期性能提升**: +2-5%

#### 问题描述

TypeHandler 的类型分派对所有类型一视同仁，但实际上 Int32, String, Double 占 80% 的字段。

#### 解决方案

将常见类型放在前面：

```java
public Object parseValue(BsonReader reader, byte type) {
    // 最常见类型放在最前面（优化分支预测）
    if (type == BsonType.INT32) return reader.readInt32();
    if (type == BsonType.STRING) return parseString(reader);
    if (type == BsonType.DOUBLE) return reader.readDouble();
    if (type == BsonType.INT64) return reader.readInt64();
    if (type == BsonType.BOOLEAN) return reader.readByte() != 0;

    // 其他类型使用查找表
    BsonTypeParser parser = PARSERS[type & 0xFF];
    if (parser != null) {
        return parser.parse(reader);
    }

    throw new InvalidBsonTypeException("Unknown type: " + type);
}
```

#### 优化效果

- **CPU 分支预测**: 提升 branch predictor 命中率
- **性能提升**: 预计 +2-5%

#### 实施步骤

1. 统计实际场景中各类型的出现频率
2. 调整类型分派顺序
3. Benchmark 验证性能提升

---

### Phase 3 总体目标

完成 Phase 3 后，预期性能指标：

| 场景 | 当前性能 | Phase 3 目标 | 改善 |
|------|---------|-------------|------|
| HashMap 完整解析 | 2.18x | **3.0x** | +37% |
| PartialParser 早退 | 7.99x | **10x** | +25% |
| IndexedDocument 零复制 | 5.64x | **7x** | +24% |
| 数组密集型 | 1.34x | **2.5-3x** | +87-124% |
| String 密集型 | 2.17x | **2.8x** | +29% |

---

## Phase 4: API 完善与生产就绪

### 概述

Phase 4 专注于 API 易用性、文档完善和生产环境就绪。

**预计时间**: 2-3 周

---

### Task 4.1: BsonDocument 类型安全访问器增强

**优先级**: P1 - 高

**预计时间**: 1-2 天

#### 功能需求

```java
BsonDocument doc = ...;

// 嵌套字段访问（点分隔路径）
String city = doc.getString("address.city");
int zipCode = doc.getInt("address.zipCode");

// Optional 风格 API
Optional<String> email = doc.getOptionalString("email");

// 类型转换（自动转换兼容类型）
long value = doc.getLong("intField");  // Int32 自动转 Long

// 批量获取
Map<String, Object> fields = doc.getFields("name", "age", "email");
```

#### 实施步骤

1. 实现嵌套字段路径解析
2. 实现 Optional 风格 API
3. 实现自动类型转换
4. 添加批量获取方法
5. 编写单元测试
6. 更新 API 文档

---

### Task 4.2: 流式 API（Stream API）

**优先级**: P2 - 中

**预计时间**: 1-2 天

#### 功能需求

```java
// 解析 BSON 数组流
List<BsonDocument> results = FastBson.parseArray(bsonArrayData)
    .filter(doc -> doc.getInt("age") > 18)
    .map(doc -> new User(
        doc.getString("name"),
        doc.getInt("age")
    ))
    .collect(Collectors.toList());

// 批量文档处理
FastBson.stream(bsonDataList)
    .parallel()
    .map(data -> FastBson.parse(data))
    .forEach(doc -> processDocument(doc));
```

#### 实施步骤

1. 实现 BsonDocumentStream 类
2. 实现 filter/map/collect 等操作
3. 支持并行处理
4. 编写单元测试
5. 性能测试（vs 传统循环）
6. 更新 API 文档

---

### Task 4.3: Builder API 增强

**优先级**: P2 - 中

**预计时间**: 1 天

#### 功能需求

```java
FastBsonParser parser = FastBsonParser.builder()
    .fields("name", "age", "email")       // 指定字段
    .earlyExit(true)                      // 提前退出
    .useIndexedMode()                     // 使用 IndexedDocument
    .enableFieldNameCaching(true)         // 字段名缓存
    .estimateFieldCount(50)               // 估算字段数
    .build();
```

#### 实施步骤

1. 增强 Builder API
2. 添加配置选项
3. 实现配置验证
4. 编写单元测试
5. 更新 API 文档

---

### Task 4.4: 异常处理增强

**优先级**: P2 - 中

**预计时间**: 1 天

#### 功能需求

```java
// 更详细的异常信息
try {
    BsonDocument doc = FastBson.parse(malformedData);
} catch (BsonParseException e) {
    System.out.println("Parse error at byte offset: " + e.getOffset());
    System.out.println("Expected type: " + e.getExpectedType());
    System.out.println("Actual type: " + e.getActualType());
    System.out.println("Field path: " + e.getFieldPath());
}

// 宽容模式（跳过错误字段）
FastBsonParser parser = FastBsonParser.builder()
    .lenientMode(true)  // 跳过无法解析的字段
    .build();
```

#### 实施步骤

1. 增强异常信息（偏移、字段路径等）
2. 实现宽容模式
3. 编写异常处理测试
4. 更新异常处理文档

---

### Task 4.5: 完整文档和示例

**优先级**: P1 - 高

**预计时间**: 2-3 天

#### 文档需求

1. **API Reference** (javadoc)
   - 所有 public API 的 JavaDoc
   - 代码示例
   - 性能说明

2. **User Guide**
   - 快速开始指南
   - 常见场景示例
   - 性能调优指南
   - 故障排查指南

3. **Design Document**
   - 架构设计说明
   - 性能优化原理
   - 与 MongoDB BSON 的对比

4. **Migration Guide**
   - 从 MongoDB BSON 迁移指南
   - API 差异说明
   - 兼容性说明

#### 实施步骤

1. 完善所有 public API 的 JavaDoc
2. 编写 User Guide
3. 编写 Design Document
4. 编写 Migration Guide
5. 创建代码示例仓库

---

### Task 4.6: 边界情况和异常数据测试

**优先级**: P1 - 高

**预计时间**: 2 天

#### 测试场景

1. **边界值测试**
   - 空文档 `{}`
   - 单字段文档
   - 超大文档（> 16MB）
   - 超深嵌套（> 100 层）

2. **异常数据测试**
   - 不完整的 BSON 数据
   - 错误的长度前缀
   - 损坏的字段名
   - 不匹配的类型

3. **兼容性测试**
   - MongoDB 各版本生成的 BSON
   - 不同字节序平台
   - 不同字符编码

#### 实施步骤

1. 生成测试数据
2. 编写边界测试
3. 编写异常数据测试
4. 编写兼容性测试
5. 确保 100% 分支覆盖率

---

### Task 4.7: 生产环境就绪检查清单

**优先级**: P1 - 高

**预计时间**: 2 天

#### 检查项

- ✅ 所有测试通过（100% 覆盖率）
- ✅ 性能目标达成（3-10x vs MongoDB）
- ✅ 内存泄漏测试
- ✅ 并发安全测试
- ✅ 压力测试（长时间运行）
- ✅ 完整文档
- ✅ 代码审查
- ✅ License 和 Copyright
- ✅ Maven Central 发布准备

---

## Phase 5: 高级特性（可选）

### Task 5.1: BSON 序列化支持

**优先级**: P3 - 低

**预计时间**: 2-3 周

#### 功能需求

```java
// 从 Java 对象序列化到 BSON
BsonDocument doc = BsonDocument.builder()
    .putString("name", "Alice")
    .putInt32("age", 25)
    .build();

byte[] bsonData = FastBson.serialize(doc);

// 从 Java Bean 序列化
User user = new User("Alice", 25);
byte[] bsonData = FastBson.serialize(user);
```

---

### Task 5.2: 流式大文档处理

**优先级**: P3 - 低

**预计时间**: 1-2 周

#### 功能需求

```java
// 流式读取大文档（无需一次性加载到内存）
try (BsonStreamReader reader = FastBson.streamReader(inputStream)) {
    while (reader.hasNext()) {
        BsonDocument doc = reader.next();
        processDocument(doc);
    }
}
```

---

### Task 5.3: MongoDB Driver 集成

**优先级**: P3 - 低

**预计时间**: 1 周

#### 功能需求

```java
// 与 MongoDB Driver 互操作
MongoCollection<BsonDocument> collection = ...;
BsonDocument doc = collection.find().first();

// 使用 FastBSON 解析
byte[] bsonData = doc.toByteArray();
BsonDocument fastDoc = FastBson.parse(bsonData);
```

---

## 未确定的未来功能

以下功能需要进一步评估可行性和优先级：

### 1. SIMD 向量化优化

使用 Java Vector API (Project Panama) 进行 SIMD 优化：
- 批量类型检查
- 并行 hash 计算
- 批量字节序转换

**预期收益**: +20-50% 在支持 AVX2/AVX512 的 CPU 上

**风险**: 需要 Java 16+，兼容性问题

---

### 2. Native JNI 优化

使用 JNI 实现关键路径：
- C/C++ 实现的零拷贝解析器
- SIMD 优化的字节操作
- 内存映射文件支持

**预期收益**: +50-100% 在关键路径上

**风险**: 跨平台兼容性、维护成本高

---

### 3. Unsafe 版本

使用 sun.misc.Unsafe 进行直接内存访问：
- 无边界检查的字节读取
- 直接内存分配
- CAS 操作

**预期收益**: +10-20%

**风险**: Java 9+ 不推荐使用，可能在未来版本中移除

---

## 参考文档

- [已完成功能](ReleaseNote.md) - 查看已实现的功能
- [架构设计](architecture.md) - 了解系统架构
- [性能分析](PERFORMANCE_ANALYSIS.md) - 性能瓶颈分析
- [实施阶段](phases.md) - 详细的实施计划

---

*最后更新: 2025-12-02*
