package com.google.android.exoplayer2.source.smoothstreaming;

import android.util.Base64;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.extractor.mp4.TrackEncryptionBox;
import com.google.android.exoplayer2.source.AdaptiveMediaSourceEventListener.EventDispatcher;
import com.google.android.exoplayer2.source.CompositeSequenceableLoader;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.SampleStream;
import com.google.android.exoplayer2.source.SequenceableLoader.Callback;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.chunk.ChunkSampleStream;
import com.google.android.exoplayer2.source.smoothstreaming.SsChunkSource.Factory;
import com.google.android.exoplayer2.source.smoothstreaming.manifest.SsManifest;
import com.google.android.exoplayer2.source.smoothstreaming.manifest.SsManifest.ProtectionElement;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.LoaderErrorThrower;
import java.io.IOException;
import java.util.ArrayList;

final class SsMediaPeriod implements MediaPeriod, Callback<ChunkSampleStream<SsChunkSource>> {
    private static final int INITIALIZATION_VECTOR_SIZE = 8;
    private final Allocator allocator;
    private MediaPeriod.Callback callback;
    private final Factory chunkSourceFactory;
    private final EventDispatcher eventDispatcher;
    private SsManifest manifest;
    private final LoaderErrorThrower manifestLoaderErrorThrower;
    private final int minLoadableRetryCount;
    private ChunkSampleStream<SsChunkSource>[] sampleStreams;
    private CompositeSequenceableLoader sequenceableLoader;
    private final TrackEncryptionBox[] trackEncryptionBoxes;
    private final TrackGroupArray trackGroups;

    public SsMediaPeriod(SsManifest manifest, Factory chunkSourceFactory, int minLoadableRetryCount, EventDispatcher eventDispatcher, LoaderErrorThrower manifestLoaderErrorThrower, Allocator allocator) {
        this.chunkSourceFactory = chunkSourceFactory;
        this.manifestLoaderErrorThrower = manifestLoaderErrorThrower;
        this.minLoadableRetryCount = minLoadableRetryCount;
        this.eventDispatcher = eventDispatcher;
        this.allocator = allocator;
        this.trackGroups = buildTrackGroups(manifest);
        ProtectionElement protectionElement = manifest.protectionElement;
        if (protectionElement != null) {
            byte[] keyId = getProtectionElementKeyId(protectionElement.data);
            this.trackEncryptionBoxes = new TrackEncryptionBox[]{new TrackEncryptionBox(true, 8, keyId)};
        } else {
            this.trackEncryptionBoxes = null;
        }
        this.manifest = manifest;
        this.sampleStreams = newSampleStreamArray(0);
        this.sequenceableLoader = new CompositeSequenceableLoader(this.sampleStreams);
    }

    public void updateManifest(SsManifest manifest) {
        this.manifest = manifest;
        for (ChunkSampleStream<SsChunkSource> sampleStream : this.sampleStreams) {
            ((SsChunkSource) sampleStream.getChunkSource()).updateManifest(manifest);
        }
        this.callback.onContinueLoadingRequested(this);
    }

    public void release() {
        for (ChunkSampleStream<SsChunkSource> sampleStream : this.sampleStreams) {
            sampleStream.release();
        }
    }

    public void prepare(MediaPeriod.Callback callback) {
        this.callback = callback;
        callback.onPrepared(this);
    }

    public void maybeThrowPrepareError() throws IOException {
        this.manifestLoaderErrorThrower.maybeThrowError();
    }

    public TrackGroupArray getTrackGroups() {
        return this.trackGroups;
    }

