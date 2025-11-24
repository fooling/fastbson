# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FastBSON is a high-performance BSON (Binary JSON) deserialization and partial field parsing library for Java. The library focuses on optimizing scenarios where only a subset of fields needs to be extracted from BSON documents, achieving 3-10x performance improvements by skipping unnecessary fields.

**Technical Constraints:**
- Java 8 compatible syntax (for maximum compatibility)
- Follows MongoDB 3.4 BSON specification
- Java-only implementation (no multi-language support in this phase)

**Key Performance Principles:**
- Zero-copy where possible
- Lazy parsing - only parse fields when actually needed
- Fast skip - leverage BSON's length prefix to skip unwanted fields in O(1)
- Field name caching and string interning
- Type-specialized parsing for common types
- ThreadLocal object pooling to reduce GC pressure

## Development Commands

### Build
```bash
mvn clean compile
```

### Run Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=BsonReaderTest

# Run tests with coverage
mvn test jacoco:report
```

### Benchmarks
```bash
# Run JMH benchmarks
mvn test -Dtest=FastBsonBenchmark
```

### Package
```bash
mvn clean package
```

## Architecture

### Core Components

**1. BsonReader** (`com.fastbson.reader`)
- Low-level byte stream reader managing byte[] or ByteBuffer
- Provides basic type reading (int32, int64, double, C-strings)
- Handles little-endian byte order as per BSON spec
- Supports position tracking and fast skip operations

**2. FieldMatcher** (`com.fastbson.matcher`)
- Implements "assumed ordered" fast matching algorithm (inspired by FastJSON)
- Uses different strategies based on field set size:
  - Small (<10 fields): linear array search
  - Medium (10-100): HashMap lookup
  - Large (>100): Trie-based lookup
- Field name interning to reduce string allocations
- Ordered optimization: remembers expected field sequence

**3. ValueSkipper** (`com.fastbson.skipper`)
- Fast skip implementation for all BSON types
- Uses static lookup table for fixed-length types (double, int32, int64, boolean, ObjectId, datetime)
- Reads length prefix for variable-length types (string, binary, document, array)
- Can skip entire nested documents in O(1) using document length

**4. TypeHandler** (`com.fastbson.handler`)
- Type-specific value parsing for all BSON types
- Optimized paths for common types (int32/int64/string)
- StringBuilder reuse for string parsing
- Zero-copy ByteBuffer views for binary data
- Recursive partial parsing for nested documents

**5. PartialParser** (`com.fastbson`)
- Main parsing orchestration logic
- Iterates through BSON elements, matching against target fields
- Delegates to TypeHandler or ValueSkipper based on match result
- Returns Map<String, Object> or type-safe BsonResult

**6. ObjectPool** (`com.fastbson`)
- ThreadLocal pools for frequently allocated objects:
  - BsonReader instances
  - Temporary byte buffers
  - StringBuilder instances
- ConcurrentHashMap for field name string interning

### BSON Format Details

BSON documents structure:
- **Document**: int32 (total length) + element_list + 0x00 (terminator)
- **Element**: type_byte + field_name (C-string) + value
- **All integers in little-endian byte order**

Common type bytes:
- 0x01: double (8 bytes)
- 0x02: string (int32 length + UTF-8 + null)
- 0x03: embedded document
- 0x04: array
- 0x05: binary (int32 length + subtype + bytes)
- 0x07: ObjectId (12 bytes)
- 0x08: boolean (1 byte)
- 0x09: UTC datetime (int64 milliseconds)
- 0x10: int32 (4 bytes)
- 0x12: int64 (8 bytes)

### Design Patterns

**Ordered Field Matching:**
The "assumed ordered" algorithm assumes fields typically appear in the same order across documents. FieldMatcher tracks expected positions and checks those first, falling back to full lookup on mismatch.

**ThreadLocal Pooling:**
Heavily used objects (BsonReader, StringBuilder, byte buffers) are pooled per thread to minimize allocations and GC pressure during high-throughput parsing.

**Type Dispatch Optimization:**
TypeHandler places most common types (string, int32, int64, double) at the top of conditional branches for better CPU branch prediction.

## Code Style and Standards

**Complete development standards are documented in DEVELOPMENT.md**

Key requirements:
- **Java 8 syntax only** - no var, no List.of(), explicit lambda types
- **SOLID principles** - especially Single Responsibility Principle
- **100% branch coverage** - all code paths must be tested
- **Lombok usage** - @Data for entities, @Builder for builders, @Slf4j for logging
- **Naming conventions**:
  - Classes: `BsonReader`, `TypeHandler`, `BsonReaderTest`
  - Methods: `readInt32()`, `parseValue()`, `matches()`
  - Variables: camelCase, constants UPPER_SNAKE_CASE
  - Packages: `com.cloud.fastbson.reader`, `com.cloud.fastbson.parser`

See DEVELOPMENT.md for complete standards on:
- Code formatting (4 spaces, K&R braces, 120 char limit)
- Testing requirements (AAA pattern, test naming)
- Performance guidelines (object pooling, string interning)
- Exception handling (custom exception hierarchy)
- JavaDoc requirements (all public APIs)
- Git commit conventions (Conventional Commits)

## Testing Requirements

- **Coverage**: 100% code coverage required for all branches
- **Unit Tests**: Each component (BsonReader, FieldMatcher, ValueSkipper, TypeHandler) must have isolated tests
- **Edge Cases**: Test empty documents, null values, boundary conditions, malformed data
- **Performance**: Benchmark tests comparing against MongoDB Java Driver BSON parser
- **Type Coverage**: Tests for all BSON type bytes (0x01-0x12 and beyond)

Test data location: `src/test/resources/test-data/`

## Performance Optimization Guidelines

1. **Avoid allocations in hot paths** - reuse objects via ThreadLocal pools
2. **Leverage BSON structure** - use length prefixes to skip entire sections
3. **String interning** - intern all field names to enable reference equality checks
4. **Branch optimization** - most common types/paths first
5. **Zero-copy** - return ByteBuffer views instead of copying binary data
6. **Lazy parsing** - consider LazyBsonValue pattern for deferred parsing

## Project Structure

```
src/main/java/com/cloud/fastbson/
├── FastBsonParser.java          # Main entry point, builder API
├── BsonDocument.java            # Type-safe accessor wrapper
├── ObjectPool.java              # ThreadLocal object pooling
├── reader/
│   └── BsonReader.java          # Low-level byte reading
├── matcher/
│   ├── FieldMatcher.java        # Field matching logic
│   └── OrderedFieldMatcher.java # Ordered optimization variant
├── skipper/
│   └── ValueSkipper.java        # Fast skip for unwanted fields
├── handler/
│   ├── TypeHandler.java         # Value parsing by type
│   └── TypeRegistry.java        # Type metadata
├── parser/
│   └── PartialParser.java       # Partial field parser
└── util/
    ├── BsonType.java            # Type constants
    └── BsonUtils.java           # Utility functions

