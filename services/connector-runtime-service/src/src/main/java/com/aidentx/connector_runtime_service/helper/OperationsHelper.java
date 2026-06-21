package com.aidentx.connector_runtime_service.helper;

import com.aidentx.connector_runtime_service.mapper.ProtoMapper;
import com.aidentx.dal.protos.runtime.CompositeFilter;
import com.aidentx.dal.protos.runtime.Filter;
import com.aidentx.dal.protos.runtime.NotFilter;
import com.aidentx.dal.protos.runtime.SearchObjectsResponse;
import com.aidentx.dal.protos.runtime.SimpleFilter;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import org.identityconnectors.framework.api.*;
import org.identityconnectors.framework.api.operations.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import com.aidentx.connector_runtime_service.config.ConnectorProperties;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A helper service that isolates all direct interactions with the ConnId
 * framework.
 * It is responsible for building configurations, creating connector facades,
 * translating filters, and executing operations like search, create, update,
 * and delete.
 */
@Service
public class OperationsHelper {

    private final IUnifiedLogger logger;
    private final TypeConverterService typeConverterService;
    private final ConnectorProperties connectorProperties;

    public OperationsHelper(TypeConverterService typeConverterService,
                            ConnectorProperties connectorProperties,
                            UnifiedLoggerFactory unifiedLoggerFactory) {
        this.typeConverterService = typeConverterService;
        this.connectorProperties = connectorProperties;
        this.logger = unifiedLoggerFactory.getLogger(OperationsHelper.class);
    }

    /**
     * Create a {@link ConnectorFacade} for the given connector info and
     * configuration, execute the provided operation and ensure proper
     * disposal of the facade.
     *
     * @param <T>           the return type of the facade operation
     * @param info          connector metadata
     * @param configuration protobuf Struct containing connector properties
     * @param operation     operation to execute with the created facade
     * @return value returned by the operation
     */
    public <T> T withConnectorFacade(ConnectorInfo info, Struct configuration, FacadeOperation<T> operation) {
        ConnectorFacade facade = null;
        try {
            APIConfiguration apiConfig = buildApiConfig(info, configuration);
            facade = ConnectorFacadeFactory.getInstance().newInstance(apiConfig);
            return operation.apply(facade);
        } catch (Exception e) {
                        logger.error(e).message("Operation failed for connector: " + info.getConnectorDisplayName()).log();
            throw new RuntimeException("Connector operation failed: " + e.getMessage(), e);
        } finally {
            if (facade != null) {
                facade.dispose();
            }
        }
    }

    /**
     * Execute a search against the connector. Supports a best-effort fallback
     * to unpaged streaming when the connector does not support the framework's
     * paged + filtered-results handler combination.
     *
     * @param info            connector metadata
     * @param configuration   connector configuration as protobuf Struct
     * @param objectClassName object class name to search
     * @param filter          protobuf filter
     * @param pageSize        requested page size ({@code <= 0} means unpaged)
     * @param resultConsumer  consumer invoked for each returned ConnectorObject
     */
    public void search(ConnectorInfo info, Struct configuration, String objectClassName, Filter filter, int pageSize,
            Consumer<ConnectorObject> resultConsumer) {
        withConnectorFacade(info, configuration, facade -> {
            ObjectClass oclass = new ObjectClass(objectClassName);
            org.identityconnectors.framework.common.objects.filter.Filter connIdFilter = buildConnIdFilter(filter);

            // If no paging requested (<=0) just stream unpaged.
            if (pageSize <= 0) {
                logger.debug().message("Executing unpaged search (pageSize <= 0)").log();
                facade.search(oclass, connIdFilter, co -> {
                    resultConsumer.accept(co);
                    return true;
                }, new OperationOptionsBuilder().build());
                return null;
            }

            try {
                executePagedSearch(facade, oclass, connIdFilter, pageSize, resultConsumer);
            } catch (RuntimeException ex) {
                String msg = ex.getMessage();
                if (msg != null
                        && msg.contains("Paged search is requested, but the filtered results handler is enabled")) {
                    logger.warn().message("Paged search unsupported with filtered results handler; falling back to unpaged. error=" + msg).log();
                    facade.search(oclass, connIdFilter, co -> {
                        resultConsumer.accept(co);
                        return true;
                    }, new OperationOptionsBuilder().build());
                } else {
                    throw ex;
                }
            }
            return null;
        });
    }

