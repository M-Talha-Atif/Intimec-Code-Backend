package com.aidentx.authservice.model.dto;

/**
 * DTO representing additional audit log details.
 * This is used to serialize and store metadata like IP address, User-Agent,
 * and identifier in a structured JSON format inside the {@code AuditLog.details} field.
 * <p>
 * Example JSON:
 * {
 * "ipAddress": "192.168.1.10",
 * "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
 * "identifier": "user@example.com"
 * }
 */
public class LogDetails {

    /**
     * The IP address of the client making the request.
     */
    private String ipAddress;

    /**
     * The User-Agent string from the client request.
     */
    private String userAgent;

    /**
     * The identifier used during the request (e.g., username or email).
     */
    private String identifier;

    /**
     * Default constructor for deserialization.
     */
    public LogDetails() {
    }

    /**
     * Constructs a new {@code LogDetails} with specified IP, User-Agent, and identifier.
     *
     * @param ipAddress  the client's IP address
     * @param userAgent  the client's User-Agent string
     * @param identifier the identifier used by the user (e.g., email or username)
     */
    public LogDetails(String ipAddress, String userAgent, String identifier) {
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.identifier = identifier;
    }

    /**
     * Gets the client's IP address.
     *
     * @return the IP address as a String
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Sets the client's IP address.
     *
     * @param ipAddress the IP address to set
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Gets the client's User-Agent string.
     *
     * @return the User-Agent as a String
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Sets the client's User-Agent string.
     *
     * @param userAgent the User-Agent to set
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * Gets the identifier used during the request.
     *
     * @return the identifier as a String
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Sets the identifier used during the request.
     *
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
