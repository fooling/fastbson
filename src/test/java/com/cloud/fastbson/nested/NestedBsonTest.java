package com.cloud.fastbson.nested;

import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.handler.TypeHandler;
import com.cloud.fastbson.handler.parsers.DocumentParser;
import com.cloud.fastbson.reader.BsonReader;
import com.cloud.fastbson.types.JavaScriptWithScope;
import org.bson.*;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.EncoderContext;
import org.bson.io.BasicOutputBuffer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 深度嵌套 BSON 测试
 *
 * <p>测试目标：
 * 1. 验证深度嵌套文档解析（2/5/10 层）
 * 2. 验证数组嵌套场景（Array of Documents, Array of Arrays）
 * 3. 验证混合嵌套和边界情况
 * 4. 验证性能和栈溢出保护
 */
public class NestedBsonTest {

    private final TypeHandler handler = new TypeHandler();

    /**
     * 序列化 BsonDocument 为字节数组
     */
    private byte[] serialize(org.bson.BsonDocument doc) {
        BasicOutputBuffer buffer = new BasicOutputBuffer();
        BsonBinaryWriter writer = new BsonBinaryWriter(buffer);
        new BsonDocumentCodec().encode(writer, doc, EncoderContext.builder().build());
        return buffer.toByteArray();
    }

    // ==================== 深度嵌套测试 ====================

    @Test
    public void test2LayerNesting() {
        // 构造 2 层嵌套文档
        org.bson.BsonDocument innerDoc = new org.bson.BsonDocument()
            .append("innerField", new BsonString("inner value"))
            .append("innerNumber", new BsonInt32(42));

        org.bson.BsonDocument outerDoc = new org.bson.BsonDocument()
            .append("outerField", new BsonString("outer value"))
            .append("nested", innerDoc);

        byte[] bsonData = serialize(outerDoc);
        BsonReader reader = new BsonReader(bsonData);
        com.cloud.fastbson.document.BsonDocument result = (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);

        // 验证外层字段
        assertEquals("outer value", result.get("outerField"));

        // 验证嵌套文档
        com.cloud.fastbson.document.BsonDocument nested = (com.cloud.fastbson.document.BsonDocument) result.get("nested");
        assertNotNull(nested);
        assertEquals("inner value", nested.get("innerField"));
        assertEquals(42, nested.get("innerNumber"));
    }

    @Test
    public void test5LayerNesting() {
        // 构造 5 层嵌套文档
        org.bson.BsonDocument level5 = new org.bson.BsonDocument()
            .append("level", new BsonInt32(5))
            .append("data", new BsonString("deepest level"));

        org.bson.BsonDocument level4 = new org.bson.BsonDocument()
            .append("level", new BsonInt32(4))
            .append("child", level5);

        org.bson.BsonDocument level3 = new org.bson.BsonDocument()
            .append("level", new BsonInt32(3))
            .append("child", level4);

        org.bson.BsonDocument level2 = new org.bson.BsonDocument()
            .append("level", new BsonInt32(2))
            .append("child", level3);

        org.bson.BsonDocument level1 = new org.bson.BsonDocument()
            .append("level", new BsonInt32(1))
            .append("child", level2);

        byte[] bsonData = serialize(level1);
        BsonReader reader = new BsonReader(bsonData);
        com.cloud.fastbson.document.BsonDocument result = (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);

        // 验证层级
        assertEquals(1, result.get("level"));

        // 逐层验证
        com.cloud.fastbson.document.BsonDocument l2 = (com.cloud.fastbson.document.BsonDocument) result.get("child");
        assertEquals(2, l2.get("level"));

        com.cloud.fastbson.document.BsonDocument l3 = (com.cloud.fastbson.document.BsonDocument) l2.get("child");
        assertEquals(3, l3.get("level"));

        com.cloud.fastbson.document.BsonDocument l4 = (com.cloud.fastbson.document.BsonDocument) l3.get("child");
        assertEquals(4, l4.get("level"));

        com.cloud.fastbson.document.BsonDocument l5 = (com.cloud.fastbson.document.BsonDocument) l4.get("child");
        assertEquals(5, l5.get("level"));
        assertEquals("deepest level", l5.get("data"));
    }

