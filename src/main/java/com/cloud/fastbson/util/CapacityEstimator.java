package com.cloud.fastbson.util;

/**
 * Configuration class for capacity estimation in BSON document and array parsing.
 *
 * <p>Capacity estimation is used to pre-allocate the right size for HashMaps and ArrayLists
 * to avoid costly rehashing/resizing operations during parsing.
 *
 * <p>Default heuristics work well for general cases, but can be tuned for specific
 * business scenarios to achieve optimal performance.
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * // Use default estimation (recommended for most cases)
 * CapacityEstimator estimator = CapacityEstimator.defaults();
 *
 * // Tune for dense documents (many small fields)
 * CapacityEstimator dense = CapacityEstimator.builder()
 *     .documentBytesPerField(10)  // Fields average 10 bytes each
 *     .arrayBytesPerElement(8)    // Array elements average 8 bytes each
 *     .build();
 *
 * // Tune for sparse documents (few large fields)
 * CapacityEstimator sparse = CapacityEstimator.builder()
 *     .documentBytesPerField(50)  // Fields average 50 bytes each
 *     .arrayBytesPerElement(30)   // Array elements average 30 bytes each
 *     .build();
 *
 * // Apply to parser
 * FastBson.setCapacityEstimator(dense);
 * }</pre>
 *
 * <p><b>Tuning Guidelines:</b>
 * <ul>
 *   <li><b>documentBytesPerField</b>: Average bytes per field in documents (default: 20)
 *       <ul>
 *         <li>Lower (10-15): Dense documents with many small fields (int32, boolean)</li>
 *         <li>Higher (30-50): Sparse documents with few large fields (strings, nested docs)</li>
 *       </ul>
 *   </li>
 *   <li><b>arrayBytesPerElement</b>: Average bytes per element in arrays (default: 15)
 *       <ul>
 *         <li>Lower (8-12): Arrays of primitive types (int32, double)</li>
 *         <li>Higher (20-30): Arrays of complex types (strings, nested objects)</li>
 *       </ul>
 *   </li>
 *   <li><b>minCapacity</b>: Minimum initial capacity (default: 4)
 *       <ul>
 *         <li>Increase (8-16): If you know documents/arrays always have many elements</li>
 *         <li>Keep low (4): For general use cases with varying sizes</li>
 *       </ul>
 *   </li>
 *   <li><b>loadFactor</b>: HashMap load factor (default: 0.75)
 *       <ul>
 *         <li>Lower (0.5-0.7): Reduce hash collisions at cost of memory</li>
 *         <li>Higher (0.8-0.9): Save memory at cost of more collisions</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * @author FastBSON
 * @since Phase 3.5
 */
public final class CapacityEstimator {
    /**
     * Average bytes per field in BSON documents (default: 20).
     * Used to estimate HashMap initial capacity from document length.
     */
    private final int documentBytesPerField;

    /**
     * Average bytes per element in BSON arrays (default: 15).
     * Used to estimate ArrayList initial capacity from array length.
     */
    private final int arrayBytesPerElement;

    /**
     * Minimum initial capacity for collections (default: 4).
     * Ensures small documents/arrays don't allocate too little.
     */
    private final int minCapacity;

    /**
     * HashMap load factor (default: 0.75).
     * Used to calculate initial capacity accounting for rehashing threshold.
     */
    private final double loadFactor;

    /**
     * Private constructor. Use {@link #defaults()} or {@link #builder()}.
     */
    private CapacityEstimator(int documentBytesPerField, int arrayBytesPerElement,
                              int minCapacity, double loadFactor) {
        this.documentBytesPerField = documentBytesPerField;
        this.arrayBytesPerElement = arrayBytesPerElement;
        this.minCapacity = minCapacity;
        this.loadFactor = loadFactor;
    }

    /**
     * Creates a CapacityEstimator with default heuristics.
     *
     * <p>Default values are optimized for general BSON documents:
     * <ul>
     *   <li>documentBytesPerField: 20 (balanced for mixed field types)</li>
     *   <li>arrayBytesPerElement: 15 (balanced for mixed element types)</li>
     *   <li>minCapacity: 4 (avoid over-allocation for small documents)</li>
     *   <li>loadFactor: 0.75 (standard HashMap load factor)</li>
     * </ul>
     *
     * @return default CapacityEstimator instance
     */
    public static CapacityEstimator defaults() {
        return new CapacityEstimator(20, 15, 4, 0.75);
    }

