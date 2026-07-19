package com.thinklab.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nonnull;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Value Object: Type-Safe Enumeration defining the authority tiers and data isolation
 * boundaries within the IAM ecosystem.
 * This enum governs the hierarchical access model, ensuring that identities are
 * strictly confined to their authorized organizational scope (Global, Holding, or Branch).
 *
 * <p><b>Architectural Principles:</b></p>
 * <ul>
 *     <li><b>Hierarchical Governance:</b> Defines clear escalation and isolation levels.</li>
 *     <li><b>AOT Optimization:</b> Pre-computed lookup maps for zero-reflection performance.</li>
 *     <li><b>Serialization Integrity:</b> Jackson-annotated for resilient JSON-to-Enum conversion.</li>
 * </ul>
 */
@Serdeable
@Introspected
public enum UserLevel {

    /**
     * Sovereign global access. Thinklab internal staff with authority to
     * manage all organizations and platform-wide configurations.
     */
    SYSTEM_ADMIN,

    /**
     * Organizational Administrator (Holding level). Authorized to manage
     * configurations, billing, and identities for a specific Root Tenant
     * and all its subsequent Branches.
     */
    TENANT_ADMIN,

    /**
     * Unit Manager (Branch level). Authorized to manage operations and
     * users strictly within a specific Sub-tenant (Branch/Filial).
     */
    BRANCH_MANAGER,

    /**
     * Operational end-user. Access is limited to specific functional roles
     * (e.g., Inventory, Sales) with no administrative authority over the
     * Tenant structure.
     */
    OPERATOR;

    private static final Map<String, UserLevel> LOOKUP_MAP = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(Enum::name, Function.identity()));

    /**
     * Standardized name for JSON representation and persistence mapping.
     *
     * @return The string name of the authority level.
     */
    @JsonValue
    public String getValue() {
        return name();
    }

    /**
     * Factory Method: Performs fail-fast, case-insensitive lookup of a User Level.
     * Crucial for robust API request handling and preventing corrupted state persistence.
     *
     * @param value The string representation of the level (e.g., "TENANT_ADMIN").
     * @return The matching {@link UserLevel} tier.
     * @throws IllegalArgumentException if the provided value does not match any tier.
     */
    @JsonCreator
    public static UserLevel fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        UserLevel level = LOOKUP_MAP.get(value.trim().toUpperCase());
        if (level == null) {
            throw new IllegalArgumentException("Unrecognized authority tier: " + value);
        }
        return level;
    }
}