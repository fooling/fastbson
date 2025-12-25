package com.cloud.fastbson.parser;

/**
 * Builder for PartialParser with fluent API.
 *
 * <p>This builder provides a fluent interface for constructing PartialParser
 * instances with schema classes and configuration options.
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * // From FastBson
 * PartialParser parser = FastBson.forClass(UserEntity.class)
 *     .selectFields("name", "email")
 *     .setEarlyExit(true)
 *     .build();
 *
 * // Direct construction
 * PartialParser parser = new PartialParserBuilder(UserEntity.class)
 *     .selectFields("name", "email")
 *     .build();
 * }</pre>
 *
 * @author FastBSON
 * @since Phase 3.4
 */
public class PartialParserBuilder {

    /**
     * Schema class for field order extraction.
     */
    private final Class<?> schemaClass;

    /**
     * Target fields to extract.
     */
    private String[] targetFields;

    /**
     * Whether to enable early exit optimization.
     */
    private boolean earlyExit = true;

    /**
     * Creates a builder for the specified schema class.
     *
     * @param schemaClass Schema class with @BsonField annotations
     */
    public PartialParserBuilder(Class<?> schemaClass) {
        if (schemaClass == null) {
            throw new IllegalArgumentException("Schema class cannot be null");
        }
        this.schemaClass = schemaClass;
    }

    /**
     * Specifies the target fields to extract.
     *
     * @param fields Target field names
     * @return this builder for chaining
     */
    public PartialParserBuilder selectFields(String... fields) {
        if (fields == null || fields.length == 0) {
            throw new IllegalArgumentException("Target fields cannot be null or empty");
        }
        this.targetFields = fields;
        return this;
    }

    /**
     * Sets whether to enable early exit optimization.
     *
     * <p>When enabled, parsing stops immediately after finding all target fields.
     *
     * @param earlyExit true to enable, false to disable
     * @return this builder for chaining
     */
    public PartialParserBuilder setEarlyExit(boolean earlyExit) {
        this.earlyExit = earlyExit;
        return this;
    }

    /**
     * Builds the PartialParser instance.
     *
     * @return configured PartialParser
     * @throws IllegalStateException if target fields not specified
     */
    public PartialParser build() {
        if (targetFields == null || targetFields.length == 0) {
            throw new IllegalStateException(
                "Target fields must be specified via selectFields() before building");
        }

        PartialParser parser = new PartialParser(schemaClass, targetFields);
        parser.setEarlyExit(earlyExit);
        return parser;
    }
}
