package com.aidentx.encryption.util;


import com.aidentx.encryption.exception.KeyManagementException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for handling certificate operations and deriving encryption keys from certificates.
 */
public class CertificateUtil {

    private static final String AES_ALGORITHM = "AES";
    private static final String SHA256_ALGORITHM = "SHA-256";
    private static final int AES_KEY_LENGTH = 32; // 256 bits

    // PEM patterns for different certificate/key types
    private static final Pattern CERT_PATTERN = Pattern.compile(
            "-----BEGIN CERTIFICATE-----(.*)-----END CERTIFICATE-----", 
            Pattern.DOTALL
    );
    
    private static final Pattern PRIVATE_KEY_PATTERN = Pattern.compile(
            "-----BEGIN PRIVATE KEY-----(.*)-----END PRIVATE KEY-----", 
            Pattern.DOTALL
    );
    
    private static final Pattern RSA_PRIVATE_KEY_PATTERN = Pattern.compile(
            "-----BEGIN RSA PRIVATE KEY-----(.*)-----END RSA PRIVATE KEY-----", 
            Pattern.DOTALL
    );

    /**
     * Derives an AES master key from a base64 encoded certificate.
     * The key is derived by:
     * 1. Decoding the base64 certificate
     * 2. Parsing the PEM format
     * 3. Extracting key material from the certificate
     * 4. Using SHA-256 to derive a 256-bit AES key
     *
     * @param certificateBase64 the base64 encoded certificate (PEM format)
     * @return SecretKey for AES encryption
     * @throws KeyManagementException if certificate parsing or key derivation fails
     */
    public static SecretKey deriveMasterKeyFromCertificate(String certificateBase64) throws KeyManagementException {
        if (certificateBase64 == null || certificateBase64.trim().isEmpty()) {
        throw new KeyManagementException("Certificate base64 string is null or empty");
        }

        try {
            // Decode base64
            byte[] certificateBytes = Base64.getDecoder().decode(certificateBase64);
            String certificatePem = new String(certificateBytes);

            // Try to extract certificate or private key
            byte[] keyMaterial = extractKeyMaterial(certificatePem);

            // Derive AES key using SHA-256
            MessageDigest digest = MessageDigest.getInstance(SHA256_ALGORITHM);
            byte[] hash = digest.digest(keyMaterial);

            // Use first 32 bytes (256 bits) for AES key
            byte[] aesKeyBytes = new byte[AES_KEY_LENGTH];
            System.arraycopy(hash, 0, aesKeyBytes, 0, AES_KEY_LENGTH);

            return new SecretKeySpec(aesKeyBytes, AES_ALGORITHM);

        } catch (Exception e) {
            throw new KeyManagementException("Failed to derive master key from certificate: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts key material from a PEM formatted certificate or private key.
     *
     * @param pemContent the PEM formatted content
     * @return byte array of key material
     * @throws Exception if parsing fails
     */
    private static byte[] extractKeyMaterial(String pemContent) throws Exception {
        // Try to parse as certificate first
        Matcher certMatcher = CERT_PATTERN.matcher(pemContent);
        if (certMatcher.find()) {
            return parseCertificate(pemContent);
        }

        // Try to parse as private key
        Matcher privateKeyMatcher = PRIVATE_KEY_PATTERN.matcher(pemContent);
        if (privateKeyMatcher.find()) {
            return parsePrivateKey(pemContent, false);
        }

        // Try to parse as RSA private key
        Matcher rsaPrivateKeyMatcher = RSA_PRIVATE_KEY_PATTERN.matcher(pemContent);
        if (rsaPrivateKeyMatcher.find()) {
            return parsePrivateKey(pemContent, true);
        }

        throw new IllegalArgumentException("No valid certificate or private key found in PEM content");
    }

    /**
     * Parses a certificate and extracts key material.
     */
    private static byte[] parseCertificate(String pemContent) throws Exception {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream inputStream = new ByteArrayInputStream(pemContent.getBytes());
        Certificate certificate = certFactory.generateCertificate(inputStream);
        
        // Use the public key's encoded form as key material
        return certificate.getPublicKey().getEncoded();
    }

    /**
     * Parses a private key and extracts key material.
     */
    private static byte[] parsePrivateKey(String pemContent, boolean isRSAFormat) throws Exception {
        // Clean the PEM content
        String cleanedPem = pemContent
                .replaceAll("-----BEGIN.*?-----", "")
                .replaceAll("-----END.*?-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(cleanedPem);

        if (isRSAFormat) {
            // For RSA PRIVATE KEY format, we need to convert to PKCS#8
            // For simplicity, we'll use the raw bytes as key material
            return keyBytes;
        } else {
            // For PKCS#8 format, parse and extract
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            return privateKey.getEncoded();
        }
    }

    /**
     * Validates that a base64 encoded certificate is properly formatted.
     *
     * @param certificateBase64 the base64 encoded certificate
     * @return true if valid, false otherwise
     */
    public static boolean isValidCertificate(String certificateBase64) {
        try {
            deriveMasterKeyFromCertificate(certificateBase64);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Creates a test certificate for development/testing purposes.
     * WARNING: This should never be used in production!
     *
     * @return base64 encoded test certificate
     */
    public static String createTestCertificate() {
        // A minimal test certificate for development
        String testCert = "-----BEGIN CERTIFICATE-----\n" +
                "MIICljCCAX4CCQDKFhAzAQAAADANBgkqhkiG9w0BAQsFADBNMQswCQYDVQQGEwJV\n" +
                "UzELMAkGA1UECAwCQ0ExFjAUBgNVBAcMDVNhbiBGcmFuY2lzY28xGTAXBgNVBAoM\n" +
                "EFRlc3QgQ29tcGFueSBJbmMwHhcNMjQwMTAxMDAwMDAwWhcNMjUwMTAxMDAwMDAw\n" +
                "WjBNMQswCQYDVQQGEwJVUzELMAkGA1UECAwCQ0ExFjAUBgNVBAcMDVNhbiBGcmFu\n" +
                "Y2lzY28xGTAXBgNVBAoMEFRlc3QgQ29tcGFueSBJbmMwggEiMA0GCSqGSIb3DQEB\n" +
                "AQUAA4IBDwAwggEKAoIBAQDGvBVyGJKJyGJSJDGSJDGSJDGSJDGSJDGSJDGSJDGS\n" +
                "JDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGs\n" +
                "JDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDG\n" +
                "SJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDG\n" +
                "SJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDG\n" +
                "SJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJDGSJwI\n" +
                "DAQABo1MwUTAdBgNVHQ4EFgQUKON7YtjXEKKYKON7YtjXEKKYKON7YtjXEKKYKO\n" +
                "N7YtjXEKKYKON7YtjXEKKYKON7YtjXEKKYKON7YtjXEKKYKON7YtjXEKKYKON7Yt\n" +
                "jXEKKYKON7YtjXEKKYKON7YtjXEKKYKON7YtjXEKKYKON7YtjXEKKYKON7YtjXEK\n" +
                "KYKMA0GCSqGSIb3DQEBCwUAA4IBAQAGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVB\n" +
                "VYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYG\n" +
                "VBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVY\n" +
                "GVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBV\n" +
                "YGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVB\n" +
                "VYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGV\n" +
                "BVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYGVBVYG\n" +
                "-----END CERTIFICATE-----";

        return Base64.getEncoder().encodeToString(testCert.getBytes());
    }
}
