package com.cloud.fastbson.matcher;

import com.cloud.fastbson.util.StringPool;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 字段匹配器，用于判断 BSON 字段是否为目标字段。
 *
 * <p>性能优化策略：
 * <ul>
 *   <li>小字段集（&lt;10）：使用数组线性查找</li>
 *   <li>大字段集（≥10）：使用 HashMap 查找</li>
 *   <li>字段名内部化：使用字符串池减少重复对象</li>
 * </ul>
 *
 * <p>线程安全性：
 * <ul>
 *   <li>FieldMatcher 实例本身是线程安全的（不可变）</li>
 *   <li>字段名内部化池是线程安全的（使用 ConcurrentHashMap）</li>
 * </ul>
 *
 * @author FastBSON
 * @since 1.0.0
 */
public class FieldMatcher {

    /**
     * 小字段集阈值：字段数量小于此值时使用数组查找
     */
    private static final int SMALL_SET_THRESHOLD = 10;

    /**
     * 目标字段数组（用于小字段集线性查找）
     */
    private final String[] fieldArray;

    /**
     * 目标字段映射（用于大字段集 HashMap 查找）
     */
    private final Map<String, Boolean> fieldMap;

    /**
     * 目标字段数量
     */
    private final int targetFieldCount;

    /**
     * 是否使用数组查找策略
     */
    private final boolean useArraySearch;

    /**
     * 构造函数（使用字段名集合）
     *
     * @param targetFields 目标字段名集合
     */
    public FieldMatcher(Set<String> targetFields) {
        if (targetFields == null || targetFields.isEmpty()) {
            throw new IllegalArgumentException("Target fields cannot be null or empty");
        }

        this.targetFieldCount = targetFields.size();
        this.useArraySearch = targetFieldCount < SMALL_SET_THRESHOLD;

        if (useArraySearch) {
            // 小字段集：使用数组
            this.fieldArray = new String[targetFieldCount];
            int index = 0;
            for (String field : targetFields) {
                this.fieldArray[index++] = internFieldName(field);
            }
            this.fieldMap = null;
        } else {
            // 大字段集：使用 HashMap
            this.fieldArray = null;
            this.fieldMap = new HashMap<String, Boolean>((int) (targetFieldCount / 0.75) + 1);
            for (String field : targetFields) {
                this.fieldMap.put(internFieldName(field), Boolean.TRUE);
            }
        }
    }

    /**
     * 构造函数（使用字段名数组）
     *
     * @param targetFields 目标字段名数组
     */
    public FieldMatcher(String... targetFields) {
        if (targetFields == null || targetFields.length == 0) {
            throw new IllegalArgumentException("Target fields cannot be null or empty");
        }

        this.targetFieldCount = targetFields.length;
        this.useArraySearch = targetFieldCount < SMALL_SET_THRESHOLD;

        if (useArraySearch) {
            // 小字段集：使用数组
            this.fieldArray = new String[targetFieldCount];
            for (int i = 0; i < targetFieldCount; i++) {
                this.fieldArray[i] = internFieldName(targetFields[i]);
            }
            this.fieldMap = null;
        } else {
            // 大字段集：使用 HashMap
            this.fieldArray = null;
            this.fieldMap = new HashMap<String, Boolean>((int) (targetFieldCount / 0.75) + 1);
            for (String field : targetFields) {
                this.fieldMap.put(internFieldName(field), Boolean.TRUE);
            }
        }
    }

    /**
     * 判断字段是否匹配目标字段
     *
     * @param fieldName 字段名
     * @return 如果匹配返回 true，否则返回 false
     */
    public boolean matches(String fieldName) {
        if (fieldName == null) {
            return false;
        }

        // 内部化字段名（利用字符串池）
        String internedFieldName = internFieldName(fieldName);

        if (useArraySearch) {
            // 数组线性查找（使用 == 比较，因为已内部化）
            for (String target : fieldArray) {
                if (target == internedFieldName) {
                    return true;
                }
            }
            return false;
        } else {
            // HashMap 查找
            return fieldMap.containsKey(internedFieldName);
        }
    }

    /**
     * 获取目标字段数量
     *
     * @return 目标字段数量
     */
    public int getTargetFieldCount() {
        return targetFieldCount;
    }

    /**
     * 判断是否使用数组查找策略
     *
     * @return 如果使用数组查找返回 true，否则返回 false
     */
    public boolean isUsingArraySearch() {
        return useArraySearch;
    }

    /**
     * 内部化字段名（使用字符串池）
     *
     * <p>字段名通常会重复出现在多个文档中，通过内部化可以：
     * <ul>
     *   <li>减少内存占用（重用字符串对象）</li>
     *   <li>提高比较效率（使用 == 而非 equals）</li>
     * </ul>
     *
     * <p>注意：此方法假定 fieldName 非 null，因为所有调用方都已进行 null 检查。
     *
     * @param fieldName 原始字段名（非 null）
     * @return 内部化后的字段名
     */
    private static String internFieldName(String fieldName) {
        return StringPool.intern(fieldName);
    }

    /**
     * 清空字段名内部化池（用于测试或内存清理）
     */
    public static void clearFieldNamePool() {
        StringPool.clear();
    }

    /**
     * 获取字段名池大小（用于监控）
     *
     * @return 字段名池中的字符串数量
     */
    public static int getFieldNamePoolSize() {
        return StringPool.getPoolSize();
    }
}
