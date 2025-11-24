# FastBSON 开发规范

本文档定义 FastBSON 项目的核心开发规范。

---

## 1. 核心设计原则

### 1.1 SOLID 原则（强制遵守）

**单一职责原则（SRP）- 最小功能模块**

每个类只负责一个明确的、最小的功能单元：

```java
// ✅ 正确：职责单一，最小化
public class BsonReader {
    // 仅负责字节读取
    public int readInt32() { }
    public String readCString() { }
    public void skip(int bytes) { }
}

public class ValueSkipper {
    // 仅负责值跳过
    public void skipValue(BsonReader reader, byte type) { }
}

public class TypeHandler {
    // 仅负责类型解析
    public Object parseValue(BsonReader reader, byte type) { }
}

// ❌ 错误：职责混杂，功能臃肿
public class BsonProcessor {
    public int readInt32() { }          // 读取
    public void skipValue() { }         // 跳过
    public Object parseValue() { }      // 解析
    public Map<String, Object> parse() { } // 协调
    // 违反单一职责！应拆分为 4 个类
}
```

**拆分原则：**
- 每个类的公共方法数 ≤ 10 个
- 每个方法的代码行数 ≤ 30 行
- 类的职责用一句话能说清楚

**开闭原则（OCP）**

通过接口扩展，不修改现有代码：

```java
// ✅ 扩展新类型只需添加新实现
public interface TypeParser {
    Object parse(BsonReader reader);
}

public class Int32Parser implements TypeParser { }
public class StringParser implements TypeParser { }
```

**依赖倒置原则（DIP）**

依赖抽象，不依赖具体实现：

```java
// ✅ 依赖接口
public class PartialParser {
    private final FieldMatcher matcher;  // 接口或抽象类
}
```

### 1.2 最小功能模块原则

**模块拆分示例：**

```
解析流程 → 拆分为独立模块
├── BsonReader      (读取字节)
├── FieldMatcher    (匹配字段)
├── ValueSkipper    (跳过值)
├── TypeHandler     (解析类型)
├── PartialParser   (协调流程)
└── ObjectPool      (对象管理)

每个模块功能最小化、职责单一
```

---

## 2. 技术约束

### 2.1 Java 8 语法（强制）

```java
// ✅ 允许
List<String> list = new ArrayList<>();
map.forEach((String k, Object v) -> process(k, v));  // 显式类型

// ❌ 禁止（Java 9+）
var result = parser.parse(data);           // var 关键字
List<String> list = List.of("a", "b");    // List.of()
Optional.ifPresentOrElse(...)             // Java 9 API
```

### 2.2 包命名规范

```
com.cloud                 // 顶层包
com.cloud.fastbson        // FastBSON 根包
com.cloud.fastbson.reader      // 读取器
com.cloud.fastbson.parser      // 解析器
com.cloud.fastbson.matcher     // 匹配器
com.cloud.fastbson.skipper     // 跳过器
com.cloud.fastbson.handler     // 处理器
com.cloud.fastbson.util        // 工具类
com.cloud.fastbson.exception   // 异常类
```

### 2.3 Lombok 使用

```java
@Data                    // 实体类
@Builder                 // 构建器
@Slf4j                   // 日志
@NonNull                 // 非空检查
@Cleanup                 // 资源清理
```

---

## 3. 命名规范

### 3.1 类命名

```java
BsonReader              // 核心组件
TypeHandler             // 处理器 (Handler/Parser/Processor)
BsonUtils               // 工具类 (Utils/Helper)
BsonType                // 常量类 (Constants/Type)
BsonReaderTest          // 测试类 (ClassName + Test)
```

### 3.2 方法命名

```java
readInt32()             // 读取：read + 类型
skipValue()             // 跳过：skip + 对象
parseValue()            // 解析：parse + 对象
matches()               // 匹配：match/matches
getInt()                // 获取：get + 属性
isEmpty()               // 判断：is/has + 属性
```

### 3.3 变量命名

```java
int docLength;                          // 局部变量：camelCase
private static final int MAX_SIZE = 8192; // 常量：UPPER_SNAKE_CASE
private byte[] buffer;                  // 成员变量：camelCase
```

---

## 4. 测试规范

### 4.1 覆盖率要求（强制）

- **分支覆盖率：100%**
- 行覆盖率：≥ 95%
- 所有公共方法必须有测试

### 4.2 测试命名

```java
public class BsonReaderTest {
    @Test
    public void testReadInt32_ValidData() { }

    @Test(expected = BufferUnderflowException.class)
    public void testReadInt32_BufferUnderflow() { }
}
```

### 4.3 测试结构（AAA 模式）

```java
@Test
public void testParse_PartialFields() {
    // Arrange - 准备
    byte[] bsonData = createTestData();
    FastBsonParser parser = FastBsonParser.builder()
        .fields(new String[]{"name", "age"})
        .build();

    // Act - 执行
    Map<String, Object> result = parser.parse(bsonData);

    // Assert - 验证
    assertEquals(2, result.size());
    assertEquals("John", result.get("name"));
}
```

