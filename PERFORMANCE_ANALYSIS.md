# FastBSON Performance Analysis & Optimization Plan

## Performance Regression Analysis

### Benchmark Results

| Version | FastBSON | MongoDB | Speedup | Implementation |
|---------|----------|---------|---------|----------------|
| **Phase 1** | 99 ms | 429 ms | **4.32x** | `HashMap<String, Object>` + boxing |
| **Phase 2.14** | 316 ms | 412 ms | **1.30x** | `FastBsonDocument` + zero-boxing |
| **Regression** | **+219ms (+221%)** | -17ms | **-70%** | Complexity explosion |

### Root Cause Analysis

#### Phase 1 Implementation (Fast & Simple)
```java
public Map<String, Object> parseDocument(BsonReader reader) {
    Map<String, Object> document = new HashMap<>();
    while (reader.position() < endPosition) {
        byte type = reader.readByte();
        String fieldName = reader.readCString();
        Object value = parseValue(reader, type);  // Direct boxing
        document.put(fieldName, value);           // Single hash lookup
    }
    return document;
}
```

**Performance Characteristics:**
- Single HashMap with one hash lookup per field
- JVM auto-boxing cache works well for small integers (-128~127)
- Direct method calls, no abstraction layers
- Memory layout: contiguous HashMap entries

#### Phase 2.14 Implementation (Complex & Slow)
```java
// Layer 1: FastBson API
BsonDocument doc = FastBson.parse(bsonData);

// Layer 2: DocumentParser
BsonDocumentBuilder builder = factory.newDocumentBuilder();
while (...) {
    switch (type) {
        case INT32:
            builder.putInt32(fieldName, reader.readInt32());
            break;
    }
}
return builder.build();

// Layer 3: FastBsonDocumentBuilder
public BsonDocumentBuilder putInt32(String fieldName, int value) {
    int fieldId = getOrCreateFieldId(fieldName);  // Hash lookup #1
    intFields.put(fieldId, value);                // Hash lookup #2
    fieldTypes.put(fieldId, BsonType.INT32);      // Hash lookup #3
    return this;
}

// Layer 4: FastBsonDocument.build()
return new FastBsonDocument(
    fieldNameToId,  // Copy
    intFields,      // Copy
    longFields,     // Copy
    doubleFields,   // Copy
    // ... 10 more collections
);
```

**Performance Killers:**
1. **Multiple Hash Lookups**: 3 per field (fieldName→fieldId, fieldId→value, fieldId→type)
2. **Builder Pattern Overhead**: Extra method calls and state management
3. **Type-Specific Collections**: 10+ separate collections (intFields, longFields, etc.)
4. **Builder Construction**: build() copies all data to new FastBsonDocument
5. **fastutil Overhead**: Object2IntMap, Int2IntMap slower than HashMap for small sizes
6. **Factory Indirection**: Extra method call to get builder

### Memory Layout Comparison

**Phase 1 (HashMap):**
```
HashMap<String, Object>
  Entry[]
    Entry {hash, key="field1", value=Integer(123), next}
    Entry {hash, key="field2", value=String("hello"), next}
```
- Cache-friendly: entries in contiguous array
- Single indirection: key → value

**Phase 2.14 (FastBsonDocument):**
```
FastBsonDocument
  Object2IntMap fieldNameToId
    Entry[] {"field1"→0, "field2"→1}
  Int2IntMap intFields
    Entry[] {0→123}
  Object2ObjectMap stringFields
    Entry[] {1→"hello"}
  Int2ByteMap fieldTypes
    Entry[] {0→INT32, 1→STRING}
```
- Cache-unfriendly: scattered across 10+ collections
- Double indirection: fieldName → fieldId → value
- Extra type map for no benefit

## Why Zero-Boxing Failed

### Misconception #1: Boxing is Expensive
**Reality**: JVM auto-boxing cache makes small integers (−128~127) essentially free
```java
Integer.valueOf(100) == Integer.valueOf(100)  // true, same object
```

### Misconception #2: Separate Collections Save Memory
**Reality**: Multiple collections have more overhead than one HashMap
- 10 separate collections = 10 separate arrays + 10 load factors
- HashMap with Object values = 1 array + 1 load factor

### Misconception #3: Avoiding Wrapper Objects is Faster
**Reality**: Modern JVM optimizes boxing away via escape analysis
- In benchmark: `int value = doc.getInt32("field")` → no boxing
- Wrapper objects that don't escape are stack-allocated

## What is True Zero-Copy?

