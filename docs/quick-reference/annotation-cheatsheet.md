# FastBSON 注解速查表

---

## @BsonField - 字段映射与优化

### 语法
```java
@BsonField(value = "bsonName", order = 1, arrayType = BsonType.INT64)
```

### 参数速查

| 参数 | 说明 | 示例 | 默认值 |
|------|------|------|--------|
| `value` | BSON字段名 | `"_id"` | `""` (使用Java字段名) |
| `order` | 字段顺序提示 (优化) | `1, 2, 3...` | `-1` (无序) |
| `arrayType` | 同构数组类型提示 | `BsonType.INT64` | `0` (自动检测) |

---

### arrayType 常用值

```java
// 数值类型 (性能提升最佳: +20-23%)
BsonType.INT32    = 0x10  // int[]
BsonType.INT64    = 0x12  // long[]
BsonType.DOUBLE   = 0x01  // double[]

// 其他类型
BsonType.STRING   = 0x02  // String[] (提升有限: +5-10%)
BsonType.BOOLEAN  = 0x08  // boolean[]
BsonType.DATE_TIME= 0x09  // long[] (timestamps)
BsonType.OBJECT_ID= 0x07  // String[] (ObjectIds)
```

---

### 快速示例

```java
// 基本映射
@BsonField("_id")
private String id;

// 字段顺序优化
@BsonField(value = "name", order = 1)
private String name;

// 同构数组优化
@BsonField(value = "timestamps", arrayType = BsonType.INT64)
private long[] timestamps;

// 组合使用
@BsonField(value = "scores", order = 3, arrayType = BsonType.DOUBLE)
private double[] scores;
```

---

## @BsonSchema - 类型安全映射

### 语法
```java
@BsonSchema("SchemaName")  // 可选,默认使用类名
public class MyClass {
    @BsonField(value = "field", order = 1)
    private String field;
}
```

### 示例
```java
@BsonSchema("User")
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
```

---

## @BranchOrder - 分支预测优化

### 语法
```java
@BranchOrder({BsonType.INT64, BsonType.DOUBLE, BsonType.STRING})
public enum MyParser implements BsonTypeParser {
    INSTANCE;
}
```

### 默认顺序 (通用文档)
```
INT32 (35%) → STRING (30%) → DOUBLE (15%) → INT64 (10%)
```

---

### 场景速查

| 场景 | 类型分布 | 推荐顺序 |
|------|---------|---------|
| **时序数据** | INT64: 60%, DOUBLE: 30% | `{INT64, DOUBLE, STRING, INT32}` |
| **Web API** | STRING: 50%, INT32: 25% | `{STRING, INT32, BOOLEAN, DOUBLE}` |
| **数值分析** | DOUBLE: 45%, INT32: 20% | `{DOUBLE, INT32, STRING, INT64}` |

---

### 示例

```java
// 时序数据Parser
@BranchOrder({
    BsonType.INT64,    // timestamps (60%)
    BsonType.DOUBLE,   // values (30%)
    BsonType.STRING,   // IDs (5%)
    BsonType.INT32     // status (5%)
})
public enum TimeSeriesParser implements BsonTypeParser {
    INSTANCE;
}

// Web API Parser
@BranchOrder(
    value = {BsonType.STRING, BsonType.INT32, BsonType.BOOLEAN},
    workload = "Web API documents"
)
public enum WebApiParser implements BsonTypeParser {
    INSTANCE;
}
```

---

## 组合使用模板

### 模板1: 时序数据 (最佳性能: +35-50%)

```java
@BsonSchema("SensorData")
public class SensorData {
    @BsonField(value = "sensorId", order = 1)
    private int sensorId;

    @BsonField(value = "timestamps", order = 2, arrayType = BsonType.INT64)
    private long[] timestamps;

    @BsonField(value = "values", order = 3, arrayType = BsonType.DOUBLE)
    private double[] values;
}

// Parser优化
@BranchOrder({BsonType.INT64, BsonType.DOUBLE, BsonType.INT32})
public enum SensorParser implements BsonTypeParser { INSTANCE; }

// 容量估算优化
static {
    CapacityEstimator timeSeries = CapacityEstimator.builder()
        .documentBytesPerField(15)
        .arrayBytesPerElement(8)
        .build();
    FastBson.setCapacityEstimator(timeSeries);
}
```