    /**
     * Execute a limited search — reads at most {@code limit} objects from the
     * connector and then <strong>stops the scan</strong>.  Unlike the paged
     * {@link #search(ConnectorInfo, Struct, String, Filter, int, Consumer)}
     * variant which iterates through ALL pages, this method uses an unpaged
     * search with a {@link ResultsHandler} that returns {@code false} once
     * the limit is reached, causing the ConnId framework to abort the
     * connector scan immediately.
     *
     * <p>Use this for preview / sample operations where you need a bounded
     * number of results without reading the entire data source.</p>
     */
    public void searchWithLimit(ConnectorInfo info, Struct configuration,
                                String objectClassName, Filter filter, int limit,
                                Consumer<ConnectorObject> resultConsumer) {
        withConnectorFacade(info, configuration, facade -> {
            ObjectClass oclass = new ObjectClass(objectClassName);
            org.identityconnectors.framework.common.objects.filter.Filter connIdFilter =
                    buildConnIdFilter(filter);
            final int[] count = {0};
            facade.search(oclass, connIdFilter, co -> {
                if (count[0] >= limit) {
                    return false; // stop the connector scan
                }
                resultConsumer.accept(co);
                count[0]++;
                return count[0] < limit; // false on the last item → stop
            }, new OperationOptionsBuilder().build());
            return null;
        });
    }

    /**
     * Execute a search against the connector with enhanced pagination and complex query support.
     *
     * @param info            connector metadata
     * @param configuration   connector configuration as protobuf Struct
     * @param objectClassName object class name to search
     * @param filter          protobuf filter
     * @param pageSize        requested page size ({@code <= 0} means unpaged)
     * @param pageOffset      offset for pagination (0-based)
     * @param sortBy          field to sort by
     * @param sortDescending  whether to sort in descending order
     * @param includeTotalCount whether to include total count in response
     * @param searchFields    fields to search in for multi-field search
     * @return SearchObjectsResponse containing results and metadata
     */
    public SearchObjectsResponse search(ConnectorInfo info, Struct configuration, String objectClassName, 
            Filter filter, int pageSize, int pageOffset, String sortBy, boolean sortDescending, 
            boolean includeTotalCount, List<String> searchFields) {
        // Safety limit: prevent unbounded memory accumulation in unary (non-streaming) responses
        final int MAX_PAGED_RESULTS = 10_000;
        return withConnectorFacade(info, configuration, facade -> {
            ObjectClass oclass = new ObjectClass(objectClassName);
            org.identityconnectors.framework.common.objects.filter.Filter connIdFilter = buildConnIdFilter(filter);
            
            SearchObjectsResponse.Builder responseBuilder = SearchObjectsResponse.newBuilder();
            List<ConnectorObject> results = new ArrayList<>();
            
            // Build operation options
            OperationOptionsBuilder optionsBuilder = new OperationOptionsBuilder();
            
            // Set page size if specified
            if (pageSize > 0) {
                optionsBuilder.setPageSize(pageSize);
                if (pageOffset > 0) {
                    optionsBuilder.setPagedResultsOffset(pageOffset);
                }
            }
            
            // Set sorting if specified
            if (sortBy != null && !sortBy.trim().isEmpty()) {
                SortKey sortKey = new SortKey(sortBy, !sortDescending);
                optionsBuilder.setSortKeys(sortKey);
            }
            
            // Execute search
            if (pageSize <= 0) {
                // Unpaged search — apply safety limit
                logger.debug().message("Executing unpaged search (safety limit=" + MAX_PAGED_RESULTS + ")").log();
                facade.search(oclass, connIdFilter, co -> {
                    results.add(co);
                    return results.size() < MAX_PAGED_RESULTS;
                }, optionsBuilder.build());
            } else {
                // Paged search
                try {
                    executePagedSearch(facade, oclass, connIdFilter, pageSize, pageOffset, results, optionsBuilder);
                } catch (RuntimeException ex) {
                    String msg = ex.getMessage();
                    if (msg != null && msg.contains("Paged search is requested, but the filtered results handler is enabled")) {
                        logger.warn().message("Paged search unsupported with filtered results handler; falling back to unpaged (safety limit=" + MAX_PAGED_RESULTS + "). error=" + msg).log();
                        results.clear();
                        facade.search(oclass, connIdFilter, co -> {
                            results.add(co);
                            return results.size() < MAX_PAGED_RESULTS;
                        }, new OperationOptionsBuilder().build());
                    } else {
                        throw ex;
                    }
                }
            }
            
            // Convert results to protobuf objects
            for (ConnectorObject connIdObject : results) {
                responseBuilder.addObjects(ProtoMapper.toProtoConnectorObject(connIdObject));
            }
            
            // Set pagination metadata
            responseBuilder.setHasMore(results.size() == pageSize && pageSize > 0);
            
            // Calculate total count if requested (this is an approximation for most connectors)
            if (includeTotalCount) {
                // For many connectors, getting exact count requires a separate query
                // This is a simplified implementation - in production you might want to
                // execute a separate count query or use connector-specific features
                if (pageSize <= 0) {
                    responseBuilder.setTotalCount(results.size());
                } else {
                    // Estimate total count based on current page
                    responseBuilder.setTotalCount(pageOffset + results.size() + (results.size() == pageSize ? 1 : 0));
                }
            }
            
            return responseBuilder.build();
        });
    }

