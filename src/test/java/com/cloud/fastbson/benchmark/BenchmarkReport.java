package com.cloud.fastbson.benchmark;

import java.util.List;

/**
 * Benchmark报告生成器
 */
public class BenchmarkReport {
    private static final String LINE = "=".repeat(100);
    private static final String TABLE_LINE = "─".repeat(96);

    /**
     * 生成完整的benchmark报告
     */
    public static String generate(List<BenchmarkResult> results) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n").append(LINE).append("\n");
        sb.append("                         FastBSON 性能基线测试报告\n");
        sb.append(LINE).append("\n\n");

        // 性能对比表
        generateComparisonTable(sb, results);

        // 详细结果
        sb.append("\n").append(LINE).append("\n");
        sb.append("                            详细测试结果\n");
        sb.append(LINE).append("\n");

        for (BenchmarkResult result : results) {
            generateDetailedResult(sb, result);
        }

        // 使用建议
        generateUsageRecommendations(sb);

        sb.append(LINE).append("\n");

        return sb.toString();
    }

    /**
     * 生成性能对比表
     */
    private static void generateComparisonTable(StringBuilder sb, List<BenchmarkResult> results) {
        sb.append("┌──────────────────────────────┬──────────────────────┬──────────┬──────────┬────────────┐\n");
        sb.append("│ 场景                          │ 实现方式              │ FastBSON │ MongoDB  │ 性能提升   │\n");
        sb.append("├──────────────────────────────┼──────────────────────┼──────────┼──────────┼────────────┤\n");

        for (BenchmarkResult result : results) {
            sb.append(String.format("│ %-28s │ %-20s │ %6d ms │ %6d ms │ %-10s │\n",
                truncate(result.getScenarioName(), 28),
                truncate(result.getFastbsonMode(), 20),
                result.getFastbsonTimeMs(),
                result.getMongoTimeMs(),
                result.getSpeedupFormatted()));
        }

        sb.append("└──────────────────────────────┴──────────────────────┴──────────┴──────────┴────────────┘\n");
    }

    /**
     * 生成详细结果
     */
    private static void generateDetailedResult(StringBuilder sb, BenchmarkResult result) {
        sb.append("\n");
        sb.append("📊 ").append(result.getScenarioName()).append("\n");
        sb.append("   ").append(TABLE_LINE).append("\n");
        sb.append(String.format("   实现方式: %s\n", result.getFastbsonMode()));
        sb.append(String.format("   场景描述: %s\n", result.getDescription()));
        sb.append(String.format("   FastBSON: %d ms\n", result.getFastbsonTimeMs()));
        sb.append(String.format("   MongoDB:  %d ms\n", result.getMongoTimeMs()));
        sb.append(String.format("   性能提升: %s\n", result.getSpeedupFormatted()));
        sb.append(String.format("   目标性能: %s\n", result.getTarget()));
        sb.append(String.format("   测试评级: %s\n", result.getGrade()));

        if (result.getNote() != null && !result.getNote().isEmpty()) {
            sb.append(String.format("   额外说明: %s\n", result.getNote()));
        }
    }

    /**
     * 生成使用建议
     */
    private static void generateUsageRecommendations(StringBuilder sb) {
        sb.append("\n").append(LINE).append("\n");
        sb.append("                              使用建议\n");
        sb.append(LINE).append("\n\n");

        sb.append("📌 Phase 1: HashMap 完整解析模式\n");
        sb.append("   场景: 50字段文档，完整解析 + 构建索引\n");
        sb.append("   性能: 3.5-4.0x vs MongoDB\n");
        sb.append("   ✓ 需要访问大部分字段（>50%）\n");
        sb.append("   ✓ 中小型文档（<100字段）\n");
        sb.append("   ✓ 标准BSON解析场景\n");
        sb.append("   使用: FastBson.useHashMapFactory()\n\n");

        sb.append("📌 Phase 2.A: PartialParser 早退优化模式\n");
        sb.append("   场景: 100字段文档，提取5个字段 (5%)\n");
        sb.append("   性能: 7-10x vs MongoDB\n");
        sb.append("   ✓ 一次性部分字段提取（5-10个字段）\n");
        sb.append("   ✓ 大文档场景（100+字段）\n");
        sb.append("   ✓ 追求极致速度\n");
        sb.append("   ✓ 管道/流式处理场景\n");
        sb.append("   ✗ 不适合重复访问同一文档\n");
        sb.append("   使用: new PartialParser(\"field1\", \"field2\").setEarlyExit(true)\n\n");

        sb.append("📌 Phase 2.B: IndexedBsonDocument 零复制惰性解析模式\n");
        sb.append("   场景: 100字段文档，构建索引 + 访问5个字段\n");
        sb.append("   性能: 3-3.5x vs MongoDB + 70%内存优势\n");
        sb.append("   ✓ 需要重复访问同一文档\n");
        sb.append("   ✓ 内存敏感应用\n");
        sb.append("   ✓ 零复制架构要求\n");
        sb.append("   ✓ 不确定访问哪些字段\n");
        sb.append("   ✗ 不适合一次性字段提取\n");
        sb.append("   使用: FastBson.useIndexedFactory()\n\n");
    }

    /**
     * 截断字符串到指定长度
     */
    private static String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        // 考虑中文字符宽度
        int displayLength = 0;
        int charCount = 0;
        for (char c : str.toCharArray()) {
            displayLength += (c > 127) ? 2 : 1;
            charCount++;
            if (displayLength > maxLength) {
                return str.substring(0, charCount - 1) + "...";
            }
        }
        return str;
    }
}
