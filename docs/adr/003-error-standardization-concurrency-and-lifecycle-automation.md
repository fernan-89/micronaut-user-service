# ADR 003: Error Standardization (RFC 7807), Concurrency Control, and Lifecycle Automation

## Status
Accepted

## Context
Following the stabilization of the reactive infrastructure (ADR 002), we needed to address three operational requirements for production readiness:
1. **Ambiguous Error Contracts:** Business failures were not consistently projected to API consumers, leading to integration friction.
2. **Race Conditions in State Mutations:** High-concurrency reactive pipelines posed a risk of "Lost Updates" when multiple agents modified the same User aggregate.
3. **Manual Integration Testing:** Validating the multi-tenant hierarchy and state machine transitions (PENDING -> ACTIVE -> SUSPENDED) required repetitive manual effort.

## Decision
We have implemented a tripartite stabilization protocol:

### 1. Unified Error Boundary (RFC 7807)
We adopted the **Problem Details for HTTP APIs (RFC 7807)** standard.
- **Implementation:** A centralized `GlobalExceptionHandler` intercepts all `BusinessException` types.
- **Output:** Every error response contains a machine-readable `errorCode`, a human-readable message, a `timestamp`, and the request `path`.
- **Security:** Internal stacktraces are logged at the `ERROR` level for SRE but are strictly omitted from the public response.

### 2. Optimistic Concurrency Control
To ensure data integrity without blocking threads:
- **Requirement:** All Domain Records and Infrastructure Entities must include a `Long version` field.
- **Mechanism:** Leveraged Micronaut Data's `@Version` support to trigger `OptimisticLockException` during conflicting writes, which is then mapped to an HTTP 409 (Conflict).

### 3. Automated Lifecycle Validation (Postman E2E)
Established a continuous validation suite to guarantee "NASA-level" reliability.
- **Dynamic Chains:** Scripts capture the `userId` from provisioning responses to feed subsequent mutation requests (Status/Roles).
- **Hierarchy Testing:** The suite explicitly validates the binding between a Root Tenant and its Branch Tenants through automated variable injection.

## Consequences

### Positive
* **Predictability:** Clients receive standardized error metadata, reducing Mean Time to Resolution (MTTR).
* **Reliability:** Data collisions are handled gracefully at the database level.
* **Agility:** The Postman Lifecycle Suite allows for instant regression testing after infrastructure or domain changes.

### Negative
* **Schema Overhead:** The mandatory `@Version` field adds small storage overhead per document in MongoDB.

## Compliance
* **Contract Review:** No new API endpoint shall be approved without being added to the Postman Lifecycle Collection.
* **Auditability:** All state transitions tracked in the Postman suite must generate a correspon