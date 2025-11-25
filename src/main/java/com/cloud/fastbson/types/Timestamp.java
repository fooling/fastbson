package com.cloud.fastbson.types;

/**
 * Represents a BSON timestamp.
 *
 * <p>Structure: int64 with increment in low 32 bits, seconds in high 32 bits
 */
public class Timestamp {
    public final int seconds;
    public final int increment;

    public Timestamp(int seconds, int increment) {
        this.seconds = seconds;
        this.increment = increment;
    }
}
