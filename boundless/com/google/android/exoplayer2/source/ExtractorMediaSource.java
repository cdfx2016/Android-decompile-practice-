package com.google.android.exoplayer2.source;

import android.net.Uri;
import android.os.Handler;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.Timeline.Period;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource.Listener;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.DataSource.Factory;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import java.io.IOException;

public final class ExtractorMediaSource implements MediaSource, Listener {
    public static final int DEFAULT_MIN_LOADABLE_RETRY_COUNT_LIVE = 6;
    public static final int DEFAULT_MIN_LOADABLE_RETRY_COUNT_ON_DEMAND = 3;
    public static final int MIN_RETRY_COUNT_DEFAULT_FOR_MEDIA = -1;
    private final Factory dataSourceFactory;
    private final Handler eventHandler;
    private final EventListener eventListener;
    private final ExtractorsFactory extractorsFactory;
    private final int minLoadableRetryCount;
    private final Period period;
    private Listener sourceListener;
    private Timeline timeline;
    private boolean timelineHasDuration;
    private final Uri uri;

    public interface EventListener {
        void onLoadError(IOException iOException);
    }

    public static final class UnrecognizedInputFormatException extends ParserException {
        public UnrecognizedInputFormatException(Extractor[] extractors) {
            super("None of the available extractors (" + Util.getCommaDelimitedSimpleClassNames(extractors) + ") could read the stream.");
        }
    }

    public ExtractorMediaSource(Uri uri, Factory dataSourceFactory, ExtractorsFactory extractorsFactory, Handler eventHandler, EventListener eventListener) {
        this(uri, dataSourceFactory, extractorsFactory, -1, eventHandler, eventListener);
    }

    public ExtractorMediaSource(Uri uri, Factory dataSourceFactory, ExtractorsFactory extractorsFactory, int minLoadableRetryCount, Handler eventHandler, EventListener eventListener) {
        this.uri = uri;
        this.dataSourceFactory = dataSourceFactory;
        this.extractorsFactory = extractorsFactory;
        this.minLoadableRetryCount = minLoadableRetryCount;
        this.eventHandler = eventHandler;
        this.eventListener = eventListener;
        this.period = new Period();
    }

    public void prepareSource(Listener listener) {
        this.sourceListener = listener;
        this.timeline = new SinglePeriodTimeline(C.TIME_UNSET, false);
        listener.onSourceInfoRefreshed(this.timeline, null);
    }

    public void maybeThrowSourceInfoRefreshError() throws IOException {
    }

    public MediaPeriod createPeriod(int index, Allocator allocator, long positionUs) {
        Assertions.checkArgument(index == 0);
        return new ExtractorMediaPeriod(this.uri, this.dataSourceFactory.createDataSource(), this.extractorsFactory.createExtractors(), this.minLoadableRetryCount, this.eventHandler, this.eventListener, this, allocator);
    }

    public void releasePeriod(MediaPeriod mediaPeriod) {
        ((ExtractorMediaPeriod) mediaPeriod).release();
    }

    public void releaseSource() {
        this.sourceListener = null;
    }

    public void onSourceInfoRefreshed(Timeline newTimeline, Object manifest) {
        boolean newTimelineHasDuration = false;
        if (newTimeline.getPeriod(0, this.period).getDurationUs() != C.TIME_UNSET) {
            newTimelineHasDuration = true;
        }
        if (!this.timelineHasDuration || newTimelineHasDuration) {
            this.timeline = newTimeline;
            this.timelineHasDuration = newTimelineHasDuration;
            this.sourceListener.onSourceInfoRefreshed(this.timeline, null);
        }
    }
}
