package com.cloud.fastbson.document;

/**
 * BSON数组构建器接口
 *
 * <p>用于高效构建数组，避免中间对象分配。
 *
 * <p>使用示例：
 * <pre>{@code
 * BsonArrayBuilder builder = factory.newArrayBuilder();
 * builder.addInt32(10);
 * builder.addString("hello");
 * builder.addDouble(3.14);
 * BsonArray array = builder.build();
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
public interface BsonArrayBuilder {

    // ==================== Primitive类型添加 (无装箱) ====================

    /**
     * 添加Int32元素
     *
     * @param value 元素值（primitive int，无装箱）
     * @return this (支持链式调用)
     */
    BsonArrayBuilder addInt32(int value);

    /**
     * 添加Int64元素
     *
     * @param value 元素值（primitive long，无装箱）
     * @return this
     */
    BsonArrayBuilder addInt64(long value);

    /**
     * 添加Double元素
     *
     * @param value 元素值（primitive double，无装箱）
     * @return this
     */
    BsonArrayBuilder addDouble(double value);

    /**
     * 添加Boolean元素
     *
     * @param value 元素值（primitive boolean，无装箱）
     * @return this
     */
    BsonArrayBuilder addBoolean(boolean value);

    // ==================== 引用类型添加 ====================

    /**
     * 添加String元素
     *
     * @param value 元素值
     * @return this
     */
    BsonArrayBuilder addString(String value);

    /**
     * 添加嵌套Document
     *
     * @param value 嵌套文档
     * @return this
     */
    BsonArrayBuilder addDocument(BsonDocument value);

    /**
     * 添加嵌套Array
     *
     * @param value 嵌套数组
     * @return this
     */
    BsonArrayBuilder addArray(BsonArray value);

    /**
     * 添加Null元素
     *
     * @return this
     */
    BsonArrayBuilder addNull();

    /**
     * 添加ObjectId
     *
     * @param hexString ObjectId的十六进制字符串表示（24字符）
     * @return this
     */
    BsonArrayBuilder addObjectId(String hexString);

    /**
     * 添加DateTime
     *
     * @param timestamp UTC datetime的毫秒时间戳
     * @return this
     */
    BsonArrayBuilder addDateTime(long timestamp);

    /**
     * 添加Binary数据
     *
     * @param subtype 二进制子类型
     * @param data 二进制数据
     * @return this
     */
    BsonArrayBuilder addBinary(byte subtype, byte[] data);

    // ==================== 构建 ====================

    /**
     * 构建最终的BsonArray
     *
     * <p>注意：build()后Builder失效，不能继续使用
     *
     * @return 不可变的BsonArray
     */
    BsonArray build();

    /**
     * 预估数组大小（用于优化容量）
     *
     * @param size 预估的元素数量
     * @return this
     */
    BsonArrayBuilder estimateSize(int size);

    /**
     * 重置Builder（复用）
     *
     * <p>清除所有已添加的元素，可重新使用此Builder
     */
    void reset();
}
