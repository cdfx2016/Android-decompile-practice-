package cn.dreamtobe.kpswitch.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build.VERSION;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.InputMethodManager;
import cn.dreamtobe.kpswitch.IPanelHeightTarget;
import cn.dreamtobe.kpswitch.R;

public class KeyboardUtil {
    private static int LAST_SAVE_KEYBOARD_HEIGHT = 0;
    private static int MAX_PANEL_HEIGHT = 0;
    private static int MIN_KEYBOARD_HEIGHT = 0;
    private static int MIN_PANEL_HEIGHT = 0;

    private static class KeyboardStatusListener implements OnGlobalLayoutListener {
        private static final String TAG = "KeyboardStatusListener";
        private final ViewGroup contentView;
        private final boolean isFitSystemWindows;
        private final boolean isFullScreen;
        private boolean isOverlayLayoutDisplayHContainStatusBar = false;
        private final boolean isTranslucentStatus;
        private final OnKeyboardShowingListener keyboardShowingListener;
        private boolean lastKeyboardShowing;
        private int maxOverlayLayoutHeight;
        private final IPanelHeightTarget panelHeightTarget;
        private int previousDisplayHeight = 0;
        private final int screenHeight;
        private final int statusBarHeight;

        KeyboardStatusListener(boolean isFullScreen, boolean isTranslucentStatus, boolean isFitSystemWindows, ViewGroup contentView, IPanelHeightTarget panelHeightTarget, OnKeyboardShowingListener listener, int screenHeight) {
            this.contentView = contentView;
            this.panelHeightTarget = panelHeightTarget;
            this.isFullScreen = isFullScreen;
            this.isTranslucentStatus = isTranslucentStatus;
            this.isFitSystemWindows = isFitSystemWindows;
            this.statusBarHeight = StatusBarHeightUtil.getStatusBarHeight(contentView.getContext());
            this.keyboardShowingListener = listener;
            this.screenHeight = screenHeight;
        }

        @TargetApi(13)
        public void onGlobalLayout() {
            int displayHeight;
            boolean z = false;
            View userRootView = this.contentView.getChildAt(0);
            View actionBarOverlayLayout = (View) this.contentView.getParent();
            Rect r = new Rect();
            if (this.isTranslucentStatus) {
                actionBarOverlayLayout.getWindowVisibleDisplayFrame(r);
                int overlayLayoutDisplayHeight = r.bottom - r.top;
                if (!this.isOverlayLayoutDisplayHContainStatusBar) {
                    if (overlayLayoutDisplayHeight == this.screenHeight) {
                        z = true;
                    }
                    this.isOverlayLayoutDisplayHContainStatusBar = z;
                }
                if (this.isOverlayLayoutDisplayHContainStatusBar) {
                    displayHeight = overlayLayoutDisplayHeight;
                } else {
                    displayHeight = overlayLayoutDisplayHeight + this.statusBarHeight;
                }
            } else {
                userRootView.getWindowVisibleDisplayFrame(r);
                displayHeight = r.bottom - r.top;
            }
            calculateKeyboardHeight(displayHeight);
            calculateKeyboardShowing(displayHeight);
            this.previousDisplayHeight = displayHeight;
        }

        private void calculateKeyboardHeight(int displayHeight) {
            if (this.previousDisplayHeight == 0) {
                this.previousDisplayHeight = displayHeight;
                this.panelHeightTarget.refreshHeight(KeyboardUtil.getValidPanelHeight(getContext()));
                return;
            }
            int keyboardHeight;
            if (KPSwitchConflictUtil.isHandleByPlaceholder(this.isFullScreen, this.isTranslucentStatus, this.isFitSystemWindows)) {
                keyboardHeight = ((View) this.contentView.getParent()).getHeight() - displayHeight;
                Log.d(TAG, String.format("action bar over layout %d display height: %d", new Object[]{Integer.valueOf(((View) this.contentView.getParent()).getHeight()), Integer.valueOf(displayHeight)}));
            } else {
                keyboardHeight = Math.abs(displayHeight - this.previousDisplayHeight);
            }
            if (keyboardHeight > KeyboardUtil.getMinKeyboardHeight(getContext())) {
                Log.d(TAG, String.format("pre display height: %d display height: %d keyboard: %d ", new Object[]{Integer.valueOf(this.previousDisplayHeight), Integer.valueOf(displayHeight), Integer.valueOf(keyboardHeight)}));
                if (keyboardHeight == this.statusBarHeight) {
                    Log.w(TAG, String.format("On global layout change get keyboard height just equal statusBar height %d", new Object[]{Integer.valueOf(keyboardHeight)}));
                } else if (KeyboardUtil.saveKeyboardHeight(getContext(), keyboardHeight)) {
                    int validPanelHeight = KeyboardUtil.getValidPanelHeight(getContext());
                    if (this.panelHeightTarget.getHeight() != validPanelHeight) {
                        this.panelHeightTarget.refreshHeight(validPanelHeight);
                    }
                }
            }
        }