    @Test
    public void test10LayerNesting() {
        // 构造 10 层嵌套文档（极限递归深度）
        org.bson.BsonDocument doc = new org.bson.BsonDocument()
            .append("level", new BsonInt32(10))
            .append("data", new BsonString("level 10"));

        for (int i = 9; i >= 1; i--) {
            org.bson.BsonDocument parent = new org.bson.BsonDocument()
                .append("level", new BsonInt32(i))
                .append("child", doc);
            doc = parent;
        }

        byte[] bsonData = serialize(doc);
        BsonReader reader = new BsonReader(bsonData);
        com.cloud.fastbson.document.BsonDocument result = (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);

        // 验证最外层
        assertEquals(1, result.get("level"));

        // 遍历到最深层
        com.cloud.fastbson.document.BsonDocument current = (com.cloud.fastbson.document.BsonDocument) result.get("child");
        for (int i = 2; i <= 9; i++) {
            assertNotNull(current);
            assertEquals(i, current.get("level"));
            current = (com.cloud.fastbson.document.BsonDocument) current.get("child");
        }

        // 验证最深层
        assertNotNull(current);
        assertEquals(10, current.get("level"));
        assertEquals("level 10", current.get("data"));
    }

    // ==================== 数组嵌套测试 ====================

    @Test
    public void testArrayOfDocuments() {
        // 数组中嵌套文档
        org.bson.BsonArray array = new org.bson.BsonArray();
        array.add(new org.bson.BsonDocument()
            .append("name", new BsonString("Alice"))
            .append("age", new BsonInt32(30)));
        array.add(new org.bson.BsonDocument()
            .append("name", new BsonString("Bob"))
            .append("age", new BsonInt32(25)));
        array.add(new org.bson.BsonDocument()
            .append("name", new BsonString("Charlie"))
            .append("age", new BsonInt32(35)));

        org.bson.BsonDocument doc = new org.bson.BsonDocument()
            .append("users", array);

        byte[] bsonData = serialize(doc);
        BsonReader reader = new BsonReader(bsonData);
        com.cloud.fastbson.document.BsonDocument result = (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);

        com.cloud.fastbson.document.BsonArray users = (com.cloud.fastbson.document.BsonArray) result.get("users");
        assertNotNull(users);
        assertEquals(3, users.size());

        com.cloud.fastbson.document.BsonDocument user1 = (com.cloud.fastbson.document.BsonDocument) users.get(0);
        assertEquals("Alice", user1.get("name"));
        assertEquals(30, user1.get("age"));

        com.cloud.fastbson.document.BsonDocument user2 = (com.cloud.fastbson.document.BsonDocument) users.get(1);
        assertEquals("Bob", user2.get("name"));
        assertEquals(25, user2.get("age"));
    }

    @Test
    public void testArrayOfArrays() {
        // 数组中嵌套数组
        org.bson.BsonArray innerArray1 = new org.bson.BsonArray();
        innerArray1.add(new BsonInt32(1));
        innerArray1.add(new BsonInt32(2));
        innerArray1.add(new BsonInt32(3));

        org.bson.BsonArray innerArray2 = new org.bson.BsonArray();
        innerArray2.add(new BsonString("a"));
        innerArray2.add(new BsonString("b"));
        innerArray2.add(new BsonString("c"));

        org.bson.BsonArray outerArray = new org.bson.BsonArray();
        outerArray.add(innerArray1);
        outerArray.add(innerArray2);

        org.bson.BsonDocument doc = new org.bson.BsonDocument()
            .append("matrix", outerArray);

        byte[] bsonData = serialize(doc);
        BsonReader reader = new BsonReader(bsonData);
        com.cloud.fastbson.document.BsonDocument result = (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);

        com.cloud.fastbson.document.BsonArray matrix = (com.cloud.fastbson.document.BsonArray) result.get("matrix");
        assertNotNull(matrix);
        assertEquals(2, matrix.size());

        com.cloud.fastbson.document.BsonArray row1 = (com.cloud.fastbson.document.BsonArray) matrix.get(0);
        assertEquals(3, row1.size());
        assertEquals(1, row1.get(0));
        assertEquals(2, row1.get(1));
        assertEquals(3, row1.get(2));

        com.cloud.fastbson.document.BsonArray row2 = (com.cloud.fastbson.document.BsonArray) matrix.get(1);
        assertEquals(3, row2.size());
        assertEquals("a", row2.get(0));
        assertEquals("b", row2.get(1));
        assertEquals("c", row2.get(2));
    }

