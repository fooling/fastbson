package com.cloud.fastbson.benchmark;

/**
 * Benchmark场景接口
 */
public interface BenchmarkScenario {
    /**
     * 场景名称
     */
    String getName();

    /**
     * 场景描述
     */
    String getDescription();

    /**
     * 目标性能
     */
    String getTarget();

    /**
     * FastBSON实现方式
     */
    String getFastbsonMode();

    /**
     * 运行benchmark
     *
     * @return benchmark结果
     */
    BenchmarkResult run();
}