    /**
     * Creates a builder for customizing capacity estimation.
     *
     * @return new Builder instance with default values
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Estimates field count for a document based on its total length.
     *
     * @param documentLength total document length in bytes
     * @return estimated number of fields
     */
    public int estimateDocumentFields(int documentLength) {
        return Math.max(minCapacity, documentLength / documentBytesPerField);
    }

    /**
     * Estimates array element count based on array total length.
     *
     * @param arrayLength total array length in bytes
     * @return estimated number of elements
     */
    public int estimateArrayElements(int arrayLength) {
        return Math.max(minCapacity, arrayLength / arrayBytesPerElement);
    }

    /**
     * Calculates HashMap initial capacity accounting for load factor.
     *
     * <p>This ensures the HashMap won't rehash when filled with estimated number of entries.
     *
     * @param estimatedFields estimated number of fields/entries
     * @return initial capacity for HashMap constructor
     */
    public int hashMapCapacity(int estimatedFields) {
        return (int) (estimatedFields / loadFactor) + 1;
    }

    /**
     * Gets document bytes per field heuristic.
     *
     * @return average bytes per field
     */
    public int getDocumentBytesPerField() {
        return documentBytesPerField;
    }

    /**
     * Gets array bytes per element heuristic.
     *
     * @return average bytes per element
     */
    public int getArrayBytesPerElement() {
        return arrayBytesPerElement;
    }

    /**
     * Gets minimum capacity setting.
     *
     * @return minimum initial capacity
     */
    public int getMinCapacity() {
        return minCapacity;
    }

    /**
     * Gets load factor setting.
     *
     * @return HashMap load factor
     */
    public double getLoadFactor() {
        return loadFactor;
    }

    /**
     * Builder for creating customized CapacityEstimator instances.
     */
    public static final class Builder {
        private int documentBytesPerField = 20;
        private int arrayBytesPerElement = 15;
        private int minCapacity = 4;
        private double loadFactor = 0.75;

        private Builder() {
        }

        /**
         * Sets average bytes per field in documents.
         *
         * @param documentBytesPerField average bytes per field (must be > 0)
         * @return this builder
         * @throws IllegalArgumentException if value is not positive
         */
        public Builder documentBytesPerField(int documentBytesPerField) {
            if (documentBytesPerField <= 0) {
                throw new IllegalArgumentException("documentBytesPerField must be positive: " + documentBytesPerField);
            }
            this.documentBytesPerField = documentBytesPerField;
            return this;
        }

        /**
         * Sets average bytes per element in arrays.
         *
         * @param arrayBytesPerElement average bytes per element (must be > 0)
         * @return this builder
         * @throws IllegalArgumentException if value is not positive
         */
        public Builder arrayBytesPerElement(int arrayBytesPerElement) {
            if (arrayBytesPerElement <= 0) {
                throw new IllegalArgumentException("arrayBytesPerElement must be positive: " + arrayBytesPerElement);
            }
            this.arrayBytesPerElement = arrayBytesPerElement;
            return this;
        }

        /**
         * Sets minimum initial capacity.
         *
         * @param minCapacity minimum capacity (must be > 0)
         * @return this builder
         * @throws IllegalArgumentException if value is not positive
         */
        public Builder minCapacity(int minCapacity) {
            if (minCapacity <= 0) {
                throw new IllegalArgumentException("minCapacity must be positive: " + minCapacity);
            }
            this.minCapacity = minCapacity;
            return this;
        }

        /**
         * Sets HashMap load factor.
         *
         * @param loadFactor load factor (must be in range 0.1 to 1.0)
         * @return this builder
         * @throws IllegalArgumentException if value is out of valid range
         */
        public Builder loadFactor(double loadFactor) {
            if (loadFactor <= 0.0 || loadFactor > 1.0) {
                throw new IllegalArgumentException("loadFactor must be in range (0.0, 1.0]: " + loadFactor);
            }
            this.loadFactor = loadFactor;
            return this;
        }

        /**
         * Builds the CapacityEstimator with configured values.
         *
         * @return new CapacityEstimator instance
         */
        public CapacityEstimator build() {
            return new CapacityEstimator(documentBytesPerField, arrayBytesPerElement,
                minCapacity, loadFactor);
        }
    }
}
