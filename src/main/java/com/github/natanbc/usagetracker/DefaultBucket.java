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
    LAST_5_MINUTES(tracker -> tracker.hourBuffer().sumLast(4) + tracker.minuteUsages()),
    LAST_15_MINUTES(tracker -> tracker.hourBuffer().sumLast(14) + tracker.minuteUsages()),
    LAST_30_MINUTES(tracker -> tracker.hourBuffer().sumLast(29) + tracker.minuteUsages()),
    LAST_HOUR(UsageTracker::hourlyUsages),
    LAST_2_HOURS(tracker -> tracker.dayBuffer().sumLast(1) + tracker.hourlyUsages()),
    LAST_6_HOURS(tracker -> tracker.dayBuffer().sumLast(5) + tracker.hourlyUsages()),
    LAST_12_HOURS(tracker -> tracker.dayBuffer().sumLast(11) + tracker.hourlyUsages()),
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
