package com.cloud.fastbson.matcher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FieldMatcher 单元测试
 *
 * @author FastBSON
 * @since 1.0.0
 */
public class FieldMatcherTest {

    @BeforeEach
    public void setUp() {
        // 每个测试前清空字段名池，确保测试独立
        FieldMatcher.clearFieldNamePool();
    }

    @AfterEach
    public void tearDown() {
        // 每个测试后清空字段名池
        FieldMatcher.clearFieldNamePool();
    }

    // ==================== 构造函数测试 ====================

    @Test
    public void testConstructorWithVarargs() {
        // Arrange & Act
        FieldMatcher matcher = new FieldMatcher("field1", "field2", "field3");

        // Assert
        assertEquals(3, matcher.getTargetFieldCount());
        assertTrue(matcher.isUsingArraySearch());
    }

    @Test
    public void testConstructorWithSet() {
        // Arrange
        Set<String> fields = new HashSet<String>(Arrays.asList("name", "age", "email"));

        // Act
        FieldMatcher matcher = new FieldMatcher(fields);

        // Assert
        assertEquals(3, matcher.getTargetFieldCount());
        assertTrue(matcher.isUsingArraySearch());
    }

    @Test
    public void testConstructorWithLargeSet() {
        // Arrange - 10 个字段，测试 Set 构造函数的 HashMap 分支
        Set<String> fields = new HashSet<String>();
        for (int i = 0; i < 10; i++) {
            fields.add("field" + i);
        }

        // Act
        FieldMatcher matcher = new FieldMatcher(fields);

        // Assert
        assertEquals(10, matcher.getTargetFieldCount());
        assertFalse(matcher.isUsingArraySearch());

        // 验证所有字段都能匹配
        for (int i = 0; i < 10; i++) {
            assertTrue(matcher.matches("field" + i));
        }
        assertFalse(matcher.matches("field10"));
    }