    public long selectTracks(TrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams, boolean[] streamResetFlags, long positionUs) {
        ArrayList<ChunkSampleStream<SsChunkSource>> sampleStreamsList = new ArrayList();
        int i = 0;
        while (i < selections.length) {
            ChunkSampleStream<SsChunkSource> stream;
            if (streams[i] != null) {
                stream = streams[i];
                if (selections[i] == null || !mayRetainStreamFlags[i]) {
                    stream.release();
                    streams[i] = null;
                } else {
                    sampleStreamsList.add(stream);
                }
            }
            if (streams[i] == null && selections[i] != null) {
                stream = buildSampleStream(selections[i], positionUs);
                sampleStreamsList.add(stream);
                streams[i] = stream;
                streamResetFlags[i] = true;
            }
            i++;
        }
        this.sampleStreams = newSampleStreamArray(sampleStreamsList.size());
        sampleStreamsList.toArray(this.sampleStreams);
        this.sequenceableLoader = new CompositeSequenceableLoader(this.sampleStreams);
        return positionUs;
    }

    public boolean continueLoading(long positionUs) {
        return this.sequenceableLoader.continueLoading(positionUs);
    }

    public long getNextLoadPositionUs() {
        return this.sequenceableLoader.getNextLoadPositionUs();
    }

    public long readDiscontinuity() {
        return C.TIME_UNSET;
    }

    public long getBufferedPositionUs() {
        long bufferedPositionUs = Long.MAX_VALUE;
        for (ChunkSampleStream<SsChunkSource> sampleStream : this.sampleStreams) {
            long rendererBufferedPositionUs = sampleStream.getBufferedPositionUs();
            if (rendererBufferedPositionUs != Long.MIN_VALUE) {
                bufferedPositionUs = Math.min(bufferedPositionUs, rendererBufferedPositionUs);
            }
        }
        return bufferedPositionUs == Long.MAX_VALUE ? Long.MIN_VALUE : bufferedPositionUs;
    }

    public long seekToUs(long positionUs) {
        for (ChunkSampleStream<SsChunkSource> sampleStream : this.sampleStreams) {
            sampleStream.seekToUs(positionUs);
        }
        return positionUs;
    }

    public void onContinueLoadingRequested(ChunkSampleStream<SsChunkSource> chunkSampleStream) {
        this.callback.onContinueLoadingRequested(this);
    }

    private ChunkSampleStream<SsChunkSource> buildSampleStream(TrackSelection selection, long positionUs) {
        int streamElementIndex = this.trackGroups.indexOf(selection.getTrackGroup());
        return new ChunkSampleStream(this.manifest.streamElements[streamElementIndex].type, this.chunkSourceFactory.createChunkSource(this.manifestLoaderErrorThrower, this.manifest, streamElementIndex, selection, this.trackEncryptionBoxes), this, this.allocator, positionUs, this.minLoadableRetryCount, this.eventDispatcher);
    }

    private static TrackGroupArray buildTrackGroups(SsManifest manifest) {
        TrackGroup[] trackGroups = new TrackGroup[manifest.streamElements.length];
        for (int i = 0; i < manifest.streamElements.length; i++) {
            trackGroups[i] = new TrackGroup(manifest.streamElements[i].formats);
        }
        return new TrackGroupArray(trackGroups);
    }

    private static ChunkSampleStream<SsChunkSource>[] newSampleStreamArray(int length) {
        return new ChunkSampleStream[length];
    }

    private static byte[] getProtectionElementKeyId(byte[] initData) {
        StringBuilder initDataStringBuilder = new StringBuilder();
        for (int i = 0; i < initData.length; i += 2) {
            initDataStringBuilder.append((char) initData[i]);
        }
        String initDataString = initDataStringBuilder.toString();
        byte[] keyId = Base64.decode(initDataString.substring(initDataString.indexOf("<KID>") + 5, initDataString.indexOf("</KID>")), 0);
        swap(keyId, 0, 3);
        swap(keyId, 1, 2);
        swap(keyId, 4, 5);
        swap(keyId, 6, 7);
        return keyId;
    }

    private static void swap(byte[] data, int firstPosition, int secondPosition) {
        byte temp = data[firstPosition];
        data[firstPosition] = data[secondPosition];
        data[secondPosition] = temp;
    }
}
