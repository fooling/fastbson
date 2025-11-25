package com.cloud.fastbson.document;

import java.util.Iterator;
import java.util.List;

/**
 * BSON数组抽象接口，提供类型安全的元素访问。
 *
 * <p>使用示例：
 * <pre>{@code
 * BsonArray array = doc.getArray("scores");
 *
 * // 获取数组长度
 * int count = array.size();
 *
 * // Primitive访问（无装箱）
 * int firstScore = array.getInt32(0);
 * long timestamp = array.getInt64(1);
 * double average = array.getDouble(2);
 *
 * // 引用类型访问
 * String name = array.getString(0);
 * BsonDocument item = array.getDocument(1);
 *
 * // 迭代（装箱）
 * for (Object element : array) {
 *     // ...
 * }
 * }</pre>
 *
 * @since 1.0
 */
public interface BsonArray extends Iterable<Object> {

    /**
     * 数组长度
     *
     * @return 数组中元素的数量
     */
    int size();

    /**
     * 是否为空
     *
     * @return 如果数组没有元素返回true
     */
    boolean isEmpty();

    /**
     * 获取指定索引元素的BSON类型
     *
     * @param index 索引（从0开始）
     * @return BSON类型码，如果索引越界返回0
     */
    byte getType(int index);

    // ==================== Primitive类型访问 (无装箱) ====================

    /**
     * 获取Int32元素
     *
     * @param index 索引（从0开始）
     * @return 元素值（primitive int，无装箱）
     * @throws IndexOutOfBoundsException 如果索引越界
     * @throws ClassCastException 如果类型不匹配
     */
    int getInt32(int index);

    /**
     * 获取Int32元素，索引越界时返回默认值
     *
     * @param index 索引
     * @param defaultValue 默认值
     * @return 元素值，索引越界返回defaultValue
     */
    int getInt32(int index, int defaultValue);

    /**
     * 获取Int64元素
     *
     * @param index 索引
     * @return 元素值（primitive long，无装箱）
     * @throws IndexOutOfBoundsException 如果索引越界
     * @throws ClassCastException 如果类型不匹配
     */
    long getInt64(int index);

    /**
     * 获取Int64元素，索引越界时返回默认值
     *
     * @param index 索引
     * @param defaultValue 默认值
     * @return 元素值，索引越界返回defaultValue
     */
    long getInt64(int index, long defaultValue);

    /**
     * 获取Double元素
     *
     * @param index 索引
     * @return 元素值（primitive double，无装箱）
     * @throws IndexOutOfBoundsException 如果索引越界
     * @throws ClassCastException 如果类型不匹配
     */
    double getDouble(int index);

    /**
     * 获取Double元素，索引越界时返回默认值
     *
     * @param index 索引
     * @param defaultValue 默认值
     * @return 元素值，索引越界返回defaultValue
     */
    double getDouble(int index, double defaultValue);

    /**
     * 获取Boolean元素
     *
     * @param index 索引
     * @return 元素值（primitive boolean，无装箱）
     * @throws IndexOutOfBoundsException 如果索引越界
     * @throws ClassCastException 如果类型不匹配
     */
    boolean getBoolean(int index);

    /**
     * 获取Boolean元素，索引越界时返回默认值
     *
     * @param index 索引
     * @param defaultValue 默认值
     * @return 元素值，索引越界返回defaultValue
     */
    boolean getBoolean(int index, boolean defaultValue);

    // ==================== 引用类型访问 ====================

    /**
     * 获取String元素
     *
     * @param index 索引
     * @return String值，索引越界返回null
     */
    String getString(int index);

    /**
     * 获取String元素，索引越界时返回默认值
     *
     * @param index 索引
     * @param defaultValue 默认值
     * @return 元素值，索引越界返回defaultValue
     */
    String getString(int index, String defaultValue);

    /**
     * 获取嵌套Document
     *
     * @param index 索引
     * @return 嵌套文档，索引越界返回null
     */
    BsonDocument getDocument(int index);

    /**
     * 获取嵌套Array
     *
     * @param index 索引
     * @return 嵌套数组，索引越界返回null
     */
    BsonArray getArray(int index);

    // ==================== 通用访问 ====================

    /**
     * 获取元素（类型擦除，会装箱）
     *
     * @param index 索引
     * @return 元素值（装箱后的对象），索引越界返回null
     * @deprecated 使用类型安全方法
     */
    @Deprecated
    Object get(int index);

    /**
     * 迭代器（元素被装箱）
     *
     * @return 迭代器
     */
    @Override
    Iterator<Object> iterator();

    /**
     * 转换为Legacy List
     *
     * <p>注意：此方法会创建新的ArrayList并装箱所有primitive值
     *
     * @return List表示的数组，所有primitive值被装箱
     * @deprecated 使用BsonArray接口
     */
    @Deprecated
    List<Object> toLegacyList();

    /**
     * 转换为JSON字符串 (用于调试)
     *
     * @return JSON格式的数组字符串
     */
    String toJson();
}
