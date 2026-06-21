# Identity Platform Documentation Guide

## Navigation Overview

This guide helps you navigate the comprehensive identity platform architecture documentation. Documents are organized into logical categories with clear directory structure - start with the **Quick Start** section and progress based on your needs.

## Directory Structure

```
docs/
├── README.md                           # This navigation guide
├── architecture/                       # System architecture documentation
│   ├── Enterprise Identity Platform Architecture Summary.md
│   ├── Identity Governance and Management Overview.md
│   ├── core-systems/                  # Critical enterprise capabilities
│   │   ├── Identity Archetypes and Personas.md
│   │   ├── Resource Schema Handling Architecture.md
│   │   ├── Live Sync and Reconciliation Architecture.md
│   │   ├── Object Marks System Architecture.md
│   │   └── Provisioning Mapping Strength Architecture.md
│   └── services/                       # Individual service designs
│       ├── The Task Orchestration Service.md
│       ├── The Aggregation Service.md
│       ├── The Provisioning Service.md
│       ├── Transformation Service.md
│       ├── The Shadow Ledger.md
│       └── The Managed Account Entity.md
├── operations/                         # Operational procedures and guides
│   ├── onboarding/                     # Resource onboarding procedures
│   │   ├── Advanced_Correlation_Architecture.md
│   │   ├── Onboarding_Overview.md
│   │   ├── Onboarding_Code_Reference.md
│   │   ├── Onboarding_Dataflow.md
│   │   ├── Onboarding_Issues_Fixes.md
│   │   ├── Onboarding_Limitations.md
│   │   ├── Onboarding_Policies.md
│   │   ├── Onboarding_Pseudo_Code.md
│   │   ├── Onboarding_Resources.md
│   │   ├── Onboarding_Shard_Linking.md
│   │   ├── Onboarding_Why_Shards.md
│   │   └── README.md
│   └── deployment/                     # Deployment and configuration
│       └── Tenant Enforcement and Contract Changes.md
```

## Document Categories

### **START HERE** - Quick Navigation
| Document | Purpose | When to Read |
|----------|---------|--------------|
| **[This README](README.md)** | Navigation guide | Always start here |
| **[Enterprise Architecture Summary](architecture/Enterprise%20Identity%20Platform%20Architecture%20Summary.md)** | High-level overview | Want the big picture |
| **[Identity Governance Overview](architecture/Identity%20Governance%20and%20Management%20Overview.md)** | System architecture | Need technical details |

### **CRITICAL FEATURES** - Core Capabilities
| Document | What It Solves | Priority |
|----------|----------------|----------|
| **[Identity Archetypes and Personas](architecture/core-systems/Identity%20Archetypes%20and%20Personas.md)** | Multi-identity problem (employees, contractors, service accounts) | CRITICAL |
| **[Resource Schema Handling](architecture/core-systems/Resource%20Schema%20Handling%20Architecture.md)** | Business meaning of technical objects (kind/intent) | CRITICAL |
| **[Advanced Correlation](operations/onboarding/Advanced_Correlation_Architecture.md)** | Complex identity matching and correlation | CRITICAL |

### **OPERATIONAL EXCELLENCE** - Day-to-Day Operations  
| Document | What It Enables | Priority |
|----------|-----------------|----------|
| **[Live Sync & Reconciliation](architecture/core-systems/Live%20Sync%20and%20Reconciliation%20Architecture.md)** | Real-time vs batch processing | HIGH |
| **[Object Marks System](architecture/core-systems/Object%20Marks%20System%20Architecture.md)** | Governance exceptions and manual overrides | HIGH |
| **[Provisioning Mapping Strength](architecture/core-systems/Provisioning%20Mapping%20Strength%20Architecture.md)** | Deterministic attribute provisioning | HIGH |

