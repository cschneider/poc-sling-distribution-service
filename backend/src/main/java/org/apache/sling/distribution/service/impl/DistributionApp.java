package org.apache.sling.distribution.service.impl;

import java.util.Set;

import javax.ws.rs.core.Application;

public class DistributionApp extends Application {

    @Override
    public Set<Object> getSingletons() {
        QueuesResource queuesResource = new QueuesResource();
        StatusResource statusResource = new StatusResource();
        DiscoveryResource discoveryResource = new DiscoveryResource();
        BinaryResource binaryResource = new BinaryResource();
        return Set.of(queuesResource, statusResource, discoveryResource, binaryResource);
    }

}
