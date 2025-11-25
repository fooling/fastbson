# FastBSON Progressive Optimization Plan
## From Typed Parsers to True Zero-Copy & Zero-Boxing

## Current State (Phase 2.10)

### Architecture Overview
```
TypeHandler (Strategy Pattern with Lookup Table)
  └── BsonTypeParser interface
       ├── Int32Parser.INSTANCE
       ├── Int64Parser.INSTANCE
       ├── DoubleParser.INSTANCE
       ├── StringParser.INSTANCE
       ├── DocumentParser.INSTANCE
       ├── ArrayParser.INSTANCE
       └── ... (17 total parsers)
```

### Achievements So Far
- ✅ **Phase 2.9**: Extracted simple type parsers (Int32, String, Boolean, etc.)
- ✅ **Phase 2.10**: Extracted medium complexity parsers (DateTime, ObjectId, Binary, etc.)
- ✅ **Phase 2.13**: Zero-boxing architecture (FastBsonDocument with fastutil)

### Current Performance Problem
| Implementation | Performance | Why Slow? |
|---------------|-------------|-----------|
| **Phase 1 (HashMap)** | 99ms (4.32x) | Simple, direct, JVM-optimized |
| **Phase 2.14 (FastBsonDocument)** | 316ms (1.30x) | Builder overhead, multiple collections, double indirection |

## Root Cause Analysis

### What Went Wrong in Phase 2.13?

**Problem 1: Builder Pattern Overhead**
```java
// Current: Multiple method calls per field
builder.putInt32(fieldName, value);    // Method call #1
  → getOrCreateFieldId(fieldName)      // Hash lookup #1
  → intFields.put(fieldId, value)      // Hash lookup #2
  → fieldTypes.put(fieldId, type)      // Hash lookup #3
```

**Problem 2: Type-Specific Collections**
```java
// Current: 10+ separate collections
Int2IntMap intFields;
Int2LongMap longFields;
Int2DoubleMap doubleFields;
Object2ObjectMap<Integer, String> stringFields;
// ... 7 more collections
```
- Each collection has overhead (array, load factor, resize)
- Poor cache locality (fields scattered across memory)
- Double indirection (fieldName → fieldId → value)

**Problem 3: Not Actually Zero-Copy**
```java
// Still allocates:
- HashMap/FastBsonDocument object
- Builder object
- All wrapper objects
- All String objects (field names and values)
```

## Optimization Strategy: Progressive Refactoring

### Philosophy
> **"Don't rewrite everything. Optimize iteratively with measurements."**

### Approach: Hybrid Optimization
1. **Keep type separation** (good for maintainability)
2. **Add zero-copy mode** (opt-in for performance)
3. **Measure every step** (no guessing)

## Phase 2.15: Parser Interface Enhancement

### Goal
Add zero-copy parse methods to parser interface without breaking existing code.

### Changes

#### Step 1: Enhance BsonTypeParser Interface
```java
public interface BsonTypeParser {
    /**
     * Legacy method: Parse to Object (with boxing).
     * Used by HashMap-based parsing.
     */
    Object parse(BsonReader reader);

    /**
     * New method: Parse to pre-allocated buffer (zero-copy).
     * Returns value size in bytes.
     *
     * @param data Source BSON data
     * @param offset Value start offset
     * @param dest Destination buffer
     * @param destOffset Destination offset
     * @return Number of bytes written
     */
    default int parseZeroCopy(byte[] data, int offset, byte[] dest, int destOffset) {
        throw new UnsupportedOperationException("Zero-copy not implemented");
    }

    /**
     * New method: Get value size without parsing.
     * Used for skip operations and index building.
     *
     * @param data Source BSON data
     * @param offset Value start offset
     * @return Value size in bytes
     */
    default int getValueSize(byte[] data, int offset) {
        throw new UnsupportedOperationException("getValueSize not implemented");
    }
}
```

#### Step 2: Implement Zero-Copy for Simple Types

