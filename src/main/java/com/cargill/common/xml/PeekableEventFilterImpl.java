package com.cargill.common.xml;

/**
 * Created by Erik Pearson
 */
public abstract class PeekableEventFilterImpl implements PeekableEventFilter {
    private volatile int count = 0;

    @Override
    public final void startPeeking() {
        count++;
        if (count == 1) startedPeeking();
    }

    @Override
    public final void stopPeeking() {
        if (count > 0) {
            count--;
            if (count == 0) stoppedPeeking();
        }
        else throw new IllegalStateException("not currently peeking");
    }

    @Override
    public final boolean isPeeking() {
        return count > 0;
    }

    @Override
    public String toString() {
        return "peekCount:" + count;
    }

    protected void startedPeeking() {}
    protected void stoppedPeeking() {}
}
