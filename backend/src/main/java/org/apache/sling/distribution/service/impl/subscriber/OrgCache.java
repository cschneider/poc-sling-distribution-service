package org.apache.sling.distribution.service.impl.subscriber;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

import org.apache.sling.distribution.service.Domain;
import org.apache.sling.distribution.service.Organization;
import org.apache.sling.distribution.service.PackageMessageMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrgCache {
    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final Map<String, DomainCache> domains;
    private String orgId;
    
    public OrgCache(String orgId) {
        this.orgId = orgId;
        domains = new ConcurrentHashMap<>();
    }
    
    public void addPackage(PackageMessageMeta pkg) {
        DomainCache queue = getOrCreateDomain(pkg.getSource());
        queue.addPackage(pkg);
    }
    
    public DomainCache getDomain(String domainId) {
        return domains.get(domainId);
    }

    public Organization self(UriBuilder uriBuilder) {
        Link selfLink = Link.fromUriBuilder(uriBuilder).build();
        return Organization.builder()
                .id(orgId)
                .domains(getDomains(uriBuilder))
                .links(Map.of("self", selfLink))
                .build();
    }

    private DomainCache getOrCreateDomain(String domainId) {
        return domains.computeIfAbsent(domainId, DomainCache::new);
    }

    private List<Domain> getDomains(UriBuilder uriBuilder) {
        return domains.values().stream()
                .map(domainCache->domainCache.self(uriBuilder.clone().path(domainCache.getName())))
                .collect(Collectors.toList());
    }
}
