# aidentx Encryption Spring Boot Starter

A Spring Boot starter that provides auto-configured, enterprise-grade encryption services. It handles persistence, configuration, and bean wiring internally, offering a true "plug-and-play" experience.

## Features

- **Automatic Configuration**: Simply add the dependency and set properties to enable encryption.
- **Secure Key Derivation**: Uses PBKDF2 to derive a strong master key from a user-provided secret.
- **Automatic Persistence**: Includes a JPA Entity (`EncryptionKey`) and Spring Data Repository to automatically manage the storage of data encryption keys.
- **AES-GCM Encryption**: Employs secure, authenticated encryption for all operations.
- **Ready-to-Inject Beans**: Automatically provides `EncryptionService` and `FieldEncryptionService` beans for use in your application.

## How to Use

### 1. Add Dependency

Add the starter to your microservice's `pom.xml`:

```xml
<dependency>
    <groupId>com.aidentx</groupId>
    <artifactId>encryption-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```
### 2. Configure Your Application

Add the required encryption properties to your `application.properties` or `application.yml`.

**Configuration Priority:**
This starter follows Spring Boot's standard externalized configuration model. Properties will be resolved in the following order (higher items take precedence):
1.  Environment Variables
2.  `application.properties` file

This allows you to define default or local development values in `application.properties` and easily override them with environment variables in your production, staging, or CI/CD environments.

**`application.properties` (for local development):**
```properties
# ===================================================================
#                    aidentx ENCRYPTION STARTER
# ===================================================================
# A high-entropy secret key used for master key derivation.
# MUST be at least 32 characters long.
# Environment Variable Override: ENCRYPTION_MASTER_KEY
encryption.master-key=YourSuperSecretAndLongKeyGoesHere_AtLeast32Chars

# A static, unique salt for your application.
# Environment Variable Override: ENCRYPTION_SALT
encryption.salt=a-unique-salt-for-your-auth-service
```

**Production Configuration (Example using Environment Variables):**
In your production environment (e.g., Docker, Kubernetes, PaaS), you would set environment variables instead of hardcoding secrets in a file:
```bash
export ENCRYPTION_MASTER_KEY="a_very_secure_and_random_key_from_your_secrets_manager"
export ENCRYPTION_SALT="the_static_salt_for_your_production_app"
```

