package com.google.android.exoplayer2.source.smoothstreaming;

import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.AdaptiveMediaSourceEventListener;
import com.google.android.exoplayer2.source.AdaptiveMediaSourceEventListener.EventDispatcher;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSource.Listener;
import com.google.android.exoplayer2.source.SinglePeriodTimeline;
import com.google.android.exoplayer2.source.smoothstreaming.SsChunkSource.Factory;
import com.google.android.exoplayer2.source.smoothstreaming.manifest.SsManifest;
import com.google.android.exoplayer2.source.smoothstreaming.manifest.SsManifest.StreamElement;
import com.google.android.exoplayer2.source.smoothstreaming.manifest.SsManifestParser;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.Loader;
import com.google.android.exoplayer2.upstream.Loader.Callback;
import com.google.android.exoplayer2.upstream.ParsingLoadable;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import java.io.IOException;
import java.util.ArrayList;

public final class SsMediaSource implements MediaSource, Callback<ParsingLoadable<SsManifest>> {
    public static final long DEFAULT_LIVE_PRESENTATION_DELAY_MS = 30000;
    public static final int DEFAULT_MIN_LOADABLE_RETRY_COUNT = 3;
    private static final int MINIMUM_MANIFEST_REFRESH_PERIOD_MS = 5000;
    private static final long MIN_LIVE_DEFAULT_START_POSITION_US = 5000000;
    private final Factory chunkSourceFactory;
    private final DataSource.Factory dataSourceFactory;
    private final EventDispatcher eventDispatcher;
    private final long livePresentationDelayMs;
    private SsManifest manifest;
    private DataSource manifestDataSource;
    private long manifestLoadStartTimestamp;
    private Loader manifestLoader;
    private final SsManifestParser manifestParser;
    private Handler manifestRefreshHandler;
    private final Uri manifestUri;
    private final ArrayList<SsMediaPeriod> mediaPeriods;
    private final int minLoadableRetryCount;
    private Listener sourceListener;

    public SsMediaSource(Uri manifestUri, DataSource.Factory manifestDataSourceFactory, Factory chunkSourceFactory, Handler eventHandler, AdaptiveMediaSourceEventListener eventListener) {
        this(manifestUri, manifestDataSourceFactory, chunkSourceFactory, 3, 30000, eventHandler, eventListener);
    }

    public SsMediaSource(Uri manifestUri, DataSource.Factory dataSourceFactory, Factory chunkSourceFactory, int minLoadableRetryCount, long livePresentationDelayMs, Handler eventHandler, AdaptiveMediaSourceEventListener eventListener) {
        if (!Util.toLowerInvariant(manifestUri.getLastPathSegment()).equals("manifest")) {
            manifestUri = Uri.withAppendedPath(manifestUri, "Manifest");
        }
        this.manifestUri = manifestUri;
        this.dataSourceFactory = dataSourceFactory;
        this.chunkSourceFactory = chunkSourceFactory;
        this.minLoadableRetryCount = minLoadableRetryCount;
        this.livePresentationDelayMs = livePresentationDelayMs;
        this.eventDispatcher = new EventDispatcher(eventHandler, eventListener);
        this.manifestParser = new SsManifestParser();
        this.mediaPeriods = new ArrayList();
    }

    public void prepareSource(Listener listener) {
        this.sourceListener = listener;
        this.manifestDataSource = this.dataSourceFactory.createDataSource();
        this.manifestLoader = new Loader("Loader:Manifest");
        this.manifestRefreshHandler = new Handler();
        startLoadingManifest();
    }

    public void maybeThrowSourceInfoRefreshError() throws IOException {
        this.manifestLoader.maybeThrowError();
    }

    public MediaPeriod createPeriod(int index, Allocator allocator, long positionUs) {
        Assertions.checkArgument(index == 0);
        SsMediaPeriod period = new SsMediaPeriod(this.manifest, this.chunkSourceFactory, this.minLoadableRetryCount, this.eventDispatcher, this.manifestLoader, allocator);
        this.mediaPeriods.add(period);
        return period;
    }

    public void releasePeriod(MediaPeriod period) {
        ((SsMediaPeriod) period).release();
        this.mediaPeriods.remove(period);
    }

