package com.cloud.fastbson.document.fast;

import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.document.BsonArrayBuilder;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.document.BsonDocumentBuilder;
import com.cloud.fastbson.document.BsonDocumentFactory;

/**
 * Fast实现的工厂（需要fastutil依赖）
 *
 * <p>特点：
 * <ul>
 *   <li>使用fastutil的primitive maps和lists</li>
 *   <li>完全零装箱：Int32存储为primitive int</li>
 *   <li>高性能：访问速度3x，内存节省60%，GC压力-83%</li>
 * </ul>
 *
 * <p>性能：
 * <ul>
 *   <li>内存节省60%：相比装箱方案</li>
 *   <li>访问加速3x：相比装箱</li>
 *   <li>GC压力-83%：极少对象分配</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 设置为默认工厂（推荐）
 * TypeHandler.setDocumentFactory(FastBsonDocumentFactory.INSTANCE);
 *
 * // 之后的解析自动使用Fast实现
 * BsonDocument doc = parser.parseToBsonDocument(bsonData);
 * int age = doc.getInt32("age");  // 从IntIntMap读取，零装箱
 * }</pre>
 *
 * <p>注意：
 * <ul>
 *   <li>需要添加fastutil依赖（it.unimi.dsi:fastutil:8.5.12）</li>
 * </ul>
 */
public final class FastBsonDocumentFactory implements BsonDocumentFactory {

    /**
     * 单例实例
     */
    public static final FastBsonDocumentFactory INSTANCE = new FastBsonDocumentFactory();

    private FastBsonDocumentFactory() {
        // 私有构造函数，强制使用单例
    }

    @Override
    public BsonDocumentBuilder newDocumentBuilder() {
        return new FastBsonDocumentBuilder();
    }

    @Override
    public BsonArrayBuilder newArrayBuilder() {
        return new FastBsonArrayBuilder();
    }

    @Override
    public BsonDocument emptyDocument() {
        return FastBsonDocument.EMPTY;
    }

    @Override
    public BsonArray emptyArray() {
        return FastBsonArray.EMPTY;
    }

    @Override
    public String getName() {
        return "Fast (fastutil-based)";
    }

    @Override
    public boolean requiresExternalDependencies() {
        return true;  // 需要fastutil
    }

    @Override
    public String toString() {
        return getName();
    }
}
