# FastBSON 待开发阶段

本文档记录 FastBSON 项目的待开发阶段和计划。

最后更新：2025-12-02

---

## 总览

- ⏳ **Phase 3**: 高级性能优化（0/6 任务完成）
- ⏳ **Phase 4**: API 完善与生产就绪（0/7 任务完成）
- 🔮 **Phase 5**: 高级特性（可选，0/3 任务）

**总计**: 0/16 任务完成（0%）

---

## Phase 3: 高级性能优化

### 目标

在当前基础上再提升 20-30% 性能，并改善特定场景（如数组密集型文档）的性能。

**当前性能基线**: 2.18x ~ 7.99x vs MongoDB

**Phase 3 目标**: 3x ~ 10x vs MongoDB

### 预计时间

2-3 周

---

### ⏳ Task 3.1: 字段名 Interning 优化

**优先级**: P0 - 关键

**状态**: ⏳ 待开始

**预计时间**: 1 天

**预期性能提升**: +5-15%

#### 目标

实现字段名缓存/interning，减少重复字段名的 String 对象创建。

#### 功能需求

- 实现 FieldNamePool 类
- 使用弱引用避免内存泄漏
- 修改 BsonReader.readCString() 使用 intern
- intern 后可使用 `==` 而非 `equals()`

#### 实施步骤

1. 创建 FieldNamePool 类
2. 修改 BsonReader.readCString()
3. 测试字段名复用率
4. Benchmark 验证性能提升
5. 更新文档

---

### ⏳ Task 3.2: ThreadLocal 对象池

**优先级**: P0 - 关键

**状态**: ⏳ 待开始

**预计时间**: 1-2 天

**预期性能提升**: +10-20%

#### 目标

实现 ThreadLocal 对象池，复用临时对象，降低 GC 压力。

#### 功能需求

- BsonReader 池化
- HashMap 池化
- StringBuilder 池化
- 对象外泄保护

#### 实施步骤

1. 创建 ObjectPool 类
2. 实现 BsonReader 池
3. 实现 HashMap 池
4. 实现 StringBuilder 池
5. 修改 Parser 使用对象池
6. 测试对象外泄保护
7. Benchmark 验证性能提升
8. 更新文档

#### 风险评估

- 对象外泄：需要确保池化对象不会逃逸到调用者
- 线程安全：ThreadLocal 本身线程安全

---

### ⏳ Task 3.3: HashMap 初始容量优化

**优先级**: P1 - 高

**状态**: ⏳ 待开始

**预计时间**: 0.5 天

**预期性能提升**: +2-5%

#### 目标

启发式容量估算，避免 HashMap rehash。

#### 功能需求

- 根据文档长度估算字段数量
- 设置合适的初始容量
- 避免 HashMap 扩容

#### 实施步骤

1. 分析实际文档的字段数量 vs 文档大小比例
2. 实现启发式估算公式
3. 修改 DocumentParser 使用初始容量
4. Benchmark 验证性能提升
5. 更新文档

---

### ⏳ Task 3.4: 数组优化

**优先级**: P1 - 高

**状态**: ⏳ 待开始

**预计时间**: 2-3 天

**预期性能提升**: 数组场景 +100-200%

#### 问题

当前数组解析性能较弱（1.34x vs MongoDB）。

#### 目标

优化数组解析，从 1.34x 提升到 **2.5-3x** vs MongoDB。

#### 解决方案

**方案 1：优化 ArrayParser**
- 直接预分配 ArrayList
- 假定数组元素按顺序出现

**方案 2：IndexedBsonArray 快速路径**
- 为连续数组提供快速路径
- 非连续数组回退到慢速路径

#### 实施步骤

1. 分析数组解析瓶颈（profiling）
2. 实现方案 1（优化 ArrayParser）
3. 实现方案 2（IndexedBsonArray 快速路径）
4. 测试非连续索引数组的兼容性
5. Benchmark 验证性能提升
6. 更新文档

---

### ⏳ Task 3.5: String 解码优化

**优先级**: P2 - 中

**状态**: ⏳ 待开始

**预计时间**: 1 天

**预期性能提升**: String 密集场景 +10-20%

#### 目标

实现 ASCII 快速路径，优化纯 ASCII 字符串解码。

#### 功能需求

- 快速检查是否为纯 ASCII
- ASCII 快速路径解码
- UTF-8 慢速路径保持兼容性

#### 实施步骤

1. 实现 ASCII 检测
2. 实现 ASCII 快速路径
3. 测试 UTF-8 字符串兼容性
4. Benchmark 验证性能提升
5. 更新文档

---

### ⏳ Task 3.6: 常见类型分支优化

**优先级**: P2 - 中

**状态**: ⏳ 待开始

**预计时间**: 0.5 天

**预期性能提升**: +2-5%

#### 目标

优化 CPU 分支预测，将常见类型放在前面。

#### 功能需求

- 统计实际场景中各类型的出现频率
- 调整类型分派顺序
- 常见类型（Int32, String, Double）优先检查

#### 实施步骤

1. 统计类型出现频率
2. 调整 TypeHandler 类型分派顺序
3. Benchmark 验证性能提升
4. 更新文档

---

### Phase 3 预期成果

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

### 目标

完善 API 易用性、文档和生产环境就绪。

### 预计时间

2-3 周

---

### ⏳ Task 4.1: BsonDocument 类型安全访问器增强

**优先级**: P1 - 高

**状态**: ⏳ 待开始

**预计时间**: 1-2 天

#### 功能需求

- 嵌套字段访问（点分隔路径）
- Optional 风格 API
- 类型转换（自动转换兼容类型）
- 批量获取方法