**Int32Parser (Zero-Copy)**
```java
public enum Int32Parser implements BsonTypeParser {
    INSTANCE;

    @Override
    public Object parse(BsonReader reader) {
        return reader.readInt32();  // Existing: boxing
    }

    @Override
    public int getValueSize(byte[] data, int offset) {
        return 4;  // Int32 is always 4 bytes
    }

    @Override
    public int parseZeroCopy(byte[] data, int offset, byte[] dest, int destOffset) {
        // Direct memory copy (4 bytes)
        System.arraycopy(data, offset, dest, destOffset, 4);
        return 4;
    }

    /**
     * NEW: Direct read from byte array without BsonReader.
     * Zero allocation, zero copy.
     */
    public static int readDirect(byte[] data, int offset) {
        return (data[offset] & 0xFF)
            | ((data[offset + 1] & 0xFF) << 8)
            | ((data[offset + 2] & 0xFF) << 16)
            | ((data[offset + 3] & 0xFF) << 24);
    }
}
```

**StringParser (Zero-Copy with Substring View)**
```java
public enum StringParser implements BsonTypeParser {
    INSTANCE;

    @Override
    public Object parse(BsonReader reader) {
        return reader.readString();  // Existing: allocates String
    }

    @Override
    public int getValueSize(byte[] data, int offset) {
        int length = Int32Parser.readDirect(data, offset);
        return 4 + length;  // int32 length + UTF-8 bytes + null terminator
    }

    /**
     * NEW: Return string metadata instead of materialized String.
     * Actual String is created lazily on first use.
     */
    public static StringView readView(byte[] data, int offset) {
        int length = Int32Parser.readDirect(data, offset);
        return new StringView(data, offset + 4, length - 1);  // Exclude null terminator
    }

    /**
     * Lightweight string view (16 bytes vs ~40 bytes for String object).
     */
    public static class StringView {
        final byte[] data;
        final int offset;
        final int length;
        String materialized;  // Lazy cache

        StringView(byte[] data, int offset, int length) {
            this.data = data;
            this.offset = offset;
            this.length = length;
        }

        public String materialize() {
            if (materialized == null) {
                materialized = new String(data, offset, length, UTF_8);
            }
            return materialized;
        }

        @Override
        public String toString() {
            return materialize();
        }
    }
}
```

**DocumentParser (Zero-Copy Child View)**
```java
public enum DocumentParser implements BsonTypeParser {
    INSTANCE;

    @Override
    public Object parse(BsonReader reader) {
        // Existing: creates FastBsonDocument
        BsonDocumentBuilder builder = factory.newDocumentBuilder();
        // ... builder pattern
        return builder.build();
    }

    @Override
    public int getValueSize(byte[] data, int offset) {
        return Int32Parser.readDirect(data, offset);  // Document length
    }

    /**
     * NEW: Create zero-copy view of nested document.
     * No parsing, just wrap byte slice.
     */
    public static BsonView readView(byte[] data, int offset) {
        int length = Int32Parser.readDirect(data, offset);
        return BsonView.parse(data, offset, length);  // Lazy index building
    }
}
```

### Migration Path
```java
// Phase 1: Add new methods (backward compatible)
interface BsonTypeParser {
    Object parse(BsonReader reader);           // Existing
    int getValueSize(byte[] data, int offset); // NEW
    int parseZeroCopy(...);                    // NEW
}

// Phase 2: Implement for each parser (gradual)
Int32Parser.INSTANCE.readDirect(data, offset);
StringParser.INSTANCE.readView(data, offset);
DocumentParser.INSTANCE.readView(data, offset);

// Phase 3: Use in BsonView (zero-copy mode)
BsonView doc = BsonView.parse(data);
int value = doc.getInt32("field");  // Uses Int32Parser.readDirect()

// Phase 4: Use in HashMap mode (legacy mode)
Map<String, Object> map = FastBson.parseToMap(data);  // Uses parse() methods
```

## Phase 2.16: Index-Based Document

### Goal
Create a hybrid document that combines HashMap simplicity with zero-copy benefits.

### Design: IndexedBsonDocument