    @Test
    public void testDocumentWithArrayOfDocumentsWithArrays() {
        // 复杂嵌套：文档 -> 数组 -> 文档 -> 数组
        org.bson.BsonArray tags1 = new org.bson.BsonArray();
        tags1.add(new BsonString("java"));
        tags1.add(new BsonString("bson"));

        org.bson.BsonArray tags2 = new org.bson.BsonArray();
        tags2.add(new BsonString("python"));
        tags2.add(new BsonString("json"));

        org.bson.BsonArray items = new org.bson.BsonArray();
        items.add(new org.bson.BsonDocument()
            .append("id", new BsonInt32(1))
            .append("tags", tags1));
        items.add(new org.bson.BsonDocument()
            .append("id", new BsonInt32(2))
            .append("tags", tags2));

        org.bson.BsonDocument doc = new org.bson.BsonDocument()
            .append("items", items);

        byte[] bsonData = serialize(doc);
        BsonReader reader = new BsonReader(bsonData);
        com.cloud.fastbson.document.BsonDocument result = (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);

        com.cloud.fastbson.document.BsonArray itemsList = (com.cloud.fastbson.document.BsonArray) result.get("items");
        assertEquals(2, itemsList.size());

        com.cloud.fastbson.document.BsonDocument item1 = (com.cloud.fastbson.document.BsonDocument) itemsList.get(0);
        assertEquals(1, item1.get("id"));

        com.cloud.fastbson.document.BsonArray item1Tags = (com.cloud.fastbson.document.BsonArray) item1.get("tags");
        assertEquals(2, item1Tags.size());
        assertEquals("java", item1Tags.get(0));
        assertEquals("bson", item1Tags.get(1));
    }

    // ==================== 混合嵌套测试 ====================

    @Test
    public void testMultipleNestedFields() {
        // 同一文档中多个嵌套字段
        org.bson.BsonDocument address = new org.bson.BsonDocument()
            .append("street", new BsonString("123 Main St"))
            .append("city", new BsonString("San Francisco"));

        org.bson.BsonDocument company = new org.bson.BsonDocument()
            .append("name", new BsonString("Acme Corp"))
            .append("department", new BsonString("Engineering"));

        org.bson.BsonDocument doc = new org.bson.BsonDocument()
            .append("name", new BsonString("John Doe"))
            .append("address", address)
            .append("company", company);

        byte[] bsonData = serialize(doc);
        BsonReader reader = new BsonReader(bsonData);
        com.cloud.fastbson.document.BsonDocument result = (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);

        assertEquals("John Doe", result.get("name"));

        com.cloud.fastbson.document.BsonDocument addr = (com.cloud.fastbson.document.BsonDocument) result.get("address");
        assertEquals("123 Main St", addr.get("street"));
        assertEquals("San Francisco", addr.get("city"));

        com.cloud.fastbson.document.BsonDocument comp = (com.cloud.fastbson.document.BsonDocument) result.get("company");
        assertEquals("Acme Corp", comp.get("name"));
        assertEquals("Engineering", comp.get("department"));
    }

