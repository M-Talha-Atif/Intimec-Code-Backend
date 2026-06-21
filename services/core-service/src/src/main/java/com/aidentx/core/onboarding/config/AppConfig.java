package com.aidentx.core.onboarding.config;

import com.google.protobuf.util.JsonFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    /**
     * Defines a singleton bean for the Protobuf JSON Printer.
     * This printer is configured to preserve the original snake_case field names
     * from the .proto files, ensuring consistency.
     *
     * @return A configured JsonFormat.Printer bean.
     */
    @Bean
    public JsonFormat.Printer protoJsonPrinter() {
        return JsonFormat.printer().preservingProtoFieldNames();
    }

    /**
     * Defines a singleton bean for the Protobuf JSON Parser.
     * This parser is configured to ignore unknown fields, making the service
     * more resilient to minor version mismatches in Protobuf messages.
     *
     * @return A configured JsonFormat.Parser bean.
     */
    @Bean
    public JsonFormat.Parser protoJsonParser() {
        return JsonFormat.parser().ignoringUnknownFields();
    }
}