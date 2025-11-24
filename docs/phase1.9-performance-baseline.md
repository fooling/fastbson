# Phase 1.9 性能基线报告

**生成时间**: 2025-11-24
**测试环境**: OpenJDK 21.0.8, Ubuntu 24.04 (WSL2)
**对比库**: MongoDB BSON 4.11.0

---

## 测试目的

为 Phase 2/3 的性能优化建立基线，通过多维度的 benchmark 场景全面评估 FastBSON 在不同工作负载下的性能表现。

---

## 测试场景总览

### 原有场景（Phase 1.8）

| 场景 | 字段数 | 文档大小 | FastBSON | MongoDB BSON | 性能提升 |
|------|--------|----------|----------|--------------|----------|
| 小文档 | 10 | ~500 bytes | - | - | ~3.5x |
| 中等文档 | 50 | ~2KB | 104 ms | 405 ms | **3.88x** |
| 大文档 | 100 | ~4KB | - | - | ~4.0x |

### 新增场景（Phase 1.9）

| 场景 | 描述 | 文档大小 | FastBSON | MongoDB BSON | 性能提升 |
|------|------|----------|----------|--------------|----------|
| String 密集型 | 80% String 字段，50 字段 | 2.3 KB | 11 ms | 24 ms | **2.17x** |
| 纯 String | 100% String 字段，50 字段 | 3.2 KB | 9 ms | 26 ms | **2.70x** |
| 数值密集型 | 100% Int32/Int64，50 字段 | 745 bytes | 16 ms | 46 ms | **2.75x** |
| 数组密集型 | 20 数组 × 100 元素 | 23.4 KB | 595 ms | 796 ms | **1.34x** |
| 100KB 文档 | 大型文档 | 100.3 KB | 10 ms | 27 ms | **2.56x** |
| 1MB 文档 | 超大文档 | 1.00 MB | 11 ms | 29 ms | **2.56x** |

---

## 详细测试结果

### 1. String 密集型场景 (80% String)

**测试配置**:
- 字段数: 50
- String 字段: 40 (80%)
- 其他字段: 10 (20% Int32)
- 平均字符串长度: ~40 字符

**性能数据** (1,000 次迭代):
- FastBSON: **11 ms**
- MongoDB BSON: **24 ms**
- **性能提升: 2.17x**

**分析**:
- String 密集型场景下 FastBSON 依然保持 2x+ 性能优势
- 主要优势来自零拷贝字符串解析和简化的 UTF-8 解码

---

### 2. 纯 String 场景 (100% String)

**测试配置**:
- 字段数: 50
- String 字段: 50 (100%)
- 平均字符串长度: ~50 字符

**性能数据** (1,000 次迭代):
- FastBSON: **9 ms**
- MongoDB BSON: **26 ms**
- **性能提升: 2.70x**

**分析**:
- 100% String 场景性能提升至 2.70x
- 说明 FastBSON 的字符串解析路径高度优化
- 避免了 MongoDB BSON 的 BsonString 对象创建开销

---

### 3. 数值密集型场景 (100% Numeric)

**测试配置**:
- 字段数: 50
- Int32 字段: 25 (50%)
- Int64 字段: 25 (50%)

**性能数据** (1,000 次迭代):
- FastBSON: **16 ms**
- MongoDB BSON: **46 ms**
- **性能提升: 2.75x**

**分析**:
- 数值解析性能提升接近 3x
- FastBSON 使用位运算直接解析，MongoDB 需要创建 BsonInt32/BsonInt64 包装对象
- 数值类型解析是 FastBSON 最强的场景之一

---

### 4. 数组密集型场景 (Array Heavy)

**测试配置**:
- 数组数量: 20
- 每个数组元素数: 100
- 数组元素类型: 混合 (Int32, String, Double)

**性能数据** (1,000 次迭代):
- FastBSON: **595 ms**
- MongoDB BSON: **796 ms**
- **性能提升: 1.34x**

**分析**:
- ⚠️ 这是所有场景中性能提升最小的 (1.34x)
- **原因**: 数组解析涉及大量递归调用和集合操作
- **优化空间**: Phase 3 可针对数组场景优化（预分配 ArrayList 容量、减少递归深度）

---

### 5. 100KB 文档

**测试配置**:
- 文档大小: 100.3 KB
- 字段类型: 混合 (String + Int32)
- 大部分字段为长字符串

**性能数据** (100 次迭代):
- FastBSON: **10 ms**
- MongoDB BSON: **27 ms**
- **性能提升: 2.56x**

**分析**:
- 大文档场景保持 2.5x+ 性能优势
- 零拷贝设计在大文档下优势更明显
- 内存分配开销对比更加突出

---

