# Architecture Documentation

This directory contains the core architectural documentation for the identity platform.

## Main Architecture Documents

- **[Enterprise Identity Platform Architecture Summary](Enterprise%20Identity%20Platform%20Architecture%20Summary.md)** - Complete high-level overview of the platform capabilities and design principles
- **[Identity Governance and Management Overview](Identity%20Governance%20and%20Management%20Overview.md)** - Detailed system architecture, service interactions, and technical design

## Core Systems

The **[core-systems/](core-systems/)** directory contains documentation for the critical enterprise capabilities that form the foundation of our identity platform:

- **Identity Archetypes and Personas** - Multi-identity support with person-centric golden records
- **Resource Schema Handling** - Kind/Intent semantic classification for business context
- **Live Sync & Reconciliation** - Real-time and batch processing capabilities  
- **Object Marks System** - Governance exceptions and policy flexibility
- **Provisioning Mapping Strength** - Deterministic provisioning with conflict resolution

## Individual Services

The **[services/](services/)** directory contains detailed documentation for individual microservices:

- **Task Orchestration Service** - Platform task execution engine
- **Aggregation Service** - Data collection from resources
- **Provisioning Service** - Account provisioning logic
- **Transformation Service** - Data transformation logic
- **Shadow Ledger** - Account correlation tracking
- **Managed Account Entity** - Identity data model

## Reading Order

1. Start with **Enterprise Architecture Summary** for the big picture
2. Read **Identity Governance Overview** for technical details
3. Focus on **core-systems/** for critical capabilities
4. Dive into **services/** for specific implementation details