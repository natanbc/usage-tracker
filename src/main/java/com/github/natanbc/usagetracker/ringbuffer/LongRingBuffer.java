package com.github.natanbc.usagetracker.ringbuffer;

import javax.annotation.Nonnegative;
import java.util.concurrent.atomic.AtomicInteger;

public class LongRingBuffer implements RingBuffer {
    protected final AtomicInteger index = new AtomicInteger();
    protected final int size;
    protected final long[] array;

    public LongRingBuffer(@Nonnegative int size) {
        this.size = size;
        this.array = new long[size];
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void put(@Nonnegative long value) {
        array[index.getAndIncrement() % size] = value;
    }

    @Override
    public long sum() {
        return sumLast(size);
    }

    @Override
    public long sumLast(int amount) {
        int nextIdx = index.get();
        int lastIdx = (nextIdx - 1) % size;
        long sum = 0;
        for(int i = 0, amt = Math.min(amount, size); i < amt; i++) {
            int idx = lastIdx - i;
            if(idx < 0) {
                idx += size;
            }
            sum += array[idx];
        }
        return sum;
    }
}
