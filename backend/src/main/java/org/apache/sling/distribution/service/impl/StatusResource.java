package org.apache.sling.distribution.service.impl;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.apache.sling.distribution.service.SubscriberStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;

@Path("status")
public class StatusResource {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @POST
    @Operation(description = "The approving subscriber is expected to send the status of each processed package to this endpoint")
    public void status(SubscriberStatus status) {
        log.info(status.toString());
    }
}
