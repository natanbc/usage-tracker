package com.github.natanbc.usagetracker;

import com.github.natanbc.usagetracker.ringbuffer.RingBuffer;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
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
    protected final AtomicLong total = new AtomicLong();
    protected final ConcurrentHashMap<K, UsageTracker<K>> children = new ConcurrentHashMap<>();
    protected final TrackerGroup<K> group;
    protected final UsageTracker<K> parent;
    protected final K key;
    protected final boolean recursiveIncrement;
    protected final RingBuffer minute;
    protected final RingBuffer hour;
    protected final RingBuffer day;

    /**
     * Creates a new usage tracker with a given parent and identifier key.
     *
     * @param group The group this tracker belongs to.
     * @param parent Parent for this tracker. May be null.
     * @param key Key for this tracker. Cannot be null.
     * @param recursiveIncrement Whether or not to recursively increment parents, until the root tracker is hit.
     */
    protected UsageTracker(@Nonnull final TrackerGroup<K> group, @Nullable UsageTracker<K> parent, @Nonnull K key, boolean recursiveIncrement) {
        this.group = group;
        this.parent = parent;
        this.key = Objects.requireNonNull(key, "Key may not be null");
        this.recursiveIncrement = recursiveIncrement;
        this.minute = group.createRingBuffer(60);
        this.hour = group.createRingBuffer(60);
        this.day = group.createRingBuffer(24);
    }

    /**
     * Returns the group this tracker belongs to.
     *
     * @return This tracker's group.
     */
    @Nonnegative
    public TrackerGroup<K> getGroup() {
        return group;
    }

    /**
     * Returns this tracker's parent.
     *
     * @return This tracker's parent. May be null.
     */
    @Nullable
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
        return children.computeIfAbsent(key, k -> group.createTracker(this, key));
    }

    /**
     * Returns the map of the existing child trackers. Modifications made to this map will have effect on this tracker.
     *
     * @return The map used to store child trackers. Never null.
     */
    @Nonnull
    public Map<K, UsageTracker<K>> children() {
        return children;
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
     * Returns the buffer containing the usages in the last minute.
     * Each entry corresponds to a second.
     *
     * @return The last minute buffer.
     */
    @Nonnull
    public RingBuffer minuteBuffer() {
        return minute;
    }

    /**
     * Returns the number of usages registered in the last minute.
     *
     * @return The number of usages in the last minute.
     */
    @Nonnegative
    public long minuteUsages() {
        return minute.sum() + second.get();
    }

    /**
     * Returns the buffer containing the usages in the last hour.
     * Each entry corresponds to a minute.
     *
     * @return The last hour buffer.
     */
    @Nonnull
    public RingBuffer hourBuffer() {
        return hour;
    }

    /**
     * Returns the number of usages registered in the last hour.
     *
     * @return The number of usages in the last hour.
     */
    @Nonnegative
    public long hourlyUsages() {
        return hour.sumLast(59) + minute.sum() + second.get();
    }

    /**
     * Returns the buffer containing the usages in the last day.
     * Each entry corresponds to an hour.
     *
     * @return The last day buffer.
     */
    @Nonnull
    public RingBuffer dayBuffer() {
        return day;
    }

    /**
     * Returns the number of usages registered in the last day.
     *
     * @return The number of usages in the last day.
     */
    @Nonnegative
    public long dailyUsages() {
        return day.sumLast(23) + hour.sumLast(59) + minute.sum() + second.get();
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
}
