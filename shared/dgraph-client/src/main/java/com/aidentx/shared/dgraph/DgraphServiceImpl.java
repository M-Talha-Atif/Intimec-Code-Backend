package com.aidentx.shared.dgraph;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.dgraph.DgraphClient;
import io.dgraph.DgraphProto;
import io.dgraph.DgraphProto.Mutation;
import io.dgraph.DgraphProto.Response;
import io.dgraph.Transaction;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class DgraphServiceImpl implements DgraphService {

    private final IUnifiedLogger logger;
    private final DgraphClient dgraphClient;
    private final Gson gson;

    public DgraphServiceImpl(DgraphClient dgraphClient, UnifiedLoggerFactory loggerFactory) {
        this.dgraphClient = dgraphClient;
        this.logger = loggerFactory.getLogger("DgraphService", "dgraph-client");
        this.gson = new Gson();
    }

    @Override
    public void applySchema(String schema) {
        try {
            DgraphProto.Operation operation = DgraphProto.Operation.newBuilder().setSchema(schema).build();
            dgraphClient.alter(operation);
            logger.info().message("Successfully applied Dgraph schema.").log();
        } catch (Exception e) {
            logger.error(e).message("Failed to apply Dgraph schema.").log();
            throw new DgraphOperationException("Failed to apply Dgraph schema", e);
        }
    }

    @Override
    public String saveNode(Map<String, Object> nodeData) {
        try (Transaction txn = dgraphClient.newTransaction()) {
            // Determine if this is an update (existing UID) or create (no UID supplied)
            boolean isUpdate = nodeData.containsKey("uid") && nodeData.get("uid") != null
                    && String.valueOf(nodeData.get("uid")).startsWith("0x");

            String blankNodeAlias = null; // alias without the "_:" prefix used for response lookup
            if (!isUpdate) {
                // Generate a unique blank node alias each call to avoid any accidental reuse
                blankNodeAlias = "n" + java.util.UUID.randomUUID().toString().replace("-", "");
                nodeData.put("uid", "_:" + blankNodeAlias);
            }

            String nodeJson = gson.toJson(nodeData);
            logger.debug().message("Preparing Dgraph mutation JSON").detail("json", nodeJson).log();

            Mutation mutation = Mutation.newBuilder()
                    .setSetJson(ByteString.copyFromUtf8(nodeJson))
                    // Commit now so we don't risk losing the UID if commit fails later
                    .setCommitNow(true)
                    .build();

            Response response = txn.mutate(mutation); // commitNow=true commits automatically

            if (isUpdate) {
                String uid = String.valueOf(nodeData.get("uid"));
                logger.info().message("Successfully updated Dgraph node.").detail("uid", uid).log();
                return uid;
            }

            // --- UID extraction logic ---
            String assignedUid = null;
            if (blankNodeAlias != null) {
                // Primary expected path
                assignedUid = response.getUidsMap().get(blankNodeAlias);
            }

            // Fallback 1: If only one UID returned, use it
            if ((assignedUid == null || assignedUid.isEmpty()) && response.getUidsMap().size() == 1) {
                assignedUid = response.getUidsMap().values().iterator().next();
                logger.debug().message("Used fallback single UID extraction").detail("uid", assignedUid).log();
            }

            // Fallback 2: Some older server versions may echo the blank node (rare). Try
            // without stripping.
            if ((assignedUid == null || assignedUid.isEmpty()) && blankNodeAlias != null) {
                assignedUid = response.getUidsMap().get("_:" + blankNodeAlias);
                if (assignedUid != null) {
                    logger.debug().message("UID found using legacy key format").detail("uid", assignedUid).log();
                }
            }

            if (assignedUid != null && !assignedUid.isEmpty()) {
                logger.info().message("Successfully created Dgraph node.").detail("uid", assignedUid).log();
                return assignedUid;
            }

            // If we reach here, provide detailed diagnostics
            logger.error()
                    .message("Dgraph mutation response did not contain expected UID mapping.")
                    .detail("blankNodeAlias", blankNodeAlias)
                    .detail("uidsMap", response.getUidsMap().toString())
                    .detail("jsonSent", nodeJson)
                    .log();
            throw new DgraphOperationException(
                    "Dgraph mutation succeeded but UID was not returned. See logs for details.");
        } catch (Exception e) {
            logger.error(e).message("Failed to save node to Dgraph.").log();
            throw new DgraphOperationException("Failed to save Dgraph node", e);
        }
    }

    @Override
    public void deleteNode(String uid) {
        Mutation mutation = Mutation.newBuilder()
                .setDeleteJson(ByteString.copyFromUtf8(String.format("{\"uid\": \"%s\"}", uid)))
                .build();
        try (Transaction txn = dgraphClient.newTransaction()) {
            txn.mutate(mutation);
            txn.commit();
            logger.info().message("Successfully deleted Dgraph node.").detail("uid", uid).log();
        } catch (Exception e) {
            logger.error(e).message("Failed to delete node from Dgraph.").detail("uid", uid).log();
            throw new DgraphOperationException("Failed to delete Dgraph node " + uid, e);
        }
    }

    public void createRelationship(String subjectUid, String predicate, String objectUid) {
        try (Transaction txn = dgraphClient.newTransaction()) {
            // N-quads are the standard way to define relationships (edges)
            String nquad = String.format("<%s> <%s> <%s> .", subjectUid, predicate, objectUid);

            Mutation mutation = Mutation.newBuilder()
                    .setSetNquads(ByteString.copyFromUtf8(nquad))
                    .build();

            txn.mutate(mutation);
            txn.commit();
            logger.info().message("Successfully created Dgraph relationship.")
                    .detail("subject", subjectUid)
                    .detail("predicate", predicate)
                    .detail("object", objectUid)
                    .log();
        } catch (Exception e) {
            throw new DgraphOperationException("Failed to create Dgraph relationship", e);
        }
    }

    @Override
    public void deleteRelationship(String subjectUid, String predicate, String objectUid) {
        String nquad = String.format("<%s> <%s> <%s> .", subjectUid, predicate, objectUid);
        DgraphProto.Mutation mutation = DgraphProto.Mutation.newBuilder().setDelNquads(ByteString.copyFromUtf8(nquad))
                .build();
        executeMutation(mutation, "delete relationship");
    }

    @Override
    public Optional<JsonElement> query(String query, String queryName) {
        try {
            DgraphProto.Response response = dgraphClient.newReadOnlyTransaction().query(query);
            JsonObject resultJson = gson.fromJson(response.getJson().toStringUtf8(), JsonObject.class);
            return Optional.ofNullable(resultJson.get(queryName));
        } catch (Exception e) {
            logger.error(e).message("Failed to execute Dgraph query.").detail("query", query).log();
            throw new DgraphOperationException("Failed to execute Dgraph query", e);
        }
    }

    private void executeMutation(DgraphProto.Mutation mutation, String operationDescription) {
        Transaction txn = dgraphClient.newTransaction();
        try {
            txn.mutate(mutation);
            txn.commit();
            logger.info().message("Successfully executed Dgraph mutation.").detail("operation", operationDescription)
                    .log();
        } catch (Exception e) {
            logger.error(e).message("Failed to execute Dgraph mutation.").detail("operation", operationDescription)
                    .log();
            throw new DgraphOperationException("Failed to " + operationDescription, e);
        } finally {
            txn.discard();
        }
    }
}