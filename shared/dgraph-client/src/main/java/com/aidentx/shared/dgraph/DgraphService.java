package com.aidentx.shared.dgraph;

import com.google.gson.JsonElement;

import java.util.Map;
import java.util.Optional;

public interface DgraphService {

    /**
     * Applies a schema definition to the Dgraph database.
     * @param schema The Dgraph schema string.
     */
    void applySchema(String schema);

    /**
     * Creates a new node in Dgraph or updates it if a 'uid' is present in the payload.
     * @param nodeData A map representing the node's predicates and values.
     *                 If it contains a "uid" key, it performs an update.
     *                 If it does not, it creates a new node and returns the assigned UID.
     * @return The UID of the created or updated node.
     */
    String saveNode(Map<String, Object> nodeData);

    /**
     * Deletes a node by its UID.
     * @param uid The UID of the node to delete.
     */
    void deleteNode(String uid);

    /**
     * Creates a relationship (edge) between two nodes.
     * @param subjectUid The UID of the source node.
     * @param predicate The name of the edge (e.g., "has_role").
     * @param objectUid The UID of the target node.
     */
    void createRelationship(String subjectUid, String predicate, String objectUid);
    
    /**
     * Deletes a relationship (edge) between two nodes.
     * @param subjectUid The UID of the source node.
     * @param predicate The name of the edge.
     * @param objectUid The UID of the target node.
     */
    void deleteRelationship(String subjectUid, String predicate, String objectUid);

    /**
     * Executes a read-only DQL query.
     * @param query The DQL query string.
     * @param queryName The name of the root query block in the DQL (e.g., "q").
     * @return An Optional containing the JSON result for the specified query block.
     */
    Optional<JsonElement> query(String query, String queryName);
}