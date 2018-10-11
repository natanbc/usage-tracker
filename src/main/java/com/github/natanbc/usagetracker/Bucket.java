package com.github.natanbc.usagetracker;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public interface Bucket {
    /**
     * Returns a comparator that sorts results from low to high.
     *
     * @return The comparator used for sorting.
     *
     * @see #highest(Stream, int)
     * @see #lowest(Stream, int)
     */
    @Nonnull
    Comparator<UsageTracker<?>> comparator();

    /**
     * Returns the amount in this bucket for a given tracker.
     *
     * @param tracker The tracker to get the amount from.
     *
     * @return The amount in the tracker.
     */
    @Nonnegative
    long amount(UsageTracker<?> tracker);

    /**
     * Returns a stream of up to {@code amount} elements from the given stream, sorted from high to low.
     * <br>This method is equivalent to {@code all.sorted(comparator().reversed()).limit(amount)}
     *
     * @param all Data to sort. Cannot be null.
     * @param amount Maximum number of elements in the returned stream.
     * @param <K> Type of the keys for the provided trackers.
     *
     * @return A stream of up to {@code amount} elements sorted from high to low.
     */
    @Nonnull
    default <K> Stream<UsageTracker<K>> highest(@Nonnull Stream<UsageTracker<K>> all, @Nonnegative int amount) {
        return all.sorted(comparator().reversed()).limit(amount);
    }

    /**
     * Returns a stream of up to {@code amount} elements from the given stream, sorted from low to high.
     * <br>This method is equivalent to {@code all.sorted(comparator()).limit(amount)}
     *
     * @param all Data to sort. Cannot be null.
     * @param amount Maximum number of elements in the returned stream.
     * @param <K> Type of the keys for the provided trackers.
     *
     * @return A stream of up to {@code amount} elements sorted from low to high.
     */
    @Nonnull
    default <K> Stream<UsageTracker<K>> lowest(@Nonnull Stream<UsageTracker<K>> all, @Nonnegative int amount) {
        return all.sorted(comparator()).limit(amount);
    }
}
