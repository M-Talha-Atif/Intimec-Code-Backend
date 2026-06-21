package com.aidentx.authservice.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Utility class for extracting client-related information from HTTP requests,
 * such as IP address and User-Agent.
 */
@Component
public class RequestUtils {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP",
            "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
    };

    /**
     * Extracts the client's IP address by inspecting common proxy headers.
     *
     * @param request the current HTTP request
     * @return the resolved IP address as a String
     */
    public static String getClientIP(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (isValidIP(ip)) {
                return ip.contains(",") ? ip.split(",")[0].trim() : ip.trim();
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * Returns the User-Agent string from the request.
     * Checks X-User-Agent header first (forwarded from frontend), then falls back to User-Agent.
     *
     * @param request the HTTP request
     * @return the User-Agent string, or null if not present
     */
    public static String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("X-User-Agent");
        if (userAgent != null && !userAgent.isEmpty()) {
            return userAgent;
        }
        return request.getHeader("User-Agent");
    }

    /**
     * Checks if the extracted IP is valid (not empty, not "unknown").
     *
     * @param ip the IP address string
     * @return true if valid, false otherwise
     */
    private static boolean isValidIP(String ip) {
        return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip);
    }
}
