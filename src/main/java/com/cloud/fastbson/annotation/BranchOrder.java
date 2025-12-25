package com.cloud.fastbson.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the order of type branches in switch-case statements for CPU branch prediction optimization.
 *
 * <p>This annotation allows you to customize the order in which BSON types are checked,
 * optimizing CPU branch prediction for your specific workload.
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * @BranchOrder(value = {BsonType.INT64, BsonType.DOUBLE}, workload = "timeseries")
 * public abstract class TimeSeriesDispatcher implements TypeDispatcher {}
 * }</pre>
 *
 * @author FastBSON
 * @since Phase 3.5
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BranchOrder {
    /**
     * Ordered array of BSON type codes, from most frequent to least frequent.
     *
     * @return array of BSON type codes in priority order
     */
    byte[] value();

    /**
     * Optional description of the workload this branch order optimizes for.
     *
     * @return workload description
     */
    String workload() default "";
}
