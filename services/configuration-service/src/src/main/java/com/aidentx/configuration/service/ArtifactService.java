package com.aidentx.configuration.service;

import com.aidentx.configuration.exception.artifact.ArtifactEncryptionException;
import com.aidentx.configuration.exception.artifact.ArtifactFileException;
import com.aidentx.configuration.exception.artifact.ArtifactServiceException;
import com.aidentx.configuration.model.entity.Artifact;
import com.aidentx.configuration.model.entity.Configuration;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Optional;

/**
 * Service interface for managing supplementary configuration artifacts.
 * <p>
 * Provides a complete API for creating, retrieving, updating, and deleting both
 * file-based and inline JSON artifacts associated with a {@link Configuration}.
 * This service
 * handles all persistence, file system interaction, and encryption/decryption
 * logic
 * specific to artifacts.
 * </p>
 *
 * @author aidentx
 * @since 1.0
 */
public interface ArtifactService {

    /**
     * Saves or updates an inline JSON artifact for a given configuration.
     * <p>
     * If an artifact with the same {@code artifactType} already exists for the
     * given
     * configuration, it will be updated. Otherwise, a new artifact is created. This
     * enforces the uniqueness of {@code artifactType} per configuration.
     * </p>
     *
     * @param configuration   the parent Configuration entity
     * @param artifactType    the unique type of the artifact (e.g., "schema",
     *                        "mappings")
     * @param jsonData        the JSON data to store
     * @param encryptEntirely if true, the entire JSON blob is encrypted; otherwise,
     *                        field-level encryption is applied based on rules
     * @return the saved or updated Artifact entity
     * @throws ArtifactServiceException if JSON processing or encryption fails
     */
    Artifact saveInlineArtifact(Configuration configuration, String artifactType, JsonNode jsonData,
            boolean encryptEntirely) throws ArtifactServiceException;

    /**
     * Saves or updates a file-based artifact (text or structured object) for a given configuration.
     * If an artifact with the same {@code artifactType} already exists, its physical file is replaced.
     * @param configuration parent configuration
     * @param artifactType unique artifact type
     * @param data text or serializable object content
     */
    Artifact saveFileArtifact(Configuration configuration, String artifactType, Object data) throws ArtifactServiceException;

    /**
     * Saves or updates a file-based artifact using raw binary data.
     * @param configuration parent configuration
     * @param artifactType unique artifact type
     * @param binary raw bytes
     */
    Artifact saveBinaryFileArtifact(Configuration configuration, String artifactType, byte[] binary) throws ArtifactServiceException;

    /**
     * Retrieves the decrypted content of a specific file-based artifact.
     *
     * @param artifact the artifact entity whose content is to be retrieved
     * @return an Optional containing the decrypted content as a String, or empty if
     *         not found, not a file, or an error occurs
     * @throws ArtifactEncryptionException if decryption fails
     * @throws ArtifactFileException if file reading fails
     */
    Optional<String> getArtifactContent(Artifact artifact) throws ArtifactEncryptionException, ArtifactFileException;

    /**
     * Retrieves the raw binary content of a specific file-based artifact.
     * If encrypted, the content is transparently decrypted and base64-decoded
     * (since binary artifacts are stored encrypted from a Base64 form).
     *
     * @param artifact the artifact entity
     * @return Optional of bytes; empty if not found/active/not a file
     * @throws ArtifactEncryptionException if decryption fails
     * @throws ArtifactFileException if reading the file fails
     */
    Optional<byte[]> getArtifactBinaryContent(Artifact artifact) throws ArtifactEncryptionException, ArtifactFileException;

    /**
     * Retrieves the decrypted JSON data of a specific inline artifact.
     *
     * @param artifact the artifact entity whose data is to be retrieved
     * @return an Optional containing the decrypted JsonNode, or empty if not found,
     *         not inline, or an error occurs
     * @throws ArtifactEncryptionException if decryption fails
     */
    Optional<JsonNode> getArtifactInlineContent(Artifact artifact) throws ArtifactEncryptionException;

    /**
     * Gathers data and metadata for all active artifacts related to a given
     * configuration.
     *
     * @param configuration the configuration entity
     * @return a map where keys are artifact types and values are maps of the
     *         artifact's data and metadata.
     */
    Map<String, Object> getRelatedArtifactsData(Configuration configuration);

    /**
     * Deletes a specific artifact (both the database record and any associated
     * physical file)
     * from a configuration.
     *
     * @param configuration the parent Configuration entity
     * @param artifactType  the unique type of the artifact to delete
     * @return true if the artifact was found and deleted, false otherwise
     */
    boolean deleteArtifact(Configuration configuration, String artifactType);

    /**
     * Deletes all physical artifact files associated with a given configuration.
     * This is typically called when a {@link Configuration} entity is being
     * deleted.
     *
     * @param configuration the configuration whose artifacts should be deleted
     */
    void deleteAllPhysicalArtifacts(Configuration configuration);
}