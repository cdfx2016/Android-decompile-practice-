package com.fanyu.boundless.view.home;

import android.view.View;
import android.widget.TextView;
import butterknife.ButterKnife.Finder;
import butterknife.ButterKnife.ViewBinder;
import butterknife.internal.DebouncingOnClickListener;
import com.fanyu.boundless.R;

public class ShowFuJianActivity$$ViewBinder<T extends ShowFuJianActivity> implements ViewBinder<T> {
    public void bind(Finder finder, final T target, Object source) {
        target.messageTitle = (TextView) finder.castView((View) finder.findRequiredView(source, R.id.message_title, "field 'messageTitle'"), R.id.message_title, "field 'messageTitle'");
        ((View) finder.findRequiredView(source, R.id.img_return, "method 'onClick'")).setOnClickListener(new DebouncingOnClickListener() {
            public void doClick(View p0) {
                target.onClick();
            }
        });
    }

    public void unbind(T target) {
        target.messageTitle = null;
    }
}
