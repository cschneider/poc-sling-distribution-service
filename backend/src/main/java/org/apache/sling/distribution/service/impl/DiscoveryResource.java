package org.apache.sling.distribution.service.impl;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.apache.sling.distribution.service.Discovery;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;

@Component(service = DiscoveryResource.class)
@JaxrsResource
@JSONRequired
@Path("discovery")
public class DiscoveryResource {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @POST
    @Operation(description = "Subscribers are expected to send their status this this endpoint regularly")
    public void discovery(Discovery discovery) {
        log.info(discovery.toString());
    }
}
