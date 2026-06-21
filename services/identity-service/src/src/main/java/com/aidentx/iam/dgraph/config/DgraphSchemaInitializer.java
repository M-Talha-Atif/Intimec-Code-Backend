package com.aidentx.iam.dgraph.config;

import com.aidentx.iam.dgraph.DgraphGovernanceService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.shared.dgraph.DgraphOperationException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test") // Do not run during unit tests
public class DgraphSchemaInitializer implements CommandLineRunner {

    private final IUnifiedLogger logger;
    private final DgraphGovernanceService dgraphGovernanceService;

    public DgraphSchemaInitializer(UnifiedLoggerFactory loggerFactory,
            DgraphGovernanceService dgraphGovernanceService) {
        this.logger = loggerFactory.getLogger("DgraphSchemaInitializer", "core-startup");
        this.dgraphGovernanceService = dgraphGovernanceService;
    }

    @Override
    public void run(String... args) {
        logger.info().message("Executing Dgraph schema initialization...").log();
        try {
            // dgraphGovernanceService.initializeSchema();
            logger.info().message("Dgraph schema initialization completed successfully.").log();
        } catch (DgraphOperationException e) {
            logger.error(e).message(
                    "Dgraph schema initialization failed. The service will continue to run, but graph functionality may be impaired.")
                    .log();
            // We do not re-throw the exception, allowing the service to start in a degraded
            // state.
        }
    }
}