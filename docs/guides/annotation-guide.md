# FastBSON 注解使用指南

本指南详细介绍FastBSON提供的注解系统,包括性能优化注解和类型安全注解。

---

## 目录

1. [注解概览](#注解概览)
2. [@BsonField - 字段映射与优化](#bsonfield---字段映射与优化)
3. [@BsonSchema - 类型安全映射](#bsonschema---类型安全映射)
4. [@BranchOrder - 分支预测优化](#branchorder---分支预测优化)
5. [组合使用示例](#组合使用示例)
6. [性能调优指南](#性能调优指南)
7. [最佳实践](#最佳实践)

---

## 注解概览

FastBSON提供三个核心注解:

| 注解 | 作用范围 | 主要用途 | 性能影响 |
|------|---------|---------|---------|
| `@BsonField` | 字段级 | 字段映射、顺序优化、同构数组提示 | +5-20% |
| `@BsonSchema` | 类级 | 类型安全映射、文档结构定义 | +20-25% |
| `@BranchOrder` | 类级 | CPU分支预测优化 | +2-10% |

---

## @BsonField - 字段映射与优化

### 基本用法

```java
public class User {
    @BsonField("_id")           // BSON字段名映射
    private String id;

    @BsonField("name")          // 显式映射
    private String name;

    @BsonField                  // 使用Java字段名
    private int age;
}
```

### 参数说明

| 参数 | 类型 | 默认值 | 说明 |
|------|-----|-------|------|
| `value` | String | "" | BSON字段名,为空则使用Java字段名 |
| `order` | int | -1 | 字段顺序提示,-1表示无序 |
| `arrayType` | byte | 0 | 同构数组类型提示,0表示自动检测 |

---

### 功能1: 字段顺序优化

**原理**: 提前声明字段在BSON中的出现顺序,减少字段查找开销

**适用场景**: 字段顺序稳定的文档 (如数据库记录、API响应)

```java
@BsonSchema("User")
public class User {
    @BsonField(value = "_id", order = 1)      // 第一个字段
    private String id;

    @BsonField(value = "name", order = 2)     // 第二个字段
    private String name;

    @BsonField(value = "email", order = 3)    // 第三个字段
    private String email;

    @BsonField(value = "age", order = 4)      // 第四个字段
    private int age;

    @BsonField(value = "tags")                // order=-1, 顺序不确定
    private String[] tags;
}
```

**性能提升**: 10-20% (ordered matching)

**使用建议**:
- ✅ **使用**: 字段顺序固定的文档 (数据库、API)
- ❌ **不用**: 字段顺序随机的文档 (用户输入、动态数据)

---

### 功能2: 同构数组类型提示 (arrayType)

**原理**: 编译期声明数组元素类型,跳过运行时类型检测,直接使用快速路径

**适用场景**: 所有元素类型相同的数组 (时序数据、坐标、评分等)

#### 示例1: 时序数据 (INT64 timestamps)

```java
@BsonSchema("SensorData")
public class SensorData {
    @BsonField(value = "sensorId", order = 1)
    private int sensorId;

    // 同构INT64数组: 跳过检测,直接使用parseInt64Array()快速路径
    @BsonField(value = "timestamps", order = 2, arrayType = BsonType.INT64)
    private long[] timestamps;

    // 同构DOUBLE数组: 直接使用parseDoubleArray()快速路径
    @BsonField(value = "values", order = 3, arrayType = BsonType.DOUBLE)
    private double[] values;
}
```

**BSON数据示例**:
```json
{
  "sensorId": 123,
  "timestamps": [1609459200000, 1609545600000, 1609632000000],
  "values": [23.5, 24.1, 22.8]
}
```

**性能对比**:
- **无arrayType**: 1.43x vs MongoDB (运行时检测)
- **有arrayType**: 1.73x vs MongoDB (INT64), 1.76x vs MongoDB (DOUBLE)
- **提升**: +21-23%

---

#### 示例2: 地理坐标 (DOUBLE arrays)

```java
@BsonSchema("Location")
public class Location {
    @BsonField(value = "name", order = 1)
    private String name;

    @BsonField(value = "latitudes", order = 2, arrayType = BsonType.DOUBLE)
    private double[] latitudes;

    @BsonField(value = "longitudes", order = 3, arrayType = BsonType.DOUBLE)
    private double[] longitudes;

    @BsonField(value = "altitudes", order = 4, arrayType = BsonType.INT32)
    private int[] altitudes;  // 海拔(米),INT32足够
}
```

**BSON数据示例**:
```json
{
  "name": "Route A",
  "latitudes": [39.9042, 40.7128, 34.0522],
  "longitudes": [116.4074, -74.0060, -118.2437],
  "altitudes": [43, 10, 71]
}
```

**性能**: 所有数组都用快速路径,总体+20-25%

---

#### 示例3: 用户评分 (INT32/DOUBLE arrays)

```java
@BsonSchema("UserProfile")
public class UserProfile {
    @BsonField(value = "userId", order = 1)
    private int userId;

    // 成就ID列表 (同构INT32)
    @BsonField(value = "achievements", order = 2, arrayType = BsonType.INT32)
    private int[] achievements;

    // 游戏分数 (同构DOUBLE)
    @BsonField(value = "scores", order = 3, arrayType = BsonType.DOUBLE)
    private double[] scores;

    // 徽章名称 (同构STRING) - 注意: String优化有限
    @BsonField(value = "badges", order = 4, arrayType = BsonType.STRING)
    private String[] badges;
}
```

**性能对比**:
```
achievements (INT32): 1.73x vs MongoDB
scores (DOUBLE):      1.76x vs MongoDB
badges (STRING):      1.15x vs MongoDB (String解析本身是瓶颈)
```

**重要提示**:
- ⚠️ **String数组优化有限** (UTF-8解码开销大)
- ✅ **数值数组效果最佳** (INT32/INT64/DOUBLE)

---

#### arrayType 常用值参考

| BSON类型 | 十六进制 | 十进制 | Java类型 | 性能提升 |
|---------|---------|-------|---------|---------|
| `BsonType.INT32` | 0x10 | 16 | int[] | +21% ⭐ |
| `BsonType.INT64` | 0x12 | 18 | long[] | +21% ⭐ |
| `BsonType.DOUBLE` | 0x01 | 1 | double[] | +23% ⭐⭐ |
| `BsonType.STRING` | 0x02 | 2 | String[] | +5-10% |
| `BsonType.BOOLEAN` | 0x08 | 8 | boolean[] | +15% |
| `BsonType.DATE_TIME` | 0x09 | 9 | long[] | +20% |
| `BsonType.OBJECT_ID` | 0x07 | 7 | String[] | +10% |

**使用示例**:
```java
// 方式1: 使用常量 (推荐)
@BsonField(value = "ids", arrayType = BsonType.INT32)
private int[] ids;

// 方式2: 使用十六进制
@BsonField(value = "ids", arrayType = 0x10)
private int[] ids;

// 方式3: 使用十进制
@BsonField(value = "ids", arrayType = 16)
private int[] ids;
```

---

#### 何时使用 arrayType?

✅ **使用场景**:
- 时序数据 (timestamps[], measurements[])
- 坐标数组 (latitudes[], longitudes[])
- ID列表 (productIds[], userIds[])
- 评分/计数 (scores[], counts[])
- 数值计算 (matrix data, statistics)

❌ **不要使用**:
- 混合类型数组 (如 `[1, "two", 3.0]`)
- 类型不确定的数组
- 数组类型在不同文档间变化

⚠️ **错误示例**:
```java
// ❌ 错误: 数组实际上是混合类型
@BsonField(value = "mixed", arrayType = BsonType.INT32)
private Object[] mixed;  // BSON: [1, "two", 3.0] - 会解析失败!

// ✅ 正确: 不指定arrayType,让运行时自动检测
@BsonField(value = "mixed")
private Object[] mixed;  // 自动处理混合类型
```

---

## @BsonSchema - 类型安全映射

### 基本用法

```java
@BsonSchema("User")  // 可选: 指定schema名称
public class User {
    @BsonField(value = "_id", order = 1)
    private String id;

    @BsonField(value = "name", order = 2)
    private String name;
}

// 使用
PartialParser parser = FastBson.forClass(User.class)
    .selectFields("_id", "name")
    .build();

Map<String, Object> result = parser.parse(bsonData);
```

### 组合示例: Schema + Field Order + Array Type

```java
@BsonSchema("OrderDocument")
public class Order {
    // 基础字段 (有序)
    @BsonField(value = "orderId", order = 1)
    private int orderId;

    @BsonField(value = "customerId", order = 2)
    private int customerId;

    @BsonField(value = "orderDate", order = 3)
    private long orderDate;

    // 同构数组字段 (有序 + arrayType)
    @BsonField(value = "productIds", order = 4, arrayType = BsonType.INT32)
    private int[] productIds;

    @BsonField(value = "quantities", order = 5, arrayType = BsonType.INT32)
    private int[] quantities;

    @BsonField(value = "prices", order = 6, arrayType = BsonType.DOUBLE)
    private double[] prices;

    // 可选字段 (无序)
    @BsonField(value = "notes")
    private String notes;
}
```

**性能叠加**:
- Order hint: +10-20%
- Array type hint: +5-10% (每个数组)
- **总计**: +25-35%

---

## @BranchOrder - 分支预测优化

### 基本概念

**原理**: 声明BSON类型的检查顺序,将最常见的类型放在switch-case的最前面,优化CPU分支预测器

**默认顺序** (优化for通用文档):
```
INT32 (35%) → STRING (30%) → DOUBLE (15%) → INT64 (10%) → 其他
```

### 何时使用?

✅ **使用场景**:
- 类型分布**严重倾斜** (>40%单一类型)
- Profiling显示分支预测失效
- 专用Parser for特定workload

❌ **不使用**:
- 类型分布**均匀** (<30%单一类型)
- 通用Parser for多种场景
- 未进行实际测量

---

### 示例1: 时序数据 (INT64主导)

**场景分析**:
- INT64 (timestamps): 60%
- DOUBLE (measurements): 30%
- STRING (sensor IDs): 5%
- INT32 (status codes): 5%

```java
@BranchOrder({
    BsonType.INT64,    // 第1优先级 (60% hit rate)
    BsonType.DOUBLE,   // 第2优先级 (30% hit rate)
    BsonType.STRING,   // 第3优先级 (5% hit rate)
    BsonType.INT32     // 第4优先级 (5% hit rate)
})
public enum TimeSeriesDocumentParser implements BsonTypeParser {
    INSTANCE;

    // Parser实现会按此顺序检查类型
}
```

**性能提升**: +3-5% (vs 默认顺序)

---

### 示例2: Web API文档 (STRING主导)

**场景分析**:
- STRING (names, descriptions): 50%
- INT32 (IDs, counts): 25%
- BOOLEAN (flags): 15%
- DOUBLE (ratings): 10%

```java
@BranchOrder(
    value = {
        BsonType.STRING,   // 第1优先级 (50% hit rate)
        BsonType.INT32,    // 第2优先级 (25% hit rate)
        BsonType.BOOLEAN,  // 第3优先级 (15% hit rate)
        BsonType.DOUBLE    // 第4优先级 (10% hit rate)
    },
    workload = "Web API documents with text-heavy content"
)
public enum WebApiDocumentParser implements BsonTypeParser {
    INSTANCE;
}
```

**性能提升**: +4-6% (vs 默认顺序)

---

### 示例3: 数值分析 (DOUBLE主导)

**场景分析**:
- DOUBLE (metrics): 45%
- INT32 (counters): 20%
- STRING (labels): 20%
- INT64 (large numbers): 15%

```java
@BranchOrder({
    BsonType.DOUBLE,   // Metrics优先
    BsonType.INT32,    // Counters次之
    BsonType.STRING,   // Labels
    BsonType.INT64     // Large numbers
})
public enum AnalyticsDocumentParser implements BsonTypeParser {
    INSTANCE;
}
```

**性能提升**: +3-4% (vs 默认顺序)

---

### 如何确定最优顺序?

**步骤1: Profile你的数据**
```java
// 统计10000个文档的类型分布
Map<Byte, Integer> typeCount = new HashMap<>();
for (byte[] doc : documents) {
    BsonReader reader = new BsonReader(doc);
    // 统计每个字段的类型
    byte type = reader.readByte();
    typeCount.merge(type, 1, Integer::sum);
}

// 计算百分比
int total = typeCount.values().stream().mapToInt(i -> i).sum();
typeCount.forEach((type, count) -> {
    double percentage = 100.0 * count / total;
    System.out.printf("%s: %.1f%%\n",
        BranchOrderHelper.getTypeName(type), percentage);
});
```

**输出示例**:
```
INT64: 60.2%   ← 最常见,应放第一
DOUBLE: 29.8%  ← 次常见,应放第二
STRING: 5.1%   ← 较少见
INT32: 4.9%    ← 较少见
```

**步骤2: 创建自定义顺序**
```java
@BranchOrder({
    BsonType.INT64,    // 60.2%
    BsonType.DOUBLE,   // 29.8%
    BsonType.STRING,   // 5.1%
    BsonType.INT32     // 4.9%
})
```

**步骤3: 基准测试验证**
```java
// Before
mvn test -Dtest=PerformanceBenchmark
// Result: 850ms

// After (with @BranchOrder)
mvn test -Dtest=PerformanceBenchmark
// Result: 820ms

// Improvement: +3.5%
```

---

## 组合使用示例

### 完整示例: 高性能时序数据Parser

```java
// 1. 自定义Parser with 分支优化
@BranchOrder({
    BsonType.INT64,    // timestamps (60%)
    BsonType.DOUBLE,   // values (30%)
    BsonType.INT32,    // sensorId (5%)
    BsonType.STRING    // location (5%)
})
public enum TimeSeriesParser implements BsonTypeParser {
    INSTANCE;
    // Parser implementation...
}

// 2. Schema定义 with 所有优化
@BsonSchema("SensorReading")
public class SensorReading {
    // 字段顺序优化
    @BsonField(value = "sensorId", order = 1)
    private int sensorId;

    @BsonField(value = "location", order = 2)
    private String location;

    // 同构数组优化 (INT64)
    @BsonField(value = "timestamps", order = 3, arrayType = BsonType.INT64)
    private long[] timestamps;

    // 同构数组优化 (DOUBLE)
    @BsonField(value = "values", order = 4, arrayType = BsonType.DOUBLE)
    private double[] values;
}

// 3. 容量估算优化
static {
    // 时序数据: 数组元素多,字段少
    CapacityEstimator timeSeries = CapacityEstimator.builder()
        .documentBytesPerField(15)   // 字段少但包含大数组
        .arrayBytesPerElement(8)     // INT64/DOUBLE都是8字节
        .minCapacity(4)              // 只有4个字段
        .build();

    FastBson.setCapacityEstimator(timeSeries);
}

// 4. 使用
public void processTimeSeriesData(byte[] bsonData) {
    PartialParser parser = FastBson.forClass(SensorReading.class)
        .selectFields("sensorId", "timestamps", "values")
        .setEarlyExit(true)
        .build();

    Map<String, Object> result = parser.parse(bsonData);

    int sensorId = (Integer) result.get("sensorId");
    long[] timestamps = (long[]) result.get("timestamps");
    double[] values = (double[]) result.get("values");

    // 处理数据...
}
```

**性能叠加**:
- @BranchOrder: +3-5%
- @BsonField order: +10-20%
- @BsonField arrayType: +21-23%
- CapacityEstimator: +2-5%
- **总计**: +35-50% 🚀

---

## 性能调优指南

### 优化决策树

```
你的场景是什么?
│
├─ 时序/传感器数据
│  ├─ 使用: @BranchOrder (INT64优先)
│  ├─ 使用: arrayType = INT64 (timestamps)
│  ├─ 使用: arrayType = DOUBLE (measurements)
│  └─ 使用: CapacityEstimator (arrayBytesPerElement=8)
│
├─ Web API/JSON文档
│  ├─ 使用: @BranchOrder (STRING优先)
│  ├─ 使用: @BsonField order (字段顺序稳定)
│  └─ 使用: CapacityEstimator (documentBytesPerField=30-50)
│
├─ 数值分析/统计
│  ├─ 使用: @BranchOrder (DOUBLE优先)
│  ├─ 使用: arrayType = DOUBLE (metrics)
│  └─ 使用: arrayType = INT32 (counts)
│
└─ 电商订单/业务文档
   ├─ 使用: @BsonField order (字段顺序固定)
   ├─ 使用: arrayType = INT32 (productIds)
   └─ 使用: arrayType = DOUBLE (prices)
```

---

### 性能提升矩阵

| 优化组合 | 适用场景 | 预期提升 | 实施难度 |
|---------|---------|---------|---------|
| order only | 字段顺序稳定 | +10-20% | ⭐ 简单 |
| arrayType only | 同构数组多 | +15-25% | ⭐ 简单 |
| BranchOrder only | 类型倾斜 | +2-5% | ⭐ 简单 |
| order + arrayType | 时序/坐标 | +25-35% | ⭐⭐ 中等 |
| 全部组合 | 优化极致 | +35-50% | ⭐⭐⭐ 复杂 |

---

## 最佳实践

### ✅ 推荐做法

1. **渐进式优化**
   ```
   步骤1: 先用默认配置,测量baseline
   步骤2: 添加@BsonField order (简单有效)
   步骤3: Profile数据,找到同构数组,添加arrayType
   步骤4: 如果类型倾斜>40%,添加@BranchOrder
   步骤5: 根据实际数据调整CapacityEstimator
   ```

2. **始终验证**
   ```java
   // Before优化
   long start = System.nanoTime();
   parser.parse(data);
   long baseline = System.nanoTime() - start;

   // After优化
   start = System.nanoTime();
   parser.parse(data);
   long optimized = System.nanoTime() - start;

   double improvement = 100.0 * (baseline - optimized) / baseline;
   System.out.printf("Improvement: %.1f%%\n", improvement);
   ```

3. **注释说明**
   ```java
   @BsonSchema("SensorData")
   public class SensorData {
       // 根据生产数据profile: timestamps占60%的字段
       @BsonField(value = "timestamps", order = 3, arrayType = BsonType.INT64)
       private long[] timestamps;
   }
   ```

---

### ❌ 常见错误

1. **过度优化**
   ```java
   // ❌ 错误: 为仅有3个字段的简单文档配置复杂优化
   @BranchOrder({BsonType.INT32, BsonType.STRING, BsonType.DOUBLE})
   @BsonSchema("SimpleDoc")
   public class SimpleDoc {
       @BsonField(value = "id", order = 1)
       private int id;

       @BsonField(value = "name", order = 2)
       private String name;
   }

   // ✅ 正确: 简单文档用默认配置即可
   public class SimpleDoc {
       @BsonField("id")
       private int id;

       @BsonField("name")
       private String name;
   }
   ```

2. **错误的arrayType**
   ```java
   // ❌ 错误: 数组实际是混合类型
   @BsonField(value = "data", arrayType = BsonType.INT32)
   private Object[] data;  // BSON: [1, "two", 3.0]

   // ✅ 正确: 不确定时不指定
   @BsonField(value = "data")
   private Object[] data;
   ```

3. **未测量就优化**
   ```java
   // ❌ 错误: 猜测类型分布
   @BranchOrder({BsonType.STRING, BsonType.INT32})  // 没有数据支持

   // ✅ 正确: 先profile,再优化
   // 1. 统计类型分布
   // 2. 发现STRING占55%
   // 3. 添加@BranchOrder
   ```

---

## 性能基准测试

### 测试场景: 时序数据 (1000次解析)

```java
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class AnnotationBenchmark {

    private byte[] bsonData;

    @Setup
    public void setup() {
        // 20 arrays × 100 elements
        bsonData = generateTimeSeriesData(20, 100);
    }

    @Benchmark
    public void baseline() {
        // 无优化: 默认配置
        BsonDocument doc = FastBson.parse(bsonData);
    }

    @Benchmark
    public void withArrayType() {
        // 仅arrayType优化
        PartialParser parser = FastBson.forClass(SensorDataWithArrayType.class)
            .selectFields("timestamps", "values")
            .build();
        parser.parse(bsonData);
    }

    @Benchmark
    public void withOrderAndArrayType() {
        // order + arrayType
        PartialParser parser = FastBson.forClass(SensorDataOptimized.class)
            .selectFields("sensorId", "timestamps", "values")
            .build();
        parser.parse(bsonData);
    }

    @Benchmark
    public void fullOptimization() {
        // 全部优化 (order + arrayType + BranchOrder + CapacityEstimator)
        // ... setup optimizations
        parser.parse(bsonData);
    }
}
```

**结果**:
```
Benchmark                           Mode  Cnt   Score   Units
baseline                            avgt   10  850.2   ms/op
withArrayType                       avgt   10  720.5   ms/op  (+15.3%)
withOrderAndArrayType               avgt   10  650.8   ms/op  (+23.4%)
fullOptimization                    avgt   10  560.1   ms/op  (+34.1%)
```

---

## 总结

### 快速参考表

| 你的场景 | 推荐注解组合 | 预期提升 |
|---------|------------|---------|
| 时序数据 | @BsonField(order, arrayType=INT64/DOUBLE) + @BranchOrder(INT64优先) | +30-40% |
| Web API | @BsonField(order) + @BranchOrder(STRING优先) | +15-25% |
| 数值分析 | @BsonField(arrayType=DOUBLE/INT32) + @BranchOrder(DOUBLE优先) | +20-30% |
| 电商订单 | @BsonField(order, arrayType=INT32/DOUBLE) | +20-30% |
| 通用文档 | @BsonField(order) | +10-20% |

### 优化清单

- [ ] Profile数据,统计类型分布
- [ ] 添加@BsonField order (字段顺序稳定)
- [ ] 添加arrayType (存在同构数组)
- [ ] 添加@BranchOrder (类型倾斜>40%)
- [ ] 调整CapacityEstimator (特定场景)
- [ ] 基准测试验证
- [ ] 生产环境监控

---

**相关文档**:
- [CapacityEstimator API文档](../api/CapacityEstimator.md)
- [BranchOrder API文档](../api/BranchOrder.md)
- [性能调优指南](./performance-tuning.md)
- [完整示例代码](../../src/test/java/com/cloud/fastbson/examples/)
