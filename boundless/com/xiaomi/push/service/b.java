package com.xiaomi.push.service;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import com.xiaomi.smack.packet.c;
import com.xiaomi.smack.packet.d;
import com.xiaomi.smack.packet.f;
import java.util.Collection;
import java.util.Iterator;

public class b {
    private s a = new s();

    public static String a(String str) {
        return str + ".permission.MIPUSH_RECEIVE";
    }

    private static void a(Context context, Intent intent, String str) {
        if ("com.xiaomi.xmsf".equals(context.getPackageName())) {
            context.sendBroadcast(intent);
        } else {
            context.sendBroadcast(intent, a(str));
        }
    }

    com.xiaomi.push.service.ak.b a(com.xiaomi.slim.b bVar) {
        Collection c = ak.a().c(Integer.toString(bVar.c()));
        if (c.isEmpty()) {
            return null;
        }
        Iterator it = c.iterator();
        if (c.size() == 1) {
            return (com.xiaomi.push.service.ak.b) it.next();
        }
        CharSequence j = bVar.j();
        while (it.hasNext()) {
            com.xiaomi.push.service.ak.b bVar2 = (com.xiaomi.push.service.ak.b) it.next();
            if (TextUtils.equals(j, bVar2.b)) {
                return bVar2;
            }
        }
        return null;
    }

    com.xiaomi.push.service.ak.b a(d dVar) {
        Collection c = ak.a().c(dVar.l());
        if (c.isEmpty()) {
            return null;
        }
        Iterator it = c.iterator();
        if (c.size() == 1) {
            return (com.xiaomi.push.service.ak.b) it.next();
        }
        CharSequence n = dVar.n();
        CharSequence m = dVar.m();
        while (it.hasNext()) {
            com.xiaomi.push.service.ak.b bVar = (com.xiaomi.push.service.ak.b) it.next();
            if (TextUtils.equals(n, bVar.b)) {
                return bVar;
            }
            if (TextUtils.equals(m, bVar.b)) {
                return bVar;
            }
        }
        return null;
    }

    public void a(Context context) {
        Intent intent = new Intent();
        intent.setAction("com.xiaomi.push.service_started");
        context.sendBroadcast(intent);
    }

    public void a(Context context, com.xiaomi.push.service.ak.b bVar, int i) {
        if (!"5".equalsIgnoreCase(bVar.h)) {
            Intent intent = new Intent();
            intent.setAction("com.xiaomi.push.channel_closed");
            intent.setPackage(bVar.a);
            intent.putExtra(am.q, bVar.h);
            intent.putExtra("ext_reason", i);
            intent.putExtra(am.p, bVar.b);
            intent.putExtra(am.B, bVar.j);
            a(context, intent, bVar.a);
        }
    }

    public void a(Context context, com.xiaomi.push.service.ak.b bVar, String str, String str2) {
        if ("5".equalsIgnoreCase(bVar.h)) {
            com.xiaomi.channel.commonutils.logger.b.d("mipush kicked by server");
            return;
        }
        Intent intent = new Intent();
        intent.setAction("com.xiaomi.push.kicked");
        intent.setPackage(bVar.a);
        intent.putExtra("ext_kick_type", str);
        intent.putExtra("ext_kick_reason", str2);
        intent.putExtra("ext_chid", bVar.h);
        intent.putExtra(am.p, bVar.b);
        intent.putExtra(am.B, bVar.j);
        a(context, intent, bVar.a);
    }

    public void a(Context context, com.xiaomi.push.service.ak.b bVar, boolean z, int i, String str) {
        if ("5".equalsIgnoreCase(bVar.h)) {
            this.a.a(context, bVar, z, i, str);
            return;
        }
        Intent intent = new Intent();
        intent.setAction("com.xiaomi.push.channel_opened");
        intent.setPackage(bVar.a);
        intent.putExtra("ext_succeeded", z);
        if (!z) {
            intent.putExtra("ext_reason", i);
        }
        if (!TextUtils.isEmpty(str)) {
            intent.putExtra("ext_reason_msg", str);
        }
        intent.putExtra("ext_chid", bVar.h);
        intent.putExtra(am.p, bVar.b);
        intent.putExtra(am.B, bVar.j);
        a(context, intent, bVar.a);
    }

    public void a(XMPushService xMPushService, String str, com.xiaomi.slim.b bVar) {
        com.xiaomi.push.service.ak.b a = a(bVar);
        if (a == null) {
            com.xiaomi.channel.commonutils.logger.b.d("error while notify channel closed! channel " + str + " not registered");
        } else if ("5".equalsIgnoreCase(str)) {
            this.a.a(xMPushService, bVar, a);
        } else {
            com.xiaomi.channel.commonutils.logger.b.a("don't support binary yet");
        }
    }

    public void a(XMPushService xMPushService, String str, d dVar) {
        com.xiaomi.push.service.ak.b a = a(dVar);
        if (a == null) {
            com.xiaomi.channel.commonutils.logger.b.d("error while notify channel closed! channel " + str + " not registered");
        } else if ("5".equalsIgnoreCase(str)) {
            this.a.a(xMPushService, dVar, a);
        } else {
            String str2;
            String str3 = a.a;
            if (dVar instanceof c) {
                str2 = "com.xiaomi.push.new_msg";
            } else if (dVar instanceof com.xiaomi.smack.packet.b) {
                str2 = "com.xiaomi.push.new_iq";
            } else if (dVar instanceof f) {
                str2 = "com.xiaomi.push.new_pres";
            } else {
                com.xiaomi.channel.commonutils.logger.b.d("unknown packet type, drop it");
                return;
            }
            Intent intent = new Intent();
            intent.setAction(str2);
            intent.setPackage(str3);
            intent.putExtra("ext_chid", str);
            intent.putExtra("ext_packet", dVar.b());
            intent.putExtra(am.B, a.j);
            intent.putExtra(am.u, a.i);
            a((Context) xMPushService, intent, str3);
        }
    }
}
