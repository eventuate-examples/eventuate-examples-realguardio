package io.eventuate.examples.realguardio.securitysystemservice.osointegration;

import io.realguardio.osointegration.ososervice.LocalAuthorizationConfigFileSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Map;


public class ClasspathLocalAuthorizationConfigFileSupplier implements LocalAuthorizationConfigFileSupplier {

    private static final Logger logger = LoggerFactory.getLogger(ClasspathLocalAuthorizationConfigFileSupplier.class);
    private static final String DEFAULT_CONFIG_PATH = "/local_authorization_config.yaml";

    private final String configFilePath;
    private FileSystem fs;

    public ClasspathLocalAuthorizationConfigFileSupplier() {
        this(DEFAULT_CONFIG_PATH);
    }

    public ClasspathLocalAuthorizationConfigFileSupplier(String configFilePath) {
        this.configFilePath = configFilePath;
    }

    @Override
    public Path get() {
        var resourceName = configFilePath;

        URL url = ResourceUtils.class.getResource(resourceName);
        if (url == null) {
            throw new IllegalArgumentException("Resource not found: " + resourceName);
        }

        try {
            URI uri = url.toURI();
            logger.info("Loading local authorization config file from {}", uri);
            switch (uri.getScheme()) {
                case "file" -> {
                    return Paths.get(uri);
                }
                case "jar" -> {
                    try {
                        fs = FileSystems.newFileSystem(uri, Map.of());
                    } catch (FileSystemAlreadyExistsException e) {
                        fs = FileSystems.getFileSystem(uri);
                    }
                    return Path.of(uri);
                }
                default -> {
                    try (InputStream in = url.openStream()) {
                        Path tmp = Files.createTempFile("resource-", getFileName(resourceName));
                        Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                        tmp.toFile().deleteOnExit();
                        return tmp;
                    }
                }
            }
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getFileName(String resourceName) {
        int idx = resourceName.lastIndexOf('/');
        return (idx >= 0 ? resourceName.substring(idx + 1) : resourceName);
    }
}
