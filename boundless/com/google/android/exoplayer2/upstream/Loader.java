package com.google.android.exoplayer2.upstream;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.TraceUtil;
import com.google.android.exoplayer2.util.Util;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

public final class Loader implements LoaderErrorThrower {
    public static final int DONT_RETRY = 2;
    public static final int DONT_RETRY_FATAL = 3;
    private static final int MSG_CANCEL = 1;
    private static final int MSG_END_OF_SOURCE = 2;
    private static final int MSG_FATAL_ERROR = 4;
    private static final int MSG_IO_EXCEPTION = 3;
    private static final int MSG_START = 0;
    public static final int RETRY = 0;
    public static final int RETRY_RESET_ERROR_COUNT = 1;
    private LoadTask<? extends Loadable> currentTask;
    private final ExecutorService downloadExecutorService;
    private IOException fatalError;

    public interface Loadable {
        void cancelLoad();

        boolean isLoadCanceled();

        void load() throws IOException, InterruptedException;
    }

    public interface Callback<T extends Loadable> {
        void onLoadCanceled(T t, long j, long j2, boolean z);

        void onLoadCompleted(T t, long j, long j2);

        int onLoadError(T t, long j, long j2, IOException iOException);
    }

    @SuppressLint({"HandlerLeak"})
    private final class LoadTask<T extends Loadable> extends Handler implements Runnable {
        private static final String TAG = "LoadTask";
        private final Callback<T> callback;
        private IOException currentError;
        public final int defaultMinRetryCount;
        private int errorCount;
        private volatile Thread executorThread;
        private final T loadable;
        private volatile boolean released;
        private final long startTimeMs;

        public LoadTask(Looper looper, T loadable, Callback<T> callback, int defaultMinRetryCount, long startTimeMs) {
            super(looper);
            this.loadable = loadable;
            this.callback = callback;
            this.defaultMinRetryCount = defaultMinRetryCount;
            this.startTimeMs = startTimeMs;
        }

        public void maybeThrowError(int minRetryCount) throws IOException {
            if (this.currentError != null && this.errorCount > minRetryCount) {
                throw this.currentError;
            }
        }

        public void start(long delayMillis) {
            Assertions.checkState(Loader.this.currentTask == null);
            Loader.this.currentTask = this;
            if (delayMillis > 0) {
                sendEmptyMessageDelayed(0, delayMillis);
            } else {
                submitToExecutor();
            }
        }

        public void cancel(boolean released) {
            this.released = released;
            this.currentError = null;
            if (hasMessages(0)) {
                removeMessages(0);
                if (!released) {
                    sendEmptyMessage(1);
                }
            } else {
                this.loadable.cancelLoad();
                if (this.executorThread != null) {
                    this.executorThread.interrupt();
                }
            }
            if (released) {
                finish();
                long nowMs = SystemClock.elapsedRealtime();
                this.callback.onLoadCanceled(this.loadable, nowMs, nowMs - this.startTimeMs, true);
            }
        }

        public void run() {
            try {
                this.executorThread = Thread.currentThread();
                if (!this.loadable.isLoadCanceled()) {
                    TraceUtil.beginSection("load:" + this.loadable.getClass().getSimpleName());
                    this.loadable.load();
                    TraceUtil.endSection();
                }
                if (!this.released) {
                    sendEmptyMessage(2);
                }
            } catch (IOException e) {
                if (!this.released) {
                    obtainMessage(3, e).sendToTarget();
                }
            } catch (InterruptedException e2) {
                Assertions.checkState(this.loadable.isLoadCanceled());
                if (!this.released) {
                    sendEmptyMessage(2);
                }
            } catch (Exception e3) {
                Log.e(TAG, "Unexpected exception loading stream", e3);
                if (!this.released) {
                    obtainMessage(3, new UnexpectedLoaderException(e3)).sendToTarget();
                }
            } catch (Error e4) {
                Log.e(TAG, "Unexpected error loading stream", e4);
                if (!this.released) {
                    obtainMessage(4, e4).sendToTarget();
                }
                throw e4;
            } catch (Throwable th) {
                TraceUtil.endSection();
            }
        }

        public void handleMessage(Message msg) {
            if (!this.released) {
                if (msg.what == 0) {
                    submitToExecutor();
                } else if (msg.what == 4) {
                    throw ((Error) msg.obj);
                } else {
                    finish();
                    long nowMs = SystemClock.elapsedRealtime();
                    long durationMs = nowMs - this.startTimeMs;
                    if (this.loadable.isLoadCanceled()) {
                        this.callback.onLoadCanceled(this.loadable, nowMs, durationMs, false);
                        return;
                    }
                    switch (msg.what) {
                        case 1:
                            this.callback.onLoadCanceled(this.loadable, nowMs, durationMs, false);
                            return;
                        case 2:
                            this.callback.onLoadCompleted(this.loadable, nowMs, durationMs);
                            return;
                        case 3:
                            this.currentError = (IOException) msg.obj;
                            int retryAction = this.callback.onLoadError(this.loadable, nowMs, durationMs, this.currentError);
                            if (retryAction == 3) {
                                Loader.this.fatalError = this.currentError;
                                return;
                            } else if (retryAction != 2) {
                                int i;
                                if (retryAction == 1) {
                                    i = 1;
                                } else {
                                    i = this.errorCount + 1;
                                }
                                this.errorCount = i;
                                start(getRetryDelayMillis());
                                return;
                            } else {
                                return;
                            }
                        default:
                            return;
                    }
                }
            }
        }

        private void submitToExecutor() {
            this.currentError = null;
            Loader.this.downloadExecutorService.submit(Loader.this.currentTask);
        }

        private void finish() {
            Loader.this.currentTask = null;
        }

        private long getRetryDelayMillis() {
            return (long) Math.min((this.errorCount - 1) * 1000, 5000);
        }
    }

    public static final class UnexpectedLoaderException extends IOException {
        public UnexpectedLoaderException(Exception cause) {
            super("Unexpected " + cause.getClass().getSimpleName() + ": " + cause.getMessage(), cause);
        }
    }

    public Loader(String threadName) {
        this.downloadExecutorService = Util.newSingleThreadExecutor(threadName);
    }

    public <T extends Loadable> long startLoading(T loadable, Callback<T> callback, int defaultMinRetryCount) {
        Looper looper = Looper.myLooper();
        Assertions.checkState(looper != null);
        long startTimeMs = SystemClock.elapsedRealtime();
        new LoadTask(looper, loadable, callback, defaultMinRetryCount, startTimeMs).start(0);
        return startTimeMs;
    }

    public boolean isLoading() {
        return this.currentTask != null;
    }

    public void cancelLoading() {
        this.currentTask.cancel(false);
    }

    public void release() {
        release(null);
    }

    public void release(Runnable postLoadAction) {
        if (this.currentTask != null) {
            this.currentTask.cancel(true);
        }
        if (postLoadAction != null) {
            this.downloadExecutorService.submit(postLoadAction);
        }
        this.downloadExecutorService.shutdown();
    }

    public void maybeThrowError() throws IOException {
        maybeThrowError(Integer.MIN_VALUE);
    }

    public void maybeThrowError(int minRetryCount) throws IOException {
        if (this.fatalError != null) {
            throw this.fatalError;
        } else if (this.currentTask != null) {
            LoadTask loadTask = this.currentTask;
            if (minRetryCount == Integer.MIN_VALUE) {
                minRetryCount = this.currentTask.defaultMinRetryCount;
            }
            loadTask.maybeThrowError(minRetryCount);
        }
    }
}
