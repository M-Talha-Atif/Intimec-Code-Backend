package com.aidentx.connector_runtime_service.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ConnectorDiscoveryService {

    private final IUnifiedLogger logger;
    private final String connectorDirectoryPath;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Map<String, ConnectorInfo> availableConnectors = new ConcurrentHashMap<>();

    private final Map<String, String> idToKeyMap = new ConcurrentHashMap<>();

    public ConnectorDiscoveryService(@Value("${connector.directory}") String connectorDirectoryPath,
            UnifiedLoggerFactory unifiedLoggerFactory) {
        this.connectorDirectoryPath = connectorDirectoryPath;
        this.logger = unifiedLoggerFactory.getLogger(ConnectorDiscoveryService.class);
    }

    @PostConstruct
    public void startWatching() {
        // Use the class field directly now
        if (!StringUtils.hasText(this.connectorDirectoryPath)) {
            logger.error().message(
                    "Connector directory path is not configured. Please set 'connector.directory' in your application.yml")
                    .log();
            throw new IllegalStateException("Connector directory is not configured.");
        }

        logger.info().message("Starting to watch directory: " + this.connectorDirectoryPath).log();
        File directory = new File(this.connectorDirectoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            logger.error().message("Connector directory '" + this.connectorDirectoryPath
                    + "' does not exist or is not a directory. Please create it.").log();
            if (!directory.mkdirs()) {
                logger.error().message("Failed to create connector directory.").log();
                return;
            }
        }

        // Initial scan of the directory
        scanForExistingJars();

        // Start the watch service in a new thread
        executorService.submit(this::watchDirectory);
    }

    private void watchDirectory() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            // Use the class field directly now
            Path path = Paths.get(this.connectorDirectoryPath);
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            WatchKey key;
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        Path newFilePath = (Path) event.context();
                        logger.info().message("New file detected: " + newFilePath).log();
                        if (newFilePath.toString().endsWith(".jar")) {
                            processJarFile(path.resolve(newFilePath).toFile());
                        }
                    }
                }
                key.reset();
            }
        } catch (Exception e) {
            logger.error(e).message("Error in directory watch service").log();
        }
    }

    private void scanForExistingJars() {
        // Use the class field directly now
        File directory = new File(this.connectorDirectoryPath);
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (files != null) {
            for (File file : files) {
                logger.info().message("Processing existing JAR file: " + file.getName()).log();
                processJarFile(file);
            }
        }
    }

    private void processJarFile(File jarFile) {
        try {
            URL jarUrl = jarFile.toURI().toURL();
            URLClassLoader loader = new URLClassLoader(new URL[] { jarUrl }, getClass().getClassLoader());
            Thread.currentThread().setContextClassLoader(loader);

            ConnectorInfoManagerFactory factory = ConnectorInfoManagerFactory.getInstance();
            ConnectorInfoManager manager = factory.getLocalManager(jarUrl);

            for (ConnectorInfo info : manager.getConnectorInfos()) {
                String keyString = info.getConnectorKey().toString();
                String id = generateId(keyString); // Generate the stable hash ID
                String bundleName = info.getConnectorKey().getBundleName();

                logger.info().message(String.format("Discovered Connector: '%s' with Bundle: '%s' and ID: %s",
                        info.getConnectorDisplayName(), bundleName, id)).log();
                availableConnectors.put(keyString, info);
                idToKeyMap.put(id, keyString); // Populate the index map
            }

        } catch (MalformedURLException e) {
            logger.error().message("Invalid JAR file path: " + jarFile.getAbsolutePath()).log();
        } catch (Exception e) {
            logger.error(e).message("Failed to process connector JAR: " + jarFile.getName()).log();
        } finally {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        }
    }

    // Helper method to generate a stable SHA-1 hash ID
    public static String generateId(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            try (Formatter formatter = new Formatter()) {
                for (byte b : hash) {
                    formatter.format("%02x", b);
                }
                return formatter.toString().substring(0, 12);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not found", e);
        }
    }

    public String getKeyFromId(String id) {
        return this.idToKeyMap.get(id);
    }

    public Map<String, ConnectorInfo> getAvailableConnectors() {
        return this.availableConnectors;
    }

    public ConnectorInfo getConnectorInfo(String key) {
        return this.availableConnectors.get(key);
    }

    @PreDestroy
    public void stopWatching() {
        logger.info().message("Shutting down directory watch service.").log();
        executorService.shutdownNow();
    }
}