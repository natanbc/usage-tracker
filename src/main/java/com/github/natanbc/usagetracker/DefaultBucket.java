package com.github.natanbc.usagetracker;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.function.ToLongFunction;

/**
 * Default bucket implementations
 */
@SuppressWarnings("unused")
public enum DefaultBucket implements Bucket {
    SECOND(UsageTracker::secondUsages),
    MINUTE(UsageTracker::minuteUsages),
    HOUR(UsageTracker::hourlyUsages),
    DAY(UsageTracker::dailyUsages),
    TOTAL(UsageTracker::totalUsages);

    private final ToLongFunction<UsageTracker<?>> amountFunction;
    private final Comparator<UsageTracker<?>> comparator;

    DefaultBucket(ToLongFunction<UsageTracker<?>> amountFunction) {
        this.amountFunction = amountFunction;
        this.comparator = Comparator.comparingLong(amountFunction);
    }

    @Override
    @Nonnull
    public Comparator<UsageTracker<?>> comparator() {
        return comparator;
    }

    @Override
    @Nonnegative
    public long amount(UsageTracker<?> tracker) {
        return amountFunction.applyAsLong(tracker);
    }
}
