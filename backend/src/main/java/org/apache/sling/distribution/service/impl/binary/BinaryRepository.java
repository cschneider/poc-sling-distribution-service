package org.apache.sling.distribution.service.impl.binary;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.osgi.service.component.annotations.Component;

@Component(service = BinaryRepository.class)
public class BinaryRepository {
    private java.nio.file.Path baseDir;
    
    public BinaryRepository() throws IOException {
        Path baseDir = Files.createTempDirectory("dist-binary");
        baseDir.toFile().mkdirs();
    }
    
    public String upload(InputStream is) throws IOException {
        String id = UUID.randomUUID().toString();
        java.nio.file.Path destFile = baseDir.resolve(id);
        Files.copy(is, destFile, StandardCopyOption.REPLACE_EXISTING);
        return id;
    }
    
    public void get(String id, OutputStream output) throws IOException {
        java.nio.file.Path sourceFile = baseDir.resolve(id);
        Files.copy(sourceFile, output);
    }
}
