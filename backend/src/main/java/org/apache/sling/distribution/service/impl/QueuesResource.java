package org.apache.sling.distribution.service.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.sling.distribution.service.DistributionEvent;
import org.apache.sling.distribution.service.DistributionQueueInfo;
import org.apache.sling.distribution.service.DistributionQueueInfo.DistributionQueueInfoBuilder;
import org.apache.sling.distribution.service.Domain;
import org.apache.sling.distribution.service.Organization;
import org.apache.sling.distribution.service.PackageMessageMeta;
import org.apache.sling.distribution.service.QueuePackages;
import org.apache.sling.distribution.service.impl.publisher.DistributionPublisher;
import org.apache.sling.distribution.service.impl.subscriber.DomainCache;
import org.apache.sling.distribution.service.impl.subscriber.OrgCache;
import org.apache.sling.distribution.service.impl.subscriber.PackageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@Path("domains")
public class QueuesResource {
    static final String HEADER_IMS_ORG = "x-gw-ims-org-id";
    static final String APPLICATION_HAL_JSON = "application/hal+json";
    
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Context 
    private UriInfo uriInfo;
    
    @Context 
    private HttpHeaders headers;
    
    private final DistributionPublisher publisher;
    private final PackageRepository repository;
    private final Counter queuesCounter;

    public QueuesResource(MetricRegistry metricRegistry, DistributionPublisher publisher, PackageRepository repository) {
        this.publisher = publisher;
        this.repository = repository;
        this.queuesCounter = metricRegistry.counter("getQueues");
    }
    
    
    @GET
    @Path("")
    @Produces(APPLICATION_HAL_JSON)
    @Operation(description =  "List available replication domains of this IMS Org")
    public Organization getDomains() {
        String imsOrg = getImsOrg();
        OrgCache orgCache = repository.getOrg(imsOrg);
        return orgCache.self(uriInfo.getAbsolutePathBuilder());
    }

    @GET
    @Path("{domainId}")
    @Produces(APPLICATION_HAL_JSON)
    @Operation(description =  "List available queues")
    public Domain getDomain(
            @PathParam("domainId") String domainId) {
        queuesCounter.inc();
        String imsOrg = getImsOrg();
        OrgCache orgCache = repository.getOrg(imsOrg);
        DomainCache domain = orgCache.getDomain(domainId);
        return domain.self(uriInfo.getAbsolutePathBuilder());
    }
    
    @GET
    @Path("{domainId}/queues/{queueId}")
    @Produces(APPLICATION_HAL_JSON)
    @Operation(description = "Get queue info")
    public DistributionQueueInfo getQueueInfo(
            @PathParam("domainId") String domain,
            @PathParam("queueId") String queueId) {
        Link domainLink = Link.fromUriBuilder(uriInfo.getAbsolutePathBuilder().path("..")).build();
        Link selfLink = Link.fromUriBuilder(uriInfo.getAbsolutePathBuilder()).build();
        Link packagesLink = Link.fromUriBuilder(uriInfo.getAbsolutePathBuilder().path("packages")).build();
        Link eventsLink = Link.fromUriBuilder(uriInfo.getAbsolutePathBuilder().path("events")).build();

        Map<String, Link> links = Map.of(
                "self", selfLink,
                "packages", packagesLink,
                "events", eventsLink,
                "domain", domainLink);
        return createQueue(queueId, uriInfo.getAbsolutePathBuilder()).links(links).build();
    }
    
    @GET
    @Path("{domainId}/queues/{queueId}/next/{position}")
    public void getNext(
            @PathParam("domainId") String domainId,
            @PathParam("queueId") String queueId, 
            @PathParam("position") long position, 
            @Parameter(description = "Get only package appliedBy by the named subscriber")
            @QueryParam("appliedBy") String appliedBy,
            @Suspended final AsyncResponse response) {
        String imsOrg = getImsOrg();
        response.setTimeout(10, TimeUnit.SECONDS);
        response.setTimeoutHandler(res -> res.cancel(10));
        repository.getOrg(imsOrg).getDomain(domainId).getQueue(queueId).register(position, response);
    }
    
