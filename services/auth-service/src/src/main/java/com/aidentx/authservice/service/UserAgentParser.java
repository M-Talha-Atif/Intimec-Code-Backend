package com.aidentx.authservice.service;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing User-Agent strings to extract device and browser information.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class UserAgentParser {

    /**
     * Parse user agent and return formatted device string.
     * Examples: "Chrome 120 on Windows 11", "Safari on iPhone 15"
     */
    public String parseDevice(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        String browser = extractBrowser(userAgent);
        String version = extractBrowserVersion(userAgent);
        String os = extractOS(userAgent);

        StringBuilder result = new StringBuilder(browser);
        if (version != null && !version.isEmpty()) {
            result.append(" ").append(version);
        }
        result.append(" on ").append(os);

        return result.toString();
    }

    /**
     * Check if the user agent is from a mobile device.
     */
    public boolean isMobile(String userAgent) {
        if (userAgent == null) return false;
        String ua = userAgent.toLowerCase();
        return ua.contains("mobile") || ua.contains("android") || ua.contains("iphone");
    }

    /**
     * Check if the user agent is from a desktop device.
     */
    public boolean isDesktop(String userAgent) {
        if (userAgent == null) return true; // Default to desktop
        // If not mobile, tablet, or iPad, consider it desktop
        return !isMobile(userAgent);
    }

    private String extractBrowser(String userAgent) {
        String ua = userAgent.toLowerCase();

        // Check for API clients and non-browser tools first
        if (ua.contains("postman")) return "Postman";
        if (ua.equals("node") || ua.contains("node/")) return "Node.js";
        if (ua.contains("curl")) return "cURL";
        if (ua.contains("axios")) return "Axios";
        if (ua.contains("java")) return "Java Client";
        if (ua.contains("python")) return "Python Client";
        
        // Check for browsers
        if (ua.contains("edg/") || ua.contains("edge/")) return "Edge";
        if (ua.contains("opr/") || ua.contains("opera/")) return "Opera";
        if (ua.contains("chrome/") && !ua.contains("edg")) return "Chrome";
        if (ua.contains("safari/") && !ua.contains("chrome")) return "Safari";
        if (ua.contains("firefox/")) return "Firefox";
        if (ua.contains("msie") || ua.contains("trident/")) return "Internet Explorer";

        return "Unknown Browser";
    }

    private String extractBrowserVersion(String userAgent) {
        try {
            // Check for API clients first
            Pattern postmanPattern = Pattern.compile("PostmanRuntime/(\\d+\\.\\d+)");
            Matcher postmanMatcher = postmanPattern.matcher(userAgent);
            if (postmanMatcher.find()) return postmanMatcher.group(1);
            
            Pattern nodePattern = Pattern.compile("node/(\\d+)");
            Matcher nodeMatcher = nodePattern.matcher(userAgent);
            if (nodeMatcher.find()) return nodeMatcher.group(1);
            
            // Check for browsers
            Pattern chromePattern = Pattern.compile("Chrome/(\\d+)");
            Matcher chromeMatcher = chromePattern.matcher(userAgent);
            if (chromeMatcher.find()) return chromeMatcher.group(1);

            Pattern firefoxPattern = Pattern.compile("Firefox/(\\d+)");
            Matcher firefoxMatcher = firefoxPattern.matcher(userAgent);
            if (firefoxMatcher.find()) return firefoxMatcher.group(1);

            Pattern safariPattern = Pattern.compile("Version/(\\d+)");
            Matcher safariMatcher = safariPattern.matcher(userAgent);
            if (safariMatcher.find()) return safariMatcher.group(1);

            Pattern edgePattern = Pattern.compile("(?:Edg|Edge)/(\\d+)");
            Matcher edgeMatcher = edgePattern.matcher(userAgent);
            if (edgeMatcher.find()) return edgeMatcher.group(1);
        } catch (Exception e) {
            // Ignore
        }
        return "";
    }

    private String extractOS(String userAgent) {
        String ua = userAgent.toLowerCase();

        // Mobile OS
        if (ua.contains("iphone")) {
            Pattern pattern = Pattern.compile("iPhone OS (\\d+)");
            Matcher matcher = pattern.matcher(userAgent);
            if (matcher.find()) {
                return "iPhone";
            }
            return "iOS";
        }
        if (ua.contains("ipad")) return "iPad";
        if (ua.contains("android")) {
            Pattern pattern = Pattern.compile("Android (\\d+)");
            Matcher matcher = pattern.matcher(userAgent);
            if (matcher.find()) {
                return "Android " + matcher.group(1);
            }
            return "Android";
        }

        // Desktop OS
        if (ua.contains("windows nt 10.0")) return "Windows 11";
        if (ua.contains("windows nt 6.3")) return "Windows 8.1";
        if (ua.contains("windows nt 6.2")) return "Windows 8";
        if (ua.contains("windows nt 6.1")) return "Windows 7";
        if (ua.contains("windows")) return "Windows";

        if (ua.contains("mac os x")) {
            Pattern pattern = Pattern.compile("Mac OS X (\\d+[._]\\d+)");
            Matcher matcher = pattern.matcher(userAgent);
            if (matcher.find()) {
                String version = matcher.group(1).replace('_', '.');
                return "macOS " + version;
            }
            return "macOS";
        }

        if (ua.contains("linux")) return "Linux";
        if (ua.contains("x11")) return "Unix";
        
        // For API clients without OS info, indicate it's a server/client
        if (ua.equals("node") || ua.contains("postman") || ua.contains("curl") || ua.contains("axios")) {
            return "API Client";
        }

        return "Unknown OS";
    }
}
