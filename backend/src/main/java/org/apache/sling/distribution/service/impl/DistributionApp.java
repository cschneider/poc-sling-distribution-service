package org.apache.sling.distribution.service.impl;

import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.sling.distribution.service.impl.binary.BinaryRepository;
import org.apache.sling.distribution.service.impl.publisher.DistributionPublisher;
import org.apache.sling.distribution.service.impl.subscriber.PackageRepository;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardResource;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationBase;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;

import com.codahale.metrics.MetricRegistry;

@Component(service = Application.class)
@JaxrsApplicationBase("/distribution")
@JaxrsName(".default")
@HttpWhiteboardResource(pattern = "/openapi.json", prefix = "/META-INF/classes/openapi.json")
public class DistributionApp extends Application {

    private final MetricRegistry metricRegistry;
    private final DistributionPublisher publisher;
    private final PackageRepository repository;
    private final BinaryRepository binaryRepository;
    
    @Activate
    public DistributionApp(
            @Reference MetricRegistry metricRegistry,
            @Reference DistributionPublisher publisher,
            @Reference PackageRepository repository,
            @Reference BinaryRepository binaryRepository) {
                this.metricRegistry = metricRegistry;
                this.publisher = publisher;
                this.repository = repository;
                this.binaryRepository = binaryRepository;
    }
    
    @Override
    public Set<Object> getSingletons() {
        QueuesResource queuesResource = new QueuesResource(metricRegistry, publisher, repository);
        StatusResource statusResource = new StatusResource();
        DiscoveryResource discoveryResource = new DiscoveryResource();
        BinaryResource binaryResource = new BinaryResource(binaryRepository);
        OrgsResource orgResource = new OrgsResource(repository);
        return Set.of(
                queuesResource, 
                statusResource, 
                discoveryResource, 
                binaryResource,
                orgResource);
    }

}
