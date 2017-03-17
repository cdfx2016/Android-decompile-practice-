package com.google.android.exoplayer2.extractor;

import com.google.android.exoplayer2.C;

public final class TimestampAdjuster {
    public static final long DO_NOT_OFFSET = Long.MAX_VALUE;
    private static final long MAX_PTS_PLUS_ONE = 8589934592L;
    private final long firstSampleTimestampUs;
    private volatile long lastSampleTimestamp = C.TIME_UNSET;
    private long timestampOffsetUs;

    public TimestampAdjuster(long firstSampleTimestampUs) {
        this.firstSampleTimestampUs = firstSampleTimestampUs;
    }

    public void reset() {
        this.lastSampleTimestamp = C.TIME_UNSET;
    }

    public long adjustTsTimestamp(long pts) {
        if (this.lastSampleTimestamp != C.TIME_UNSET) {
            long lastPts = usToPts(this.lastSampleTimestamp);
            long closestWrapCount = (4294967296L + lastPts) / MAX_PTS_PLUS_ONE;
            long ptsWrapBelow = pts + (MAX_PTS_PLUS_ONE * (closestWrapCount - 1));
            long ptsWrapAbove = pts + (MAX_PTS_PLUS_ONE * closestWrapCount);
            if (Math.abs(ptsWrapBelow - lastPts) < Math.abs(ptsWrapAbove - lastPts)) {
                pts = ptsWrapBelow;
            } else {
                pts = ptsWrapAbove;
            }
        }
        return adjustSampleTimestamp(ptsToUs(pts));
    }

    public long adjustSampleTimestamp(long timeUs) {
        if (this.lastSampleTimestamp != C.TIME_UNSET) {
            this.lastSampleTimestamp = timeUs;
        } else {
            if (this.firstSampleTimestampUs != Long.MAX_VALUE) {
                this.timestampOffsetUs = this.firstSampleTimestampUs - timeUs;
            }
            synchronized (this) {
                this.lastSampleTimestamp = timeUs;
                notifyAll();
            }
        }
        return this.timestampOffsetUs + timeUs;
    }

    public synchronized void waitUntilInitialized() throws InterruptedException {
        while (this.lastSampleTimestamp == C.TIME_UNSET) {
            wait();
        }
    }

    public static long ptsToUs(long pts) {
        return (C.MICROS_PER_SECOND * pts) / 90000;
    }

    public static long usToPts(long us) {
        return (90000 * us) / C.MICROS_PER_SECOND;
    }
}
