package com.aidentx.encryption.repository;

import com.aidentx.encryption.model.EncryptionKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// This is now a Spring Data JPA repository interface
public interface EncryptionKeyRepository extends JpaRepository<EncryptionKey, UUID> {

    /**
     * Finds the currently active encryption key for a given type.
     * Spring Data JPA will automatically implement this method based on its name.
     */
    Optional<EncryptionKey> findByKeyTypeAndIsActiveTrue(String keyType);
}