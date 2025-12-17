package com.cloud.fastbson.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as a BSON field with optional order hint.
 *
 * <p>The order hint is used for field order optimization in partial parsing.
 * Fields are sorted by order value in ascending order.
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
}
