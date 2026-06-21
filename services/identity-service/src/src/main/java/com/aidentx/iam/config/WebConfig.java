package com.aidentx.iam.config;

import com.aidentx.iam.identity_management.model.enums.IdentityType;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToIdentityTypeConverter());
    }

    private static class StringToIdentityTypeConverter implements Converter<String, IdentityType> {
        @Override
        public IdentityType convert(String source) {
            try {
                return IdentityType.valueOf(source.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Try fromValue if valueOf fails (though valueOf covers the standard case)
                try {
                    return IdentityType.fromValue(source);
                } catch (IllegalArgumentException ex) {
                    // Throwing IllegalArgumentException will result in 400 Bad Request
                    throw new IllegalArgumentException("Invalid identity type: " + source);
                }
            }
        }
    }
}
