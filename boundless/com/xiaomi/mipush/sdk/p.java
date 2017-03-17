package com.xiaomi.mipush.sdk;

import android.content.Context;

public class p {
    private static p a = null;
    private Context b;
    private j c = new j();

    private p(Context context) {
        this.b = context.getApplicationContext();
        if (this.b == null) {
            this.b = context;
        }
    }

    public static p a(Context context) {
        if (a == null) {
            synchronized (p.class) {
                if (a == null) {
                    a = new p(context);
                }
            }
        }
        return a;
    }

    public synchronized String a() {
        return this.b.getSharedPreferences(MiPushClient.PREF_EXTRA, 0).getString("enable_disable_sync_status", "");
    }

    public void a(String str) {
        synchronized (this) {
            if (this.c == null) {
                this.c = new j();
            }
            this.c.a = 0;
            this.c.b = str;
        }
    }

    public void b(String str) {
        synchronized (this) {
            if (this.c == null) {
                this.c = new j();
            }
            this.c.a++;
            this.c.b = str;
        }
    }

    public int c(String str) {
        int i;
        synchronized (this) {
            if (this.c == null || !this.c.b.equals(str)) {
                i = 0;
            } else {
                i = this.c.a;
            }
        }
        return i;
    }

    public void d(String str) {
        synchronized (this) {
            if (this.c != null && this.c.b.equals(str)) {
                this.c = null;
            }
        }
    }

    public boolean e(String str) {
        boolean z;
        synchronized (this) {
            if (this.c == null || !this.c.b.equals(str)) {
                z = false;
            } else {
                z = true;
            }
        }
        return z;
    }

    public synchronized void f(String str) {
        this.b.getSharedPreferences(MiPushClient.PREF_EXTRA, 0).edit().putString("enable_disable_sync_status", str).commit();
    }
}
