# Unified Logging Package

A comprehensive, fluent logging solution for aidentx microservices that provides structured, standardized, and traceable
logging across all services.

## Features

- **Fluent API**: An intuitive, chainable API for building log messages.
- **Structured JSON Logging**: All log entries are formatted as structured JSON for easy parsing by systems like ELK or Splunk.
- **Automatic Context**: Automatically captures the source class, correlation ID, and a globally unique log ID.
- **Distributed Tracing**: Automatic propagation of Correlation IDs across service calls made with `RestTemplate` and `WebClient`.
- **Default Category**: Set a default logging category per class to reduce boilerplate.
- **Spring Boot Integration**: Zero-effort auto-configuration for Spring Boot applications.
- **Configurable**: Highly configurable through `application.properties`.

## Installation

Add the dependency to your microservice's `pom.xml`:

```xml
<dependency>
    <groupId>com.aidentx.logging</groupId>
    <artifactId>unified-logging-starter</artifactId>
    <version>1.1.0</version> <!-- Use the latest version -->
</dependency>