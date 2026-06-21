package com.aidentx.authservice.service;

import com.aidentx.authservice.config.AppConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service for resolving IP addresses to geographic locations.
 * Uses ip-api.com free service (45 requests per minute limit).
 * Returns location as a simple string like "New York, USA" or "London, UK".
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class IpGeolocationService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public IpGeolocationService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Get location information for an IP address as a formatted string.
     * Returns "City, Country" format (e.g., "New York, USA").
     * Returns "Local" for localhost/private IPs.
     * Returns null if the lookup fails.
     * 
     * @param ipAddress The IP address to lookup
     * @return Location string or null if not available
     */
    public String getLocation(String ipAddress) {
        // Skip geolocation for localhost and private IPs
        if (isLocalOrPrivateIp(ipAddress)) {
            return AppConstants.LOCATION_LOCAL;
        }
        
        try {
            String url = AppConstants.IP_API_URL + ipAddress + AppConstants.IP_API_QUERY_FIELDS;
            String response = restTemplate.getForObject(url, String.class);
            
            if (response == null) {
                return null;
            }
            
            JsonNode jsonNode = objectMapper.readTree(response);
            
            // Check if the request was successful
            if (!AppConstants.IP_GEO_STATUS_SUCCESS.equals(jsonNode.get("status").asText())) {
                return null;
            }
            
            String city = jsonNode.has("city") && !jsonNode.get("city").asText().isEmpty() 
                ? jsonNode.get("city").asText() 
                : null;
            String country = jsonNode.has("country") ? jsonNode.get("country").asText() : AppConstants.LOCATION_UNKNOWN;
            
            // Format: "City, Country" or just "Country" if city is not available
            if (city != null && !city.equals(AppConstants.LOCATION_UNKNOWN)) {
                return city + ", " + country;
            } else {
                return country;
            }
            
        } catch (Exception e) {
            // Log error but don't fail the request
            System.err.println("Failed to get geolocation for IP " + ipAddress + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if an IP address is local or private.
     */
    private boolean isLocalOrPrivateIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return true;
        }
        
        // Check for localhost (both IPv4 and IPv6)
        if (ipAddress.equals("127.0.0.1") || ipAddress.equals("localhost") || 
            ipAddress.equals("::1") || ipAddress.equals("0:0:0:0:0:0:0:1")) {
            return true;
        }
        
        // Check for private IP ranges (IPv4 only)
        String[] parts = ipAddress.split("\\.");
        if (parts.length != 4) {
            // If it's not IPv4, check if it's IPv6 local/private
            return isPrivateIPv6(ipAddress);
        }
        
        try {
            int firstOctet = Integer.parseInt(parts[0]);
            int secondOctet = Integer.parseInt(parts[1]);
            
            // 10.0.0.0 - 10.255.255.255
            if (firstOctet == 10) {
                return true;
            }
            
            // 172.16.0.0 - 172.31.255.255
            if (firstOctet == 172 && secondOctet >= 16 && secondOctet <= 31) {
                return true;
            }
            
            // 192.168.0.0 - 192.168.255.255
            if (firstOctet == 192 && secondOctet == 168) {
                return true;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        
        return false;
    }
    
    /**
     * Check if an IPv6 address is local or private.
     */
    private boolean isPrivateIPv6(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return true;
        }
        
        // IPv6 localhost variations
        if (ipAddress.equals("::1") || ipAddress.equals("0:0:0:0:0:0:0:1")) {
            return true;
        }
        
        // IPv6 link-local addresses (fe80::/10)
        if (ipAddress.toLowerCase().startsWith("fe80:")) {
            return true;
        }
        
        // IPv6 unique local addresses (fc00::/7 and fd00::/8)
        if (ipAddress.toLowerCase().startsWith("fc") || ipAddress.toLowerCase().startsWith("fd")) {
            return true;
        }
        
        // Default to not private for other IPv6 addresses
        return false;
    }
}
