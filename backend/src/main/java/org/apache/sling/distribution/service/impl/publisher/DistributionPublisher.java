package org.apache.sling.distribution.service.impl.publisher;

import org.apache.sling.distribution.service.PackageMessageMeta;

public interface DistributionPublisher {
    void publish(PackageMessageMeta pkg);
}