    private void executePagedSearch(ConnectorFacade facade,
            ObjectClass oclass,
            org.identityconnectors.framework.common.objects.filter.Filter connIdFilter,
            int pageSize,
            int pageOffset,
            List<ConnectorObject> results,
            OperationOptionsBuilder optionsBuilder) {
        final List<ConnectorObject> pageResults = new ArrayList<>();
        final ResultsHandler handler = connectorObject -> {
            pageResults.add(connectorObject);
            return true;
        };

        // Set page offset if specified
        if (pageOffset > 0) {
            optionsBuilder.setPagedResultsOffset(pageOffset);
        }

        logger.debug().message(String.format("Executing paged search (pageSize=%d, pageOffset=%d)", pageSize, pageOffset)).log();
        SearchResult searchResult = facade.search(oclass, connIdFilter, handler, optionsBuilder.build());
        
        // Add results from first page
        results.addAll(pageResults);
        
        // If we got a full page and there might be more, try to get the next page
        if (pageResults.size() == pageSize) {
            String pagingCookie = searchResult.getPagedResultsCookie();
            if (pagingCookie != null) {
                logger.debug().message("Fetching next page, cookie=" + pagingCookie).log();
                pageResults.clear();
                optionsBuilder.setPagedResultsCookie(pagingCookie);
                searchResult = facade.search(oclass, connIdFilter, handler, optionsBuilder.build());
                results.addAll(pageResults);
            }
        }
    }

    private void executePagedSearch(ConnectorFacade facade,
            ObjectClass oclass,
            org.identityconnectors.framework.common.objects.filter.Filter connIdFilter,
            int pageSize,
            Consumer<ConnectorObject> resultConsumer) {
        OperationOptionsBuilder optionsBuilder = new OperationOptionsBuilder().setPageSize(pageSize);
        final List<ConnectorObject> pageResults = new ArrayList<>();
        final ResultsHandler handler = connectorObject -> {
            pageResults.add(connectorObject);
            return true;
        };

        logger.debug().message(String.format("Executing first page of paged search (pageSize=%d)", pageSize)).log();
        SearchResult searchResult = facade.search(oclass, connIdFilter, handler, optionsBuilder.build());
        for (ConnectorObject obj : pageResults) {
            resultConsumer.accept(obj);
        }
        String pagingCookie = searchResult.getPagedResultsCookie();

        while (pagingCookie != null) {
            logger.debug().message("Fetching next page, cookie=" + pagingCookie).log();
            pageResults.clear();
            optionsBuilder.setPagedResultsCookie(pagingCookie);
            searchResult = facade.search(oclass, connIdFilter, handler, optionsBuilder.build());
            for (ConnectorObject obj : pageResults) {
                resultConsumer.accept(obj);
            }
            pagingCookie = searchResult.getPagedResultsCookie();
        }
    }

