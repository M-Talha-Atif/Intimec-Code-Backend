# Core Systems Architecture

This directory contains documentation for the critical enterprise capabilities that solve fundamental identity management problems and provide MidPoint parity.

## The Big Three Critical Systems

These three systems solve the fundamental identity management problems:

### 1. Identity Archetypes and Personas
**[Identity Archetypes and Personas.md](Identity%20Archetypes%20and%20Personas.md)**

**Problem Solved**: "Who are they?" - Multi-identity support with person-centric golden records

**Key Capabilities**:
- Person entity with multiple Identity personas
- Archetype-based identity classification (Employee, Contractor, ServiceAccount, etc.)
- Archetype-aware correlation and provisioning
- Lifecycle management per persona type

### 2. Resource Schema Handling  
**[Resource Schema Handling Architecture.md](Resource%20Schema%20Handling%20Architecture.md)**

**Problem Solved**: "What is this account for?" - Business purpose of technical objects

**Key Capabilities**:
- Kind classification (account, entitlement, generic)
- Intent classification (employee, contractor, service_account, etc.)
- Intent detection during aggregation
- Intent-specific policies and correlation rules

### 3. Live Sync and Reconciliation
**[Live Sync and Reconciliation Architecture.md](Live%20Sync%20and%20Reconciliation%20Architecture.md)**

**Problem Solved**: "How do we process changes?" - Real-time vs batch processing optimization

**Key Capabilities**:
- Real-time Live Sync for immediate change processing
- Batch Reconciliation for comprehensive data consistency
- Separate optimized data paths and error handling
- Different task types and performance characteristics

## Operational Excellence Trio

These systems handle day-to-day operations and governance:

### 4. Object Marks System
**[Object Marks System Architecture.md](Object%20Marks%20System%20Architecture.md)**

**Problem Solved**: Governance exceptions and manual overrides with audit trails

**Key Capabilities**:
- Mark any object with governance indicators
- Policy override capabilities with approval workflows
- Processing control (skip provisioning, quarantine, etc.)
- Comprehensive audit trail for compliance

### 5. Provisioning Mapping Strength
**[Provisioning Mapping Strength Architecture.md](Provisioning%20Mapping%20Strength%20Architecture.md)**

**Problem Solved**: "Which system wins?" - Deterministic attribute provisioning

**Key Capabilities**:
- Strength-based evaluation (0-100) for competing sources
- Intent-aware strength calculation
- Conflict resolution with transparent reasoning
- Dynamic strength based on context and system health

## Implementation Priority

**Phase 1: Critical Systems** (Implement First)
1. Identity Archetypes & Personas
2. Resource Schema Handling
3. Advanced Correlation (see operations/onboarding/)

**Phase 2: Operational Excellence** (Next Priority)  
4. Live Sync & Reconciliation
5. Object Marks System
6. Provisioning Mapping Strength

## Integration Points

All core systems integrate with:
- Task Execution Service for workflow orchestration
- Identity Service for identity lifecycle management
- Core Service for governance policy management
- Audit services for compliance tracking

Each document includes complete data models, service implementations, configuration examples, and migration strategies.