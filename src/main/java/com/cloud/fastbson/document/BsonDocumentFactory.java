package com.cloud.fastbson.document;

/**
 * BSON文档工厂接口
 *
 * <p>用于创建Document和Builder实例。
 *
 * <p>FastBSON使用FastBsonDocumentFactory实现（fastutil-based，零装箱，高性能）。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 使用默认工厂
 * BsonDocumentFactory factory = BsonDocumentFactories.getDefault();
 * BsonDocumentBuilder builder = factory.newDocumentBuilder();
 * builder.putInt32("age", 25);
 * BsonDocument doc = builder.build();
 * }</pre>
 *
 * @see com.cloud.fastbson.document.fast.FastBsonDocumentFactory
 * @since 1.0
 */
public interface BsonDocumentFactory {

    /**
     * 创建文档构建器
     *
     * @return 新的文档构建器实例
     */
    BsonDocumentBuilder newDocumentBuilder();

    /**
     * 创建数组构建器
     *
     * @return 新的数组构建器实例
     */
    BsonArrayBuilder newArrayBuilder();

    /**
     * 创建空文档
     *
     * @return 空的不可变文档
     */
    BsonDocument emptyDocument();

    /**
     * 创建空数组
     *
     * @return 空的不可变数组
     */
    BsonArray emptyArray();

    /**
     * 获取工厂名称（用于诊断）
     *
     * @return 工厂名称，例如 "Fast (fastutil-based)" 或 "Simple (Zero-Dependency)"
     */
    String getName();

    /**
     * 是否需要外部依赖
     *
     * @return 如果需要外部依赖（如fastutil）返回true，零依赖返回false
     */
    boolean requiresExternalDependencies();
}
