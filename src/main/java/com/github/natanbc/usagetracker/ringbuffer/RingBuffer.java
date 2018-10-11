package com.github.natanbc.usagetracker.ringbuffer;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;

public interface RingBuffer {
    /**
     * Returns the size of this buffer. After {@code size} elements are added,
     * any new elements will replace the oldest one.
     *
     * @return The size of this buffer.
     */
    @CheckReturnValue
    @Nonnegative
    int size();

    /**
     * Adds a new value to this buffer, replacing the oldest value present.
     *
     * @param value The value to insert.
     */
    void put(@Nonnegative long value);

    /**
     * Returns the sum of all the elements in this buffer.
     *
     * @return The sum of all the elements in this buffer.
     */
    @CheckReturnValue
    @Nonnegative
    long sum();

    /**
     * Returns the sum of the last {@code amount} elements in this buffer.
     *
     * @return The sum of the last {@code amount} elements in this buffer.
     */
    @CheckReturnValue
    @Nonnegative
    long sumLast(@Nonnegative int amount);
}
