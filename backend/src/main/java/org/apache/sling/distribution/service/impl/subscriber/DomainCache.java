package org.apache.sling.distribution.service.impl.subscriber;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

import org.apache.sling.distribution.service.DistributionQueueInfo;
import org.apache.sling.distribution.service.Domain;
import org.apache.sling.distribution.service.PackageMessageMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomainCache {
    @SuppressWarnings("unused")
    private Logger log = LoggerFactory.getLogger(this.getClass());
    
    private final String domainId;
    private final Map<String, QueueCache> queues;
    
    public DomainCache(String domainId) {
        this.domainId = domainId;
        this.queues = new ConcurrentHashMap<>();
    }
    
    public void addPackage(PackageMessageMeta pkg) {
        QueueCache queue = getOrCreateQueue(pkg.getPubAgentName());
        queue.addPackage(pkg);
    }
    
    private QueueCache getOrCreateQueue(String queueId) {
        return queues.computeIfAbsent(queueId, QueueCache::new);
    }

    public QueueCache getQueue(String queueId) {
        return queues.get(queueId);
    }
    
    public String getName() {
        return domainId;
    }
    
    public Domain self(UriBuilder uriBuilder) {
        Link selfLink = Link.fromUriBuilder(uriBuilder).build();
        Map<String, Link> links = Map.of("self", selfLink);
        Map<String, DistributionQueueInfo> queues2 = new HashMap<>();
        for (Entry<String, QueueCache> entry : queues.entrySet()) {
            String queueId = entry.getKey();
            queues2.put(queueId, entry.getValue().self(uriBuilder.path("queues").path(queueId)));
        }
        return Domain.builder()
                .name(this.domainId)
                .queues(queues2)
                .links(links)
                .build();
    }
}
