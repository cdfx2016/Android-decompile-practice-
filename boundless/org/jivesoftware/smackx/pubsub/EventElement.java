package org.jivesoftware.smackx.pubsub;

import java.util.Arrays;
import java.util.List;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.pubsub.packet.PubSubNamespace;

public class EventElement implements EmbeddedPacketExtension {
    private NodeExtension ext;
    private EventElementType type;

    public EventElement(EventElementType eventElementType, NodeExtension nodeExtension) {
        this.type = eventElementType;
        this.ext = nodeExtension;
    }

    public String getElementName() {
        return "event";
    }

    public NodeExtension getEvent() {
        return this.ext;
    }

    public EventElementType getEventType() {
        return this.type;
    }

    public List<PacketExtension> getExtensions() {
        return Arrays.asList(new PacketExtension[]{getEvent()});
    }

    public String getNamespace() {
        return PubSubNamespace.EVENT.getXmlns();
    }

    public String toXML() {
        StringBuilder stringBuilder = new StringBuilder("<event xmlns='" + PubSubNamespace.EVENT.getXmlns() + "'>");
        stringBuilder.append(this.ext.toXML());
        stringBuilder.append("</event>");
        return stringBuilder.toString();
    }
}
