package com.aidentx.taskmanagement.config;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;

/**
 * Registers the OpenSearch Index State Management (ISM) retention policy for task execution logs
 * at application startup.
 * <p>
 * The policy enforces:
 * <ul>
 *   <li><b>hot</b>   — index is active and writable. Transitions to warm after
 *       {@code opensearch.log-retention.read-only-after-days} (default: 30 days).</li>
 *   <li><b>warm</b>  — index is set to read-only. Transitions to delete after
 *       {@code opensearch.log-retention.delete-after-days} (default: 365 days).</li>
 *   <li><b>delete</b> — index is deleted.</li>
 * </ul>
 * <p>
 * The {@code ism_template} block ensures the policy is automatically attached to every
 * new {@code task-execution-logs-*} index at creation time — no manual onboarding required.
 * <p>
 * This is idempotent: if the policy already exists it is updated in place. Startup will not
 * fail if OpenSearch is temporarily unreachable — the error is logged as a warning and the
 * application continues normally.
 */
@Component
public class OpenSearchRetentionConfig implements ApplicationRunner {

    private static final String POLICY_NAME = "task-log-retention";
    private static final String POLICY_ENDPOINT = "/_plugins/_ism/policies/" + POLICY_NAME;

    @Value("${opensearch.url}")
    private String opensearchUrl;

    @Value("${opensearch.username}")
    private String username;

    @Value("${opensearch.password}")
    private String password;

    @Value("${opensearch.log-retention.read-only-after-days:30}")
    private int readOnlyAfterDays;

    @Value("${opensearch.log-retention.delete-after-days:365}")
    private int deleteAfterDays;

    private final IUnifiedLogger log;

    public OpenSearchRetentionConfig(UnifiedLoggerFactory loggerFactory) {
        this.log = loggerFactory.getLogger(OpenSearchRetentionConfig.class);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            registerPolicy();
        } catch (Exception e) {
            // A retention policy failure must never prevent the application from starting.
            // Log it prominently so ops are alerted, but do not rethrow.
            log.warn().message(
                "Failed to register OpenSearch ISM retention policy '" + POLICY_NAME + "'. " +
                "Task execution logs will NOT be protected by retention policy until this is resolved. " +
                "Error: " + e.getMessage()).log();
        }
    }

    private void registerPolicy() throws IOException, InterruptedException {
        String policyJson = buildPolicyJson();

        // Use the Java 11+ HttpClient — no extra dependencies required.
        // The OpenSearch Java client does not expose a raw ISM policy API, so we call
        // the REST endpoint directly.
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        String basicAuth = Base64.getEncoder().encodeToString(
                (username + ":" + password).getBytes(StandardCharsets.UTF_8));

        String url = opensearchUrl.replaceAll("/$", "") + POLICY_ENDPOINT;

        // PUT is idempotent — creates or updates the policy
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + basicAuth)
                .PUT(HttpRequest.BodyPublishers.ofString(policyJson, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        if (status == 200 || status == 201) {
            log.info().message(String.format(
                "OpenSearch ISM policy '%s' registered: read-only after %d days, " +
                "deleted after %d days. Applies to all task-execution-logs-* indices.",
                POLICY_NAME, readOnlyAfterDays, deleteAfterDays)).log();
        } else {
            // 409 = policy already exists with a higher sequence — safe to ignore
            if (status != 409) {
                log.warn().message(String.format(
                    "OpenSearch ISM policy registration returned HTTP %d: %s",
                    status, response.body())).log();
            } else {
                log.info().message("OpenSearch ISM policy '" + POLICY_NAME
                    + "' already up-to-date (HTTP 409).").log();
            }
        }
    }

    /**
     * Builds the ISM policy JSON with the configured retention durations.
     * The {@code ism_template} block auto-applies the policy to all new
     * {@code task-execution-logs-*} indices at creation time.
     */
    private String buildPolicyJson() {
        return "{\n" +
               "  \"policy\": {\n" +
               "    \"description\": \"IGA task execution log retention — " + deleteAfterDays + " days minimum, read-only after " + readOnlyAfterDays + " days\",\n" +
               "    \"default_state\": \"hot\",\n" +
               "    \"states\": [\n" +
               "      {\n" +
               "        \"name\": \"hot\",\n" +
               "        \"actions\": [],\n" +
               "        \"transitions\": [{\n" +
               "          \"state_name\": \"warm\",\n" +
               "          \"conditions\": { \"min_index_age\": \"" + readOnlyAfterDays + "d\" }\n" +
               "        }]\n" +
               "      },\n" +
               "      {\n" +
               "        \"name\": \"warm\",\n" +
               "        \"actions\": [{ \"read_only\": {} }],\n" +
               "        \"transitions\": [{\n" +
               "          \"state_name\": \"delete\",\n" +
               "          \"conditions\": { \"min_index_age\": \"" + deleteAfterDays + "d\" }\n" +
               "        }]\n" +
               "      },\n" +
               "      {\n" +
               "        \"name\": \"delete\",\n" +
               "        \"actions\": [{ \"delete\": {} }],\n" +
               "        \"transitions\": []\n" +
               "      }\n" +
               "    ],\n" +
               "    \"ism_template\": [{\n" +
               "      \"index_patterns\": [\"task-execution-logs-*\"],\n" +
               "      \"priority\": 100\n" +
               "    }]\n" +
               "  }\n" +
               "}";
    }
}
