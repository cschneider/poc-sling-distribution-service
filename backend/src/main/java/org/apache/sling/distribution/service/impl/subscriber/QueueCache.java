package org.apache.sling.distribution.service.impl.subscriber;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

import javax.ws.rs.container.AsyncResponse;
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
    private final NavigableMap<Long, Set<AsyncResponse>> listeners;
    private final String queueId;

    public QueueCache(String queueId) {
        this.queueId = queueId;
        this.packages = new ConcurrentSkipListMap<>();
        this.listeners = new ConcurrentSkipListMap<>();
    }

    public Stream<PackageMessageMeta> packagesFrom(long position) {
        return packages.tailMap(position).values().stream();
    }
    
    public Stream<PackageMessageMeta> packagesAfter(long position) {
        return packages.tailMap(position, false).values().stream();
    }

    public synchronized void addPackage(PackageMessageMeta pkg) {
        long position = pkg.getPosition();
        packages.put(position, pkg);
        listeners.headMap(position).entrySet()
            .forEach(this::processListener);
    }
    
    private void processListener(Entry<Long, Set<AsyncResponse>> listener) {
        long position = listener.getKey();
        PackageMessageMeta nextPackage = packages.ceilingEntry(position).getValue();
        listener.getValue().stream()
            .forEach(resp -> resp.resume(nextPackage));
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

    public synchronized void register(long position, AsyncResponse response) {
        Entry<Long, PackageMessageMeta> entry = packages.ceilingEntry(position);
        if (entry != null) {
            response.resume(entry.getValue());
         } else {
            Set<AsyncResponse> responses = listeners.computeIfAbsent(position, pos -> new HashSet<>());
            responses.add(response);
         }
    }

}