        private void calculateKeyboardShowing(int displayHeight) {
            boolean isKeyboardShowing;
            View actionBarOverlayLayout = (View) this.contentView.getParent();
            int actionBarOverlayLayoutHeight = actionBarOverlayLayout.getHeight() - actionBarOverlayLayout.getPaddingTop();
            if (!KPSwitchConflictUtil.isHandleByPlaceholder(this.isFullScreen, this.isTranslucentStatus, this.isFitSystemWindows)) {
                int phoneDisplayHeight = this.contentView.getResources().getDisplayMetrics().heightPixels;
                if (this.isTranslucentStatus || phoneDisplayHeight != actionBarOverlayLayoutHeight) {
                    if (this.maxOverlayLayoutHeight == 0) {
                        isKeyboardShowing = this.lastKeyboardShowing;
                    } else {
                        isKeyboardShowing = displayHeight < this.maxOverlayLayoutHeight - KeyboardUtil.getMinKeyboardHeight(getContext());
                    }
                    this.maxOverlayLayoutHeight = Math.max(this.maxOverlayLayoutHeight, actionBarOverlayLayoutHeight);
                } else {
                    Log.w(TAG, String.format("skip the keyboard status calculate, the current activity is paused. and phone-display-height %d, root-height+actionbar-height %d", new Object[]{Integer.valueOf(phoneDisplayHeight), Integer.valueOf(actionBarOverlayLayoutHeight)}));
                    return;
                }
            } else if (this.isTranslucentStatus || actionBarOverlayLayoutHeight - displayHeight != this.statusBarHeight) {
                isKeyboardShowing = actionBarOverlayLayoutHeight > displayHeight;
            } else {
                isKeyboardShowing = this.lastKeyboardShowing;
            }
            if (this.lastKeyboardShowing != isKeyboardShowing) {
                Log.d(TAG, String.format("displayHeight %d actionBarOverlayLayoutHeight %d keyboard status change: %B", new Object[]{Integer.valueOf(displayHeight), Integer.valueOf(actionBarOverlayLayoutHeight), Boolean.valueOf(isKeyboardShowing)}));
                this.panelHeightTarget.onKeyboardShowing(isKeyboardShowing);
                if (this.keyboardShowingListener != null) {
                    this.keyboardShowingListener.onKeyboardShowing(isKeyboardShowing);
                }
            }
            this.lastKeyboardShowing = isKeyboardShowing;
        }

        private Context getContext() {
            return this.contentView.getContext();
        }
    }

    public interface OnKeyboardShowingListener {
        void onKeyboardShowing(boolean z);
    }

    public static void showKeyboard(View view) {
        view.requestFocus();
        ((InputMethodManager) view.getContext().getSystemService("input_method")).showSoftInput(view, 0);
    }

    public static void hideKeyboard(View view) {
        ((InputMethodManager) view.getContext().getSystemService("input_method")).hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private static boolean saveKeyboardHeight(Context context, int keyboardHeight) {
        if (LAST_SAVE_KEYBOARD_HEIGHT == keyboardHeight || keyboardHeight < 0) {
            return false;
        }
        LAST_SAVE_KEYBOARD_HEIGHT = keyboardHeight;
        Log.d("KeyBordUtil", String.format("save keyboard: %d", new Object[]{Integer.valueOf(keyboardHeight)}));
        return KeyBoardSharedPreferences.save(context, keyboardHeight);
    }

    public static int getKeyboardHeight(Context context) {
        if (LAST_SAVE_KEYBOARD_HEIGHT == 0) {
            LAST_SAVE_KEYBOARD_HEIGHT = KeyBoardSharedPreferences.get(context, getMinPanelHeight(context.getResources()));
        }
        return LAST_SAVE_KEYBOARD_HEIGHT;
    }

    public static int getValidPanelHeight(Context context) {
        return Math.min(getMaxPanelHeight(context.getResources()), Math.max(getMinPanelHeight(context.getResources()), getKeyboardHeight(context)));
    }

    public static int getMaxPanelHeight(Resources res) {
        if (MAX_PANEL_HEIGHT == 0) {
            MAX_PANEL_HEIGHT = res.getDimensionPixelSize(R.dimen.max_panel_height);
        }
        return MAX_PANEL_HEIGHT;
    }

    public static int getMinPanelHeight(Resources res) {
        if (MIN_PANEL_HEIGHT == 0) {
            MIN_PANEL_HEIGHT = res.getDimensionPixelSize(R.dimen.min_panel_height);
        }
        return MIN_PANEL_HEIGHT;
    }

    public static int getMinKeyboardHeight(Context context) {
        if (MIN_KEYBOARD_HEIGHT == 0) {
            MIN_KEYBOARD_HEIGHT = context.getResources().getDimensionPixelSize(R.dimen.min_keyboard_height);
        }
        return MIN_KEYBOARD_HEIGHT;
    }

    @TargetApi(13)
    public static OnGlobalLayoutListener attach(Activity activity, IPanelHeightTarget target, OnKeyboardShowingListener listener) {
        int screenHeight;
        ViewGroup contentView = (ViewGroup) activity.findViewById(16908290);
        boolean isFullScreen = ViewUtil.isFullScreen(activity);
        boolean isTranslucentStatus = ViewUtil.isTranslucentStatus(activity);
        boolean isFitSystemWindows = ViewUtil.isFitsSystemWindows(activity);
        Display display = activity.getWindowManager().getDefaultDisplay();
        if (VERSION.SDK_INT >= 13) {
            Point screenSize = new Point();
            display.getSize(screenSize);
            screenHeight = screenSize.y;
        } else {
            screenHeight = display.getHeight();
        }
        OnGlobalLayoutListener globalLayoutListener = new KeyboardStatusListener(isFullScreen, isTranslucentStatus, isFitSystemWindows, contentView, target, listener, screenHeight);
        contentView.getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutListener);
        return globalLayoutListener;
    }

    public static OnGlobalLayoutListener attach(Activity activity, IPanelHeightTarget target) {
        return attach(activity, target, null);
    }

    @TargetApi(16)
    public static void detach(Activity activity, OnGlobalLayoutListener l) {
        ViewGroup contentView = (ViewGroup) activity.findViewById(16908290);
        if (VERSION.SDK_INT >= 16) {
            contentView.getViewTreeObserver().removeOnGlobalLayoutListener(l);
        } else {
            contentView.getViewTreeObserver().removeGlobalOnLayoutListener(l);
        }
    }
}