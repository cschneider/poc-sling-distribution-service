package org.apache.sling.distribution.service.impl.subscriber;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

import org.apache.sling.distribution.service.DistributionQueueInfo;
import org.apache.sling.distribution.service.PackageMessageMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueCache implements Closeable {
    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private final NavigableMap<Long, PackageMessageMeta> packages;
    private final String queueId;

    public QueueCache(String queueId) {
        this.queueId = queueId;
        packages = new ConcurrentSkipListMap<>();
    }

    public Stream<PackageMessageMeta> packagesFrom(long position) {
        return packages.tailMap(position).values().stream();
    }
    
    public Stream<PackageMessageMeta> packagesAfter(long position) {
        return packages.tailMap(position, false).values().stream();
    }

    public void addPackage(PackageMessageMeta pkg) {
        packages.put(pkg.getPosition(), pkg);
    }

    public void close() throws IOException {
        packages.clear();
    }

    public DistributionQueueInfo self(UriBuilder uriBuilder) {
        Link selfLink = Link.fromUriBuilder(uriBuilder).build();
        Map<String, Link> links = Map.of("self", selfLink);
        return DistributionQueueInfo.builder()
                .id(queueId)
                .links(links)
                .build();
    }

}
