package org.jivesoftware.smackx.bytestreams.ibb;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.bytestreams.BytestreamRequest;
import org.jivesoftware.smackx.bytestreams.ibb.packet.Open;

public class InBandBytestreamRequest implements BytestreamRequest {
    private final Open byteStreamRequest;
    private final InBandBytestreamManager manager;

    protected InBandBytestreamRequest(InBandBytestreamManager inBandBytestreamManager, Open open) {
        this.manager = inBandBytestreamManager;
        this.byteStreamRequest = open;
    }

    public InBandBytestreamSession accept() throws XMPPException {
        Connection connection = this.manager.getConnection();
        InBandBytestreamSession inBandBytestreamSession = new InBandBytestreamSession(connection, this.byteStreamRequest, this.byteStreamRequest.getFrom());
        this.manager.getSessions().put(this.byteStreamRequest.getSessionID(), inBandBytestreamSession);
        connection.sendPacket(IQ.createResultIQ(this.byteStreamRequest));
        return inBandBytestreamSession;
    }

    public String getFrom() {
        return this.byteStreamRequest.getFrom();
    }

    public String getSessionID() {
        return this.byteStreamRequest.getSessionID();
    }

    public void reject() {
        this.manager.replyRejectPacket(this.byteStreamRequest);
    }
}