    @Test
    public void testConstructorWithNullVarargs() {
        // Arrange & Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new FieldMatcher((String[]) null);
        });
    }

    @Test
    public void testConstructorWithEmptyVarargs() {
        // Arrange & Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new FieldMatcher();
        });
    }

    @Test
    public void testConstructorWithNullSet() {
        // Arrange & Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new FieldMatcher((Set<String>) null);
        });
    }

    @Test
    public void testConstructorWithEmptySet() {
        // Arrange
        Set<String> emptySet = new HashSet<String>();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new FieldMatcher(emptySet);
        });
    }

    // ==================== 小字段集测试（数组查找）====================

    @Test
    public void testSmallFieldSet_Matches() {
        // Arrange
        FieldMatcher matcher = new FieldMatcher("field1", "field2", "field3");

        // Act & Assert
        assertTrue(matcher.matches("field1"));
        assertTrue(matcher.matches("field2"));
        assertTrue(matcher.matches("field3"));
        assertTrue(matcher.isUsingArraySearch());
    }

    @Test
    public void testSmallFieldSet_NotMatches() {
        // Arrange
        FieldMatcher matcher = new FieldMatcher("field1", "field2", "field3");

        // Act & Assert
        assertFalse(matcher.matches("field4"));
        assertFalse(matcher.matches("field5"));
        assertFalse(matcher.matches("other"));
    }

    @Test
    public void testSmallFieldSet_NullField() {
        // Arrange
        FieldMatcher matcher = new FieldMatcher("field1", "field2");

        // Act & Assert
        assertFalse(matcher.matches(null));
    }

    @Test
    public void testSmallFieldSet_Threshold() {
        // Arrange - 测试阈值边界（9 个字段，应该使用数组）
        FieldMatcher matcher = new FieldMatcher(
            "f1", "f2", "f3", "f4", "f5", "f6", "f7", "f8", "f9"
        );

        // Act & Assert
        assertEquals(9, matcher.getTargetFieldCount());
        assertTrue(matcher.isUsingArraySearch());
        assertTrue(matcher.matches("f1"));
        assertTrue(matcher.matches("f9"));
        assertFalse(matcher.matches("f10"));
    }

    // ==================== 大字段集测试（HashMap 查找）====================

    @Test
    public void testLargeFieldSet_Matches() {
        // Arrange - 10 个字段，应该使用 HashMap
        FieldMatcher matcher = new FieldMatcher(
            "f1", "f2", "f3", "f4", "f5", "f6", "f7", "f8", "f9", "f10"
        );

        // Act & Assert
        assertFalse(matcher.isUsingArraySearch());
        assertTrue(matcher.matches("f1"));
        assertTrue(matcher.matches("f5"));
        assertTrue(matcher.matches("f10"));
    }

    @Test
    public void testLargeFieldSet_NotMatches() {
        // Arrange
        FieldMatcher matcher = new FieldMatcher(
            "f1", "f2", "f3", "f4", "f5", "f6", "f7", "f8", "f9", "f10"
        );

        // Act & Assert
        assertFalse(matcher.matches("f11"));
        assertFalse(matcher.matches("f0"));
        assertFalse(matcher.matches("other"));
    }

    @Test
    public void testLargeFieldSet_NullField() {
        // Arrange
        FieldMatcher matcher = new FieldMatcher(
            "f1", "f2", "f3", "f4", "f5", "f6", "f7", "f8", "f9", "f10"
        );

        // Act & Assert
        assertFalse(matcher.matches(null));
    }

    @Test
    public void testLargeFieldSet_Threshold() {
        // Arrange - 测试阈值边界（10 个字段，应该使用 HashMap）
        FieldMatcher matcher = new FieldMatcher(
            "f1", "f2", "f3", "f4", "f5", "f6", "f7", "f8", "f9", "f10"
        );

        // Act & Assert
        assertEquals(10, matcher.getTargetFieldCount());
        assertFalse(matcher.isUsingArraySearch());
    }

    @Test
    public void testLargeFieldSet_ManyFields() {
        // Arrange - 20 个字段
        String[] fields = new String[20];
        for (int i = 0; i < 20; i++) {
            fields[i] = "field" + i;
        }
        FieldMatcher matcher = new FieldMatcher(fields);

        // Act & Assert
        assertEquals(20, matcher.getTargetFieldCount());
        assertFalse(matcher.isUsingArraySearch());

        // 测试所有字段都能匹配
        for (int i = 0; i < 20; i++) {
            assertTrue(matcher.matches("field" + i));
        }

        // 测试不匹配的字段
        assertFalse(matcher.matches("field20"));
        assertFalse(matcher.matches("field21"));
    }

    // ==================== 字段名内部化测试 ====================

    @Test
    public void testFieldNameInterning() {
        // Arrange
        FieldMatcher matcher = new FieldMatcher("name", "age");
        int poolSizeBefore = FieldMatcher.getFieldNamePoolSize();

        // Act - 多次匹配相同的字段名
        matcher.matches("name");
        matcher.matches("name");
        matcher.matches("name");

        // Assert - 池大小应该保持不变（因为字段名已经内部化）
        int poolSizeAfter = FieldMatcher.getFieldNamePoolSize();
        assertEquals(poolSizeBefore, poolSizeAfter);
    }

    @Test
    public void testFieldNameInterning_NewField() {
        // Arrange
        FieldMatcher matcher = new FieldMatcher("field1", "field2");
        int poolSizeBefore = FieldMatcher.getFieldNamePoolSize();

        // Act - 匹配新的字段名
        matcher.matches("field3");

        // Assert - 池大小应该增加
        int poolSizeAfter = FieldMatcher.getFieldNamePoolSize();
        assertEquals(poolSizeBefore + 1, poolSizeAfter);
    }

    @Test
    public void testFieldNameInterning_MultipleMatchers() {
        // Arrange
        FieldMatcher matcher1 = new FieldMatcher("name", "age");
        FieldMatcher matcher2 = new FieldMatcher("name", "email");

        // Act
        matcher1.matches("name");
        matcher2.matches("name");

        // Assert - 两个 matcher 应该共享相同的内部化字符串
        assertTrue(matcher1.matches("name"));
        assertTrue(matcher2.matches("name"));
    }

    // ==================== 目标字段计数测试 ====================

    @Test
    public void testGetTargetFieldCount_SmallSet() {
        // Arrange
        FieldMatcher matcher = new FieldMatcher("f1", "f2", "f3");

        // Act & Assert
        assertEquals(3, matcher.getTargetFieldCount());
    }

    @Test
    public void testGetTargetFieldCount_LargeSet() {
        // Arrange
        FieldMatcher matcher = new FieldMatcher(
            "f1", "f2", "f3", "f4", "f5", "f6", "f7", "f8", "f9", "f10"
        );

        // Act & Assert
        assertEquals(10, matcher.getTargetFieldCount());
    }

    @Test
    public void testGetTargetFieldCount_SingleField() {
        // Arrange
        FieldMatcher matcher = new FieldMatcher("singleField");

        // Act & Assert
        assertEquals(1, matcher.getTargetFieldCount());
    }

    // ==================== 字段名池管理测试 ====================

    @Test
    public void testClearFieldNamePool() {
        // Arrange
        FieldMatcher matcher = new FieldMatcher("field1", "field2");
        matcher.matches("field1");
        int poolSizeBefore = FieldMatcher.getFieldNamePoolSize();
        assertTrue(poolSizeBefore > 0);

        // Act
        FieldMatcher.clearFieldNamePool();

        // Assert
        assertEquals(0, FieldMatcher.getFieldNamePoolSize());
    }

    @Test
    public void testGetFieldNamePoolSize() {
        // Arrange
        FieldMatcher matcher = new FieldMatcher("field1", "field2", "field3");

        // Act
        int poolSize = FieldMatcher.getFieldNamePoolSize();

        // Assert
        assertTrue(poolSize >= 3); // 至少有 3 个字段名
    }

    // ==================== 边界情况测试 ====================

    @Test
    public void testMatches_EmptyString() {
        // Arrange
        FieldMatcher matcher = new FieldMatcher("field1", "field2");

        // Act & Assert
        assertFalse(matcher.matches(""));
    }

    @Test
    public void testMatches_WhitespaceField() {
        // Arrange
        FieldMatcher matcher = new FieldMatcher("  ", "field1");

        // Act & Assert
        assertTrue(matcher.matches("  "));
        assertFalse(matcher.matches(" "));
    }

    @Test
    public void testMatches_CaseSensitive() {
        // Arrange
        FieldMatcher matcher = new FieldMatcher("Field1", "field2");

        // Act & Assert
        assertTrue(matcher.matches("Field1"));
        assertFalse(matcher.matches("field1")); // 大小写敏感
        assertFalse(matcher.matches("FIELD1"));
    }

    @Test
    public void testMatches_SpecialCharacters() {
        // Arrange
        FieldMatcher matcher = new FieldMatcher("field_1", "field-2", "field.3");

        // Act & Assert
        assertTrue(matcher.matches("field_1"));
        assertTrue(matcher.matches("field-2"));
        assertTrue(matcher.matches("field.3"));
    }

    @Test
    public void testMatches_UnicodeFields() {
        // Arrange
        FieldMatcher matcher = new FieldMatcher("姓名", "年龄", "メール");

        // Act & Assert
        assertTrue(matcher.matches("姓名"));
        assertTrue(matcher.matches("年龄"));
        assertTrue(matcher.matches("メール"));
        assertFalse(matcher.matches("名前"));
    }

    // ==================== 性能相关测试 ====================

    @Test
    public void testPerformance_SmallVsLargeFieldSet() {
        // Arrange
        FieldMatcher smallMatcher = new FieldMatcher("f1", "f2", "f3");
        FieldMatcher largeMatcher = new FieldMatcher(
            "f1", "f2", "f3", "f4", "f5", "f6", "f7", "f8", "f9", "f10"
        );

        // Act & Assert - 验证策略选择正确
        assertTrue(smallMatcher.isUsingArraySearch());
        assertFalse(largeMatcher.isUsingArraySearch());

        // 两者都应该能正确匹配
        assertTrue(smallMatcher.matches("f1"));
        assertTrue(largeMatcher.matches("f1"));
    }
}
