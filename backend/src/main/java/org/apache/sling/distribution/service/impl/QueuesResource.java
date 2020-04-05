package org.apache.sling.distribution.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.MultipartConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.apache.felix.metrics.PrometheusMetricsRegistry;
import org.apache.sling.distribution.service.DistributionEvent;
import org.apache.sling.distribution.service.DistributionQueueInfo;
import org.apache.sling.distribution.service.DistributionQueueInfo.DistributionQueueInfoBuilder;
import org.apache.sling.distribution.service.Environment;
import org.apache.sling.distribution.service.PackageMessageMeta;
import org.apache.sling.distribution.service.PackageMessageMeta.ReqType;
import org.apache.sling.distribution.service.QueuePackages;

import io.prometheus.client.Summary;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@Component(service = QueuesResource.class)
@JaxrsResource
@MultipartConfig
@Path("queues")
@SuppressWarnings("serial")
public class QueuesResource {
    private static final String APPLICATION_HAL_JSON = "application/hal+json";
    //private static final String APPLICATION_HAL_JSON = MediaType.APPLICATION_JSON;
    
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Context 
    private UriInfo uriInfo;
    
    private Map<String, DistributionQueueInfo> queues;
    private DistributionQueueInfo queueProd;
    private DistributionQueueInfo queueStage;

    public QueuesResource() {
    }
    
    @Reference
    private PrometheusMetricsRegistry metricsRegistry;
    private Summary requestLatency;

    @Activate
    protected void activate() {
        requestLatency = metricsRegistry.summary(
                "queues_latency",
                "latency summary for queues request",
                b -> b.quantile(0.5, 0.1)
                      .quantile(.75, 0.1)
        );
    }

    @GET
    @Produces(APPLICATION_HAL_JSON)
    @Operation(description =  "List available queues")
    public Environment getQueues() {
        try (Summary.Timer ignore = requestLatency.startTimer()) {
            queueProd = createQueue("stage").build();
            queueStage = createQueue("prod").build();
            queues = new HashMap<String, DistributionQueueInfo>() {{
                put("stage", queueStage);
                put("prod", queueProd);
            }};
            Link selfLink = Link.fromUriBuilder(uriInfo.getAbsolutePathBuilder()).build();
            Map<String, Link> links = new HashMap<String, Link>() {{
                put("self", selfLink);
            }};
            return Environment.builder().queues(queues).links(links).build();
        }
    }
    
    @GET
    @Path("{queueId}")
    @Produces(APPLICATION_HAL_JSON)
    @Operation(description = "Get queue info")
    public DistributionQueueInfo getQueueInfo(
            @PathParam("queueId") String queueId) {
        Link envLink = Link.fromUriBuilder(uriInfo.getBaseUriBuilder().path(QueuesResource.class)).build();
        Link selfLink = Link.fromUriBuilder(queuePackgesUri(queueId)).build();
        Link packagesLink = Link.fromUriBuilder(queuePackgesUri(queueId)).build();
        Link eventsLink = Link.fromUriBuilder(queueUri(queueId).path("events")).build();

        Map<String, Link> links = new HashMap<String, Link>() {{
                put("self", selfLink);
                put("packages", packagesLink);
                put("events", eventsLink);
                put("env", envLink);
                }};
        return createQueue(queueId).links(links).build();
    }

    @GET
    @Path("{queueId}/packages")
    @Produces(APPLICATION_HAL_JSON)
    @Operation(description =  "Get queue contents. Paged view")
    public QueuePackages getQueueMessages(
            @PathParam("queueId") String queueId, 
            @Parameter(allowEmptyValue = true, description = "Position to start from. If emtpy starts from position of oldest package.")
            @QueryParam("position") String position, 
            @Parameter(description = "Maximum number of packages to return. -1 will fetch all.")
            @QueryParam("limit") long limit,
            @Parameter(description = "Get only packages that are appliedBy by the named subscriber")
            @QueryParam("appliedBy") String appliedBy) {
        List<PackageMessageMeta> packages = Arrays.asList(
                getPackage(queueId, 1000, false), 
                getPackage(queueId, 1001, false));
        Link messagesLink = Link.fromUriBuilder(queuePackgesUri(queueId)).build();
        Link queueLink = Link.fromUriBuilder(queueUri(queueId)).build();

        Map<String, Link> links = new HashMap<String, Link>() {{
                put("self", messagesLink);
                put("queue", queueLink);
        }};
        return QueuePackages.builder().packages(packages).links(links).build();
    }
    
    @DELETE
    @Path("{queueId}")
    @Operation(description = "Delete a number of packages starting with oldest")
    public void postDistributionPackage(
            @QueryParam("limit") long limit
            ) throws IOException {
        log.info("Deleting {} packages", limit);
    }
    
    @POST
    @Path("{queueId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Upload and distribute a content package")
    public void postDistributionPackage(PackageMessageMeta pkgMeta) throws IOException {
        System.out.println(pkgMeta);
    }
    
    @GET
    @Path("{queueId}/messages/{position}")
    @Produces(APPLICATION_HAL_JSON)
    @Operation(description = "Get meta data of a single package")
    public PackageMessageMeta getPackageMeta(@PathParam("queueId") String queueId, @PathParam("position") long position) {
        return getPackage(queueId, position, true);
    }

    @GET
    @Path("{queueId}/messages/{position}.zip")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(description = "Get content of a package")
    public StreamingOutput getPackageContent(@PathParam("queueId") String queueId, @PathParam("position") long position) {
        return new StreamingOutput() {

            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                File packageFile = new File("content-package.zip");
                FileInputStream is = new FileInputStream(packageFile);
                IOUtils.copy(is, output);
                output.close();
                is.close();
            }};
    }
    
    @GET
    @Path("{queueId}/events")
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

    private PackageMessageMeta getPackage(String queueId, long position, boolean showQueueLink) {
        String pkgId = "pk" + position;
        Link binaryLink = Link.fromUriBuilder(queuePackgesUri(queueId).path(position + ".zip")).build();
        Link selfLink = Link.fromUriBuilder(queuePackgesUri(queueId).path("" + position)).build();
        Link queueLink = Link.fromUriBuilder(queuePackgesUri(queueId)).build();
        Map<String, Link> links = showQueueLink ? new HashMap<String, Link>() {{
                put("self", selfLink);
                put("contentPackage", binaryLink);
                put("queue", queueLink); }}
        : new HashMap<String, Link>() {{
                        put("self", selfLink);
                        put("binary", binaryLink);
        }};
        PackageMessageMeta package1 = PackageMessageMeta.builder()
                .pkgId(pkgId)
                .position(position)
                .pubSlingId("pubSlingId")
                .pubAgentName("pubAgentName")
                .reqType(ReqType.ADD)
                .pkgType("pkgType")
                .links(links)
                .userId("userId")
                .paths(Collections.singletonList("/test"))
                .deepPaths(Collections.emptyList())
                .build();
        return package1;
    }

    private UriBuilder queuePackgesUri(String queueId) {
        return queueUri(queueId).path("packages");
    }
    
    private UriBuilder queueUri(String queueId) {
        return uriInfo.getBaseUriBuilder().path(QueuesResource.class).path(queueId);
    }

    private DistributionQueueInfoBuilder createQueue(String queueId) {
        Link link = Link.fromUriBuilder(queueUri(queueId)).build();
        return DistributionQueueInfo.builder()
                .id(queueId)
                .size(20)
                .links(new HashMap<String, Link>() {{ put("self", link); }});
    }
}
