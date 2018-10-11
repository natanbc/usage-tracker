package com.github.natanbc.usagetracker;

import com.github.natanbc.usagetracker.ringbuffer.IntRingBuffer;
import com.github.natanbc.usagetracker.ringbuffer.RingBuffer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Represents a group of {@link UsageTracker UsageTracker}s, and handles updating their usage buckets.
 *
 * @param <K> The type of the key used to identify each tracker.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class TrackerGroup<K> {
    protected final ConcurrentHashMap<K, UsageTracker<K>> map = new ConcurrentHashMap<>();
    protected final ScheduledExecutorService executor;
    protected final boolean recursiveIncrements;

    /**
     * Creates a new tracker group with a given executor.
     *
     * @param executor Executor used to schedule updates. Cannot be null.
     * @param recursiveIncrements Whether or not to recursively increment a tracker's parents.
     */
    public TrackerGroup(@Nonnull ScheduledExecutorService executor, boolean recursiveIncrements) {
        this.executor = Objects.requireNonNull(executor, "Executor may not be null");
        this.recursiveIncrements = recursiveIncrements;
        executor.scheduleAtFixedRate(()->map.values().forEach(UsageTracker::rollSecond), 1, 1, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(()->map.values().forEach(UsageTracker::rollMinute), 1, 1, TimeUnit.MINUTES);
        executor.scheduleAtFixedRate(()->map.values().forEach(UsageTracker::rollHour), 1, 1, TimeUnit.HOURS);
    }

    /**
     * Creates a new tracker group with a given executor.
     *
     * @param executor Executor used to schedule updates. Cannot be null.
     */
    public TrackerGroup(@Nonnull ScheduledExecutorService executor) {
        this(executor, false);
    }

    /**
     * Creates a new tracker group, with a single threaded executor and a given thread factory.
     *
     * @param factory Factory used to create the executor thread.
     * @param recursiveIncrements Whether or not to recursively increment a tracker's parents.
     */
    public TrackerGroup(@Nonnull ThreadFactory factory, boolean recursiveIncrements) {
        this(Executors.newSingleThreadScheduledExecutor(factory), recursiveIncrements);
    }

    /**
     * Creates a new tracker group, with a single threaded executor and a given thread factory.
     *
     * @param factory Factory used to create the executor thread.
     */
    public TrackerGroup(@Nonnull ThreadFactory factory) {
        this(Executors.newSingleThreadScheduledExecutor(factory), false);
    }

    /**
     * Creates a new tracker group, with a single threaded daemon executor.
     *
     * @param recursiveIncrements Whether or not to recursively increment a tracker's parents.
     */
    public TrackerGroup(boolean recursiveIncrements) {
        this(r->{
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("TrackerGroup-Updater");
            return t;
        }, recursiveIncrements);
    }

    /**
     * Creates a new tracker group, with a single threaded daemon executor.
     */
    public TrackerGroup() {
        this(r->{
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("TrackerGroup-Updater");
            return t;
        }, false);
    }

    /**
     * Returns the executor used to schedule updates to trackers.
     *
     * @return The executor used.
     */
    @Nonnull
    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    /**
     * Returns the tracker for the given key, creating a new one if needed.
     *
     * @param key The identifier of the wanted tracker. Cannot be null.
     *
     * @return The tracker for this key. Never null.
     */
    @Nonnull
    public UsageTracker tracker(@Nonnull K key) {
        return map.computeIfAbsent(key, k -> createTracker(null, k));
    }

    /**
     * Removes a tracker from this group. This does the same as {@code trackers().remove(key)}.
     *
     * @param key The identifier of the tracker to be removed.
     *
     * @return The removed tracker, or null if there wasn't one for this key.
     *
     * @see #trackers()
     */
    @Nullable
    public UsageTracker remove(@Nonnull K key) {
        return map.remove(key);
    }

    /**
     * Returns the map of the existing trackers. Modifications made to this map will have effect on this group.
     *
     * @return The map used to store trackers. Never null.
     */
    @Nonnull
    public Map<K, UsageTracker<K>> trackers() {
        return map;
    }

    /**
     * Returns the trackers with the highest uses in the given bucket.
     * <br>This is equivalent to {@code bucket.highest(trackers().values().stream(), amount)}
     *
     * @param bucket The bucket to sort trackers.
     * @param amount The maximum amount of results.
     *
     * @return The highest trackers in the bucket.
     */
    public Stream<UsageTracker<K>> highest(Bucket bucket, int amount) {
        return bucket.highest(trackers().values().stream(), amount);
    }

    /**
     * Returns the trackers with the lowest uses in the given bucket.
     * <br>This is equivalent to {@code bucket.lowest(trackers().values().stream(), amount)}
     *
     * @param bucket The bucket to sort trackers.
     * @param amount The maximum amount of results.
     *
     * @return The lowest trackers in the bucket.
     */
    public Stream<UsageTracker<K>> lowest(Bucket bucket, int amount) {
        return bucket.lowest(trackers().values().stream(), amount);
    }

    /**
     * Returns the sum of all usages in the given bucket.
     * <br>This is equivalent to {@code trackers().values().stream().mapToLong(bucket::amount).sum()}
     *
     * @param bucket The bucket of the wanted total.
     *
     * @return The sum of the usages in all trackers for this bucket.
     */
    public long total(Bucket bucket) {
        return trackers().values().stream().mapToLong(bucket::amount).sum();
    }

    /**
     * Creates a new tracker for the given key.
     *
     * @param parent Parent for the new tracker.
     * @param key Key for the new tracker.
     *
     * @return A new tracker.
     *
     * @implNote This method does not register the tracker, so it should not be used
     * directly. Use {@link #tracker(Object) tracker(K)} instead. This method is
     * available so subclasses can provide a different tracker implementation.
     */
    public UsageTracker<K> createTracker(UsageTracker<K> parent, K key) {
        return new UsageTracker<>(this, parent, key, recursiveIncrements);
    }

    /**
     * Creates a new ring buffer with a given size.
     *
     * @param size Size for the new buffer.
     *
     * @return A new ring buffer.
     *
     * @implNote  This method should not be used directly, it's available so
     * subclasses can provide a different buffer implementation.
     */
    public RingBuffer createRingBuffer(int size) {
        return new IntRingBuffer(size);
    }
}