---

### 模板2: Web API文档 (+20-30%)

```java
@BsonSchema("ApiResponse")
public class ApiResponse {
    @BsonField(value = "id", order = 1)
    private int id;

    @BsonField(value = "message", order = 2)
    private String message;

    @BsonField(value = "data", order = 3)
    private Object data;
}

@BranchOrder({BsonType.STRING, BsonType.INT32, BsonType.BOOLEAN})
public enum ApiParser implements BsonTypeParser { INSTANCE; }
```

---

### 模板3: 电商订单 (+25-35%)

```java
@BsonSchema("Order")
public class Order {
    @BsonField(value = "orderId", order = 1)
    private int orderId;

    @BsonField(value = "productIds", order = 2, arrayType = BsonType.INT32)
    private int[] productIds;

    @BsonField(value = "quantities", order = 3, arrayType = BsonType.INT32)
    private int[] quantities;

    @BsonField(value = "prices", order = 4, arrayType = BsonType.DOUBLE)
    private double[] prices;

    @BsonField(value = "total", order = 5)
    private double total;
}
```

---

## 性能提升速查

| 优化 | 适用场景 | 提升 | 难度 |
|------|---------|------|------|
| `order` | 字段顺序固定 | +10-20% | ⭐ |
| `arrayType` (INT32/INT64/DOUBLE) | 同构数值数组 | +20-23% | ⭐ |
| `arrayType` (STRING) | 同构字符串数组 | +5-10% | ⭐ |
| `@BranchOrder` | 类型倾斜>40% | +2-5% | ⭐ |
| **组合使用** | 时序/专用场景 | **+35-50%** | ⭐⭐ |

---

## 决策流程图

```
开始
  ↓
字段顺序是否稳定?
  ├─ 是 → 使用 @BsonField(order = ...)  [+10-20%]
  └─ 否 → 跳过
  ↓
是否有同构数组?
  ├─ 是 → 使用 arrayType = BsonType.XXX  [+15-25%]
  └─ 否 → 跳过
  ↓
单一类型占比 >40%?
  ├─ 是 → 使用 @BranchOrder({...})      [+2-5%]
  └─ 否 → 跳过
  ↓
特定文档结构?
  ├─ 是 → 调整 CapacityEstimator        [+2-5%]
  └─ 否 → 使用默认
  ↓
基准测试验证
  ↓
完成
```

---

## 常见错误速查

| 错误 | 后果 | 正确做法 |
|------|------|---------|
| 混合数组用arrayType | 解析失败 | 不指定或用runtime检测 |
| 过度优化简单文档 | 复杂度无收益 | 简单文档用默认配置 |
| 未验证就优化 | 无效优化 | Profile → 优化 → 验证 |
| arrayType用于String数组期待大幅提升 | 失望 | String数组优化有限(+5-10%) |

---

## 一行代码速查

```java
// 时序数据优化 (一行搞定)
@BsonField(value = "timestamps", order = 2, arrayType = BsonType.INT64)

// Web API优化 (Parser级别)
@BranchOrder({BsonType.STRING, BsonType.INT32, BsonType.BOOLEAN})

// 容量估算优化 (全局配置)
FastBson.setCapacityEstimator(CapacityEstimator.builder().arrayBytesPerElement(8).build());
```

---

## 验证命令速查

```bash
# 编译
mvn clean compile

# 运行测试
mvn test

# 运行基准测试
mvn test -Dtest=ExtendedBenchmarkValidationTest

# 检查覆盖率
mvn jacoco:report
```

---

## 获取帮助

- 📖 完整文档: [annotation-guide.md](../guides/annotation-guide.md)
- 💡 示例代码: [examples/](../../src/test/java/com/cloud/fastbson/examples/)
- 🔬 测试用例: [BranchOrderHelperTest.java](../../src/test/java/com/cloud/fastbson/util/BranchOrderHelperTest.java)
