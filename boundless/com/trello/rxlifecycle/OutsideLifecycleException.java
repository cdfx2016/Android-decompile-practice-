package com.trello.rxlifecycle;

import javax.annotation.Nullable;

public class OutsideLifecycleException extends IllegalStateException {
    public OutsideLifecycleException(@Nullable String detailMessage) {
        super(detailMessage);
    }
}
