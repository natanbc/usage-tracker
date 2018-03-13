package com.github.natanbc.usagetracker;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks usages of a given key for the last second, minute, hour, day and total usages.
 *
 * @param <K> The type of the key used to identify this tracker in it's group.
 *
 * @see TrackerGroup
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class UsageTracker<K> {
    protected final AtomicLong second = new AtomicLong();
    protected final CircularLongArray minute = new CircularLongArray(60);
    protected final CircularLongArray hour = new CircularLongArray(60);
    protected final CircularLongArray day = new CircularLongArray(24);
    protected final AtomicLong total = new AtomicLong();
    protected final ConcurrentHashMap<K, UsageTracker<K>> children = new ConcurrentHashMap<>();
    protected final UsageTracker<K> parent;
    protected final K key;
    protected final boolean recursiveIncrement;

    /**
     * Creates a new usage tracker with a given parent and identifier key.
     *
     * @param parent Parent for this tracker. May be null.
     * @param key Key for this tracker. Cannot be null.
     * @param recursiveIncrement Whether or not to recursively increment parents, until the root tracker is hit.
     */
    protected UsageTracker(@Nullable UsageTracker<K> parent, @Nonnull K key, boolean recursiveIncrement) {
        this.parent = parent;
        this.key = Objects.requireNonNull(key, "Key may not be null");
        this.recursiveIncrement = recursiveIncrement;
    }

    /**
     * Returns this tracker's parent.
     *
     * @return This tracker's parent. May be null.
     */
    @Nonnull
    public UsageTracker<K> getParent() {
        return parent;
    }

    /**
     * Returns this tracker's key.
     *
     * @return This tracker's key. Never null.
     */
    @Nonnull
    public K getKey() {
        return key;
    }

    /**
     * Increments the number of usages in this tracker.
     *
     * <br>If enabled, also increments the parent's number of usages until the root tracker is updated.
     */
    public void increment() {
        if(recursiveIncrement && parent != null) parent.increment();
        second.incrementAndGet();
        total.incrementAndGet();
    }

    /**
     * Returns the child tracker for a given key, creating one if needed.
     *
     * @param key The child identifier. Cannot be null.
     *
     * @return The child tracker for the given key. Never null.
     */
    @Nonnull
    public UsageTracker<K> child(K key) {
        return children.computeIfAbsent(key, unused->new UsageTracker<>(this, key, recursiveIncrement));
    }

    /**
     * Returns the number of usages registered in the last second.
     *
     * @return The number of usages in the last second.
     */
    @Nonnegative
    public long secondUsages() {
        return second.get();
    }

    /**
     * Returns the number of usages registered in the last minute.
     *
     * @return The number of usages in the last minute.
     */
    @Nonnegative
    public long minuteUsages() {
        return minute.sum() + secondUsages();
    }

    /**
     * Returns the number of usages registered in the last hour.
     *
     * @return The number of usages in the last hour.
     */
    @Nonnegative
    public long hourlyUsages() {
        return hour.sum() + minuteUsages();
    }

    /**
     * Returns the number of usages registered in the last day.
     *
     * @return The number of usages in the last day.
     */
    @Nonnegative
    public long dailyUsages() {
        return day.sum() + hourlyUsages();
    }

    /**
     * Returns the total number of usages registered.
     *
     * @return The total number of usages.
     */
    @Nonnegative
    public long totalUsages() {
        return total.get();
    }

    /**
     * Clears the usages in the last second and adds to the minute, replacing the oldest entry.
     */
    protected void rollSecond() {
        minute.put(second.getAndSet(0));
        children.values().forEach(UsageTracker::rollSecond);
    }

    /**
     * Add the usages in the last minute to the hour, replacing the oldest entry.
     */
    protected void rollMinute() {
        hour.put(minute.sum());
        children.values().forEach(UsageTracker::rollMinute);
    }

    /**
     * Add the usages in the last hour to the day, replacing the oldest entry.
     */
    protected void rollHour() {
        day.put(hour.sum());
        children.values().forEach(UsageTracker::rollHour);
    }

    /**
     * Create a new circular long array with a given size.
     *
     * <br>A circular long array replaces the oldest value with a new one when requested, in addition to having a helper
     * method to calculate the sum of all elements.
     *
     * @param size The required size for the array.
     *
     * @return A new array of the given size. Never null.
     */
    @Nonnull
    protected CircularLongArray createArray(@Nonnegative int size) {
        return new CircularLongArray(size);
    }

    /**
     * A circular long array replaces the oldest value with a new one when requested, in addition to having a helper
     * method to calculate the sum of all elements.
     */
    protected static class CircularLongArray {
        protected final AtomicInteger index = new AtomicInteger();
        protected final int size;
        protected final long[] array;

        /**
         * Creates a new array with the given size.
         *
         * @param size The needed size.
         */
        protected CircularLongArray(@Nonnegative int size) {
            this.size = size;
            this.array = new long[size];
        }

        /**
         * Adds a new value to this array, replacing the oldest value present.
         *
         * @param value The value to insert.
         */
        protected void put(@Nonnegative long value) {
            array[index.getAndUpdate(v->(v + 1) % size)] = value;
        }

        /**
         * Returns the sum of all the elements in this array.
         *
         * @return The sum of all the elements in this array.
         */
        @Nonnegative
        protected long sum() {
            int idx = index.get();
            long sum = 0;
            for(int i = 0; i < size; i++) {
                sum += array[(i + idx) % size];
            }
            return sum;
        }
    }
}
