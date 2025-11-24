package com.cloud.fastbson.skipper;

import com.cloud.fastbson.reader.BsonReader;
import com.cloud.fastbson.util.BsonType;
import com.cloud.fastbson.exception.InvalidBsonTypeException;

/**
 * BSON 值跳过器，用于快速跳过不需要的字段值。
 *
 * <p>性能优化策略：
 * <ul>
 *   <li>固定长度类型：使用查找表 O(1) 跳过</li>
 *   <li>变长类型：读取长度前缀后直接跳过</li>
 *   <li>嵌套文档：利用文档长度直接跳过，无需递归解析</li>
 * </ul>
 *
 * <p>BSON 类型分类：
 * <ul>
 *   <li>固定长度：Double(8), Boolean(1), DateTime(8), ObjectId(12), Int32(4), Int64(8), Timestamp(8), Decimal128(16)</li>
 *   <li>变长类型：String, Binary, Document, Array, JavaScript, JavaScriptWithScope</li>
 *   <li>零长度类型：Null, MinKey, MaxKey</li>
 *   <li>特殊类型：Regex (2个C-string), DBPointer (string + 12 bytes)</li>
 * </ul>
 *
 * @author FastBSON
 * @since 1.0.0
 */
public class ValueSkipper {

    /**
     * 固定长度类型查找表（索引为类型码，值为字节长度）
     * -1 表示非固定长度类型
     */
    private static final int[] FIXED_LENGTH_TABLE = new int[256];

    static {
        // 初始化为 -1（表示非固定长度）
        for (int i = 0; i < 256; i++) {
            FIXED_LENGTH_TABLE[i] = -1;
        }

        // 固定长度类型
        FIXED_LENGTH_TABLE[BsonType.DOUBLE & 0xFF] = 8;          // 0x01: Double
        FIXED_LENGTH_TABLE[BsonType.OBJECT_ID & 0xFF] = 12;      // 0x07: ObjectId
        FIXED_LENGTH_TABLE[BsonType.BOOLEAN & 0xFF] = 1;         // 0x08: Boolean
        FIXED_LENGTH_TABLE[BsonType.DATE_TIME & 0xFF] = 8;       // 0x09: UTC datetime
        FIXED_LENGTH_TABLE[BsonType.INT32 & 0xFF] = 4;           // 0x10: int32
        FIXED_LENGTH_TABLE[BsonType.TIMESTAMP & 0xFF] = 8;       // 0x11: Timestamp
        FIXED_LENGTH_TABLE[BsonType.INT64 & 0xFF] = 8;           // 0x12: int64
        FIXED_LENGTH_TABLE[BsonType.DECIMAL128 & 0xFF] = 16;     // 0x13: Decimal128

        // 零长度类型
        FIXED_LENGTH_TABLE[BsonType.NULL & 0xFF] = 0;            // 0x0A: Null
        FIXED_LENGTH_TABLE[BsonType.MIN_KEY & 0xFF] = 0;         // 0xFF: MinKey
        FIXED_LENGTH_TABLE[BsonType.MAX_KEY & 0xFF] = 0;         // 0x7F: MaxKey
        FIXED_LENGTH_TABLE[BsonType.UNDEFINED & 0xFF] = 0;       // 0x06: Undefined (deprecated)
    }

    /**
     * BsonReader 实例（用于读取和跳过）
     */
    private final BsonReader reader;

    /**
     * 构造函数
     *
     * @param reader BsonReader 实例
     */
    public ValueSkipper(BsonReader reader) {
        if (reader == null) {
            throw new IllegalArgumentException("BsonReader cannot be null");
        }
        this.reader = reader;
    }

    /**
     * 根据类型跳过值
     *
     * @param type BSON 类型码
     * @throws InvalidBsonTypeException 如果类型码无效
     */
    public void skipValue(byte type) {
        // 1. 尝试使用固定长度表
        int fixedLength = FIXED_LENGTH_TABLE[type & 0xFF];
        if (fixedLength >= 0) {
            reader.skip(fixedLength);
            return;
        }

        // 2. 处理变长类型
        switch (type) {
            case BsonType.STRING:
                skipString();
                break;

            case BsonType.DOCUMENT:
                skipDocument();
                break;

            case BsonType.ARRAY:
                skipArray();
                break;

            case BsonType.BINARY:
                skipBinary();
                break;

            case BsonType.REGEX:
                skipRegex();
                break;

            case BsonType.DB_POINTER:
                skipDBPointer();
                break;

            case BsonType.JAVASCRIPT:
                skipJavaScript();
                break;

            case BsonType.SYMBOL:
                skipSymbol();
                break;

            case BsonType.JAVASCRIPT_WITH_SCOPE:
                skipJavaScriptWithScope();
                break;

            default:
                throw new InvalidBsonTypeException(type);
        }
    }

    /**
     * 跳过 String 类型
     * 格式：int32 (length, 包括结尾的 0x00) + UTF-8 bytes + 0x00
     */
    private void skipString() {
        int length = reader.readInt32();
        reader.skip(length); // 包括结尾的 0x00
    }

    /**
     * 跳过 Document 类型
     * 格式：int32 (total length) + content + 0x00
     *
     * <p>优化：利用文档长度直接跳过，无需递归解析
     */
    private void skipDocument() {
        int length = reader.readInt32();
        reader.skip(length - 4); // 减去已读取的 4 字节长度字段
    }

    /**
     * 跳过 Array 类型
     * 格式：与 Document 相同
     */
    private void skipArray() {
        int length = reader.readInt32();
        reader.skip(length - 4);
    }

    /**
     * 跳过 Binary 类型
     * 格式：int32 (length) + subtype (1 byte) + bytes
     */
    private void skipBinary() {
        int length = reader.readInt32();
        reader.skip(1);        // subtype
        reader.skip(length);   // binary data
    }

    /**
     * 跳过 Regex 类型
     * 格式：pattern (C-string) + options (C-string)
     */
    private void skipRegex() {
        reader.readCString(); // pattern
        reader.readCString(); // options
    }

    /**
     * 跳过 DBPointer 类型
     * 格式：string + 12 bytes ObjectId
     */
    private void skipDBPointer() {
        int stringLength = reader.readInt32();
        reader.skip(stringLength); // string (包括 0x00)
        reader.skip(12);           // ObjectId
    }

    /**
     * 跳过 JavaScript 类型
     * 格式：与 String 相同
     */
    private void skipJavaScript() {
        int length = reader.readInt32();
        reader.skip(length);
    }

    /**
     * 跳过 Symbol 类型
     * 格式：与 String 相同
     */
    private void skipSymbol() {
        int length = reader.readInt32();
        reader.skip(length);
    }

    /**
     * 跳过 JavaScriptWithScope 类型
     * 格式：int32 (total length) + string (code) + document (scope)
     *
     * <p>优化：利用总长度直接跳过
     */
    private void skipJavaScriptWithScope() {
        int totalLength = reader.readInt32();
        reader.skip(totalLength - 4); // 减去已读取的 4 字节长度字段
    }

    /**
     * 获取固定长度类型的字节数（用于测试）
     *
     * @param type BSON 类型码
     * @return 固定长度字节数，-1 表示非固定长度类型
     */
    public static int getFixedLength(byte type) {
        return FIXED_LENGTH_TABLE[type & 0xFF];
    }
}
