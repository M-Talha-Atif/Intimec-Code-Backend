package com.aidentx.connector_runtime_service;

import com.aidentx.dal.protos.runtime.ConnectorRuntimeServiceGrpc;
import com.aidentx.dal.protos.runtime.DiscoverConnectorsResponse;
import com.aidentx.dal.protos.runtime.TestConnectionRequest;
import com.aidentx.dal.protos.runtime.TestConnectionResponse;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
    "grpc.server.inProcessName=test", // Run the server in-process for tests
    "grpc.server.port=-1",            // Disable external port
    "grpc.client.inProcess.address=in-process:test" // Configure client to connect to the in-process server
})
@ActiveProfiles("test") // Use a test profile to avoid conflicts
public class ConnectorRuntimeServiceE2ETest {

    // Inject a gRPC client stub that is pre-configured to connect to our in-process server
    @GrpcClient("inProcess")
    private ConnectorRuntimeServiceGrpc.ConnectorRuntimeServiceBlockingStub clientStub;

    @Test
    void testConnection_Success() throws Exception {
        // 1. Discover Connectors to get a valid ID
        DiscoverConnectorsResponse discoveryResponse = clientStub.discoverConnectors(com.google.protobuf.Empty.getDefaultInstance());
        String dbConnectorId = discoveryResponse.getConnectorsList().stream()
                .filter(c -> c.getLabel().startsWith("Database Table"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Database connector not found"))
                .getId();

        assertNotNull(dbConnectorId);

        // 2. Build the Configuration Struct in pure Java
        String configJson = """
            {
                "user": "postgres",
                "password": "lol123a@",
                "table": "users",
                "keyColumn": "id",
                "passwordColumn": "password_hash",
                "jdbcDriver": "org.postgresql.Driver",
                "jdbcUrlTemplate": "jdbc:postgresql://localhost:5432/intimec"
            }
            """;
        Struct.Builder structBuilder = Struct.newBuilder();
        JsonFormat.parser().merge(configJson, structBuilder);
        Struct configuration = structBuilder.build();

        // 3. Build the Request
        TestConnectionRequest request = TestConnectionRequest.newBuilder()
                .setId(dbConnectorId)
                .setConfiguration(configuration)
                .build();

        // 4. Make the gRPC Call
        TestConnectionResponse response = clientStub.testConnection(request);

        // 5. Assert the Response
        // NOTE: This will still fail if your DB isn't running, but the key is that it will
        // no longer fail with "Table configuration property is empty".
        // It should now fail with a database connection error, proving our service works.
        // If your DB IS running, this will pass.
        assertTrue(response.getSuccess(), "The connection test should have succeeded, but failed with: " + response.getMessage());
        assertEquals("Connection successful.", response.getMessage());
    }
}