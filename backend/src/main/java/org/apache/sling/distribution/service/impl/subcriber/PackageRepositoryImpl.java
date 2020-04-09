package org.apache.sling.distribution.service.impl.subcriber;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

import org.apache.sling.distribution.journal.HandlerAdapter;
import org.apache.sling.distribution.journal.MessageInfo;
import org.apache.sling.distribution.journal.MessageSender;
import org.apache.sling.distribution.journal.MessagingProvider;
import org.apache.sling.distribution.journal.Reset;
import org.apache.sling.distribution.journal.messages.Messages.PackageMessage;
import org.apache.sling.distribution.service.PackageMessageMeta;
import org.apache.sling.distribution.service.PackageMessageMeta.ReqType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.protobuf.GeneratedMessage;

@Component(property = "type=journal")
public class PackageRepositoryImpl implements Closeable, PackageRepository {

    private static final String DEFAULT_PACKAGE_TOPIC = "package";
    private final NavigableMap<Long, PackageMessageMeta> packages;
    private final Closeable consumer;
    private final MessageSender<GeneratedMessage> sender;
    private final String topicName;

    @Activate
    public PackageRepositoryImpl(
            @Reference MessagingProvider messagingProvider
            ) {
        this.topicName = DEFAULT_PACKAGE_TOPIC;
        consumer = messagingProvider.createPoller(topicName, Reset.earliest,
                HandlerAdapter.create(PackageMessage.class, this::handlePackage));
        sender = messagingProvider.createSender();
        packages = new TreeMap<>();
        packages.put(1l, getPackage("prod", 1, false));
    }

    @Override
    public void publish(PackageMessageMeta pkgMeta) {
        PackageMessage pkgMessage = PackageMessageMetaConverter.toPackageMessage(pkgMeta);
        sender.send(topicName, pkgMessage);
    }

    @Override
    public synchronized Optional<PackageMessageMeta> getNextPackage(String queueId, Long position) {
        Entry<Long, PackageMessageMeta> entry = packages.ceilingEntry(position);
        return entry != null ? Optional.of(entry.getValue()) : Optional.empty();
    }

    @Override
    public synchronized List<PackageMessageMeta> getPackages(String queueId, long position, Integer limit) {
        long effectiveLimit = limit == null ? 30 : limit;
        Long curPosition = packages.ceilingKey(position);
        int c = 0;
        List<PackageMessageMeta> result = new ArrayList<>();
        while (c < effectiveLimit && curPosition != null) {
            PackageMessageMeta curPkg = packages.get(curPosition);
            result.add(curPkg);
            curPosition = packages.higherKey(curPosition);
        }
        return result;
    }

    @Override
    public synchronized Optional<PackageMessageMeta> getPackage(String queueId, Long position) {
        PackageMessageMeta pkg = packages.get(position);
        return Optional.ofNullable(pkg);
    }

    private synchronized void handlePackage(MessageInfo info, PackageMessage message) {
        PackageMessageMeta pkg = PackageMessageMetaConverter.convert(info, message);
        packages.put(info.getOffset(), pkg);
    }

    @Override
    public void close() throws IOException {
        consumer.close();
        packages.clear();
    }

    private PackageMessageMeta getPackage(String queueId, long position, boolean showQueueLink) {
        String pkgId = "pk" + position;
        Link binaryLink = Link.fromUriBuilder(queuePackgesUri(queueId).path(position + ".zip")).build();
        Link selfLink = Link.fromUriBuilder(queuePackgesUri(queueId).path("" + position)).build();
        Link queueLink = Link.fromUriBuilder(queuePackgesUri(queueId)).build();
        Map<String, Link> links = showQueueLink
                ? Map.of("self", selfLink, "contentPackage", binaryLink, "queue", queueLink)
                : Map.of("self", selfLink, "binary", binaryLink);
        return PackageMessageMeta.builder().pkgId(pkgId).position(position)
                .pubSlingId("pubSlingId").pubAgentName("pubAgentName").reqType(ReqType.ADD).pkgType("pkgType")
                .links(links).userId("userId").paths(Collections.singletonList("/test"))
                .deepPaths(Collections.emptyList()).build();
    }

    private UriBuilder queuePackgesUri(String queueId) {
        return UriBuilder.fromPath("/distribution/queues/" + queueId + "/packages");
    }
}