src/test/java/com/cloud/fastbson/
├── *Test.java                   # Unit tests
└── benchmark/
    └── FastBsonBenchmark.java   # JMH benchmarks
```

## Implementation Phases

See `architecture.md` for detailed 4-phase implementation plan (8 weeks total):
1. Phase 1: Basic framework (BsonReader, ValueSkipper, basic PartialParser)
2. Phase 2: Performance optimization (FieldMatcher, ObjectPool, string interning)
3. Phase 3: Advanced features (nested fields, streaming API, type-safe accessors)
4. Phase 4: Testing and benchmarking

## Important Notes

- **Java 8 compatible** - use Java 8 syntax only (no lambda with type inference, no var, no modern APIs)
- **MongoDB 3.4 BSON spec** - follow BSON specification as defined in MongoDB 3.4
- All multi-byte values in BSON use little-endian byte order
- Field names in BSON are C-style null-terminated strings (UTF-8)
- Document length includes the 4-byte length field itself and the trailing 0x00
- When skipping embedded documents/arrays, read the length and skip (length - 4) bytes
- Use `127.0.0.1` instead of `localhost` for all network operations

## Java 8 Compatibility Guidelines

- Use explicit lambda parameter types: `(String s) -> ...` instead of `s -> ...`
- No `var` keyword - use explicit types
- Use `new ArrayList<>()` instead of `List.of()`
- Use `new HashMap<>()` instead of `Map.of()`
- Use traditional try-catch instead of try-with-resources where Java 7 compatibility needed
- Avoid `Optional.ifPresentOrElse()` - use `isPresent()` checks instead
