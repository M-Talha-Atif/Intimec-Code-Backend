package com.aidentx.encryption.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "encryption_keys")
public class EncryptionKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "key_type", nullable = false, unique = true)
    private String keyType;

    @Column(name = "key_version", nullable = false)
    private int version;

    @Lob
    @Column(name = "encrypted_key_value", nullable = false)
    private byte[] encryptedKeyValue;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Default constructor is required by JPA
    public EncryptionKey() {
        this.createdAt = Instant.now();
    }

    public EncryptionKey(String keyType, int version, byte[] encryptedKeyValue, boolean isActive) {
        this();
        this.keyType = keyType;
        this.version = version;
        this.encryptedKeyValue = encryptedKeyValue;
        this.isActive = isActive;
    }

    // --- Getters and Setters ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getKeyType() {
        return keyType;
    }

    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public byte[] getEncryptedKeyValue() {
        return encryptedKeyValue;
    }

    public void setEncryptedKeyValue(byte[] encryptedKeyValue) {
        this.encryptedKeyValue = encryptedKeyValue;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}