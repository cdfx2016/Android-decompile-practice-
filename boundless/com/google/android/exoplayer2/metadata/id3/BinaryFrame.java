package com.google.android.exoplayer2.metadata.id3;

public final class BinaryFrame extends Id3Frame {
    public final byte[] data;

    public BinaryFrame(String type, byte[] data) {
        super(type);
        this.data = data;
    }
}