    public void releaseSource() {
        this.sourceListener = null;
        this.manifest = null;
        this.manifestDataSource = null;
        this.manifestLoadStartTimestamp = 0;
        if (this.manifestLoader != null) {
            this.manifestLoader.release();
            this.manifestLoader = null;
        }
        if (this.manifestRefreshHandler != null) {
            this.manifestRefreshHandler.removeCallbacksAndMessages(null);
            this.manifestRefreshHandler = null;
        }
    }

    public void onLoadCompleted(ParsingLoadable<SsManifest> loadable, long elapsedRealtimeMs, long loadDurationMs) {
        Timeline timeline;
        this.eventDispatcher.loadCompleted(loadable.dataSpec, loadable.type, elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded());
        this.manifest = (SsManifest) loadable.getResult();
        this.manifestLoadStartTimestamp = elapsedRealtimeMs - loadDurationMs;
        for (int i = 0; i < this.mediaPeriods.size(); i++) {
            ((SsMediaPeriod) this.mediaPeriods.get(i)).updateManifest(this.manifest);
        }
        if (this.manifest.isLive) {
            long startTimeUs = Long.MAX_VALUE;
            long endTimeUs = Long.MIN_VALUE;
            for (StreamElement element : this.manifest.streamElements) {
                if (element.chunkCount > 0) {
                    startTimeUs = Math.min(startTimeUs, element.getStartTimeUs(0));
                    endTimeUs = Math.max(endTimeUs, element.getStartTimeUs(element.chunkCount - 1) + element.getChunkDurationUs(element.chunkCount - 1));
                }
            }
            if (startTimeUs == Long.MAX_VALUE) {
                timeline = new SinglePeriodTimeline(C.TIME_UNSET, false);
            } else {
                if (this.manifest.dvrWindowLengthUs != C.TIME_UNSET && this.manifest.dvrWindowLengthUs > 0) {
                    startTimeUs = Math.max(startTimeUs, endTimeUs - this.manifest.dvrWindowLengthUs);
                }
                long durationUs = endTimeUs - startTimeUs;
                long defaultStartPositionUs = durationUs - C.msToUs(this.livePresentationDelayMs);
                if (defaultStartPositionUs < MIN_LIVE_DEFAULT_START_POSITION_US) {
                    defaultStartPositionUs = Math.min(MIN_LIVE_DEFAULT_START_POSITION_US, durationUs / 2);
                }
                timeline = new SinglePeriodTimeline(C.TIME_UNSET, durationUs, startTimeUs, defaultStartPositionUs, true, true);
            }
        } else {
            timeline = new SinglePeriodTimeline(this.manifest.durationUs, this.manifest.durationUs != C.TIME_UNSET);
        }
        this.sourceListener.onSourceInfoRefreshed(timeline, this.manifest);
        scheduleManifestRefresh();
    }

    public void onLoadCanceled(ParsingLoadable<SsManifest> loadable, long elapsedRealtimeMs, long loadDurationMs, boolean released) {
        this.eventDispatcher.loadCompleted(loadable.dataSpec, loadable.type, elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded());
    }

    public int onLoadError(ParsingLoadable<SsManifest> loadable, long elapsedRealtimeMs, long loadDurationMs, IOException error) {
        boolean isFatal = error instanceof ParserException;
        this.eventDispatcher.loadError(loadable.dataSpec, loadable.type, elapsedRealtimeMs, loadDurationMs, loadable.bytesLoaded(), error, isFatal);
        return isFatal ? 3 : 0;
    }

    private void scheduleManifestRefresh() {
        if (this.manifest.isLive) {
            this.manifestRefreshHandler.postDelayed(new Runnable() {
                public void run() {
                    SsMediaSource.this.startLoadingManifest();
                }
            }, Math.max(0, (this.manifestLoadStartTimestamp + ExoPlayerFactory.DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS) - SystemClock.elapsedRealtime()));
        }
    }

    private void startLoadingManifest() {
        ParsingLoadable<SsManifest> loadable = new ParsingLoadable(this.manifestDataSource, this.manifestUri, 4, this.manifestParser);
        this.eventDispatcher.loadStarted(loadable.dataSpec, loadable.type, this.manifestLoader.startLoading(loadable, this, this.minLoadableRetryCount));
    }
}