```java
/**
 * Hybrid document: HashMap-like API with zero-copy storage.
 *
 * <p>Combines best of both worlds:
 * <ul>
 *   <li>Simple HashMap-like API (no Builder pattern)</li>
 *   <li>Zero-copy storage (byte[] + field index)</li>
 *   <li>Lazy parsing (parse on access)</li>
 *   <li>Smart caching (auto-boxing cache, string interning)</li>
 * </ul>
 */
public class IndexedBsonDocument implements BsonDocument {
    // Phase 1: Original data (zero-copy)
    private final byte[] data;
    private final int offset;
    private final int length;

    // Phase 2: Field index (built once during parse)
    private final FieldIndex[] fields;  // Sorted by name hash

    // Phase 3: Value cache (lazy, sparse)
    private Object[] cache;  // null until accessed

    // Phase 4: Field name cache (interned strings)
    private String[] nameCache;  // null until accessed

    static class FieldIndex {
        int nameOffset;
        int nameLength;
        int nameHash;
        int valueOffset;
        int valueSize;     // Pre-computed for fast skip
        byte type;
    }

    /**
     * Parse: O(n) to build field index, no value parsing.
     */
    public static IndexedBsonDocument parse(byte[] data) {
        FieldIndex[] fields = buildIndex(data);
        return new IndexedBsonDocument(data, 0, data.length, fields);
    }

    /**
     * Access: O(log n) binary search + lazy parse + cache.
     */
    public int getInt32(String fieldName) {
        int index = findField(fieldName);  // Binary search on hash
        if (cache != null && cache[index] != null) {
            return (Integer) cache[index];  // Cache hit
        }
        // Parse on demand using zero-copy parser
        int value = Int32Parser.readDirect(data, fields[index].valueOffset);
        cacheValue(index, value);  // Auto-boxing (JVM cache works!)
        return value;
    }

    /**
     * Skip: O(1) using pre-computed value size.
     */
    public void skip(int fieldIndex) {
        // No parsing needed, just use pre-computed offset
        int nextOffset = fields[fieldIndex].valueOffset + fields[fieldIndex].valueSize;
    }
}
```

### Performance Characteristics

| Operation | HashMap | FastBsonDocument | IndexedBsonDocument |
|-----------|---------|------------------|---------------------|
| **Parse** | 99ms | 316ms | **30ms** (index only) |
| **getInt32()**  | O(1) hash | O(1) cache / O(log n) map lookup | O(log n) binary search |
| **Memory** | ~50n bytes | ~200n bytes | **~30n bytes** |
| **Cache locality** | ✅ Good | ❌ Poor (scattered) | ✅ Good (contiguous) |

## Phase 2.17: Full Zero-Copy Type Parsers

### Goal
Convert all 17 type parsers to zero-copy implementations.

### Priority Order

**Tier 1: Hot path types (80% of fields)**
1. ✅ Int32Parser - Direct 4-byte read
2. ✅ StringParser - StringView with lazy materialization
3. ✅ DoubleParser - Direct 8-byte read + IEEE 754 conversion
4. ✅ BooleanParser - Direct 1-byte read
5. ✅ Int64Parser - Direct 8-byte read

**Tier 2: Common types (15% of fields)**
6. ✅ DocumentParser - BsonView wrapper (recursive zero-copy)
7. ✅ ArrayParser - BsonArrayView wrapper
8. ⏳ ObjectIdParser - Direct 12-byte read
9. ⏳ DateTimeParser - Direct 8-byte read (milliseconds since epoch)
10. ⏳ BinaryParser - Binary view (offset + length + subtype)

**Tier 3: Rare types (5% of fields)**
11. ⏳ RegexParser - Two C-string views
12. ⏳ TimestampParser - Direct 8-byte read
13. ⏳ Decimal128Parser - Direct 16-byte read
14. ⏳ DBPointerParser - String view + 12-byte ObjectId
15. ⏳ JavaScriptWithScopeParser - String view + document view
16. ⏳ MinKeyParser / MaxKeyParser - Zero bytes (marker only)
17. ⏳ NullParser - Zero bytes (marker only)

### Implementation Template

