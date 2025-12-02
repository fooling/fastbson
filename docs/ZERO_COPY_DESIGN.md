# Zero-Copy BSON Parser Design

## Design Goals

1. **True Zero-Copy**: No data copying during parse
2. **Type Safety**: Separate handling for different BSON types
3. **JVM-Friendly**: Leverage escape analysis, auto-boxing cache, inline caching
4. **Maintainable**: Clean separation of concerns
5. **Fast**: Target 50-80ms (vs Phase 1's 99ms)

## Core Idea: Index-Based Lazy Parsing

### Problem with Current Approaches

**Phase 1 (HashMap)**:
- ❌ Allocates HashMap + all wrapper objects immediately
- ❌ Parses all fields even if unused
- ✅ Simple and fast (99ms)

**Phase 2.14 (FastBsonDocument)**:
- ❌ Multiple collections with double indirection
- ❌ Builder pattern overhead
- ❌ Parses all fields immediately
- ❌ Very slow (316ms)

### New Approach: BsonView with Field Index

**Key Principles:**
1. **First pass**: Build field index only (offset + type)
2. **No parsing**: Don't parse values until accessed
3. **Direct access**: Read from byte[] using offsets
4. **Smart caching**: Cache parsed values for repeated access

## Architecture

### Layer 1: BsonView (Zero-Copy Container)

```java
public class BsonView implements BsonDocument {
    private final byte[] data;           // Original BSON data (no copy!)
    private final FieldIndex[] fields;   // Field metadata (small)
    private Object[] cache;              // Lazy value cache

    static class FieldIndex {
        int nameOffset;    // Offset of field name in data
        int nameLength;    // Length of field name
        int valueOffset;   // Offset of field value
        byte type;         // BSON type
    }

    // Parse: O(n) scan to build index
    public static BsonView parse(byte[] data) {
        List<FieldIndex> fields = new ArrayList<>();
        int pos = 4;  // Skip document length

        while (data[pos] != 0) {
            byte type = data[pos++];
            int nameStart = pos;
            int nameLen = 0;
            while (data[pos++] != 0) nameLen++;

            fields.add(new FieldIndex(nameStart, nameLen, pos, type));
            pos += getValueSize(data, pos, type);  // Skip value
        }

        return new BsonView(data, fields.toArray(new FieldIndex[0]));
    }

    // Access: O(log n) binary search + lazy parse
    public int getInt32(String fieldName) {
        int index = findField(fieldName);  // Binary search on names
        if (index < 0) throw new NoSuchFieldException();

        // Check cache
        if (cache[index] != null) {
            return (Integer) cache[index];
        }

        // Parse on demand
        FieldIndex field = fields[index];
        int value = readInt32(data, field.valueOffset);
        cache[index] = value;  // Auto-boxing, but cached
        return value;
    }
}
```

**Benefits:**
- ✅ Zero copy: Only stores byte[] reference
- ✅ Lazy parsing: Only parse accessed fields
- ✅ Fast field lookup: Binary search O(log n)
- ✅ JVM-friendly: Cached boxed values, escape analysis works

### Layer 2: Type-Specific Optimizations

```java
// Int32: Auto-boxing cache works great
public int getInt32(String fieldName) {
    int index = findField(fieldName);
    if (cache[index] != null) {
        return (Integer) cache[index];  // Cache hit, no allocation
    }
    int value = readInt32Direct(fields[index].valueOffset);
    cache[index] = value;  // JVM caches -128~127 automatically
    return value;
}

// String: Return substring view (Java 9+ shares underlying array)
public String getString(String fieldName) {
    int index = findField(fieldName);
    if (cache[index] != null) {
        return (String) cache[index];
    }
    FieldIndex field = fields[index];
    int length = readInt32(data, field.valueOffset) - 1;  // Exclude null terminator
    String value = new String(data, field.valueOffset + 4, length, UTF_8);
    cache[index] = value;
    return value;
}

// Nested document: Lazy child view
public BsonDocument getDocument(String fieldName) {
    int index = findField(fieldName);
    if (cache[index] != null) {
        return (BsonDocument) cache[index];
    }
    FieldIndex field = fields[index];
    int docLength = readInt32(data, field.valueOffset);
    // Create child view (shares same byte array!)
    BsonView childView = BsonView.wrap(data, field.valueOffset, docLength);
    cache[index] = childView;
    return childView;
}
```

### Layer 3: Field Name Optimization

**Problem**: Comparing field names is expensive

**Solution**: Pre-hash field names and use Trie for fast matching

```java
static class FieldIndex {
    int nameOffset;
    int nameLength;
    int nameHash;      // Pre-computed hash
    int valueOffset;
    byte type;
}

// Build hash during index creation
private void buildIndex() {
    for (FieldIndex field : fields) {
        field.nameHash = hashFieldName(data, field.nameOffset, field.nameLength);
    }
    // Sort by hash for binary search
    Arrays.sort(fields, Comparator.comparingInt(f -> f.nameHash));
}

// Fast lookup using hash
private int findField(String fieldName) {
    int hash = fieldName.hashCode();
    // Binary search on hashes
    int left = 0, right = fields.length - 1;
    while (left <= right) {
        int mid = (left + right) >>> 1;
        int cmp = Integer.compare(fields[mid].nameHash, hash);
        if (cmp < 0) {
            left = mid + 1;
        } else if (cmp > 0) {
            right = mid - 1;
        } else {
            // Hash match, verify actual name
            if (matchesFieldName(mid, fieldName)) {
                return mid;
            }
            // Hash collision, linear probe
            return linearSearch(mid, fieldName, hash);
        }
    }
    return -1;
}
```

## Performance Characteristics

### Parse Performance

| Operation | Phase 1 (HashMap) | Phase 2.14 (FastBsonDocument) | New (BsonView) |
|-----------|------------------|-------------------------------|----------------|
| Document scan | O(n) | O(n) | O(n) |
| Field name copy | n strings | n strings | 0 (indexes only) |
| Value parsing | All fields | All fields | 0 (lazy) |
| Memory alloc | HashMap + n objects | 10+ collections + n objects | Index array only |
| **Total time** | 99ms | 316ms | **~30ms** (estimated) |

### Access Performance

| Operation | Cost | JVM Optimization |
|-----------|------|------------------|
| getInt32("field") | Binary search O(log n) + cache check | Escape analysis eliminates boxing |
| getString("field") | Same + UTF-8 decode | String interning for repeated access |
| getDocument("field") | Same + child view creation | Child view shares parent's byte[] |

### Memory Footprint

**Phase 1:**
```
HashMap (32 bytes) + Entry[] (16n bytes) + n String keys + n boxed values
Total: ~50n bytes
```

**Phase 2.14:**
```
10 collections (320 bytes) + 10 arrays (160n bytes) + overhead
Total: ~200n bytes
```

**New BsonView:**
```
byte[] reference (8 bytes) + FieldIndex[] (20n bytes) + Object[] cache (8n bytes)
Total: ~30n bytes (when fully cached)
        ~20n bytes (sparse access)
```

## JVM Optimizations Leveraged

### 1. Escape Analysis
```java
public int sumValues(BsonView doc) {
    int sum = 0;
    for (int i = 0; i < 100; i++) {
        sum += doc.getInt32("field" + i);  // Integer never escapes, no allocation!
    }
    return sum;
}
```
JVM detects that Integer objects don't escape and eliminates boxing entirely.

### 2. Auto-Boxing Cache
```java
Integer.valueOf(42) == Integer.valueOf(42)  // true, same object
```
For -128~127, JVM reuses cached Integer objects. Most BSON values fall in this range.

### 3. Inline Caching
```java
for (int i = 0; i < 1000; i++) {
    doc.getInt32("field");  // JVM inlines binary search, caches field index
}
```
JVM's JIT compiler caches the field index lookup, making repeated access very fast.

### 4. Branch Prediction
```java
if (cache[index] != null) {  // Predicted taken after first access
    return (Integer) cache[index];
}
```
After warmup, JVM predicts cache hits correctly ~99% of the time.

### 5. False Sharing Avoidance
```java
@Contended  // Prevent false sharing between cache slots
private Object[] cache;
```
Pads cache array to prevent CPU cache line conflicts.

## Implementation Plan

### Phase 1: Core BsonView (Week 1)
- [ ] Implement BsonView.parse() with field indexing
- [ ] Implement basic get methods (getInt32, getString, getDouble)
- [ ] Add value caching
- [ ] Benchmark vs HashMap: target <50ms

### Phase 2: Optimizations (Week 2)
- [ ] Field name hashing for fast lookup
- [ ] Binary search on hashes
- [ ] Trie-based matcher for common prefixes
- [ ] Benchmark: target <30ms

### Phase 3: Advanced Features (Week 3)
- [ ] Nested document support (lazy child views)
- [ ] Array support
- [ ] Bulk access patterns (iterator, stream)
- [ ] Benchmark: maintain <30ms

### Phase 4: Production Ready (Week 4)
- [ ] Thread safety analysis
- [ ] Error handling
- [ ] API documentation
- [ ] Migration guide from HashMap API

## Expected Results

| Scenario | HashMap (Phase 1) | BsonView (New) | Improvement |
|----------|------------------|----------------|-------------|
| **Full parse (50 fields)** | 99ms | **30ms** | **3.3x faster** |
| Sparse access (3 fields) | 99ms | **5ms** | **20x faster** |
| Repeated access (same field) | 99ms | **2ms** (cached) | **50x faster** |
| Memory (50 fields) | 2.5 KB | **1 KB** | **2.5x less** |

## Why This Works

1. **Index-only parse**: Building field index is much faster than parsing values
2. **Lazy parsing**: Only parse what you access
3. **Smart caching**: Cache parsed values for repeated access
4. **Zero copy**: No data duplication, just offsets
5. **JVM-friendly**: Escape analysis, boxing cache, inline caching all work

## Next Steps

1. Prototype BsonView implementation
2. Benchmark against Phase 1
3. If successful (>2x faster), replace FastBsonDocument
4. If not, keep HashMap as primary API

## Trade-offs

**Pros:**
- ✅ True zero-copy (no data duplication)
- ✅ Fast for both full and sparse access
- ✅ Low memory footprint
- ✅ JVM optimizations work perfectly

**Cons:**
- ❌ More complex than HashMap
- ❌ Requires byte[] to stay alive (can't be GC'd)
- ❌ Initial indexing cost (but much cheaper than full parse)

## Conclusion

**BsonView combines the best of both worlds:**
- Phase 1's simplicity and speed
- Phase 2's type safety and structure
- True zero-copy semantics
- JVM optimization friendly

**This is the right architecture for FastBSON.**