    @Test
    public void testNestedDocumentWithAllTypes() {
        // 嵌套文档中包含多种 BSON 类型
        org.bson.BsonDocument nested = new org.bson.BsonDocument()
            .append("stringField", new BsonString("test"))
            .append("int32Field", new BsonInt32(42))
            .append("int64Field", new BsonInt64(9999999999L))
            .append("doubleField", new BsonDouble(3.14))
            .append("boolField", new BsonBoolean(true))
            .append("nullField", new BsonNull())
            .append("arrayField", new org.bson.BsonArray());

        org.bson.BsonDocument doc = new org.bson.BsonDocument()
            .append("nested", nested);

        byte[] bsonData = serialize(doc);
        BsonReader reader = new BsonReader(bsonData);
        com.cloud.fastbson.document.BsonDocument result = (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);

        com.cloud.fastbson.document.BsonDocument nestedResult = (com.cloud.fastbson.document.BsonDocument) result.get("nested");
        assertNotNull(nestedResult);
        assertEquals("test", nestedResult.get("stringField"));
        assertEquals(42, nestedResult.get("int32Field"));
        assertEquals(9999999999L, nestedResult.get("int64Field"));
        assertEquals(3.14, (Double) nestedResult.get("doubleField"), 0.001);
        assertEquals(true, nestedResult.get("boolField"));
        assertNull(nestedResult.get("nullField"));

        com.cloud.fastbson.document.BsonArray array = (com.cloud.fastbson.document.BsonArray) nestedResult.get("arrayField");
        assertEquals(0, array.size());
    }

    @Test
    public void testJavaScriptWithScopeNesting() {
        // JavaScriptWithScope 中的嵌套 scope 文档
        org.bson.BsonDocument scope = new org.bson.BsonDocument()
            .append("x", new BsonInt32(10))
            .append("config", new org.bson.BsonDocument()
                .append("timeout", new BsonInt32(5000))
                .append("retry", new BsonBoolean(true)));

        BsonJavaScriptWithScope js = new BsonJavaScriptWithScope(
            "function() { return x * config.timeout; }", scope);

        org.bson.BsonDocument doc = new org.bson.BsonDocument()
            .append("script", js);

        byte[] bsonData = serialize(doc);
        BsonReader reader = new BsonReader(bsonData);
        com.cloud.fastbson.document.BsonDocument result = (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);

        JavaScriptWithScope parsed =
            (JavaScriptWithScope) result.get("script");
        assertNotNull(parsed);
        assertEquals("function() { return x * config.timeout; }", parsed.code);
        assertEquals(10, parsed.scope.get("x"));

        com.cloud.fastbson.document.BsonDocument config = (com.cloud.fastbson.document.BsonDocument) parsed.scope.get("config");
        assertEquals(5000, config.get("timeout"));
        assertEquals(true, config.get("retry"));
    }

    // ==================== 边界情况测试 ====================

    @Test
    public void testEmptyNestedDocument() {
        // 空嵌套文档
        org.bson.BsonDocument doc = new org.bson.BsonDocument()
            .append("outerField", new BsonString("outer"))
            .append("emptyNested", new org.bson.BsonDocument());

        byte[] bsonData = serialize(doc);
        BsonReader reader = new BsonReader(bsonData);
        com.cloud.fastbson.document.BsonDocument result = (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);

        assertEquals("outer", result.get("outerField"));

        com.cloud.fastbson.document.BsonDocument empty = (com.cloud.fastbson.document.BsonDocument) result.get("emptyNested");
        assertNotNull(empty);
        assertEquals(0, empty.size());
    }

    @Test
    public void testEmptyNestedArray() {
        // 空嵌套数组
        org.bson.BsonDocument doc = new org.bson.BsonDocument()
            .append("field", new BsonString("value"))
            .append("emptyArray", new org.bson.BsonArray());

        byte[] bsonData = serialize(doc);
        BsonReader reader = new BsonReader(bsonData);
        com.cloud.fastbson.document.BsonDocument result = (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);

        assertEquals("value", result.get("field"));

        com.cloud.fastbson.document.BsonArray array = (com.cloud.fastbson.document.BsonArray) result.get("emptyArray");
        assertNotNull(array);
        assertEquals(0, array.size());
    }

    @Test
    public void testNestedNullValues() {
        // 嵌套文档中的 null 值
        org.bson.BsonDocument nested = new org.bson.BsonDocument()
            .append("field1", new BsonString("value1"))
            .append("field2", new BsonNull())
            .append("field3", new BsonString("value3"));

        org.bson.BsonDocument doc = new org.bson.BsonDocument()
            .append("nested", nested);

        byte[] bsonData = serialize(doc);
        BsonReader reader = new BsonReader(bsonData);
        com.cloud.fastbson.document.BsonDocument result = (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);

        com.cloud.fastbson.document.BsonDocument nestedResult = (com.cloud.fastbson.document.BsonDocument) result.get("nested");
        assertNotNull(nestedResult);
        assertEquals("value1", nestedResult.get("field1"));
        assertNull(nestedResult.get("field2"));
        assertEquals("value3", nestedResult.get("field3"));
    }

