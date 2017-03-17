package com.google.android.exoplayer2.upstream;

import com.google.android.exoplayer2.util.Assertions;
import java.io.IOException;
import java.io.InputStream;

public final class DataSourceInputStream extends InputStream {
    private boolean closed = false;
    private final DataSource dataSource;
    private final DataSpec dataSpec;
    private boolean opened = false;
    private final byte[] singleByteArray;
    private long totalBytesRead;

    public DataSourceInputStream(DataSource dataSource, DataSpec dataSpec) {
        this.dataSource = dataSource;
        this.dataSpec = dataSpec;
        this.singleByteArray = new byte[1];
    }

    public long bytesRead() {
        return this.totalBytesRead;
    }

    public void open() throws IOException {
        checkOpened();
    }

    public int read() throws IOException {
        if (read(this.singleByteArray) == -1) {
            return -1;
        }
        this.totalBytesRead++;
        return this.singleByteArray[0] & 255;
    }

    public int read(byte[] buffer) throws IOException {
        int bytesRead = read(buffer, 0, buffer.length);
        if (bytesRead != -1) {
            this.totalBytesRead += (long) bytesRead;
        }
        return bytesRead;
    }

    public int read(byte[] buffer, int offset, int length) throws IOException {
        Assertions.checkState(!this.closed);
        checkOpened();
        int bytesRead = this.dataSource.read(buffer, offset, length);
        if (bytesRead == -1) {
            return -1;
        }
        this.totalBytesRead += (long) bytesRead;
        return bytesRead;
    }

    public long skip(long byteCount) throws IOException {
        Assertions.checkState(!this.closed);
        checkOpened();
        long bytesSkipped = super.skip(byteCount);
        this.totalBytesRead += bytesSkipped;
        return bytesSkipped;
    }

    public void close() throws IOException {
        if (!this.closed) {
            this.dataSource.close();
            this.closed = true;
        }
    }

    private void checkOpened() throws IOException {
        if (!this.opened) {
            this.dataSource.open(this.dataSpec);
            this.opened = true;
        }
    }
}
