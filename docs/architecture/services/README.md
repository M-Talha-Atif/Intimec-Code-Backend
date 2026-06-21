# Service Architecture Documentation

This directory contains detailed documentation for individual microservices in the identity platform.

## Platform Services

### Task Orchestration Service
**[The Task Orchestration Service.md](The%20Task%20Orchestration%20Service.md)**

Multi-domain platform service supporting identity, entitlements, and access reviews with:
- Generic task execution framework
- Pipeline-based processing
- Cross-service task coordination
- Progress tracking and monitoring

### Data Processing Services

### Aggregation Service  
**[The Aggregation Service.md](The%20Aggregation%20Service.md)**

Data collection and ingestion from external resources:
- Connector-based resource integration
- Schema discovery and validation
- Change detection and streaming
- Intent classification during aggregation

### Transformation Service
**[Transformation Service.md](Transformation%20Service.md)**

Data transformation and mapping logic:
- Attribute transformation rules
- Expression evaluation engine
- Data type conversion and validation
- Integration with provisioning pipelines

### Provisioning Service
**[The Provisioning Service.md](The%20Provisioning%20Service.md)**

Outbound account provisioning and lifecycle management:
- Account creation, updates, and deletion
- Intent-aware provisioning logic
- Mapping strength evaluation integration
- Error handling and retry mechanisms

## Data Management Services

### Shadow Ledger
**[The Shadow Ledger.md](The%20Shadow%20Ledger.md)**

Account correlation tracking and shadow management:
- Shadow record lifecycle
- Correlation state management
- MidPoint synchronization situations
- Terminal vs active state handling

### Managed Account Entity
**[The Managed Account Entity.md](The%20Managed%20Account%20Entity.md)**

Identity data model and entity relationships:
- Core identity structure
- Attribute management
- Relationship modeling
- Data consistency patterns

## Service Integration

All services integrate through:
- **Task Execution Service** for workflow coordination
- **Core Service** for governance policies and configuration
- **Event Streaming** for asynchronous processing
- **Audit Services** for compliance and monitoring

## Implementation Notes

Each service document includes:
- Service responsibilities and boundaries
- Data models and entity relationships  
- API specifications and integration points
- Configuration examples and operational procedures
- Migration strategies and implementation phases

## Reading Order

1. **Task Orchestration Service** - Understand the platform foundation
2. **Aggregation Service** - Data ingestion patterns
3. **Transformation Service** - Data processing logic
4. **Shadow Ledger** - Correlation and state management
5. **Provisioning Service** - Outbound processing
6. **Managed Account Entity** - Data modeling details