package com.jakewharton.rxbinding.widget;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.AdapterView;
import com.jakewharton.rxbinding.view.ViewEvent;

public final class AdapterViewItemLongClickEvent extends ViewEvent<AdapterView<?>> {
    private final View clickedView;
    private final long id;
    private final int position;

    @CheckResult
    @NonNull
    public static AdapterViewItemLongClickEvent create(@NonNull AdapterView<?> view, @NonNull View clickedView, int position, long id) {
        return new AdapterViewItemLongClickEvent(view, clickedView, position, id);
    }

    private AdapterViewItemLongClickEvent(@NonNull AdapterView<?> view, @NonNull View clickedView, int position, long id) {
        super(view);
        this.clickedView = clickedView;
        this.position = position;
        this.id = id;
    }

    @NonNull
    public View clickedView() {
        return this.clickedView;
    }

    public int position() {
        return this.position;
    }

    public long id() {
        return this.id;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AdapterViewItemLongClickEvent)) {
            return false;
        }
        AdapterViewItemLongClickEvent other = (AdapterViewItemLongClickEvent) o;
        if (other.view() == view() && other.clickedView == this.clickedView && other.position == this.position && other.id == this.id) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return ((((((((AdapterView) view()).hashCode() + 629) * 37) + this.clickedView.hashCode()) * 37) + this.position) * 37) + ((int) (this.id ^ (this.id >>> 32)));
    }

    public String toString() {
        return "AdapterViewItemLongClickEvent{view=" + view() + ", clickedView=" + this.clickedView + ", position=" + this.position + ", id=" + this.id + '}';
    }
}
