package org.apache.sling.distribution.service.impl.binary;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = BinaryRepository.class)
public class BinaryRepository {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final java.nio.file.Path baseDir;
    
    public BinaryRepository() throws IOException {
        baseDir = Files.createTempDirectory("dist-binary");
        baseDir.toFile().mkdirs();
        log.info("Created temp dir {}", baseDir);
    }
    
    @Deactivate
    public void close() {
        delete(baseDir);
    }

    private void delete(Path path) {
        try (Stream<Path> files = Files.walk(path)) {
            files
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        } catch (IOException e) {
            log.warn("Error deleting path {}", path);
        }
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