### **SPECIALIZED TOPICS** - Deep Dives
| Document | What It Covers | When Needed |
|----------|----------------|-------------|
| **[Task Orchestration Service](architecture/services/The%20Task%20Orchestration%20Service.md)** | Platform task execution engine | Building workflows |
| **[Managed Account Entity](architecture/services/The%20Managed%20Account%20Entity.md)** | Identity data model | Data modeling |
| **[Shadow Ledger](architecture/services/The%20Shadow%20Ledger.md)** | Account correlation tracking | Understanding shadows |
| **[Transformation Service](architecture/services/Transformation%20Service.md)** | Data transformation logic | Attribute mapping |
| **[Aggregation Service](architecture/services/The%20Aggregation%20Service.md)** | Data collection from resources | Resource integration |
| **[Provisioning Service](architecture/services/The%20Provisioning%20Service.md)** | Account provisioning logic | Outbound provisioning |

### **OPERATIONAL PROCEDURES** - Onboarding & Deployment
| Document | Reference Type | Use Case |
|----------|----------------|----------|
| **[Onboarding Overview](operations/onboarding/Onboarding_Overview.md)** | Process guide | Adding new resources |
| **[Tenant Enforcement](operations/deployment/Tenant%20Enforcement%20and%20Contract%20Changes.md)** | Multi-tenancy | Enterprise deployment |

### **DEVELOPMENT GUIDES** - Standards & Procedures
| Document | Reference Type | Use Case |
|----------|----------------|----------|
| **[Development Guide Overview](development/README.md)** | Development standards | Development setup and coding standards |

### **REFERENCE DOCS** - Legacy & Historical
| Document | Reference Type | Use Case |
|----------|----------------|----------|
| **[IAM Policy & Reconciliation Services](reference/IAM%20Policy%20&%20Reconciliation%20Services.md)** | Legacy reference | Historical context |
| **[Identity Synchronization and Shards](reference/Identity%20Synchronization%20and%20Shards.md)** | Legacy reference | Historical context |

---

## Reading Paths by Role

### **Business Stakeholder Path**
1. **[Enterprise Architecture Summary](architecture/Enterprise%20Identity%20Platform%20Architecture%20Summary.md)** - What capabilities we provide
2. **[Identity Archetypes and Personas](architecture/core-systems/Identity%20Archetypes%20and%20Personas.md)** - How we handle different identity types  
3. **[Object Marks System](architecture/core-systems/Object%20Marks%20System%20Architecture.md)** - How governance exceptions work

### **System Administrator Path**
1. **[Identity Governance Overview](architecture/Identity%20Governance%20and%20Management%20Overview.md)** - System architecture
2. **[Live Sync & Reconciliation](architecture/core-systems/Live%20Sync%20and%20Reconciliation%20Architecture.md)** - Operational processes
3. **[Onboarding Overview](operations/onboarding/Onboarding_Overview.md)** - Adding resources
4. **[Tenant Enforcement](operations/deployment/Tenant%20Enforcement%20and%20Contract%20Changes.md)** - Multi-tenant setup

### **Developer Path**  
1. **[Development Guide Overview](development/README.md)** - Development setup and coding standards
2. **[Identity Governance Overview](architecture/Identity%20Governance%20and%20Management%20Overview.md)** - Technical architecture
3. **[Task Orchestration Service](architecture/services/The%20Task%20Orchestration%20Service.md)** - Task execution framework
4. **[Resource Schema Handling](architecture/core-systems/Resource%20Schema%20Handling%20Architecture.md)** - Kind/Intent system
5. **[Advanced Correlation](operations/onboarding/Advanced_Correlation_Architecture.md)** - Correlation engine
6. **[Provisioning Mapping Strength](architecture/core-systems/Provisioning%20Mapping%20Strength%20Architecture.md)** - Mapping logic

### **Implementation Team Path**
1. **[Enterprise Architecture Summary](architecture/Enterprise%20Identity%20Platform%20Architecture%20Summary.md)** - Complete picture
2. **[Identity Archetypes and Personas](architecture/core-systems/Identity%20Archetypes%20and%20Personas.md)** - Foundation implementation
3. **[Resource Schema Handling](architecture/core-systems/Resource%20Schema%20Handling%20Architecture.md)** - Phase 1 implementation  
4. **[Advanced Correlation](operations/onboarding/Advanced_Correlation_Architecture.md)** - Phase 2 implementation
5. All other documents as needed for specific features

---

## Quick Problem-Solution Guide

### "How do we handle different types of users?"
→ **[Identity Archetypes and Personas](architecture/core-systems/Identity%20Archetypes%20and%20Personas.md)**

