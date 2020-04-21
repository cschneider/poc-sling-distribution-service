package org.apache.sling.distribution.service.impl.subscriber;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.sling.distribution.service.PackageMessageMeta;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = PackageRepository.class)
public class PackageRepository {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final Map<String, OrgCache> orgs;
    private int count;
    
    public PackageRepository() {
        this.orgs = new ConcurrentHashMap<>();
    }
    
    public void addPackage(PackageMessageMeta pkg) {
            this.count++;
            getOrg(pkg.getImsOrg()).addPackage(pkg);
            log.info("Adding package num={}, meta={}", this.count, pkg);
    }
    
    public OrgCache getOrg(String orgId) {
        return this.orgs.computeIfAbsent(orgId, OrgCache::new);
    }
    
    public Set<String> getOrgs() {
        return orgs.keySet();
    }

}