```java
public enum XxxParser implements BsonTypeParser {
    INSTANCE;

    // ===== LEGACY API (for HashMap mode) =====
    @Override
    public Object parse(BsonReader reader) {
        // Existing implementation (boxing)
        return reader.readXxx();
    }

    // ===== ZERO-COPY API =====
    @Override
    public int getValueSize(byte[] data, int offset) {
        // Fast size computation (no parsing)
        return computeSize(data, offset);
    }

    /**
     * Direct read from byte array (zero-copy, zero-boxing).
     * Returns primitive or lightweight view object.
     */
    public static XxxType readDirect(byte[] data, int offset) {
        // Read directly from bytes
        return ...;
    }

    /**
     * Returns view object for lazy materialization.
     * View object is lightweight (<= 24 bytes).
     */
    public static XxxView readView(byte[] data, int offset) {
        return new XxxView(data, offset, ...);
    }

    // ===== VIEW CLASS (for complex types) =====
    public static class XxxView {
        final byte[] data;
        final int offset;
        final int length;
        XxxType materialized;  // Lazy cache

        public XxxType materialize() {
            if (materialized == null) {
                materialized = readDirect(data, offset);
            }
            return materialized;
        }
    }
}
```

## Phase 2.18: Optimized Field Lookup

### Current Problem: String Comparison is Slow

```java
// Current: O(n) string comparison in hash collision resolution
private int findField(String fieldName) {
    int hash = fieldName.hashCode();
    // Binary search on hash: O(log n) - GOOD
    int mid = binarySearch(fields, hash);
    // String comparison for verification: O(n) - BAD!
    if (matchesFieldName(mid, fieldName)) {
        return mid;
    }
}
```

### Optimization: Trie-Based Fast Path

```java
/**
 * Two-level lookup:
 * 1. Trie for common field names (90% hit rate)
 * 2. Binary search for uncommon names (10% hit rate)
 */
class FieldLookup {
    // Level 1: Trie for common names (compile-time known)
    private static final TrieNode COMMON_FIELDS = buildCommonFieldTrie(
        "_id", "name", "email", "createdAt", "updatedAt",
        "userId", "status", "type", "data", "value"
    );

    // Level 2: Runtime field index (sorted by hash)
    private final FieldIndex[] fields;

    public int findField(String fieldName) {
        // Fast path: Trie lookup O(k) where k = field name length
        int trieIndex = COMMON_FIELDS.find(fieldName);
        if (trieIndex >= 0) {
            return trieIndex;  // ~90% hit rate, 2-3ns
        }

        // Slow path: Binary search O(log n)
        return binarySearchByHash(fieldName);  // ~10% hit rate, 20-30ns
    }
}

/**
 * Compact trie node (cache-friendly).
 */
static class TrieNode {
    byte[] chars;     // Sorted child characters
    short[] indices;  // Field indices
    TrieNode[] children;  // Child nodes

    int find(String fieldName) {
        TrieNode node = this;
        for (int i = 0; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            int childIndex = Arrays.binarySearch(node.chars, (byte) c);
            if (childIndex < 0) return -1;
            node = node.children[childIndex];
        }
        return node.indices[0];  // Leaf node
    }
}
```

## Phase 2.19: JVM Optimization Tuning

### Goal
Leverage JVM-specific optimizations for maximum performance.

### Optimization 1: Method Inlining

```java
// Make critical methods eligible for inlining (< 35 bytecode instructions)

// BEFORE: Too large to inline (50+ bytecode instructions)
public int getInt32(String fieldName) {
    int index = findField(fieldName);  // 20 instructions
    if (index < 0) throw new RuntimeException();  // 10 instructions
    if (cache != null && cache[index] != null) {  // 5 instructions
        return (Integer) cache[index];  // 5 instructions
    }
    int value = parseValue(index);  // 15 instructions
    cacheValue(index, value);  // 5 instructions
    return value;
}

// AFTER: Split into inlineable methods
public int getInt32(String fieldName) {
    int index = findFieldFast(fieldName);  // 15 instructions (inline candidate)
    return getInt32AtIndex(index);  // 20 instructions (inline candidate)
}

private int getInt32AtIndex(int index) {
    Object cached = getCached(index);  // 8 instructions (inline candidate)
    if (cached != null) return (Integer) cached;
    return parseAndCacheInt32(index);  // 12 instructions (inline candidate)
}
```

