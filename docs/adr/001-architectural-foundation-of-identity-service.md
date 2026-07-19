# ADR 001: Architectural Foundation of the Identity Service (Hexagonal, Reactive, and Mission-Critical Patterns)

## Status
Accepted

## Context
The `user-manager-service` is a core component of the Thinklab IAM ecosystem, responsible for orchestrating identity provisioning, privilege mutations, and forensic auditing. To ensure "NASA-level" reliability, the service requires an architecture capable of handling high-throughput asynchronous operations while maintaining absolute data integrity, multi-tenant isolation, and GraalVM compatibility.

Historical technical debts in similar services, such as framework proxy conflicts, weak typing for identifiers, and database upsert collisions, necessitated a standardized, strict architectural blueprint.

## Decision
We have decided to adopt the **Standardized Mission-Critical Pattern**, which integrates the following engineering pillars:

### 1. Hexagonal Architecture (Ports and Adapters)
We strictly separate the **Core Domain** (Pure Java Records, no frameworks) from the **Application Layer** (Interactors and Use Cases) and the **Infrastructure Layer** (Web Adapters, Persistence Adapters, and External Clients). This ensures that business logic is isolated and testable without infrastructure dependencies.

### 2. Reactive Stack Sovereignty
The service utilizes **Micronaut 4.4.2** as the foundation, powered by **Project Reactor** (Mono/Flux) and **MongoDB Reactive Streams**. All I/O operations are non-blocking, ensuring optimal resource utilization under high concurrency.

### 3. Native UUID Sovereignty (BSON Binary)
We have banished String-based identifiers in favor of `java.util.UUID` for all resource IDs and hierarchical links.
*   **Infrastructure:** Persisted as BSON Binary Subtype 4 for 2x faster indexing and reduced storage.
*   **Domain:** Ensures strong typing and prevents malformed ID injection at the boundary.

### 4. AOT-Compliant Dependency Injection
To ensure full compatibility with **Ahead-of-Time (AOT)** compilation and **GraalVM Native Image**, we have banned the use of Lombok's `@RequiredArgsConstructor` on any component requiring framework proxying (Singletons, Repositories, Controllers).
*   **Protocol:** Explicit constructor injection using the `@Inject` annotation is mandatory.

### 5. Functional State Mutations & Optimistic Locking
Mutations on Aggregate Roots are performed functionally (returning new instances). Persistence is governed by:
*   **Atomic Replacement:** Using `.update()` instead of `.save()` for existing entities to prevent E11000 key collisions.
*   **Concurrency Control:** Strict enforcement of `@Version` fields to detect and prevent "Lost Update" scenarios in reactive pipelines.

### 6. Append-Only Forensic Audit Trail (Tier 3)
Every state mutation (Provisioning, Roles Update, Status Transition) must trigger a mandatory, immutable audit record persisted in an append-only collection. This ensures forensic traceability and regulatory compliance.

### 7. Boundary Defense (RFC 7807)
The service enforces Fail-Fast validation at the edge using Jakarta Validation and projects all domain/infrastructure failures into standardized **RFC 7807 (Problem Details)** responses, shielding internal state and providing predictable error contracts.

## Consequences

### Positive
*   **Determinism:** Predictable behavior across concurrent reactive threads.
*   **Observability:** Unified log patterns and forensic audit trails facilitate SRE operations.
*   **Portability:** Domain logic remains pure and independent of database or web framework shifts.
*   **Performance:** AOT processing and binary UUIDs optimize startup and runtime overhead.

### Negative
*   **Boilerplate:** Requires explicit mapping between Domain Records and Persistence Entities.
*   **Learning Curve:** Developers must be proficient in Functional Reactive Programming (FRP) and strict layering.

## Compliance
*   **CI/CD:** Pipelines must fail if Lombok DI is detected in Micronaut-managed components.
*   **Code Review:** Any mutation of an Aggregate Root without a corresponding forensic audit entry must be rejected.
*   **Sovereignty:** All IDs in the Domain layer must be strictly typed as `java.util.UUID`.