    @GET
    @Path("{domainId}/queues/{queueId}/packages")
    @Produces(APPLICATION_HAL_JSON)
    @Operation(description =  "Get queue contents. Paged view")
    public QueuePackages getQueuePackages(
            @PathParam("domainId") String domainId,
            @PathParam("queueId") String queueId, 
            @Parameter(allowEmptyValue = true, description = "Position to start from. If emtpy starts from position of oldest package.")
            @QueryParam("position") long position, 
            @Parameter(description = "Maximum number of packages to return. -1 will fetch all.")
            @QueryParam("limit") Integer limit,
            @Parameter(description = "Get only packages that are appliedBy by the named subscriber")
            @QueryParam("appliedBy") String appliedBy) {
        long effectiveLimit = limit == null ? Long.MAX_VALUE : limit;
        String imsOrg = getImsOrg();
        List<PackageMessageMeta> packages = repository.getOrg(imsOrg).getDomain(domainId).getQueue(queueId).packagesFrom(position)
                .limit(effectiveLimit).collect(Collectors.toList());
        Link selfLink = Link.fromUriBuilder(uriInfo.getAbsolutePathBuilder()).build();
        Link queueLink = Link.fromUriBuilder(queueUri(domainId, queueId)).build();
        Map<String, Link> links = Map.of(
                "self", selfLink,
                "queue", queueLink);
        return QueuePackages.builder().packages(packages).links(links).build();
    }
    
    @DELETE
    @Path("{domainId}/queues/{queueId}")
    @Operation(description = "Delete a number of packages starting with oldest. By default one package is deleted.")
    public void deleteDistributionPackage(
            @PathParam("domainId") String domainId,
            @PathParam("queueId") String queueId,
            @QueryParam("limit") long limit
            ) throws IOException {
        log.info("Deleting {} packages", limit);
    }
    
    @POST
    @Path("{domainId}/queues/{queueId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Upload and distribute a content package")
    public void publishPackage(
            @PathParam("domainId") String domainId,
            @PathParam("queueId") String queueId,
            PackageMessageMeta pkgMeta) throws IOException {
        publisher.publish(pkgMeta);
    }
    
    @GET
    @Path("{domainId}/queues/{queueId}/packages/{position}")
    @Produces(APPLICATION_HAL_JSON)
    @Operation(description = "Get meta data of a single package")
    public Response getPackageMeta(
            @PathParam("domainId") String domainId,
            @PathParam("queueId") String queueId,
            @PathParam("position") Long position) {
        String imsOrg = getImsOrg();
        Optional<PackageMessageMeta> pkg = repository.getOrg(imsOrg).getDomain(domainId).getQueue(queueId).packagesFrom(position).findFirst();
        boolean found = pkg.isPresent() && pkg.get().getPubAgentName().equals(queueId);
        return found 
                ? Response.ok(pkg.get()).build() 
                : Response.status(404).build();
    }

    @GET
    @Path("{domainId}/queues/{queueId}/events")
    @Produces(APPLICATION_HAL_JSON)
    @Operation(description = "Get distribution events. Paged view")
    public List<DistributionEvent> getEvents(
            @PathParam("queueId") String queueId, 
            @Parameter(allowEmptyValue = true, description = "Event position to start from. If emtpy starts from position of oldest event.")
            @QueryParam("position") String position, 
            @Parameter(description = "Maximum number of events to return.")
            @QueryParam("limit") long limit) {
        return Arrays.asList(DistributionEvent.builder()
                .queueId(queueId)
                .offset(1000)
                .packageId("pkg1000")
                .build());
    }

    private String getImsOrg() {
        String orgFromHeader = headers.getHeaderString(HEADER_IMS_ORG);
        if (orgFromHeader != null) {
            return orgFromHeader;
        }
        Cookie cookie = headers.getCookies().get(HEADER_IMS_ORG);
        if (cookie == null) {
            throw new RuntimeException("Must set header or cookie " + HEADER_IMS_ORG);
        }
        return cookie.getValue();
    }


    private UriBuilder queueUri(String domainId, String queueId) {
        return uriInfo.getBaseUriBuilder().path(QueuesResource.class).path(domainId).path("queues").path(queueId);
    }

    private DistributionQueueInfoBuilder createQueue(String queueId, UriBuilder selfUri) {
        Link selfLink = Link.fromUriBuilder(selfUri).build();
        return DistributionQueueInfo.builder()
                .id(queueId)
                .links(Map.of("self", selfLink));
    }
}
