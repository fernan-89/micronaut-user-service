# ADR 002: Strong Typing (UUID), Explicit DI, and Atomic Mutation Standards

## Status
Accepted

## Context
During the initial stabilization of the `user-manager-service`, we identified technical risks that compromised the "NASA-level" reliability standards:
1. **Identifier Ambiguity:** String-based IDs in the persistence layer caused BSON encoding failures and type mismatches between the Core Domain and MongoDB.
2. **DI Proxy Collisions:** Using Lombok's `@RequiredArgsConstructor` on Micronaut-managed beans (Controllers, Repositories) led to `NoSuchMethodError` during server bootstrap due to conflicts with AOT proxy generation.
3. **Persistence State Drift:** Default `.save()` operations in reactive pipelines triggered `MongoWriteException` (E11000) when handling deterministic UUIDs, as the framework attempted INSERTs for already existing identities.

## Decision
To eliminate architectural drift and ensure production-grade stability, we have mandated the following standards:

### 1. Mandatory Native UUID Sovereignty
We have migrated all identity and link identifiers from String to `java.util.UUID`.
*   **Domain Sovereignty:** Identification must be strictly typed as UUID in all Records.
*   **Infrastructure:** MongoDB is locked to `uuid-representation: STANDARD`, persisting identifiers as BSON Binary Subtype 4 for optimal performance.

### 2. Explicit Dependency Injection (AOT-Safe)
We have banned the use of Lombok for constructor injection in components managed by the Micronaut container.
*   **Protocol:** Mandatory use of explicit constructors annotated with `@Inject`.
*   **Rationale:** Ensures deterministic bean instantiation and full compatibility with GraalVM Native Image.

### 3. Atomic State Mutation Protocol (REPLACE)
State mutations must avoid the ambiguity of `.save()` and use explicit update semantics.
*   **Standard:** Use the `.update()` port method to trigger an atomic REPLACE operation.
*   **Versioning:** All mutations must preserve and increment the `@Version` field to maintain optimistic locking integrity in high-concurrency scenarios.

### 4. Resilient Boundary Defense (RFC 7807)
Standardized the `GlobalExceptionHandler` to translate all Business and Infrastructure failures into compliant **RFC 7807 (Problem Details)** responses, protecting the internal stacktrace while providing actionable metadata to API consumers.

## Consequences

### Positive
*   **Determinism:** Eliminates runtime casting errors and bean instantiation failures.
*   **Performance:** Binary UUIDs and AOT-compliant DI reduce startup latency and memory overhead.
*   **Observability:** Standardized error contracts and explicit logs improve MTTR (Mean Time To Recovery).

### Negative
*   **Boilerplate:** Requires manual constructor definitions and explicit mapping in the Anti-Corruption Layer (ACL).

## Compliance
*   **CI/CD:** Pipelines must fail if `@RequiredArgsConstructor` is detected on Singletons or Controllers.
*   **Code Review:** Any mutation of an Aggregate Root without a corresponding forensic audit trail must be rejected.
*   **Sovereignty:** Persistence entities must remain encapsulated within the infrastructure l