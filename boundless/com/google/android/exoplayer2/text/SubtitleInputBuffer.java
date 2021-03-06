package com.google.android.exoplayer2.text;

import com.google.android.exoplayer2.decoder.DecoderInputBuffer;

public final class SubtitleInputBuffer extends DecoderInputBuffer implements Comparable<SubtitleInputBuffer> {
    public long subsampleOffsetUs;

    public SubtitleInputBuffer() {
        super(1);
    }

    public int compareTo(SubtitleInputBuffer other) {
        long delta = this.timeUs - other.timeUs;
        if (delta == 0) {
            return 0;
        }
        return delta > 0 ? 1 : -1;
    }
}
