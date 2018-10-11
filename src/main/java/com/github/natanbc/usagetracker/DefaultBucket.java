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
    LAST_SECOND(UsageTracker::secondUsages),
    LAST_MINUTE(UsageTracker::minuteUsages),
    LAST_5_MINUTES(tracker -> tracker.hourBuffer().sumLast(5)),
    LAST_15_MINUTES(tracker -> tracker.hourBuffer().sumLast(15)),
    LAST_30_MINUTES(tracker -> tracker.hourBuffer().sumLast(30)),
    LAST_HOUR(UsageTracker::hourlyUsages),
    LAST_2_HOURS(tracker -> tracker.dayBuffer().sumLast(2)),
    LAST_6_HOURS(tracker -> tracker.dayBuffer().sumLast(6)),
    LAST_12_HOURS(tracker -> tracker.dayBuffer().sumLast(12)),
    LAST_DAY(UsageTracker::dailyUsages),
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