### Optimization 2: Escape Analysis Friendly

```java
// Ensure boxed values don't escape (enable scalar replacement)

// BEFORE: Integer escapes to cache array
public int getInt32(String fieldName) {
    Integer cached = (Integer) cache[index];  // Box escapes
    if (cached != null) return cached;
    int value = parse();
    cache[index] = value;  // Box escapes
    return value;
}

// AFTER: Use primitive in hot path, only box for cache
public int getInt32(String fieldName) {
    // Fast path: cached primitive (no boxing)
    if (isPrimitiveCached(index)) {
        return primitiveCache[index];  // int[], no boxing!
    }
    // Slow path: parse and cache
    int value = parse();
    primitiveCache[index] = value;  // Cache as primitive
    setCached(index);
    return value;  // Return primitive, never boxed!
}

// Dual cache: primitives separate from objects
private int[] primitiveIntCache;  // For int32, boolean (as 0/1)
private long[] primitiveLongCache;  // For int64, double (as bits), datetime
private Object[] objectCache;  // For string, document, array
```

### Optimization 3: Branch Prediction

```java
// Order branches by frequency (most common first)

// BEFORE: Rare case first (poor branch prediction)
public Object getValue(int index) {
    if (fields[index].type == BsonType.NULL) {  // 1% frequency
        return null;
    }
    if (fields[index].type == BsonType.INT32) {  // 30% frequency
        return getInt32(index);
    }
    if (fields[index].type == BsonType.STRING) {  // 40% frequency
        return getString(index);
    }
}

// AFTER: Common cases first (good branch prediction)
public Object getValue(int index) {
    byte type = fields[index].type;
    // Hot types first (80% of fields)
    if (type == BsonType.STRING) return getString(index);  // 40%
    if (type == BsonType.INT32) return getInt32(index);    // 30%
    if (type == BsonType.BOOLEAN) return getBoolean(index); // 10%
    // Medium types (15% of fields)
    if (type == BsonType.INT64) return getInt64(index);    // 8%
    if (type == BsonType.DOUBLE) return getDouble(index);  // 7%
    // Cold types (5% of fields) - grouped together
    return getOtherTypes(index, type);  // Slow path
}
```

### Optimization 4: False Sharing Avoidance

```java
// Pad frequently accessed fields to separate cache lines

import jdk.internal.vm.annotation.Contended;

public class IndexedBsonDocument {
    // Hot fields (accessed on every get)
    private final byte[] data;
    private final FieldIndex[] fields;

    @Contended  // Pad to separate cache line
    private Object[] cache;  // Written on cache miss

    @Contended  // Separate cache line for primitive cache
    private int[] primitiveIntCache;

    @Contended
    private long[] primitiveLongCache;
}
```

## Phase 2.20: Vectorization (Optional)

### Goal
Use SIMD instructions for bulk operations.

### Use Case: Field Name Comparison

```java
// Traditional: Compare bytes one by one
private boolean matchesFieldName(int index, String fieldName) {
    FieldIndex field = fields[index];
    for (int i = 0; i < field.nameLength; i++) {
        if (data[field.nameOffset + i] != (byte) fieldName.charAt(i)) {
            return false;
        }
    }
    return true;
}

// Vectorized: Compare 16 bytes at once using AVX2
private boolean matchesFieldNameVectorized(int index, String fieldName) {
    FieldIndex field = fields[index];
    int len = field.nameLength;

    // Process 16 bytes at a time
    int i = 0;
    for (; i + 16 <= len; i += 16) {
        // Load 16 bytes from data array
        long chunk1 = UNSAFE.getLong(data, field.nameOffset + i);
        long chunk2 = UNSAFE.getLong(data, field.nameOffset + i + 8);

        // Load 16 bytes from field name
        long name1 = packCharsToLong(fieldName, i);
        long name2 = packCharsToLong(fieldName, i + 8);

        // Compare 16 bytes in parallel
        if (chunk1 != name1 || chunk2 != name2) {
            return false;
        }
    }

    // Handle remaining bytes
    for (; i < len; i++) {
        if (data[field.nameOffset + i] != (byte) fieldName.charAt(i)) {
            return false;
        }
    }
    return true;
}
```

