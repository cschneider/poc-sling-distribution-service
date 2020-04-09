package org.apache.sling.distribution.service.impl;

import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.sling.distribution.service.impl.subcriber.PackageRepository;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationBase;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;

import com.codahale.metrics.MetricRegistry;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;

@Component(service = Application.class)
@JaxrsApplicationBase("/distribution")
@JaxrsName(".default")
public class DistributionApp extends Application {

    @Reference
    MetricRegistry metricRegistry;
    
    @Reference
    PackageRepository repository;
    
    @Override
    public Set<Object> getSingletons() {
        QueuesResource queuesResource = new QueuesResource(metricRegistry, repository);
        StatusResource statusResource = new StatusResource();
        DiscoveryResource discoveryResource = new DiscoveryResource();
        BinaryResource binaryResource = new BinaryResource();
        return Set.of(
                queuesResource, 
                statusResource, 
                discoveryResource, 
                binaryResource,
                new OpenApiResource());
    }

}