    public Set<Attribute> protoToConnIdAttributes(Struct attributesStruct) {
        if (attributesStruct == null) {
            return Set.of();
        }
        return attributesStruct.getFieldsMap().entrySet().stream()
                .map(entry -> {
                    String name = entry.getKey();
                    Object value = fromProtobufValue(entry.getValue());
                    if (value instanceof List) {
                        return AttributeBuilder.build(name, (List<?>) value);
                    }
                    return AttributeBuilder.build(name, value);
                })
                .collect(Collectors.toSet());
    }

    private org.identityconnectors.framework.common.objects.filter.Filter buildConnIdFilter(Filter protoFilter) {
        if (protoFilter == null || protoFilter.getFilterTypeCase() == Filter.FilterTypeCase.FILTERTYPE_NOT_SET) {
            return null;
        }

        switch (protoFilter.getFilterTypeCase()) {
            case COMPOSITE_FILTER:
                CompositeFilter composite = protoFilter.getCompositeFilter();
                List<org.identityconnectors.framework.common.objects.filter.Filter> nestedFilters = composite
                        .getFiltersList().stream()
                        .map(this::buildConnIdFilter)
                        .collect(Collectors.toList());
                return composite.getOperator() == CompositeFilter.LogicalOperator.AND
                        ? FilterBuilder.and(nestedFilters)
                        : FilterBuilder.or(nestedFilters);

            case SIMPLE_FILTER:
                SimpleFilter simple = protoFilter.getSimpleFilter();
                String name = simple.getAttributeName();
                Object value = fromProtobufValue(simple.getAttributeValue());
                Attribute attr = AttributeBuilder.build(name, value);

                switch (simple.getOperator()) {
                    case EQUALS:
                        return FilterBuilder.equalTo(attr);
                    case CONTAINS:
                        return FilterBuilder.contains(attr);
                    case STARTS_WITH:
                        return FilterBuilder.startsWith(attr);
                    case ENDS_WITH:
                        return FilterBuilder.endsWith(attr);
                    case GREATER_THAN:
                        return FilterBuilder.greaterThan(attr);
                    case LESS_THAN:
                        return FilterBuilder.lessThan(attr);
                    case GREATER_THAN_OR_EQUAL:
                        return FilterBuilder.greaterThanOrEqualTo(attr);
                    case LESS_THAN_OR_EQUAL:
                        return FilterBuilder.lessThanOrEqualTo(attr);
                    case IS_PRESENT:
                        return FilterBuilder.containsAllValues(AttributeBuilder.build(name));
                    case BETWEEN:
                        if (simple.hasAttributeValueEnd()) {
                            Object endValue = fromProtobufValue(simple.getAttributeValueEnd());
                            Attribute startAttr = AttributeBuilder.build(name, value);
                            Attribute endAttr = AttributeBuilder.build(name, endValue);
                            return FilterBuilder.and(
                                FilterBuilder.greaterThanOrEqualTo(startAttr),
                                FilterBuilder.lessThanOrEqualTo(endAttr)
                            );
                        } else {
                            throw new IllegalArgumentException("BETWEEN operator requires attribute_value_end");
                        }
                    case IN:
                        if (!simple.getAttributeValuesList().isEmpty()) {
                            List<Object> values = simple.getAttributeValuesList().stream()
                                .map(this::fromProtobufValue)
                                .collect(Collectors.toList());
                            return FilterBuilder.containsAllValues(AttributeBuilder.build(name, values));
                        } else {
                            throw new IllegalArgumentException("IN operator requires attribute_values");
                        }
                    case LIKE:
                        // For LIKE, we'll use contains as the closest approximation
                        // since ConnId doesn't have a direct LIKE operator
                        return FilterBuilder.contains(attr);
                    case REGEX:
                        // For REGEX, we'll also use contains as approximation
                        // since ConnId doesn't have a direct REGEX operator
                        return FilterBuilder.contains(attr);
                    default:
                        throw new IllegalArgumentException(
                                "Unsupported simple filter operator: " + simple.getOperator());
                }

            case NOT_FILTER:
                NotFilter notFilter = protoFilter.getNotFilter();
                org.identityconnectors.framework.common.objects.filter.Filter innerFilterToNegate = buildConnIdFilter(
                        notFilter.getFilter());
                return FilterBuilder.not(innerFilterToNegate);

            default:
                throw new IllegalArgumentException("Unknown filter type.");
        }
    }

