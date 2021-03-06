package com.google.android.exoplayer2.source;

import android.os.Handler;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.util.Assertions;
import java.io.IOException;

public interface AdaptiveMediaSourceEventListener {

    public static final class EventDispatcher {
        private final Handler handler;
        private final AdaptiveMediaSourceEventListener listener;

        public EventDispatcher(Handler handler, AdaptiveMediaSourceEventListener listener) {
            this.handler = listener != null ? (Handler) Assertions.checkNotNull(handler) : null;
            this.listener = listener;
        }

        public void loadStarted(DataSpec dataSpec, int dataType, long elapsedRealtimeMs) {
            loadStarted(dataSpec, dataType, -1, null, 0, null, C.TIME_UNSET, C.TIME_UNSET, elapsedRealtimeMs);
        }

        public void loadStarted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeUs, long mediaEndTimeUs, long elapsedRealtimeMs) {
            if (this.listener != null) {
                final DataSpec dataSpec2 = dataSpec;
                final int i = dataType;
                final int i2 = trackType;
                final Format format = trackFormat;
                final int i3 = trackSelectionReason;
                final Object obj = trackSelectionData;
                final long j = mediaStartTimeUs;
                final long j2 = mediaEndTimeUs;
                final long j3 = elapsedRealtimeMs;
                this.handler.post(new Runnable() {
                    public void run() {
                        EventDispatcher.this.listener.onLoadStarted(dataSpec2, i, i2, format, i3, obj, C.usToMs(j), C.usToMs(j2), j3);
                    }
                });
            }
        }

        public void loadCompleted(DataSpec dataSpec, int dataType, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
            loadCompleted(dataSpec, dataType, -1, null, 0, null, C.TIME_UNSET, C.TIME_UNSET, elapsedRealtimeMs, loadDurationMs, bytesLoaded);
        }

        public void loadCompleted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeUs, long mediaEndTimeUs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
            if (this.listener != null) {
                final DataSpec dataSpec2 = dataSpec;
                final int i = dataType;
                final int i2 = trackType;
                final Format format = trackFormat;
                final int i3 = trackSelectionReason;
                final Object obj = trackSelectionData;
                final long j = mediaStartTimeUs;
                final long j2 = mediaEndTimeUs;
                final long j3 = elapsedRealtimeMs;
                final long j4 = loadDurationMs;
                final long j5 = bytesLoaded;
                this.handler.post(new Runnable() {
                    public void run() {
                        EventDispatcher.this.listener.onLoadCompleted(dataSpec2, i, i2, format, i3, obj, C.usToMs(j), C.usToMs(j2), j3, j4, j5);
                    }
                });
            }
        }

        public void loadCanceled(DataSpec dataSpec, int dataType, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
            loadCanceled(dataSpec, dataType, -1, null, 0, null, C.TIME_UNSET, C.TIME_UNSET, elapsedRealtimeMs, loadDurationMs, bytesLoaded);
        }

        public void loadCanceled(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeUs, long mediaEndTimeUs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
            if (this.listener != null) {
                final DataSpec dataSpec2 = dataSpec;
                final int i = dataType;
                final int i2 = trackType;
                final Format format = trackFormat;
                final int i3 = trackSelectionReason;
                final Object obj = trackSelectionData;
                final long j = mediaStartTimeUs;
                final long j2 = mediaEndTimeUs;
                final long j3 = elapsedRealtimeMs;
                final long j4 = loadDurationMs;
                final long j5 = bytesLoaded;
                this.handler.post(new Runnable() {
                    public void run() {
                        EventDispatcher.this.listener.onLoadCanceled(dataSpec2, i, i2, format, i3, obj, C.usToMs(j), C.usToMs(j2), j3, j4, j5);
                    }
                });
            }
        }

        public void loadError(DataSpec dataSpec, int dataType, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded, IOException error, boolean wasCanceled) {
            loadError(dataSpec, dataType, -1, null, 0, null, C.TIME_UNSET, C.TIME_UNSET, elapsedRealtimeMs, loadDurationMs, bytesLoaded, error, wasCanceled);
        }

        public void loadError(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeUs, long mediaEndTimeUs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded, IOException error, boolean wasCanceled) {
            if (this.listener != null) {
                final DataSpec dataSpec2 = dataSpec;
                final int i = dataType;
                final int i2 = trackType;
                final Format format = trackFormat;
                final int i3 = trackSelectionReason;
                final Object obj = trackSelectionData;
                final long j = mediaStartTimeUs;
                final long j2 = mediaEndTimeUs;
                final long j3 = elapsedRealtimeMs;
                final long j4 = loadDurationMs;
                final long j5 = bytesLoaded;
                final IOException iOException = error;
                final boolean z = wasCanceled;
                this.handler.post(new Runnable() {
                    public void run() {
                        EventDispatcher.this.listener.onLoadError(dataSpec2, i, i2, format, i3, obj, C.usToMs(j), C.usToMs(j2), j3, j4, j5, iOException, z);
                    }
                });
            }
        }

        public void upstreamDiscarded(int trackType, long mediaStartTimeUs, long mediaEndTimeUs) {
            if (this.listener != null) {
                final int i = trackType;
                final long j = mediaStartTimeUs;
                final long j2 = mediaEndTimeUs;
                this.handler.post(new Runnable() {
                    public void run() {
                        EventDispatcher.this.listener.onUpstreamDiscarded(i, C.usToMs(j), C.usToMs(j2));
                    }
                });
            }
        }

        public void downstreamFormatChanged(int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaTimeUs) {
            if (this.listener != null) {
                final int i = trackType;
                final Format format = trackFormat;
                final int i2 = trackSelectionReason;
                final Object obj = trackSelectionData;
                final long j = mediaTimeUs;
                this.handler.post(new Runnable() {
                    public void run() {
                        EventDispatcher.this.listener.onDownstreamFormatChanged(i, format, i2, obj, C.usToMs(j));
                    }
                });
            }
        }
    }

    void onDownstreamFormatChanged(int i, Format format, int i2, Object obj, long j);

    void onLoadCanceled(DataSpec dataSpec, int i, int i2, Format format, int i3, Object obj, long j, long j2, long j3, long j4, long j5);

    void onLoadCompleted(DataSpec dataSpec, int i, int i2, Format format, int i3, Object obj, long j, long j2, long j3, long j4, long j5);

    void onLoadError(DataSpec dataSpec, int i, int i2, Format format, int i3, Object obj, long j, long j2, long j3, long j4, long j5, IOException iOException, boolean z);

    void onLoadStarted(DataSpec dataSpec, int i, int i2, Format format, int i3, Object obj, long j, long j2, long j3);

    void onUpstreamDiscarded(int i, long j, long j2);
}
