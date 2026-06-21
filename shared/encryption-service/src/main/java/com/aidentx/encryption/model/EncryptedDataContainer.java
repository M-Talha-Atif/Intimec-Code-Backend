package com.aidentx.encryption.model;

/**
 * Represents a simple POJO to hold all parts of the encrypted payload which
 * would be serialized to JSON and then Base64 encoded to be stored in its respective
 * config file.
 *
 * <p>This class encapsulates the encrypted data, initialization vector (IV), and salt
 * used in encryption operations. All fields are expected to be Base64 encoded strings.</p>
 *
 * <p>Typical usage involves creating an instance of this class after encrypting data,
 * then serializing it to JSON for storage. When decrypting, the fields are read from
 * the stored JSON, decoded from Base64, and used in the decryption process.</p>
 *
 * @author aidentx
 * @since 1.0
 */
public class EncryptedDataContainer {
    /**
     * The Base64 encoded ciphertext resulting from encryption.
     */
    private String encryptedData; // Base64 encoded ciphertext
    /**
     * The Base64 encoded initialization vector used during encryption.
     */
    private String iv; // Base64 encoded initialization vector
    /**
     * The Base64 encoded salt used for key derivation in encryption.
     */
    private String salt; // Base64 encoded salt

    /**
     * Default constructor for deserialization frameworks.
     */
    public EncryptedDataContainer() {
    }

    /**
     * Constructs an EncryptedDataContainer with all required fields.
     *
     * @param encryptedData the Base64 encoded ciphertext
     * @param iv the Base64 encoded initialization vector
     * @param salt the Base64 encoded salt
     */
    public EncryptedDataContainer(String encryptedData, String iv, String salt) {
        this.encryptedData = encryptedData;
        this.iv = iv;
        this.salt = salt;
    }

    /**
     * Gets the Base64 encoded ciphertext.
     *
     * @return the encrypted data as a Base64 encoded string
     */
    public String getEncryptedData() {
        return encryptedData;
    }

    /**
     * Sets the Base64 encoded ciphertext.
     *
     * @param encryptedData the encrypted data as a Base64 encoded string
     */
    public void setEncryptedData(String encryptedData) {
        this.encryptedData = encryptedData;
    }

    /**
     * Gets the Base64 encoded initialization vector.
     *
     * @return the IV as a Base64 encoded string
     */
    public String getIv() {
        return iv;
    }

    /**
     * Sets the Base64 encoded initialization vector.
     *
     * @param iv the IV as a Base64 encoded string
     */
    public void setIv(String iv) {
        this.iv = iv;
    }

    /**
     * Gets the Base64 encoded salt.
     *
     * @return the salt as a Base64 encoded string
     */
    public String getSalt() {
        return salt;
    }

    /**
     * Sets the Base64 encoded salt.
     *
     * @param salt the salt as a Base64 encoded string
     */
    public void setSalt(String salt) {
        this.salt = salt;
    }

    /**
     * Returns a string representation of this container for debugging purposes.
     *
     * @return a string representation of the object
     */
    @Override
    public String toString() {
        return "EncryptedDataContainer{" +
                "encryptedData='" + encryptedData + '\'' +
                ", iv='" + iv + '\'' +
                ", salt='" + salt + '\'' +
                '}';
    }

    /**
     * Checks equality based on all fields.
     *
     * @param o the object to compare
     * @return true if all fields are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof EncryptedDataContainer))
            return false;
        EncryptedDataContainer that = (EncryptedDataContainer) o;
        return encryptedData.equals(that.encryptedData) &&
                iv.equals(that.iv) &&
                salt.equals(that.salt);
    }

    /**
     * Computes the hash code based on all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        int result = encryptedData.hashCode();
        result = 31 * result + iv.hashCode();
        result = 31 * result + salt.hashCode();
        return result;
    }
}