    private APIConfiguration buildApiConfig(ConnectorInfo info, Struct configuration) {
        APIConfiguration apiConfig = info.createDefaultAPIConfiguration();

        // Guard against half-open TCP connections and unresponsive source systems.
        // Without these, a stale LDAP/AD socket causes facade.search() to block
        // forever — the root cause of indefinitely-hung task executions.
        ConnectorProperties.Timeout t = connectorProperties.timeout();
        apiConfig.setTimeout(SearchApiOp.class, (int) t.searchMs());
        apiConfig.setTimeout(TestApiOp.class,   (int) t.testMs());
        apiConfig.setTimeout(CreateApiOp.class, (int) t.createMs());
        apiConfig.setTimeout(UpdateApiOp.class, (int) t.updateMs());
        apiConfig.setTimeout(DeleteApiOp.class, (int) t.deleteMs());

        ConfigurationProperties props = apiConfig.getConfigurationProperties();
        Map<String, Value> fieldsMap = configuration.getFieldsMap();

        for (String definedPropName : props.getPropertyNames()) {
            if (fieldsMap.containsKey(definedPropName)) {
                Value propValue = fieldsMap.get(definedPropName);
                if (propValue != null && propValue.getKindCase() != Value.KindCase.NULL_VALUE) {
                    Object javaValue = fromProtobufValue(propValue);
                    if (isEffectivelyEmpty(javaValue))
                        continue;
                    try {
                        Object convertedValue = typeConverterService.convert(javaValue,
                                props.getProperty(definedPropName).getType());
                        props.setPropertyValue(definedPropName, convertedValue);
                    } catch (Exception e) {
                        logger.error(e).message("Failed to convert or set property: " + definedPropName).log();
                    }
                }
            }
        }
        return apiConfig;
    }

    private Object fromProtobufValue(Value value) {
        return switch (value.getKindCase()) {
            case NULL_VALUE -> null;
            case NUMBER_VALUE -> value.getNumberValue();
            case STRING_VALUE -> value.getStringValue();
            case BOOL_VALUE -> value.getBoolValue();
            case LIST_VALUE ->
                value.getListValue().getValuesList().stream().map(this::fromProtobufValue).collect(Collectors.toList());
            case STRUCT_VALUE -> {
                // Handle Struct values (e.g., File type properties wrapped by TypeConverterService)
                Struct struct = value.getStructValue();
                // If the struct has exactly one field, unwrap it
                if (struct.getFieldsCount() == 1) {
                    yield fromProtobufValue(struct.getFieldsMap().values().iterator().next());
                } 
                // If the struct has a "value" field, extract it
                else if (struct.containsFields("value")) {
                    yield fromProtobufValue(struct.getFieldsOrThrow("value"));
                }
                // Otherwise return the struct as-is
                yield struct;
            }
            default -> "STRUCT_NOT_SUPPORTED";
        };
    }

    private boolean isEffectivelyEmpty(Object value) {
        if (value == null)
            return true;
        if (value instanceof String s)
            return s.trim().isEmpty();
        return false;
    }

    /**
     * Functional operation executed with a live {@link ConnectorFacade}.
     *
     * @param <R> return type
     */
    @FunctionalInterface
    public interface FacadeOperation<R> {
        /**
         * Apply the operation using the provided facade.
         *
         * @param facade the connector facade
         * @return operation result
         * @throws Exception any exception is propagated to the caller
         */
        R apply(ConnectorFacade facade) throws Exception;
    }
}