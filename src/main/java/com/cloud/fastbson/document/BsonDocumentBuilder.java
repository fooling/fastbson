package com.cloud.fastbson.document;

/**
 * BSON文档构建器接口
 *
 * <p>用于在解析时高效构建文档，避免中间对象分配。
 *
 * <p>使用方式：
 * <pre>{@code
 * BsonDocumentBuilder builder = factory.newDocumentBuilder();
 * builder.putInt32("age", 25);
 * builder.putString("name", "Alice");
 * builder.putDouble("score", 95.5);
 * BsonDocument doc = builder.build();
 * }</pre>
 *
 * <p>注意：
 * <ul>
 *   <li>build()后Builder失效，不能继续使用</li>
 *   <li>如需复用Builder，调用reset()重置</li>
 *   <li>支持链式调用</li>
 * </ul>
 *
 * @since 1.0
 */
public interface BsonDocumentBuilder {

    // ==================== Primitive类型添加 (无装箱) ====================

    /**
     * 添加Int32字段
     *
     * @param fieldName 字段名
     * @param value 字段值（primitive int，无装箱）
     * @return this (支持链式调用)
     */
    BsonDocumentBuilder putInt32(String fieldName, int value);

    /**
     * 添加Int64字段
     *
     * @param fieldName 字段名
     * @param value 字段值（primitive long，无装箱）
     * @return this
     */
    BsonDocumentBuilder putInt64(String fieldName, long value);

    /**
     * 添加Double字段
     *
     * @param fieldName 字段名
     * @param value 字段值（primitive double，无装箱）
     * @return this
     */
    BsonDocumentBuilder putDouble(String fieldName, double value);

    /**
     * 添加Boolean字段
     *
     * @param fieldName 字段名
     * @param value 字段值（primitive boolean，无装箱）
     * @return this
     */
    BsonDocumentBuilder putBoolean(String fieldName, boolean value);

    // ==================== 引用类型添加 ====================

    /**
     * 添加String字段
     *
     * @param fieldName 字段名
     * @param value 字段值
     * @return this
     */
    BsonDocumentBuilder putString(String fieldName, String value);

    /**
     * 添加嵌套Document
     *
     * @param fieldName 字段名
     * @param value 嵌套文档
     * @return this
     */
    BsonDocumentBuilder putDocument(String fieldName, BsonDocument value);

    /**
     * 添加Array
     *
     * @param fieldName 字段名
     * @param value 数组
     * @return this
     */
    BsonDocumentBuilder putArray(String fieldName, BsonArray value);

    /**
     * 添加ObjectId
     *
     * @param fieldName 字段名
     * @param hexString ObjectId的十六进制字符串表示（24字符）
     * @return this
     */
    BsonDocumentBuilder putObjectId(String fieldName, String hexString);

    /**
     * 添加DateTime
     *
     * @param fieldName 字段名
     * @param timestamp UTC datetime的毫秒时间戳
     * @return this
     */
    BsonDocumentBuilder putDateTime(String fieldName, long timestamp);

    /**
     * 添加Null字段
     *
     * @param fieldName 字段名
     * @return this
     */
    BsonDocumentBuilder putNull(String fieldName);

    /**
     * 添加Binary数据
     *
     * @param fieldName 字段名
     * @param subtype 二进制子类型
     * @param data 二进制数据
     * @return this
     */
    BsonDocumentBuilder putBinary(String fieldName, byte subtype, byte[] data);

    /**
     * 添加复杂类型（Regex, DBPointer, Timestamp, Decimal128等）
     *
     * @param fieldName 字段名
     * @param type BSON类型字节
     * @param value 值对象
     * @return this
     */
    BsonDocumentBuilder putComplex(String fieldName, byte type, Object value);

    // ==================== 构建 ====================

    /**
     * 构建最终的BsonDocument
     *
     * <p>注意：build()后Builder失效，不能继续使用
     *
     * @return 不可变的BsonDocument
     */
    BsonDocument build();

    /**
     * 预估字段数量（用于优化容量）
     *
     * <p>在添加字段前调用此方法可以避免内部数据结构扩容
     *
     * @param fieldCount 预估的字段数量
     * @return this
     */
    BsonDocumentBuilder estimateSize(int fieldCount);

    /**
     * 重置Builder（复用）
     *
     * <p>清除所有已添加的字段，可重新使用此Builder
     */
    void reset();
}