### Current Implementation (NOT Zero-Copy)
```java
byte[] bsonData = ...;
BsonDocument doc = FastBson.parse(bsonData);  // Allocates HashMap/FastBsonDocument
String value = doc.getString("field");         // Returns new String
```
**Allocations:**
- New HashMap or FastBsonDocument
- New String objects for all field names
- New String objects for all string values
- Builder object during parsing

### True Zero-Copy Implementation
```java
byte[] bsonData = ...;
BsonView view = BsonView.wrap(bsonData);       // No allocation
String value = view.getString("field");         // Returns substring view
```
**Key Principles:**
1. **No Parsing Upfront**: Don't parse until fields are accessed
2. **Direct Byte Access**: Read values directly from byte[]
3. **View Pattern**: Return views (offset + length) instead of copies
4. **Lazy Materialization**: Only create objects when explicitly requested

**Example: Lazy String**
```java
class LazyString {
    byte[] data;
    int offset;
    int length;
    String materialized;  // Cache

    public String toString() {
        if (materialized == null) {
            materialized = new String(data, offset, length, UTF_8);
        }
        return materialized;
    }
}
```

## Distance from Zero-Copy

| Feature | Current Phase 2.14 | True Zero-Copy |
|---------|-------------------|----------------|
| Parse on access | ❌ Parse all fields | ✅ Parse on demand |
| Memory allocation | ❌ 10+ collections | ✅ Single byte[] reference |
| String copying | ❌ Copy all strings | ✅ Return substrings/views |
| Field lookup | ❌ Hash lookups | ✅ Binary search on offsets |
| Nested documents | ❌ Recursive parse | ✅ Lazy child views |

**Distance: Very far (需要完全重写)**

## Optimization Strategies

### Strategy 1: Back to Basics (Recommended for Phase 2.15)
**Goal**: Match Phase 1 performance (4.32x)

**Changes:**
1. Remove FastBsonDocument, FastBsonArray, all Builder classes
2. Keep TypeHandler.parseDocument() as primary implementation
3. Add optimizations on top of simple HashMap:
   - String interning for field names
   - Object pooling for HashMap instances
   - Integer cache for common values

**Pros:**
- Proven fast (99ms)
- Simple to understand and maintain
- Easy to optimize incrementally

**Cons:**
- Uses boxing (but it's fast!)
- Not "zero-copy" in name

### Strategy 2: Optimize Current Architecture
**Goal**: Make zero-boxing actually fast

**Changes:**
1. Replace Builder pattern with direct construction
2. Use single HashMap instead of type-specific collections
3. Remove fieldId indirection, use fieldName directly
4. Lazy builder construction

**Pros:**
- Keeps "zero-boxing" branding
- Potentially faster than HashMap for large documents

**Cons:**
- Complex to implement correctly
- Uncertain if it can beat HashMap

### Strategy 3: True Zero-Copy (Phase 3+)
**Goal**: No allocations during parse

**Changes:**
1. Implement BsonView that wraps byte[]
2. Parse fields only on access
3. Return substring views instead of copies
4. Binary search on pre-indexed offsets

**Pros:**
- True zero-copy semantics
- Minimal memory footprint
- Very fast for sparse access patterns

**Cons:**
- Major API change
- Complex implementation
- Slower for full document access

## Recommendation: Strategy 1 (Back to Basics)

**Rationale:**
1. **Proven Performance**: Phase 1 already achieved 4.32x
2. **Simple > Complex**: HashMap is battle-tested and optimized
3. **Incremental Optimization**: Can add optimizations one by one
4. **User Expectations**: Most users need Map<String, Object> anyway

**Implementation Plan:**
1. Remove deprecated marker from TypeHandler.parseDocument()
2. Make it the primary API: `FastBson.parseToMap(byte[])`
3. Add FastBson.parse() as alias
4. Keep BsonDocument interface for type-safe access
5. Add optimizations:
   - Field name interning
   - HashMap pooling
   - Integer caching

**Expected Result:**
- Phase 2.15: 90-100ms (match Phase 1)
- Phase 2.16: 70-80ms (with optimizations)
- Phase 3: True zero-copy for specific use cases

## Action Items

- [ ] Remove FastBsonDocument/FastBsonArray/Builder classes
- [ ] Restore TypeHandler.parseDocument() as primary implementation
- [ ] Add FastBson.parseToMap() API
- [ ] Implement field name interning
- [ ] Implement HashMap pooling
- [ ] Benchmark and verify 4x+ speedup
- [ ] Update documentation

## Conclusion

**The zero-boxing architecture failed because it optimized the wrong thing.**

- **What we optimized**: Avoiding wrapper objects (Integer, Long, etc.)
- **What we should optimize**: Hash lookups, memory layout, method calls

**The path forward**: Go back to simple HashMap, then optimize incrementally with proven techniques (interning, pooling, caching).
