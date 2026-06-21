package com.aidentx.connector_framework;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorKey;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Implements the **Pool & Runtime Layer**.
 * This factory service provides a clean, application-centric way to obtain
 * fully configured
 * and pooled {@link ConnectorFacade} instances.
 */
@Service
public class ConnectorFacadeFactory {

    private final IUnifiedLogger logger;
    private final ConnectorRegistryService registryService;

    public ConnectorFacadeFactory(ConnectorRegistryService registryService, UnifiedLoggerFactory loggerFactory) {
        this.registryService = registryService;
        this.logger = loggerFactory.getLogger("ConnectorFacadeFactory", "connector-framework");
        logger.info().message("ConnectorFacadeFactory initialized.").log();
    }

    public ConnectorFacade createFacade(ConnectorKey key, APIConfiguration configuration) {
        logger.info()
                .message("Creating connector facade")
                .detail("bundleName", key.getBundleName())
                .detail("bundleVersion", key.getBundleVersion())
                .detail("connectorName", key.getConnectorName())
                .log();

        // 1. Get the connector info from the registry.
        ConnectorInfo info = registryService.getConnectorInfoManager().findConnectorInfo(key);
        if (info == null) {
            throw new IllegalStateException("Connector not found for key: " + key);
        }

        // 2. Use the standard ConnId factory to create the facade instance.
        // The APIConfiguration object is the key to its behavior.
        ConnectorFacade facade = org.identityconnectors.framework.api.ConnectorFacadeFactory.getInstance()
                .newInstance(configuration);

        // 3. Eagerly validate the connection to fail fast.
        // This ensures that the caller gets an immediate exception if the
        // configuration is wrong,
        // rather than on the first operation.
        try {
            facade.validate();
            logger.info().message("Connector facade validated successfully.").log();
        } catch (Exception e) {
            logger.error()
                    .message("Failed to validate connector facade")
                    .detail("exception", e.getMessage())
                    .log();
            throw new IllegalArgumentException("Invalid connector configuration", e);
        }

        return facade;
    }

    public boolean testFacade(ConnectorKey key, Map<String, Object> configurationProperties) {
        ConnectorFacade facade = null;
        try {
            APIConfiguration apiConfig = registryService.getConnectorInfoManager().findConnectorInfo(key)
                    .createDefaultAPIConfiguration();
            for (Map.Entry<String, Object> entry : configurationProperties.entrySet()) {
                apiConfig.getConfigurationProperties().setPropertyValue(entry.getKey(), entry.getValue());
            }
            facade = createFacade(key, apiConfig);
            return true;
        } catch (Exception e) {
            logger.warn().message("Connector test validation failed.").detail("connectorKey", key.toString()).log();
            return false;
        } finally {
            disposeFacade(facade, key);
        }
    }

    private void disposeFacade(ConnectorFacade facade, ConnectorKey key) {
        if (facade != null) {
            try {
                logger.trace().message("Disposing facade instance.").detail("connectorKey", key.toString()).log();
                facade.dispose();
            } catch (Exception e) {
                logger.warn().message("Error occurred while disposing facade instance after a failure.")
                        .detail("connectorKey", key.toString())
                        .detail("errorMessage", e.getMessage())
                        .log();
            }
        }
    }
}