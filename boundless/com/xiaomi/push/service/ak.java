package com.xiaomi.push.service;

import android.content.Context;
import android.support.v4.os.EnvironmentCompat;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ak {
    private static ak a;
    private ConcurrentHashMap<String, HashMap<String, b>> b = new ConcurrentHashMap();
    private List<a> c = new ArrayList();

    public interface a {
        void a();
    }

    public static class b {
        public String a;
        public String b;
        public String c;
        public String d;
        public boolean e;
        public String f;
        public String g;
        public String h;
        public String i;
        public String j;
        public b k;
        public Context l;
        c m = c.unbind;
        private int n = 0;
        private List<a> o = new ArrayList();
        private XMPushService p;
        private b q = new b(this);

        public interface a {
            void a(c cVar, c cVar2, int i);
        }

        public b(XMPushService xMPushService) {
            this.p = xMPushService;
            a(new al(this));
        }

        public long a() {
            return 1000 * (((long) ((Math.random() * 20.0d) - 10.0d)) + ((long) ((this.n + 1) * 15)));
        }

        public String a(int i) {
            switch (i) {
                case 1:
                    return "OPEN";
                case 2:
                    return "CLOSE";
                case 3:
                    return "KICK";
                default:
                    return EnvironmentCompat.MEDIA_UNKNOWN;
            }
        }

        public void a(a aVar) {
            synchronized (this.o) {
                this.o.add(aVar);
            }
        }

        public void a(c cVar, int i, int i2, String str, String str2) {
            boolean z = true;
            synchronized (this.o) {
                for (a a : this.o) {
                    a.a(this.m, cVar, i2);
                }
            }
            if (this.m != cVar) {
                com.xiaomi.channel.commonutils.logger.b.a(String.format("update the client %7$s status. %1$s->%2$s %3$s %4$s %5$s %6$s", new Object[]{this.m, cVar, a(i), am.a(i2), str, str2, this.h}));
                this.m = cVar;
            }
            if (this.k == null) {
                com.xiaomi.channel.commonutils.logger.b.d("status changed while the client dispatcher is missing");
            } else if (i == 2) {
                this.k.a(this.l, this, i2);
            } else if (i == 3) {
                this.k.a(this.l, this, str2, str);
            } else if (i == 1) {
                if (cVar != c.binded) {
                    z = false;
                }
                if (!z && "wait".equals(str2)) {
                    this.n++;
                } else if (z) {
                    this.n = 0;
                }
                this.k.a(this.l, this, z, i2, str);
            }
        }

        public void b(a aVar) {
            synchronized (this.o) {
                this.o.remove(aVar);
            }
        }
    }

    public enum c {
        unbind,
        binding,
        binded
    }

    private ak() {
    }

    public static synchronized ak a() {
        ak akVar;
        synchronized (ak.class) {
            if (a == null) {
                a = new ak();
            }
            akVar = a;
        }
        return akVar;
    }

    private String d(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        int indexOf = str.indexOf("@");
        return indexOf > 0 ? str.substring(0, indexOf) : str;
    }

    public synchronized void a(Context context) {
        for (HashMap values : this.b.values()) {
            for (b a : values.values()) {
                a.a(c.unbind, 1, 3, null, null);
            }
        }
    }

    public synchronized void a(Context context, int i) {
        for (HashMap values : this.b.values()) {
            for (b a : values.values()) {
                a.a(c.unbind, 2, i, null, null);
            }
        }
    }

    public synchronized void a(a aVar) {
        this.c.add(aVar);
    }

    public synchronized void a(b bVar) {
        HashMap hashMap = (HashMap) this.b.get(bVar.h);
        if (hashMap == null) {
            hashMap = new HashMap();
            this.b.put(bVar.h, hashMap);
        }
        hashMap.put(d(bVar.b), bVar);
        for (a a : this.c) {
            a.a();
        }
    }

    public synchronized void a(String str) {
        HashMap hashMap = (HashMap) this.b.get(str);
        if (hashMap != null) {
            hashMap.clear();
            this.b.remove(str);
        }
        for (a a : this.c) {
            a.a();
        }
    }

    public synchronized void a(String str, String str2) {
        HashMap hashMap = (HashMap) this.b.get(str);
        if (hashMap != null) {
            hashMap.remove(d(str2));
            if (hashMap.isEmpty()) {
                this.b.remove(str);
            }
        }
        for (a a : this.c) {
            a.a();
        }
    }

    public synchronized b b(String str, String str2) {
        HashMap hashMap;
        hashMap = (HashMap) this.b.get(str);
        return hashMap == null ? null : (b) hashMap.get(d(str2));
    }

    public synchronized ArrayList<b> b() {
        ArrayList<b> arrayList;
        arrayList = new ArrayList();
        for (HashMap values : this.b.values()) {
            arrayList.addAll(values.values());
        }
        return arrayList;
    }

    public synchronized List<String> b(String str) {
        List<String> arrayList;
        arrayList = new ArrayList();
        for (HashMap values : this.b.values()) {
            for (b bVar : values.values()) {
                if (str.equals(bVar.a)) {
                    arrayList.add(bVar.h);
                }
            }
        }
        return arrayList;
    }

    public synchronized int c() {
        return this.b.size();
    }

    public synchronized Collection<b> c(String str) {
        return !this.b.containsKey(str) ? new ArrayList() : ((HashMap) ((HashMap) this.b.get(str)).clone()).values();
    }

    public synchronized void d() {
        this.b.clear();
    }

    public synchronized void e() {
        this.c.clear();
    }
}