#### 示例

```java
// 嵌套字段访问
String city = doc.getString("address.city");

// Optional 风格
Optional<String> email = doc.getOptionalString("email");

// 类型转换
long value = doc.getLong("intField");  // Int32 自动转 Long

// 批量获取
Map<String, Object> fields = doc.getFields("name", "age", "email");
```

---

### ⏳ Task 4.2: 流式 API

**优先级**: P2 - 中

**状态**: ⏳ 待开始

**预计时间**: 1-2 天

#### 功能需求

- 解析 BSON 数组流
- 支持 filter/map/collect 等操作
- 支持并行处理

#### 示例

```java
List<User> users = FastBson.parseArray(bsonArrayData)
    .filter(doc -> doc.getInt("age") > 18)
    .map(doc -> new User(doc.getString("name"), doc.getInt("age")))
    .collect(Collectors.toList());
```

---

### ⏳ Task 4.3: Builder API 增强

**优先级**: P2 - 中

**状态**: ⏳ 待开始

**预计时间**: 1 天

#### 功能需求

- 增强配置选项
- 配置验证
- 更直观的 API

#### 示例

```java
FastBsonParser parser = FastBsonParser.builder()
    .fields("name", "age", "email")
    .earlyExit(true)
    .useIndexedMode()
    .enableFieldNameCaching(true)
    .estimateFieldCount(50)
    .build();
```

---

### ⏳ Task 4.4: 异常处理增强

**优先级**: P2 - 中

**状态**: ⏳ 待开始

**预计时间**: 1 天

#### 功能需求

- 更详细的异常信息（偏移、字段路径）
- 宽容模式（跳过错误字段）
- 异常处理文档

#### 示例

```java
try {
    BsonDocument doc = FastBson.parse(malformedData);
} catch (BsonParseException e) {
    System.out.println("Parse error at byte offset: " + e.getOffset());
    System.out.println("Field path: " + e.getFieldPath());
}
```

---

### ⏳ Task 4.5: 完整文档和示例

**优先级**: P1 - 高

**状态**: ⏳ 待开始

**预计时间**: 2-3 天

#### 文档需求

1. **API Reference** (JavaDoc)
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

---

### ⏳ Task 4.6: 边界情况和异常数据测试

**优先级**: P1 - 高

**状态**: ⏳ 待开始

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

---

### ⏳ Task 4.7: 生产环境就绪检查

**优先级**: P1 - 高

**状态**: ⏳ 待开始

**预计时间**: 2 天

#### 检查清单

- [ ] 所有测试通过（100% 覆盖率）
- [ ] 性能目标达成（3-10x vs MongoDB）
- [ ] 内存泄漏测试
- [ ] 并发安全测试
- [ ] 压力测试（长时间运行）
- [ ] 完整文档
- [ ] 代码审查
- [ ] License 和 Copyright
- [ ] Maven Central 发布准备

---

## Phase 5: 高级特性（可选）

这些功能为可选的高级特性，根据实际需求决定是否实现。

---

### 🔮 Task 5.1: BSON 序列化支持

**优先级**: P3 - 低

**状态**: 🔮 待评估

**预计时间**: 2-3 周

#### 功能需求

- 从 Java 对象序列化到 BSON
- 从 Java Bean 序列化
- Builder 模式构建 BSON 文档

#### 示例

```java
// 从 BsonDocument 序列化
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

### 🔮 Task 5.2: 流式大文档处理

**优先级**: P3 - 低

**状态**: 🔮 待评估

**预计时间**: 1-2 周

#### 功能需求

- 流式读取大文档
- 无需一次性加载到内存
- 支持超大文档（> 100MB）

#### 示例

```java
try (BsonStreamReader reader = FastBson.streamReader(inputStream)) {
    while (reader.hasNext()) {
        BsonDocument doc = reader.next();
        processDocument(doc);
    }
}
```

---

### 🔮 Task 5.3: MongoDB Driver 集成

**优先级**: P3 - 低

**状态**: 🔮 待评估

**预计时间**: 1 周

#### 功能需求

- 与 MongoDB Driver 互操作
- 兼容 org.bson.BsonDocument
- 透明集成

#### 示例

```java
MongoCollection<BsonDocument> collection = ...;
BsonDocument doc = collection.find().first();

byte[] bsonData = doc.toByteArray();
BsonDocument fastDoc = FastBson.parse(bsonData);
```

---

## 未确定的未来功能

以下功能需要进一步评估可行性和优先级：

### 1. SIMD 向量化优化

使用 Java Vector API (Project Panama) 进行 SIMD 优化。

**预期收益**: +20-50% 在支持 AVX2/AVX512 的 CPU 上

**风险**: 需要 Java 16+，兼容性问题

**状态**: 🔮 待评估

---

### 2. Native JNI 优化

使用 JNI 实现关键路径。

**预期收益**: +50-100% 在关键路径上

**风险**: 跨平台兼容性、维护成本高

**状态**: 🔮 待评估

---

### 3. Unsafe 版本

使用 sun.misc.Unsafe 进行直接内存访问。

**预期收益**: +10-20%

**风险**: Java 9+ 不推荐使用，可能在未来版本中移除

**状态**: 🔮 待评估

---

## 参考文档

- [已完成阶段](completed-phases.md) - 查看已完成的工作
- [Backlog](backlog.md) - 详细的待开发功能列表
- [Release Notes](ReleaseNote.md) - 功能发布说明
- [架构设计](architecture.md) - 系统架构

---

*最后更新: 2025-12-02*
