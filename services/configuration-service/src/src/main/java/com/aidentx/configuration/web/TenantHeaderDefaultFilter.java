package com.aidentx.configuration.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantHeaderDefaultFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String DEFAULT_TENANT = "tenant_001";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String tenant = request.getHeader(TENANT_HEADER);

        if (tenant == null || tenant.isBlank()) {
            HttpServletRequest wrapped = new HeaderMapRequestWrapper(request, Map.of(TENANT_HEADER, List.of(DEFAULT_TENANT)));
            filterChain.doFilter(wrapped, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    static class HeaderMapRequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, List<String>> extraHeaders;

        HeaderMapRequestWrapper(HttpServletRequest request, Map<String, List<String>> extraHeaders) {
            super(request);
            this.extraHeaders = new HashMap<>();
            // normalize header names as case-insensitive by storing lowercase keys
            extraHeaders.forEach((k, v) -> this.extraHeaders.put(k.toLowerCase(Locale.ROOT), new ArrayList<>(v)));
        }

        @Override
        public String getHeader(String name) {
            List<String> values = extraHeaders.get(name.toLowerCase(Locale.ROOT));
            if (values != null && !values.isEmpty()) {
                return values.get(0);
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            List<String> values = extraHeaders.get(name.toLowerCase(Locale.ROOT));
            if (values != null) {
                return Collections.enumeration(values);
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>();
            Enumeration<String> original = super.getHeaderNames();
            while (original.hasMoreElements()) {
                names.add(original.nextElement());
            }
            // Add our extra header names preserving original case choice
            names.addAll(extraHeaders.keySet());
            return Collections.enumeration(names);
        }
    }
}