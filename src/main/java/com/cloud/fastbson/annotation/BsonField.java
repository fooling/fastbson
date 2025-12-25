package com.cloud.fastbson.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as a BSON field with optional order hint and array type optimization.
 *
 * <p>The order hint is used for field order optimization in partial parsing.
 * Fields are sorted by order value in ascending order.
 *
 * <p>The arrayType hint is used for homogeneous array optimization. When specified,
 * the parser skips runtime type detection and directly uses the type-specialized fast path.
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * public class User {
 *     @BsonField(value = "_id", order = 1)
 *     private String id;
 *
 *     @BsonField(value = "name", order = 2)
 *     private String name;
 *
 *     @BsonField(value = "age", order = 3)
 *     private Integer age;
 *
 *     // Phase 3.5: Homogeneous array optimization
 *     @BsonField(value = "timestamps", arrayType = 0x12)  // INT64
 *     private long[] timestamps;
 *
 *     @BsonField(value = "scores", arrayType = 0x01)  // DOUBLE
 *     private double[] scores;
 * }
 * }</pre>
 *
 * @author FastBSON
 * @since Phase 3.4
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface BsonField {
    /**
     * BSON field name. Defaults to Java field name if not specified.
     *
     * @return BSON field name
     */
    String value() default "";

    /**
     * Field order hint for optimization. Lower values come first.
     *
     * <p>Use -1 to indicate no specific order (field will be placed
     * after all ordered fields in declaration order).
     *
     * @return field order (-1 for unordered)
     */
    int order() default -1;

    /**
     * Array element type hint for homogeneous array optimization.
     *
     * <p>When specified (non-zero), the parser assumes all array elements
     * have the same type and uses type-specialized fast path without runtime detection.
     *
     * <p>Common values:
     * <ul>
     *   <li>0x10 (16): INT32 - for int[] arrays</li>
     *   <li>0x12 (18): INT64 - for long[] arrays</li>
     *   <li>0x01 (1): DOUBLE - for double[] arrays</li>
     *   <li>0x02 (2): STRING - for String[] arrays</li>
     * </ul>
     *
     * <p>Use 0 (default) to disable optimization and use runtime detection.
     *
     * @return BSON type code for array elements, or 0 for auto-detection
     * @since Phase 3.5
     */
    byte arrayType() default 0;
}
