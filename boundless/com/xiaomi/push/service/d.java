package com.xiaomi.push.service;

import com.xiaomi.channel.commonutils.misc.f.a;
import com.xiaomi.push.service.module.b;
import java.util.Iterator;

public class d extends a {
    private XMPushService a;

    public d(XMPushService xMPushService) {
        this.a = xMPushService;
    }

    public int a() {
        return 15;
    }

    public void run() {
        Iterator it = g.a(this.a).a().iterator();
        while (it.hasNext()) {
            b bVar = (b) it.next();
            if (bVar.a() < System.currentTimeMillis()) {
                if (g.a(this.a).a(bVar.b()) == 0) {
                    com.xiaomi.channel.commonutils.logger.b.a("GeofenceDbCleaner delete a geofence message failed message_id:" + bVar.b());
                }
                s.a(this.a, s.a(bVar.d()), false, false, true);
            }
        }
    }
}
