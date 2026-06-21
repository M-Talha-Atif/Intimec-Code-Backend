package com.aidentx.connector_framework;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.annotation.PostConstruct;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implements the **Connector Loader** component.
 * This service is responsible for dynamically discovering and managing ConnId connector bundles (.jar files)
 * from a designated directory. It uses Spring's {@code @Scheduled} annotation to periodically
 * scan for new or removed connectors and reloads the central {@link ConnectorInfoManager} when
 * changes are detected, enabling hot-deployment of connector implementations.
 */
@Service
public class ConnectorRegistryService {

    private final IUnifiedLogger logger;
    private final Path connectorDirectory;
    private final Set<URL> discoveredJarUrls = ConcurrentHashMap.newKeySet();
    private volatile ConnectorInfoManager connectorInfoManager;
    private final String connectorDirectoryPath;

    /**
     * Constructs the ConnectorRegistryService. The UnifiedLoggerFactory is expected to be
     * injected by the consuming microservice's Spring context.
     *
     * @param connectorDirectoryPath The path to the directory where connector JARs are stored, injected from configuration.
     * @param loggerFactory The factory for creating a structured, unified logger instance.
     */
    public ConnectorRegistryService(
            @Value("${connector.directory}") String connectorDirectoryPath,
            UnifiedLoggerFactory loggerFactory) {
        this.connectorDirectoryPath = connectorDirectoryPath;
        this.connectorDirectory = Path.of(connectorDirectoryPath);
        this.logger = loggerFactory.getLogger("ConnectorRegistryService", "connector-framework");
    }

    /**
     * Initializes the service after construction. It ensures the connector directory exists
     * and performs the first scan for connectors on application startup.
     */
    @PostConstruct
    public void initialize() {
        logger.info().message("Initializing ConnectorRegistryService...")
                .detail("connectorDirectory", this.connectorDirectoryPath)
                .log();

        if (!Files.exists(this.connectorDirectory)) {
            logger.warn().message("Connector directory does not exist. Attempting to create it.")
                    .detail("directory", this.connectorDirectory.toString())
                    .log();
            try {
                Files.createDirectories(this.connectorDirectory);
            } catch (IOException e) {
                logger.error(e).message("Failed to create connector directory.")
                        .detail("directory", this.connectorDirectory.toString())
                        .log();
                throw new IllegalStateException("Could not create connector directory: " + this.connectorDirectory, e);
            }
        }
        scanForConnectors();
    }

    /**
     * Periodically scans the configured directory for new or removed connector JARs,
     * driven by Spring's scheduler. The method is synchronized to prevent race conditions
     * if a scan is still running when the next one is scheduled.
     */
    @Scheduled(fixedRateString = "${connectors.scan-interval-ms:60000}") // Default to 60s if not set
    public synchronized void scheduledScan() {
        logger.trace().message("Starting scheduled scan of connector directory.").log();
        scanForConnectors();
    }

    /**
     * Performs the core logic of scanning the filesystem, comparing the current state with the
     * known state, and reloading the {@link ConnectorInfoManager} if any changes are detected.
     */
    private void scanForConnectors() {
        boolean changed = false;
        Set<URL> currentJarUrlsOnDisk;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(connectorDirectory, "*.jar")) {
            currentJarUrlsOnDisk = ConcurrentHashMap.newKeySet();
            for (Path jarPath : stream) {
                currentJarUrlsOnDisk.add(jarPath.toUri().toURL());
            }
        } catch (IOException e) {
            logger.error(e).message("Failed to scan connector directory for JAR files.")
                    .detail("directory", connectorDirectory.toString())
                    .log();
            return;
        }

        for (URL url : currentJarUrlsOnDisk) {
            if (discoveredJarUrls.add(url)) {
                logger.info().message("Discovered new connector bundle.")
                        .detail("bundleUrl", url.toString())
                        .log();
                changed = true;
            }
        }

        Set<URL> removedUrls = discoveredJarUrls.stream()
                .filter(url -> !currentJarUrlsOnDisk.contains(url))
                .collect(Collectors.toSet());

        if (!removedUrls.isEmpty()) {
            changed = true;
            for (URL removedUrl : removedUrls) {
                logger.info().message("Connector bundle removed.")
                        .detail("bundleUrl", removedUrl.toString())
                        .log();
                discoveredJarUrls.remove(removedUrl);
            }
        }

        if (changed || this.connectorInfoManager == null) {
            if (discoveredJarUrls.isEmpty()) {
                logger.warn().message("No connector bundles found. ConnectorInfoManager will be cleared.").log();
                this.connectorInfoManager = null;
            } else {
                logger.info().message("Change detected or initial load. Reloading ConnectorInfoManager...")
                        .detail("bundleCount", discoveredJarUrls.size())
                        .log();
                try {
                    ConnectorInfoManagerFactory factory = ConnectorInfoManagerFactory.getInstance();
                    this.connectorInfoManager = factory.getLocalManager(discoveredJarUrls.toArray(new URL[0]));
                    logger.info().message("ConnectorInfoManager successfully reloaded.")
                            .detail("availableConnectors", this.connectorInfoManager.getConnectorInfos().stream()
                                    .map(info -> info.getConnectorKey().toString()).collect(Collectors.toList()))
                            .log();
                } catch (Exception e) {
                    logger.error(e).message("Failed to reload ConnectorInfoManager.")
                            .detail("bundles", discoveredJarUrls.stream().map(URL::toString).collect(Collectors.toList()))
                            .log();
                }
            }
        }
    }

    /**
     * Provides thread-safe access to the currently active {@link ConnectorInfoManager}.
     * This manager contains all information about the discovered and loaded connectors.
     *
     * @return The active ConnectorInfoManager instance.
     * @throws IllegalStateException if the manager has not been initialized or if no connectors are loaded.
     */
    public ConnectorInfoManager getConnectorInfoManager() {
        ConnectorInfoManager manager = this.connectorInfoManager; // Volatile read for thread safety
        if (manager == null) {
            logger.warn().message("Attempted to access ConnectorInfoManager but it is not initialized or no bundles are loaded.").log();
            throw new IllegalStateException("Connector Registry is not initialized or no connectors are loaded.");
        }
        return manager;
    }
}