### "Why are service accounts correlating to employees?"  
→ **[Resource Schema Handling](architecture/core-systems/Resource%20Schema%20Handling%20Architecture.md)** (Intent system)

### "How do we handle real-time changes vs batch loads?"
→ **[Live Sync & Reconciliation](architecture/core-systems/Live%20Sync%20and%20Reconciliation%20Architecture.md)**

### "We need to override policies for this specific user"
→ **[Object Marks System](architecture/core-systems/Object%20Marks%20System%20Architecture.md)**

### "Multiple systems are setting the same attribute differently"
→ **[Provisioning Mapping Strength](architecture/core-systems/Provisioning%20Mapping%20Strength%20Architecture.md)**

### "Complex identity matching isn't working"
→ **[Advanced Correlation](operations/onboarding/Advanced_Correlation_Architecture.md)**

### "How do we add a new application/resource?"
→ **[Onboarding Overview](operations/onboarding/Onboarding_Overview.md)**

---

## Architecture Quick Reference

### **The Big Three Critical Systems**
These three systems solve the fundamental identity management problems:

1. **Archetypes & Personas** = "Who are they?" (Person with multiple identities)
2. **Kind & Intent** = "What is this account for?" (Business purpose of technical objects)  
3. **Advanced Correlation** = "How do we match them?" (Complex identity correlation)

### **Operational Excellence Trio**
These three systems handle day-to-day operations:

1. **Live Sync & Reconciliation** = Real-time + batch processing
2. **Object Marks** = Governance exceptions and overrides  
3. **Mapping Strength** = Deterministic attribute provisioning

### **Platform Foundation**
- **Identity Governance Overview** = Overall system architecture
- **Task Orchestration** = Workflow execution engine
- **Tenant Enforcement** = Multi-tenant isolation

---

## Document Status Legend

| Status | Meaning | Documents |
|--------|---------|-----------|
| **COMPLETE** | Fully designed, ready for implementation | Archetypes, Schema Handling, Correlation, Live Sync, Object Marks, Mapping Strength |
| **STABLE** | Core architecture defined, may need minor updates | Task Orchestration, Governance Overview |
| **REFERENCE** | Process documentation and guides | Onboarding docs, specialized services |

---

## Implementation Priority Order

### Phase 0: Foundation *(Already Complete)*
- Service architecture and task execution framework

### Phase 1: Critical Systems *(Priority Implementation)*
1. **Identity Archetypes & Personas** - Multi-identity foundation
2. **Resource Schema Handling** - Kind/Intent semantic classification  
3. **Advanced Correlation** - Archetype-aware correlation engine

### Phase 2: Operational Excellence *(Next Priority)*
4. **Live Sync & Reconciliation** - Real-time and batch processing
5. **Object Marks System** - Governance exception handling
6. **Mapping Strength** - Deterministic provisioning

### Phase 3: Advanced Features *(Enhancement Phase)*
7. Machine learning enhancements
8. Advanced reporting and analytics
9. Additional governance workflows

---

## Tips for Reading

### **Each Document Structure**
Most architecture documents follow this pattern:
1. **Overview** - What problem it solves
2. **Core Concepts** - Key ideas and terminology  
3. **Data Model** - Entities and relationships
4. **Service Implementation** - Code examples and APIs
5. **Configuration Examples** - Real-world usage
6. **Integration Points** - How it connects to other systems
7. **Migration Strategy** - Implementation phases

### **How to Approach Complex Documents**
1. Read **Overview** and **Core Concepts** first
2. Skim **Data Model** to understand entities
3. Focus on **Configuration Examples** for practical understanding
4. Dive into **Service Implementation** when ready to build

### **Focus Areas by Need**
- **Need to understand business value?** → Read Overview + Core Concepts
- **Need to implement?** → Read Data Model + Service Implementation  
- **Need to configure?** → Read Configuration Examples + Integration Points
- **Need to plan rollout?** → Read Migration Strategy + Implementation phases

---

*This guide is your compass through the identity platform architecture. Start with your role's reading path above, and use the quick problem-solution guide when you need to solve specific challenges.*