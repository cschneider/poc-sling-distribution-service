package org.apache.sling.distribution.service.impl;

import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.sling.distribution.service.impl.subscriber.PackageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;

@Path("orgs")
public class OrgsResource {
    @SuppressWarnings("unused")
    private Logger log = LoggerFactory.getLogger(this.getClass());

    private final PackageRepository repository;
    
    @Context 
    private UriInfo uriInfo;

    public OrgsResource(PackageRepository repository) {
        this.repository = repository;
    }
    
    @GET
    @Path("")
    @Produces(QueuesResource.APPLICATION_HAL_JSON)
    @Operation(description =  "List available replication domains of this IMS Org")
    public Set<String>  getOrgs() {
        return repository.getOrgs();
    }
    
    @GET
    @Path("{imsOrgId}")
    @Produces(QueuesResource.APPLICATION_HAL_JSON)
    @Operation(description =  "List available replication domains of this IMS Org")
    public Response setOrg(@QueryParam("imsOrgId") String imsOrgId) {
        NewCookie cookie = new NewCookie(QueuesResource.HEADER_IMS_ORG, imsOrgId);
        Link link = Link.fromUriBuilder(uriInfo.getBaseUriBuilder().path(QueuesResource.class)).build(); 
        return Response.ok()
                .cookie(cookie)
                .links(link)
                .build();
    }

}
