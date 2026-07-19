# Thinklab User Manager Service

#### Mission-Critical Overview
This microservice is a high-assurance identity orchestrator responsible for the authoritative lifecycle management of Enterprise Identities within the Thinklab ecosystem [cite: 1, 2, 1613]. Engineered under Hexagonal Architecture (Ports and Adapters) and a Fully Reactive Stack, it enforces strict domain isolation, zero-blocking I/O operations, and absolute data integrity for multi-tenant hierarchical environments [cite: 1, 2, 1315, 1575].

#### Architecture Blueprint
The service strictly adheres to the NASA-Level Engineering Blueprint, ensuring the core business logic remains framework-agnostic and immune to infrastructure volatility [cite: 3, 1436, 1450, 1575].
- Domain Sovereignty: Core logic resides in pure Java 21 Records, utilizing Sealed Interfaces and deterministic State Machines (FSM) [cite: 1095, 1267, 1427, 1576].
- Reactive Pipeline: End-to-end non-blocking execution using Project Reactor (Mono/Flux), optimized for high-concurrency and backpressure-aware data streaming [cite: 2, 45, 1210, 1221].
- Dependency Inversion: Use cases (Interactors) communicate with external worlds solely through high-fidelity Ports (Interfaces), preventing architectural drift [cite: 1113, 1118, 1200, 1455].

#### Technology Stack
- Runtime: Java 21 LTS (Amazon Corretto / GraalVM) [cite: 43, 1303].
- Framework: Micronaut 4.4.2 (AOT Optimized) [cite: 45, 1303, 1318].
- Reactive Engine: Project Reactor [cite: 45, 1209, 1318].
- Persistence: Reactive MongoDB with Binary UUID Subtype 4 support for high-performance indexing [cite: 45, 1143, 1195, 1318].
- Serialization: Micronaut Serde (Reflection-free JSON processing) [cite: 33, 178, 185, 945].
- Documentation: OpenAPI 3.0 (Generated at compile-time) [cite: 45, 601, 1673].

#### Core Engineering Mandates
1. Multi-Tenant Isolation: Mandatory data segregation via tenantId and hierarchical parentId. Consultations are strictly scoped to the tenant context to prevent cross-tenant data leakage [cite: 30, 1061, 1205, 1576].
2. Deterministic State Transitions: Lifecycle changes (PENDING to ACTIVE to SUSPENDED) are governed by an internal Finite State Machine (FSM) that prevents illegal state corruption at the memory level [cite: 34, 1064, 1267, 1430].
3. Forensic Audit Ledger: Every state mutation is transactionally linked to a mandatory, immutable audit trail (Tier 3 Compliance), capturing the executor, timestamp, and business justification [cite: 1060, 1135, 1207, 1223].
4. Optimistic Concurrency Control: Prevention of "Lost Updates" in highly concurrent reactive pipelines via automated versioning (@Version) [cite: 30, 1104, 1222, 1576].
5. Strong Typing Identity: Mandatory use of java.util.UUID for all identifiers to eliminate injection risks and parsing overhead [cite: 959, 1073, 1143, 1576].

#### Operational Setup
Prerequisites: Java 21 JDK, Docker (for Testcontainers and MongoDB Atlas Local), Gradle 8.5+ [cite: 1303, 1500, 1670].
Build and Execution:
- Clean and compile with AOT optimizations: `./gradlew clean compileJava` [cite: 41, 160, 1344].
- Run the reactive application (Port 8082): `./gradlew run` [cite: 1227, 1670].
- Build GraalVM Native Image: `./gradlew nativeCompile` [cite: 1670].

#### API Specification and Observability
- OpenAPI Specs: Available at `/swagger/micronaut-user-service-1.0.0.yml` [cite: 39, 43, 60].
- Swagger UI: Access via `/swagger-ui` for interactive contract testing [cite: 47, 51].
- Health and Metrics: Integrated Micronaut Management endpoints at `/health` (Liveness/Readiness) and `/metrics` for Prometheus scraping [cite: 15, 38, 43, 1298].

#### Resilient Error Boundary (RFC 7807)
All system failures are projected as standardized Problem Details for HTTP APIs (RFC 7807), ensuring predictable contracts for consumers while shielding internal stacktraces [cite: 112, 1160, 1294, 1613]:
- 404 Not Found: Entity missing in the specific tenant context [cite: 86, 626, 1187].
- 409 Conflict: Business rule violation or Version collision [cite: 86, 1102, 1320, 1432].
- 422 Unprocessable Entity: Illegal state transition (e.g., REVOKED to ACTIVE) [cite: 634, 1432].

#### Quality Assurance Strategy
- Tier 1 (Unit): 100% coverage of Domain FSM and Interactor orchestration using JUnit 5 and Mockito [cite: 294, 296, 302, 1544].
- Tier 2 (Integration): Validation of persistence adapters and BSON UUID mapping using Testcontainers (MongoDB) [cite: 301, 302, 1304, 1546].
- Tier 3 (E2E): Automated lifecycle validation via Postman Collection with dynamic state capture [cite: 7, 1006, 1073, 1674].