    @Test
    public void testDeeplyNestedEmptyDocuments() {
        // 深层嵌套的空文档
        org.bson.BsonDocument level3 = new org.bson.BsonDocument();
        org.bson.BsonDocument level2 = new org.bson.BsonDocument().append("level3", level3);
        org.bson.BsonDocument level1 = new org.bson.BsonDocument().append("level2", level2);

        byte[] bsonData = serialize(level1);
        BsonReader reader = new BsonReader(bsonData);
        com.cloud.fastbson.document.BsonDocument result = (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);

        com.cloud.fastbson.document.BsonDocument l2 = (com.cloud.fastbson.document.BsonDocument) result.get("level2");
        assertNotNull(l2);

        com.cloud.fastbson.document.BsonDocument l3 = (com.cloud.fastbson.document.BsonDocument) l2.get("level3");
        assertNotNull(l3);
        assertEquals(0, l3.size());
    }

    // ==================== 性能测试 ====================

    @Test
    public void testNestedDocumentPerformance() {
        // 对比不同嵌套深度的解析性能
        int iterations = 1000;

        // 2 层嵌套
        org.bson.BsonDocument doc2 = new org.bson.BsonDocument()
            .append("level1", new org.bson.BsonDocument()
                .append("data", new BsonString("value")));
        byte[] data2 = serialize(doc2);

        long start2 = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            BsonReader reader = new BsonReader(data2);
            DocumentParser.INSTANCE.parse(reader);
        }
        long time2 = System.nanoTime() - start2;

        // 5 层嵌套
        org.bson.BsonDocument doc5 = new org.bson.BsonDocument();
        org.bson.BsonDocument current = doc5;
        for (int i = 0; i < 5; i++) {
            org.bson.BsonDocument child = new org.bson.BsonDocument();
            current.append("child", child);
            current = child;
        }
        current.append("data", new BsonString("value"));
        byte[] data5 = serialize(doc5);

        long start5 = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            BsonReader reader = new BsonReader(data5);
            DocumentParser.INSTANCE.parse(reader);
        }
        long time5 = System.nanoTime() - start5;

        // 10 层嵌套
        org.bson.BsonDocument doc10 = new org.bson.BsonDocument();
        current = doc10;
        for (int i = 0; i < 10; i++) {
            org.bson.BsonDocument child = new org.bson.BsonDocument();
            current.append("child", child);
            current = child;
        }
        current.append("data", new BsonString("value"));
        byte[] data10 = serialize(doc10);

        long start10 = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            BsonReader reader = new BsonReader(data10);
            DocumentParser.INSTANCE.parse(reader);
        }
        long time10 = System.nanoTime() - start10;

        System.out.println("Nested Document Performance (1000 iterations):");
        System.out.println("  2 layers:  " + (time2 / 1_000_000) + " ms");
        System.out.println("  5 layers:  " + (time5 / 1_000_000) + " ms");
        System.out.println("  10 layers: " + (time10 / 1_000_000) + " ms");

        // 验证性能随嵌套深度线性增长（允许一定误差）
        assertTrue(time10 < time2 * 6, "10 层嵌套性能应该在 2 层的 6 倍以内");
    }

    @Test
    public void testNoStackOverflow() {
        // 确保深度递归不会导致栈溢出
        // 构造 50 层嵌套（非常深，但应该能处理）
        org.bson.BsonDocument doc = new org.bson.BsonDocument()
            .append("data", new BsonString("deepest"));

        for (int i = 0; i < 50; i++) {
            doc = new org.bson.BsonDocument().append("child", doc);
        }

        byte[] bsonData = serialize(doc);
        BsonReader reader = new BsonReader(bsonData);

        // 应该能成功解析，不会抛出 StackOverflowError
        assertDoesNotThrow(() -> {
            com.cloud.fastbson.document.BsonDocument result = (com.cloud.fastbson.document.BsonDocument) DocumentParser.INSTANCE.parse(reader);
            assertNotNull(result);
        });
    }
}
