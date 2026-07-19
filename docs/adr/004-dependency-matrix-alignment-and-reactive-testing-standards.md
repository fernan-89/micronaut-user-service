# ADR 004: Dependency Matrix Alignment and Reactive Testing Standards

## Status
Accepted

## Context
Following the stabilization of the Identity service (ADR 003), we encountered two critical operational blockers:
1. **Support Matrix Mismatch:** An inadvertent attempt to use Micronaut 5.x artifacts required a JVM version (Java 25) incompatible with our Java 21 LTS standard, leading to resolution failures.
2. **Missing Runtime Drivers:** The application failed to bootstrap due to a `NoClassDefFoundError` related to `com.mongodb.ReadConcern`, caused by a conflict between synchronous and reactive MongoDB driver declarations.
3. **Reactive Testing Fragmentation:** Initial test attempts lacked a standardized way to validate non-blocking streams, leading to potential "ghost completions" where tests passed while the underlying reactive pipeline leaked errors.

## Decision
To preserve the "NASA-level" reliability of our CI/CD pipeline and runtime stability, we have mandated the following standards:

### 1. Technology Matrix Lock (Micronaut 4.4.2)
We have strictly locked the framework ecosystem to **Micronaut 4.4.2** and **Java 21 LTS**.
- **Constraint:** All dependencies must be managed via the `io.micronaut.platform:micronaut-platform` BOM.
- **Persistence:** Manual declaration of MongoDB drivers is banned. We standardized on `io.micronaut.data:micronaut-data-mongodb`, which transitively provides the correct reactive streams driver and required codecs (e.g., `ReadConcern`).

### 2. Standardized Reactive Validation (StepVerifier)
The use of `StepVerifier` from Project Reactor is now the mandatory standard for testing Interactors and Controllers.
- **Protocol:** Every reactive test must verify not only the emitted data but also the terminal signals (`verifyComplete()` or `verifyError()`).
- **Isolation:** Application tests must remain pure unit tests, using Mockito to stub Repository Ports without lifting the full framework context (Cold Testing).

### 3. Boundary Defense Validation (Command Integrity)
Testing of `application.usecase.command` records must explicitly cover "Defense in Depth".
- **Rule:** Tests must validate that compact constructors correctly apply `trim()`, lowercase normalization, and strict nullability checks.
- **Requirement:** Every Command record must have a corresponding unit test covering 100% of its validation invariants.

### 4. Build Hygiene Protocol
We established a "Nuclear Clean" command for environment stabilization:
- **Standard:** `./gradlew clean --refresh-dependencies --no-build-cache` must be executed after any dependency matrix modification to purge corrupted metadata.

## Consequences

### Positive
* **Determinism:** Eliminates "Support Matrix Mismatch" errors and ensures consistent behavior across developer machines and CI runners.
* **Resilience:** Guaranteed availability of MongoDB core classes at runtime.
* **Quality:** High-fidelity validation of asynchronous pipelines, preventing silent failures.

### Negative
* **Version Lag:** Prevents the immediate adoption of bleeding-edge framework features available only in experimental JVM versions.

## Compliance
* **CI/CD:** Pipelines must fail if dependency versions deviate from the 4.4.2 BOM without an approved architectural exception.
* **Code Review:** PRs without `StepVerifier` for reactive components or missing unit tests for new Command records shall be rejected.