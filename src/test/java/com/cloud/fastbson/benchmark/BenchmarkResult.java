package com.cloud.fastbson.benchmark;

import lombok.Builder;
import lombok.Data;

/**
 * Benchmark测试结果
 */
@Data
@Builder
public class BenchmarkResult {
    /** 场景名称 */
    private String scenarioName;

    /** FastBSON实现方式 */
    private String fastbsonMode;

    /** FastBSON耗时（纳秒） */
    private long fastbsonTimeNanos;

    /** MongoDB BSON耗时（纳秒） */
    private long mongoTimeNanos;

    /** 性能提升倍数 */
    private double speedup;

    /** 场景描述 */
    private String description;

    /** 目标性能 */
    private String target;

    /** 是否达标 */
    private boolean passed;

    /** 额外说明 */
    private String note;

    /**
     * 获取FastBSON耗时（毫秒）
     */
    public long getFastbsonTimeMs() {
        return fastbsonTimeNanos / 1_000_000;
    }

    /**
     * 获取MongoDB耗时（毫秒）
     */
    public long getMongoTimeMs() {
        return mongoTimeNanos / 1_000_000;
    }

    /**
     * 格式化性能提升倍数
     */
    public String getSpeedupFormatted() {
        return String.format("%.2fx", speedup);
    }

    /**
     * 获取评级
     */
    public String getGrade() {
        if (scenarioName.contains("Phase 1")) {
            // Phase 1: 3.88x最佳
            if (speedup >= 3.5) return "✅ 优秀";
            if (speedup >= 2.5) return "✓  良好";
            if (speedup >= 1.5) return "⚠️  一般";
            return "❌ 待优化";
        } else if (scenarioName.contains("Phase 2.A")) {
            // Phase 2.A: 10-20x目标
            if (speedup >= 10.0) return "✅ 优秀";
            if (speedup >= 5.0) return "✓  良好";
            if (speedup >= 2.0) return "⚠️  一般";
            return "❌ 待优化";
        } else if (scenarioName.contains("Phase 2.B")) {
            // Phase 2.B: 2.5-3.5x目标 + 70%内存
            if (speedup >= 2.5) return "✅ 优秀";
            if (speedup >= 1.5) return "✓  良好";
            if (speedup >= 1.0) return "⚠️  一般";
            return "❌ 待优化";
        }
        return "N/A";
    }
}