---

## 5. 性能规范

### 5.1 对象复用

```java
// ✅ ThreadLocal 对象池
private static final ThreadLocal<BsonReader> READER_POOL =
    ThreadLocal.withInitial(() -> new BsonReader(new byte[0]));

public static BsonReader getReader(byte[] data) {
    BsonReader reader = READER_POOL.get();
    reader.reset(data);
    return reader;
}
```

### 5.2 字符串内部化

```java
// ✅ 字段名内部化，使用 == 比较
private static final ConcurrentHashMap<String, String> FIELD_POOL =
    new ConcurrentHashMap<>();

public String internField(String name) {
    return FIELD_POOL.computeIfAbsent(name, (String k) -> k);
}
```

### 5.3 避免数组拷贝

```java
// ✅ 直接操作原数组
public int readInt32() {
    int value = (buffer[position] & 0xFF) |
                ((buffer[position + 1] & 0xFF) << 8) |
                ((buffer[position + 2] & 0xFF) << 16) |
                ((buffer[position + 3] & 0xFF) << 24);
    position += 4;
    return value;
}
```

---

## 6. 代码格式

- **缩进**：4 空格（不用 Tab）
- **行长度**：≤ 120 字符
- **大括号**：K&R 风格（左括号不换行）

```java
public void method() {
    if (condition) {
        doSomething();
    } else {
        doOther();
    }
}
```

---

## 7. 异常处理

### 7.1 自定义异常

```java
// 基础异常
public class BsonException extends RuntimeException {
    public BsonException(String message) {
        super(message);
    }
}

// 具体异常
public class InvalidBsonTypeException extends BsonException {
    public InvalidBsonTypeException(byte type) {
        super("Invalid BSON type: 0x" + Integer.toHexString(type & 0xFF));
    }
}
```

### 7.2 异常抛出

```java
// ✅ 提供详细上下文
if (bsonData == null) {
    throw new IllegalArgumentException("BSON data cannot be null");
}

// ✅ 包装异常
catch (IOException e) {
    throw new BsonException("Failed to read: " + path, e);
}

// ❌ 禁止吞掉异常
catch (Exception e) {
    // 什么都不做 - 绝对禁止！
}
```

---

## 8. 文档规范

### 8.1 JavaDoc

公共 API 必须有 JavaDoc：

```java
/**
 * 解析 BSON 字节数组，提取指定字段。
 *
 * @param bsonData BSON 字节数组，不能为 null
 * @return 字段名到值的映射
 * @throws BsonParseException 如果 BSON 格式无效
 */
public Map<String, Object> parse(byte[] bsonData) {
    // ...
}
```

### 8.2 注释

```java
// ✅ 解释"为什么"
// 使用 ThreadLocal 避免频繁创建对象，提升性能
private static final ThreadLocal<BsonReader> READER_POOL = ...;

// ✅ 解释复杂逻辑
// 跳过已读取的 4 字节长度字段
reader.skip(docLength - 4);

// ❌ 重复代码内容
// 读取 int32
int value = readInt32();  // 无用注释
```

---

## 9. Git 提交规范

### 9.1 Commit Message

```
<type>(<scope>): <subject>

feat(parser): 添加部分字段解析功能
fix(reader): 修复边界检查问题
perf(matcher): 优化字段匹配性能
test(skipper): 添加跳过逻辑测试
docs(readme): 更新使用示例
```

**Type 类型：**
- `feat`: 新功能
- `fix`: Bug 修复
- `perf`: 性能优化
- `test`: 测试
- `docs`: 文档
- `refactor`: 重构
- `chore`: 构建/工具

### 9.2 分支规范

```
main              # 主分支
develop           # 开发分支
feature/xxx       # 功能分支
fix/xxx           # 修复分支
perf/xxx          # 性能优化分支
```

---

## 10. Code Review 检查清单

### 功能检查
- [ ] 功能正确，边界情况已处理
- [ ] 异常路径已处理

### 代码质量
- [ ] 符合 SOLID 原则，每个类职责单一
- [ ] 方法长度 ≤ 30 行
- [ ] 无重复代码

### 性能
- [ ] 无不必要的对象创建
- [ ] 无不必要的数组拷贝

### 测试
- [ ] 分支覆盖率 100%
- [ ] 测试命名清晰

### Java 8
- [ ] 无 Java 9+ 特性
- [ ] Lambda 使用显式类型

---

## 11. 开发环境

### 必需工具
- JDK 8
- Maven 3.6+
- Git
- IDE（推荐 IntelliJ IDEA）

### IDE 配置
- 安装 Lombok 插件
- 启用注解处理
- Java SDK 设置为 1.8
- 代码格式：4 空格缩进

---

## 快速检查清单

**提交前必查：**
- [ ] Java 8 语法（无 var、无 List.of()）
- [ ] 分支覆盖率 100%
- [ ] 所有测试通过（`mvn test`）
- [ ] 类职责单一（SOLID）
- [ ] 公共 API 有 JavaDoc
- [ ] Commit message 符合规范

---

*最后更新: 2024-11*
