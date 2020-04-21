package org.apache.sling.distribution.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.sling.distribution.service.impl.binary.BinaryRepository;

@Path("binary")
public class BinaryResource {
    @Context 
    private UriInfo uriInfo;
    
    private final BinaryRepository repository;
    
    public BinaryResource(BinaryRepository repository) {
        this.repository = repository;
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response upload(InputStream is) throws IOException {
        String id = repository.upload(is);
        URI location = uriInfo.getAbsolutePathBuilder().path(id).build();
        return Response.created(location).build();
    }
    
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response get(@PathParam("id") String id) {
        StreamingOutput stream = new StreamingOutput() {

            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                repository.get(id, output);
            }
            
        };
        return Response.ok(stream).build();
    }
}
