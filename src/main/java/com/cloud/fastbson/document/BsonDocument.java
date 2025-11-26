package com.cloud.fastbson.document;

import java.util.Map;
import java.util.Set;

/**
 * BSON文档抽象接口，提供类型安全的字段访问。
 *
 * <p>设计目标：
 * <ul>
 *   <li>支持primitive类型访问，避免装箱</li>
 *   <li>隐藏底层存储实现细节</li>
 *   <li>提供统一的API，兼容Simple和Fast实现</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * BsonDocument doc = parser.parseToBsonDocument(bsonData);
 *
 * // Primitive访问（无装箱）
 * int age = doc.getInt32("age");
 * long timestamp = doc.getInt64("timestamp");
 * double score = doc.getDouble("score");
 * boolean active = doc.getBoolean("active");
 *
 * // 引用类型访问
 * String name = doc.getString("name");
 * BsonDocument address = doc.getDocument("address");
 * BsonArray tags = doc.getArray("tags");
 * }</pre>
 *
 * @since 1.0
 */
public interface BsonDocument {

    // ==================== 类型判断 ====================

    /**
     * 判断字段是否存在
     *
     * @param fieldName 字段名
     * @return 如果字段存在返回true，否则返回false
     */
    boolean contains(String fieldName);

    /**
     * 获取字段的BSON类型
     *
     * @param fieldName 字段名
     * @return BSON类型码（见{@link com.cloud.fastbson.util.BsonType}），如果字段不存在返回0
     */
    byte getType(String fieldName);

    /**
     * 判断字段是否为null
     *
     * @param fieldName 字段名
     * @return 如果字段存在且为null返回true
     */
    boolean isNull(String fieldName);

    /**
     * 获取字段数量
     *
     * @return 文档中的字段数量
     */
    int size();

    /**
     * 获取所有字段名
     *
     * @return 字段名集合（不可修改）
     */
    Set<String> fieldNames();

    /**
     * 判断文档是否为空
     *
     * @return 如果文档没有任何字段返回true
     */
    boolean isEmpty();

    // ==================== Primitive类型访问 (无装箱) ====================

    /**
     * 获取Int32字段值
     *
     * @param fieldName 字段名
     * @return 字段值（primitive int，无装箱）
     * @throws ClassCastException 如果类型不匹配
     * @throws NullPointerException 如果字段不存在
     */
    int getInt32(String fieldName);

    /**
     * 获取Int32字段值，字段不存在时返回默认值
     *
     * @param fieldName 字段名
     * @param defaultValue 默认值
     * @return 字段值，如果字段不存在返回defaultValue
     */
    int getInt32(String fieldName, int defaultValue);

    /**
     * 获取Int64字段值
     *
     * @param fieldName 字段名
     * @return 字段值（primitive long，无装箱）
     * @throws ClassCastException 如果类型不匹配
     * @throws NullPointerException 如果字段不存在
     */
    long getInt64(String fieldName);

    /**
     * 获取Int64字段值，字段不存在时返回默认值
     *
     * @param fieldName 字段名
     * @param defaultValue 默认值
     * @return 字段值，如果字段不存在返回defaultValue
     */
    long getInt64(String fieldName, long defaultValue);

    /**
     * 获取Double字段值
     *
     * @param fieldName 字段名
     * @return 字段值（primitive double，无装箱）
     * @throws ClassCastException 如果类型不匹配
     * @throws NullPointerException 如果字段不存在
     */
    double getDouble(String fieldName);

    /**
     * 获取Double字段值，字段不存在时返回默认值
     *
     * @param fieldName 字段名
     * @param defaultValue 默认值
     * @return 字段值，如果字段不存在返回defaultValue
     */
    double getDouble(String fieldName, double defaultValue);

    /**
     * 获取Boolean字段值
     *
     * @param fieldName 字段名
     * @return 字段值（primitive boolean，无装箱）
     * @throws ClassCastException 如果类型不匹配
     * @throws NullPointerException 如果字段不存在
     */
    boolean getBoolean(String fieldName);

    /**
     * 获取Boolean字段值，字段不存在时返回默认值
     *
     * @param fieldName 字段名
     * @param defaultValue 默认值
     * @return 字段值，如果字段不存在返回defaultValue
     */
    boolean getBoolean(String fieldName, boolean defaultValue);

    // ==================== 引用类型访问 ====================

    /**
     * 获取String字段值
     *
     * @param fieldName 字段名
     * @return String值，字段不存在返回null
     */
    String getString(String fieldName);

    /**
     * 获取String字段值，字段不存在时返回默认值
     *
     * @param fieldName 字段名
     * @param defaultValue 默认值
     * @return 字段值，如果字段不存在返回defaultValue
     */
    String getString(String fieldName, String defaultValue);

    /**
     * 获取嵌套Document
     *
     * @param fieldName 字段名
     * @return 嵌套文档，字段不存在返回null
     */
    BsonDocument getDocument(String fieldName);

    /**
     * 获取Array
     *
     * @param fieldName 字段名
     * @return 数组，字段不存在返回null
     */
    BsonArray getArray(String fieldName);

    /**
     * 获取ObjectId (以hex string形式)
     *
     * @param fieldName 字段名
     * @return ObjectId的十六进制字符串表示，字段不存在返回null
     */
    String getObjectId(String fieldName);

    /**
     * 获取DateTime (以timestamp形式)
     *
     * @param fieldName 字段名
     * @return UTC datetime的毫秒时间戳
     * @throws NullPointerException 如果字段不存在
     */
    long getDateTime(String fieldName);

    /**
     * 获取DateTime，字段不存在时返回默认值
     *
     * @param fieldName 字段名
     * @param defaultValue 默认值
     * @return 时间戳，如果字段不存在返回defaultValue
     */
    long getDateTime(String fieldName, long defaultValue);

    // ==================== 通用访问 (兼容旧API) ====================

    /**
     * 获取字段值（类型擦除）
     *
     * <p>注意：简单类型会被装箱
     * <ul>
     *   <li>Int32 → Integer</li>
     *   <li>Int64 → Long</li>
     *   <li>Double → Double</li>
     *   <li>Boolean → Boolean</li>
     * </ul>
     *
     * @param fieldName 字段名
     * @return 字段值（装箱后的对象），字段不存在返回null
     * @deprecated 使用类型安全的getXxx()方法避免装箱
     */
    @Deprecated
    Object get(String fieldName);


    // ==================== 调试和序列化 ====================

    /**
     * 转换为JSON字符串 (用于调试)
     *
     * @return JSON格式的文档字符串
     */
    String toJson();

    /**
     * 序列化为BSON字节数组
     *
     * @return BSON格式的字节数组
     */
    byte[] toBson();
}
