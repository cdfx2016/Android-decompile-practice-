package org.jivesoftware.smackx;

import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.PrivateDataManager.PrivateDataIQProvider;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager;
import org.jivesoftware.smackx.bytestreams.ibb.packet.DataPacketExtension;
import org.jivesoftware.smackx.bytestreams.ibb.provider.CloseIQProvider;
import org.jivesoftware.smackx.bytestreams.ibb.provider.DataPacketProvider;
import org.jivesoftware.smackx.bytestreams.ibb.provider.OpenIQProvider;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamManager;
import org.jivesoftware.smackx.bytestreams.socks5.provider.BytestreamsProvider;
import org.jivesoftware.smackx.carbons.Carbon;
import org.jivesoftware.smackx.entitycaps.EntityCapsManager;
import org.jivesoftware.smackx.entitycaps.provider.CapsExtensionProvider;
import org.jivesoftware.smackx.forward.Forwarded;
import org.jivesoftware.smackx.packet.AttentionExtension;
import org.jivesoftware.smackx.packet.ChatStateExtension.Provider;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.packet.HeadersExtension;
import org.jivesoftware.smackx.packet.LastActivity;
import org.jivesoftware.smackx.packet.MessageEvent;
import org.jivesoftware.smackx.packet.Nick;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.ping.provider.PingProvider;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DelayInformationProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.HeaderProvider;
import org.jivesoftware.smackx.provider.HeadersProvider;
import org.jivesoftware.smackx.provider.MUCAdminProvider;
import org.jivesoftware.smackx.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.provider.MUCUserProvider;
import org.jivesoftware.smackx.provider.MessageEventProvider;
import org.jivesoftware.smackx.provider.MultipleAddressesProvider;
import org.jivesoftware.smackx.provider.RosterExchangeProvider;
import org.jivesoftware.smackx.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.provider.XHTMLExtensionProvider;
import org.jivesoftware.smackx.pubsub.provider.AffiliationProvider;
import org.jivesoftware.smackx.pubsub.provider.AffiliationsProvider;
import org.jivesoftware.smackx.pubsub.provider.ConfigEventProvider;
import org.jivesoftware.smackx.pubsub.provider.EventProvider;
import org.jivesoftware.smackx.pubsub.provider.FormNodeProvider;
import org.jivesoftware.smackx.pubsub.provider.ItemProvider;
import org.jivesoftware.smackx.pubsub.provider.ItemsProvider;
import org.jivesoftware.smackx.pubsub.provider.PubSubProvider;
import org.jivesoftware.smackx.pubsub.provider.RetractEventProvider;
import org.jivesoftware.smackx.pubsub.provider.SimpleNodeProvider;
import org.jivesoftware.smackx.pubsub.provider.SubscriptionProvider;
import org.jivesoftware.smackx.pubsub.provider.SubscriptionsProvider;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;
import org.jivesoftware.smackx.search.UserSearch;

