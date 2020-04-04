package org.apache.sling.distribution.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

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

@Path("binary")
public class BinaryResource {
    @Context 
    private UriInfo uriInfo;
    
    private java.nio.file.Path baseDir;
    
    public BinaryResource() {
        baseDir = java.nio.file.Path.of("target", "binaries");
        baseDir.toFile().mkdirs();
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response upload(InputStream is) throws IOException {
        String id = UUID.randomUUID().toString();
        java.nio.file.Path destFile = baseDir.resolve(id);
        System.out.println(destFile);
        Files.copy(is, destFile, StandardCopyOption.REPLACE_EXISTING);
        URI location = uriInfo.getAbsolutePathBuilder().path(id).build();
        return Response.created(location).build();
    }
    
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response get(@PathParam("id") String id) {
        java.nio.file.Path sourceFile = baseDir.resolve(id);
        StreamingOutput stream = new StreamingOutput() {

            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                Files.copy(sourceFile, output);
            }
            
        };
        return Response.ok(stream).build();
    }
}
