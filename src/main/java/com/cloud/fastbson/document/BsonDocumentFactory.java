package com.cloud.fastbson.document;

/**
 * BSON文档工厂接口
 *
 * <p>用于创建Document和Builder实例，支持不同的实现策略。
 *
 * <p>FastBSON提供两种实现：
 * <ul>
 *   <li><b>Fast实现（默认）</b>: 使用fastutil的primitive maps，零装箱，性能最优，需要fastutil依赖</li>
 *   <li><b>Simple实现</b>: 零依赖，使用Union类型（BsonValue），性能次优但仍优于装箱</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 使用默认工厂（Fast实现）
 * BsonDocumentFactory factory = BsonDocumentFactories.getDefault();
 * BsonDocumentBuilder builder = factory.newDocumentBuilder();
 * builder.putInt32("age", 25);
 * BsonDocument doc = builder.build();
 *
 * // 或者显式选择Simple实现（零依赖）
 * TypeHandler.setDocumentFactory(SimpleBsonDocumentFactory.INSTANCE);
 * }</pre>
 *
 * @see com.cloud.fastbson.document.simple.SimpleBsonDocumentFactory
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