public class ConfigureProviderManager {
    public static void configureProviderManager() {
        ProviderManager instance = ProviderManager.getInstance();
        instance.addIQProvider("query", "jabber:iq:private", new PrivateDataIQProvider());
        try {
            instance.addIQProvider("query", "jabber:iq:time", Class.forName("org.jivesoftware.smackx.packet.Time"));
        } catch (ClassNotFoundException e) {
            System.err.println("Can't load class for org.jivesoftware.smackx.packet.Time");
        }
        instance.addExtensionProvider("x", "jabber:x:roster", new RosterExchangeProvider());
        instance.addExtensionProvider("x", "jabber:x:event", new MessageEventProvider());
        instance.addExtensionProvider("active", "http://jabber.org/protocol/chatstates", new Provider());
        instance.addExtensionProvider(MessageEvent.COMPOSING, "http://jabber.org/protocol/chatstates", new Provider());
        instance.addExtensionProvider("paused", "http://jabber.org/protocol/chatstates", new Provider());
        instance.addExtensionProvider("inactive", "http://jabber.org/protocol/chatstates", new Provider());
        instance.addExtensionProvider("gone", "http://jabber.org/protocol/chatstates", new Provider());
        instance.addExtensionProvider("html", "http://jabber.org/protocol/xhtml-im", new XHTMLExtensionProvider());
        instance.addExtensionProvider("x", GroupChatInvitation.NAMESPACE, new GroupChatInvitation.Provider());
        instance.addIQProvider("query", DiscoverItems.NAMESPACE, new DiscoverItemsProvider());
        instance.addIQProvider("query", DiscoverInfo.NAMESPACE, new DiscoverInfoProvider());
        instance.addExtensionProvider("x", Form.NAMESPACE, new DataFormProvider());
        instance.addExtensionProvider("x", "http://jabber.org/protocol/muc#user", new MUCUserProvider());
        instance.addIQProvider("query", "http://jabber.org/protocol/muc#admin", new MUCAdminProvider());
        instance.addIQProvider("query", "http://jabber.org/protocol/muc#owner", new MUCOwnerProvider());
        instance.addExtensionProvider("x", "jabber:x:delay", new DelayInformationProvider());
        instance.addExtensionProvider("delay", "urn:xmpp:delay", new DelayInformationProvider());
        try {
            instance.addIQProvider("query", "jabber:iq:version", Class.forName("org.jivesoftware.smackx.packet.Version"));
        } catch (ClassNotFoundException e2) {
            System.err.println("Can't load class for org.jivesoftware.smackx.packet.Version");
        }
        instance.addIQProvider("vCard", "vcard-temp", new VCardProvider());
        instance.addIQProvider(MessageEvent.OFFLINE, "http://jabber.org/protocol/offline", new OfflineMessageRequest.Provider());
        instance.addExtensionProvider(MessageEvent.OFFLINE, "http://jabber.org/protocol/offline", new OfflineMessageInfo.Provider());
        instance.addIQProvider("query", LastActivity.NAMESPACE, new LastActivity.Provider());
        instance.addIQProvider("query", "jabber:iq:search", new UserSearch.Provider());
        instance.addIQProvider("sharedgroup", "http://www.jivesoftware.org/protocol/sharedgroup", new SharedGroupsInfo.Provider());
        instance.addExtensionProvider("addresses", "http://jabber.org/protocol/address", new MultipleAddressesProvider());
        instance.addIQProvider("si", "http://jabber.org/protocol/si", new StreamInitiationProvider());
        instance.addIQProvider("query", Socks5BytestreamManager.NAMESPACE, new BytestreamsProvider());
        instance.addIQProvider("open", InBandBytestreamManager.NAMESPACE, new OpenIQProvider());
        instance.addIQProvider(DataPacketExtension.ELEMENT_NAME, InBandBytestreamManager.NAMESPACE, new DataPacketProvider());
        instance.addIQProvider("close", InBandBytestreamManager.NAMESPACE, new CloseIQProvider());
        instance.addExtensionProvider(DataPacketExtension.ELEMENT_NAME, InBandBytestreamManager.NAMESPACE, new DataPacketProvider());
        instance.addIQProvider("query", "jabber:iq:privacy", new PrivacyProvider());
        instance.addExtensionProvider("headers", HeadersExtension.NAMESPACE, new HeadersProvider());
        instance.addExtensionProvider("header", HeadersExtension.NAMESPACE, new HeaderProvider());
        instance.addIQProvider("pubsub", "http://jabber.org/protocol/pubsub", new PubSubProvider());
        instance.addExtensionProvider("create", "http://jabber.org/protocol/pubsub", new SimpleNodeProvider());
        instance.addExtensionProvider("items", "http://jabber.org/protocol/pubsub", new ItemsProvider());
        instance.addExtensionProvider("item", "http://jabber.org/protocol/pubsub", new ItemProvider());
        instance.addExtensionProvider("subscriptions", "http://jabber.org/protocol/pubsub", new SubscriptionsProvider());
        instance.addExtensionProvider("subscription", "http://jabber.org/protocol/pubsub", new SubscriptionProvider());
        instance.addExtensionProvider("affiliations", "http://jabber.org/protocol/pubsub", new AffiliationsProvider());
        instance.addExtensionProvider("affiliation", "http://jabber.org/protocol/pubsub", new AffiliationProvider());
        instance.addExtensionProvider("options", "http://jabber.org/protocol/pubsub", new FormNodeProvider());
        instance.addIQProvider("pubsub", "http://jabber.org/protocol/pubsub#owner", new PubSubProvider());
        instance.addExtensionProvider("configure", "http://jabber.org/protocol/pubsub#owner", new FormNodeProvider());
        instance.addExtensionProvider("default", "http://jabber.org/protocol/pubsub#owner", new FormNodeProvider());
        instance.addExtensionProvider("event", "http://jabber.org/protocol/pubsub#event", new EventProvider());
        instance.addExtensionProvider("configuration", "http://jabber.org/protocol/pubsub#event", new ConfigEventProvider());
        instance.addExtensionProvider("delete", "http://jabber.org/protocol/pubsub#event", new SimpleNodeProvider());
        instance.addExtensionProvider("options", "http://jabber.org/protocol/pubsub#event", new FormNodeProvider());
        instance.addExtensionProvider("items", "http://jabber.org/protocol/pubsub#event", new ItemsProvider());
        instance.addExtensionProvider("item", "http://jabber.org/protocol/pubsub#event", new ItemProvider());
        instance.addExtensionProvider("retract", "http://jabber.org/protocol/pubsub#event", new RetractEventProvider());
        instance.addExtensionProvider("purge", "http://jabber.org/protocol/pubsub#event", new SimpleNodeProvider());
        instance.addExtensionProvider(Nick.ELEMENT_NAME, Nick.NAMESPACE, new Nick.Provider());
        instance.addExtensionProvider(AttentionExtension.ELEMENT_NAME, AttentionExtension.NAMESPACE, new AttentionExtension.Provider());
        instance.addExtensionProvider("forwarded", "urn:xmpp:forward:0", new Forwarded.Provider());
        instance.addExtensionProvider("sent", "urn:xmpp:carbons:2", new Carbon.Provider());
        instance.addExtensionProvider("received", "urn:xmpp:carbons:2", new Carbon.Provider());
        instance.addIQProvider("ping", "urn:xmpp:ping", new PingProvider());
        instance.addExtensionProvider("received", "urn:xmpp:receipts", new DeliveryReceipt.Provider());
        instance.addExtensionProvider("request", "urn:xmpp:receipts", new DeliveryReceiptRequest.Provider());
        instance.addExtensionProvider(EntityCapsManager.ELEMENT, EntityCapsManager.NAMESPACE, new CapsExtensionProvider());
    }
}