**Benefit**: 4-8x faster field name comparison for long field names (>16 chars).

## Performance Roadmap

| Phase | Target | Strategy | Expected Result |
|-------|--------|----------|-----------------|
| **2.14 (Current)** | - | FastBsonDocument (failed) | 316ms (1.30x) ❌ |
| **2.15** | 150ms | Parser interface enhancement | 2.7x speedup |
| **2.16** | 100ms | IndexedBsonDocument | 4.1x speedup |
| **2.17** | 70ms | Full zero-copy parsers | 5.8x speedup |
| **2.18** | 50ms | Trie-based field lookup | 8.2x speedup |
| **2.19** | 30ms | JVM optimization tuning | 13.6x speedup |
| **2.20** | 20ms | SIMD vectorization (optional) | 20x speedup |

**Milestone**: Phase 2.17 should surpass Phase 1 (99ms → 70ms = 1.4x improvement).

## Migration Strategy

### Backward Compatibility

```java
// OLD API (Phase 1-2.14): Still works
Map<String, Object> map = new TypeHandler().parseDocument(new BsonReader(data));

// NEW API (Phase 2.15+): Zero-copy mode
BsonDocument doc = IndexedBsonDocument.parse(data);  // Zero-copy
int value = doc.getInt32("field");  // Lazy parse

// LEGACY API (backward compatible): Boxing mode
Map<String, Object> map = doc.toLegacyMap();  // Converts to HashMap
```

### Gradual Rollout

1. **Phase 2.15**: Add zero-copy methods, keep legacy API
2. **Phase 2.16**: Make IndexedBsonDocument default, deprecated FastBsonDocument
3. **Phase 2.17**: Remove FastBsonDocument (breaking change in major version)

## Testing & Validation

### Performance Benchmarks

```java
@State(Scope.Thread)
public class ZeroCopyBenchmark {
    byte[] testData;

    @Setup
    public void setup() {
        testData = BsonTestDataGenerator.generateDocument(50);
    }

    @Benchmark
    public void baseline_Phase1_HashMap() {
        new TypeHandler().parseDocument(new BsonReader(testData));
    }

    @Benchmark
    public void current_Phase2_14_FastBsonDocument() {
        FastBson.parse(testData);
    }

    @Benchmark
    public void new_Phase2_16_IndexedBsonDocument() {
        IndexedBsonDocument.parse(testData);
    }
}
```

### Correctness Tests

```java
@Test
public void testZeroCopyEquivalence() {
    byte[] data = generateTestData();

    // Parse with all implementations
    Map<String, Object> hashMap = Phase1Parser.parse(data);
    BsonDocument fastDoc = FastBsonDocument.parse(data);
    BsonDocument indexedDoc = IndexedBsonDocument.parse(data);

    // All should produce identical results
    assertEquals(hashMap.get("field"), fastDoc.get("field"));
    assertEquals(hashMap.get("field"), indexedDoc.get("field"));
}
```

## Summary

### Key Principles

1. **Incremental**: Don't rewrite everything at once
2. **Measurable**: Benchmark every change
3. **Compatible**: Keep backward compatibility
4. **Pragmatic**: Simple > complex

### Success Criteria

- ✅ Parse time: <70ms (1.4x faster than Phase 1's 99ms)
- ✅ Memory: <30 bytes per field (vs 50 bytes for HashMap)
- ✅ API: Clean, type-safe, zero-copy
- ✅ Maintainability: Each parser is independent and testable

### Timeline

- **Phase 2.15-2.16**: 2 weeks (core implementation)
- **Phase 2.17**: 1 week (remaining parsers)
- **Phase 2.18-2.19**: 1 week (optimizations)
- **Phase 2.20**: Optional (SIMD)

**Total: 4-5 weeks to surpass Phase 1 performance.**
