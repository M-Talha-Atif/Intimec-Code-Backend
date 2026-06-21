# SAML Certificates Directory

This directory contains certificates for SAML authentication:

- `saml-keystore.jks` - Java KeyStore containing the private key and certificate for SAML signing
- Additional IdP certificates can be placed here for verification

## Creating a SAML KeyStore

To create a keystore for SAML authentication, use the following command:

```bash
keytool -genkeypair -alias saml-signing -keyalg RSA -keysize 2048 \
        -keystore saml-keystore.jks -storepass changeit -keypass changeit \
        -validity 365 -dname "CN=Auth Service, OU=IT, O=Company, L=City, ST=State, C=US"
```

## Security Note

In production:

1. Use strong passwords for keystores
2. Store passwords securely (e.g., using environment variables or secret management)
3. Use certificates signed by trusted CAs
4. Rotate certificates regularly
