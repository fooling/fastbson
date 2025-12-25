package com.cloud.fastbson.processor;

import com.cloud.fastbson.reader.BsonReader;

/**
 * Interface for type-optimized value parsing dispatchers.
 *
 * <p>Implementations of this interface are generated at compile time by
 * {@link BranchOrderProcessor} based on {@link com.cloud.fastbson.annotation.BranchOrder}
 * annotations.
 *
 * <p>Generated dispatchers use if-else chains ordered by type frequency,
 * optimizing CPU branch prediction for specific workloads.
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * // User defines their workload-optimized parser
 * @BranchOrder(value = {BsonType.INT64, BsonType.DOUBLE}, workload = "timeseries")
 * public abstract class TimeSeriesDispatcher implements TypeDispatcher {}
 *
 * // APT generates: TimeSeriesDispatcherImpl with optimized dispatch
 * TypeDispatcher dispatcher = new TimeSeriesDispatcherImpl();
 * Object value = dispatcher.dispatch(reader, type);
 * }</pre>
 *
 * <p><b>Generated Code Pattern:</b>
 * <pre>{@code
 * public Object dispatch(BsonReader reader, byte type) {
 *     // Priority types checked first (per @BranchOrder)
 *     if (type == BsonType.INT64) {
 *         return reader.readInt64();
 *     }
 *     if (type == BsonType.DOUBLE) {
 *         return reader.readDouble();
 *     }
 *     // ... remaining types in default order
 * }
 * }</pre>
 *
 * @author FastBSON
 * @since Phase 3.5
 * @see com.cloud.fastbson.annotation.BranchOrder
 * @see BranchOrderProcessor
 */
public interface TypeDispatcher {

    /**
     * Dispatches parsing based on BSON type, using optimized branch order.
     *
     * <p>The order of type checks is determined by the {@code @BranchOrder}
     * annotation on the implementing class. Types listed in the annotation
     * are checked first, followed by remaining types in default order.
     *
     * @param reader the BSON reader positioned at the value to parse
     * @param type the BSON type byte (e.g., BsonType.INT32, BsonType.STRING)
     * @return the parsed value, or null if type is not supported
     */
    Object dispatch(BsonReader reader, byte type);

    /**
     * Skips a value of the specified type without parsing.
     *
     * <p>The skip order follows the same optimization as {@link #dispatch}.
     *
     * @param reader the BSON reader positioned at the value to skip
     * @param type the BSON type byte
     * @return the number of bytes skipped
     */
    int skip(BsonReader reader, byte type);

    /**
     * Returns the optimized type order for this dispatcher.
     *
     * <p>This is the order in which types are checked during dispatch,
     * as specified by the {@code @BranchOrder} annotation.
     *
     * @return array of BSON type bytes in priority order
     */
    byte[] getBranchOrder();

    /**
     * Returns the workload description for this dispatcher.
     *
     * @return the workload description, or empty string if not specified
     */
    String getWorkloadDescription();
}