### 6. 1MB 文档

**测试配置**:
- 文档大小: 1.00 MB
- 字段类型: 混合 (String + Int32)
- 超大文档场景

**性能数据** (10 次迭代):
- FastBSON: **11 ms**
- MongoDB BSON: **29 ms**
- **性能提升: 2.56x**

**分析**:
- 超大文档 (1MB) 性能提升保持在 2.56x
- 说明 FastBSON 性能随文档大小线性扩展
- 没有因为文档增大而出现性能退化

---

## 性能总结

### 各场景性能排名

| 排名 | 场景 | 性能提升 | 备注 |
|------|------|----------|------|
| 1 | 中等文档 (混合类型) | **3.88x** | ✅ 最佳场景 |
| 2 | 数值密集型 | **2.75x** | ✅ 第二强场景 |
| 3 | 纯 String | **2.70x** | ✅ String 解析高效 |
| 4 | 大文档 (100KB/1MB) | **2.56x** | ✅ 大文档保持优势 |
| 5 | String 密集型 (80%) | **2.17x** | ✅ 稳定性能 |
| 6 | 数组密集型 | **1.34x** | ⚠️ 需要优化 |

### 关键发现

1. **混合类型场景最优**: 3.88x (Phase 1.8 中等文档)
2. **数值解析强项**: 2.75x，位运算优势明显
3. **String 解析高效**: 2.70x，零拷贝字符串处理
4. **大文档线性扩展**: 100KB 和 1MB 保持 2.56x，无性能退化
5. **数组场景待优化**: 1.34x，是 Phase 3 的优化重点

---

## 性能优化建议（Phase 3）

### 高优先级

1. **数组解析优化**
   - 预分配 ArrayList 容量（根据数组长度）
   - 减少递归深度，考虑迭代解析
   - 优化数组索引处理（"0", "1", "2" 字符串比较）

2. **字段名内部化 (String Interning)**
   - 使用 ConcurrentHashMap 缓存常见字段名
   - 避免重复创建相同的字段名字符串
   - 预期提升: 5-10%

3. **ThreadLocal 对象池**
   - BsonReader 对象复用
   - StringBuilder 对象池
   - 预期提升: 3-5%

### 中优先级

4. **有序匹配优化**
   - 假定字段顺序稳定，优先检查预期位置
   - 适用于 Phase 2 部分字段解析场景

5. **常见类型优先级调整**
   - 将 String, Int32, Int64 放在 switch 最前面
   - 优化 CPU 分支预测

---

## 基线用途

此报告将作为 Phase 2/3 性能对比的基线：

1. **Phase 2**: 部分字段解析性能对比
   - 预期: 部分解析（5/50 字段）提升至 **8-10x**
   - 提前退出机制预期提升至 **15-20x**

2. **Phase 3**: 性能优化效果验证
   - 对象池优化预期: +3-5%
   - 字段名内部化预期: +5-10%
   - 数组优化预期: 1.34x → 2.0x+

---

## 测试数据详细信息

### 测试环境

```
Java版本: OpenJDK 21.0.8
操作系统: Ubuntu 24.04 (WSL2)
CPU: [未指定]
内存: [未指定]
FastBSON版本: 1.0.0-SNAPSHOT
MongoDB BSON版本: 4.11.0
```

### 测试方法

- 预热: 避免 JIT 编译影响
- 迭代次数: 根据文档大小调整（1000/100/10 次）
- 时间测量: System.nanoTime()
- 测试框架: JUnit 5

---

## 附录：完整测试日志

```
String Heavy Document size: 2357 bytes
  FastBSON: 11 ms (1000 iterations)
  MongoDB:  24 ms (1000 iterations)
  Speedup:  2.17x

Pure String Document size: 3285 bytes
  FastBSON: 9 ms (1000 iterations)
  MongoDB:  26 ms (1000 iterations)
  Speedup:  2.70x

Numeric Heavy Document size: 745 bytes
  FastBSON: 16 ms (1000 iterations)
  MongoDB:  46 ms (1000 iterations)
  Speedup:  2.75x

Array Heavy Document size: 23935 bytes
  FastBSON: 595 ms (1000 iterations)
  MongoDB:  796 ms (1000 iterations)
  Speedup:  1.34x

100KB Document size: 102697 bytes (100.3 KB)
  FastBSON: 10 ms (100 iterations)
  MongoDB:  27 ms (100 iterations)
  Speedup:  2.56x

1MB Document size: 1049637 bytes (1.00 MB)
  FastBSON: 11 ms (10 iterations)
  MongoDB:  29 ms (10 iterations)
  Speedup:  2.56x
```

---

*生成日期: 2025-11-24*
*文档版本: 1.0*
