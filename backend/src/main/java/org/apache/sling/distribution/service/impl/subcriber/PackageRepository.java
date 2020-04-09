package org.apache.sling.distribution.service.impl.subcriber;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.sling.distribution.service.PackageMessageMeta;

public interface PackageRepository {

    Optional<PackageMessageMeta> getNextPackage(String queueId, Long position);

    List<PackageMessageMeta> getPackages(String queueId, long position, Integer limit);

    Optional<PackageMessageMeta> getPackage(String queueId, Long position);

    void close() throws IOException;

    void publish(PackageMessageMeta pkgMeta);

}