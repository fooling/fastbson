package com.cloud.fastbson.parser;

import com.cloud.fastbson.handler.TypeHandler;
import com.cloud.fastbson.matcher.FieldMatcher;
import com.cloud.fastbson.reader.BsonReader;
import com.cloud.fastbson.skipper.ValueSkipper;
import com.cloud.fastbson.util.BsonType;

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

        BsonReader reader = new BsonReader(bsonData);
        return parseDocument(reader);
    }

    /**
     * 解析 BSON 文档
     *
     * @param reader BsonReader 实例
     * @return 包含目标字段的 Map
     */
    private Map<String, Object> parseDocument(BsonReader reader) {
        // 创建结果 Map
        int targetFieldCount = fieldMatcher.getTargetFieldCount();
        Map<String, Object> result = new HashMap<String, Object>(
            (int) (targetFieldCount / 0.75) + 1
        );

        // 读取文档长度
        reader.readInt32();

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
                // 解析字段值
                Object value = typeHandler.parseValue(reader, type);
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
}
