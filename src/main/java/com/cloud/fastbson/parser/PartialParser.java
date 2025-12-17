package com.cloud.fastbson.parser;

import com.cloud.fastbson.document.BsonArray;
import com.cloud.fastbson.document.BsonDocument;
import com.cloud.fastbson.handler.TypeHandler;
import com.cloud.fastbson.matcher.FieldMatcher;
import com.cloud.fastbson.reader.BsonReader;
import com.cloud.fastbson.skipper.ValueSkipper;
import com.cloud.fastbson.util.BsonType;
import com.cloud.fastbson.util.ObjectPool;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 部分字段解析器，用于从 BSON 文档中提取指定字段。
 *
 * <p>核心特性：
 * <ul>
 *   <li>只解析需要的字段，跳过其他字段</li>
 *   <li>提前退出：找到所有目标字段后立即停止解析</li>
 *   <li>性能优化：避免解析整个文档</li>
 * </ul>
 *
 * <p><b>注意：</b>此类返回Map&lt;String, Object&gt;会产生装箱开销。
 * 对于追求极致性能的场景，建议直接使用 {@link com.cloud.fastbson.FastBson#parse(byte[])}
 * 返回的 {@link BsonDocument}，它提供零拷贝、零装箱的lazy parsing。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 创建解析器
 * PartialParser parser = new PartialParser("name", "age", "email");
 * parser.setEarlyExit(true);
 *
 * // 解析 BSON 数据
 * byte[] bsonData = ...;
 * Map<String, Object> result = parser.parse(bsonData);
 *
 * // 获取字段值
 * String name = (String) result.get("name");
 * Integer age = (Integer) result.get("age");
 * }</pre>
 *
 * @author FastBSON
 * @since 1.0.0
 */
public class PartialParser {

    /**
     * 字段匹配器
     */
    private final FieldMatcher fieldMatcher;

    /**
     * 类型处理器（用于解析匹配的字段）
     */
    private final TypeHandler typeHandler;

    /**
     * 是否启用提前退出优化
     */
    private boolean earlyExit;

    /**
     * 构造函数（使用字段名数组）
     *
     * @param targetFields 目标字段名数组
     */
    public PartialParser(String... targetFields) {
        if (targetFields == null || targetFields.length == 0) {
            throw new IllegalArgumentException("Target fields cannot be null or empty");
        }
        this.fieldMatcher = new FieldMatcher(targetFields);
        this.typeHandler = new TypeHandler();
        this.earlyExit = true; // 默认启用提前退出
    }

    /**
     * 构造函数（使用字段名集合）
     *
     * @param targetFields 目标字段名集合
     */
    public PartialParser(Set<String> targetFields) {
        if (targetFields == null || targetFields.isEmpty()) {
            throw new IllegalArgumentException("Target fields cannot be null or empty");
        }
        this.fieldMatcher = new FieldMatcher(targetFields);
        this.typeHandler = new TypeHandler();
        this.earlyExit = true;
    }

    /**
     * 设置是否启用提前退出
     *
     * @param earlyExit true 启用，false 禁用
     */
    public void setEarlyExit(boolean earlyExit) {
        this.earlyExit = earlyExit;
    }

    /**
     * 是否启用提前退出
     *
     * @return true 启用，false 禁用
     */
    public boolean isEarlyExitEnabled() {
        return earlyExit;
    }

    /**
     * 解析 BSON 数据，提取目标字段
     *
     * @param bsonData BSON 字节数组
     * @return 包含目标字段的 Map
     */
    public Map<String, Object> parse(byte[] bsonData) {
        if (bsonData == null || bsonData.length < 5) {
            throw new IllegalArgumentException("Invalid BSON data");
        }

        BsonReader reader = ObjectPool.borrowReader(bsonData);
        return parseDocument(reader);
    }

    /**
     * 解析 BSON 文档
     *
     * @param reader BsonReader 实例
     * @return 包含目标字段的 Map
     */
    private Map<String, Object> parseDocument(BsonReader reader) {
        // 读取文档长度
        reader.readInt32();

        // 目标字段数量（用于提前退出判断和容量估算）
        int targetFieldCount = fieldMatcher.getTargetFieldCount();

        // Phase 3 优化：根据目标字段数量预分配容量，避免 rehash
        // 使用负载因子 0.75 计算初始容量
        int initialCapacity = (int)(targetFieldCount / 0.75) + 1;
        Map<String, Object> result = new HashMap<String, Object>(initialCapacity);

        // 已找到的字段数量（用于提前退出）
        int foundCount = 0;

        // 创建 ValueSkipper（用于跳过不需要的字段）
        ValueSkipper skipper = new ValueSkipper(reader);

        // 遍历文档元素
        while (true) {
            // 读取类型码
            byte type = reader.readByte();

            // 如果遇到文档结束标记（0x00），退出循环
            if (type == 0x00) {
                break;
            }

            // 读取字段名
            String fieldName = reader.readCString();

            // 判断是否为目标字段
            if (fieldMatcher.matches(fieldName)) {
                // 解析字段值 (直接使用parser，避免装箱转换)
                Object value = typeHandler.getParsedValue(reader, type);
                // 对于BsonDocument/BsonArray，需要转换为普通Object以保持API兼容
                if (value instanceof BsonDocument) {
                    // 暂时保留装箱行为以保持API兼容性
                    // TODO: 考虑提供返回BsonDocument的高性能API
                    value = convertDocumentToMap((BsonDocument) value);
                } else if (value instanceof BsonArray) {
                    value = convertArrayToList((BsonArray) value);
                }
                result.put(fieldName, value);

                // 增加已找到的字段计数
                foundCount++;

                // 提前退出：如果已找到所有目标字段，立即返回
                if (earlyExit && foundCount >= targetFieldCount) {
                    break;
                }
            } else {
                // 跳过不需要的字段值
                skipper.skipValue(type);
            }
        }

        return result;
    }

    /**
     * 获取目标字段数量
     *
     * @return 目标字段数量
     */
    public int getTargetFieldCount() {
        return fieldMatcher.getTargetFieldCount();
    }

    /**
     * 将BsonDocument转换为Map (用于保持API兼容性)
     */
    private Map<String, Object> convertDocumentToMap(BsonDocument doc) {
        // Phase 3 优化：根据文档字段数量预分配容量
        int fieldCount = doc.size();
        int initialCapacity = (int)(fieldCount / 0.75) + 1;
        Map<String, Object> map = new HashMap<String, Object>(initialCapacity);
        for (String fieldName : doc.fieldNames()) {
            Object value = doc.get(fieldName);
            // 递归转换嵌套文档和数组
            if (value instanceof BsonDocument) {
                value = convertDocumentToMap((BsonDocument) value);
            } else if (value instanceof BsonArray) {
                value = convertArrayToList((BsonArray) value);
            }
            map.put(fieldName, value);
        }
        return map;
    }

    /**
     * 将BsonArray转换为List (用于保持API兼容性)
     */
    private java.util.List<Object> convertArrayToList(BsonArray array) {
        java.util.List<Object> list = new java.util.ArrayList<Object>(array.size());
        for (int i = 0; i < array.size(); i++) {
            Object value = array.get(i);
            // 递归转换嵌套文档和数组
            if (value instanceof BsonDocument) {
                value = convertDocumentToMap((BsonDocument) value);
            } else if (value instanceof BsonArray) {
                value = convertArrayToList((BsonArray) value);
            }
            list.add(value);
        }
        return list;
    }
}
