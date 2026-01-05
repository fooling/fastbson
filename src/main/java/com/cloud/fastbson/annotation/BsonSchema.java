package com.cloud.fastbson.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a BSON schema definition.
 *
 * <p>This annotation is optional. If present, the value() can specify
 * a custom schema name. If not specified, the simple class name is used.
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * @BsonSchema("User")
 * public class UserEntity {
 *     @BsonField(value = "_id", order = 1)
 *     private String id;
 *
 *     @BsonField(value = "name", order = 2)
 *     private String name;
 * }
 * }</pre>
 *
 * @author FastBSON
 * @since Phase 3.4
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BsonSchema {
    /**
     * Schema name. Defaults to simple class name if not specified.
     *
     * @return schema name
     */
    String value() default "";
}
