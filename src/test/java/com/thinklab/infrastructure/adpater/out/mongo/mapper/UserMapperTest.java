package com.thinklab.infrastructure.adapter.out.mongo.mapper;

import com.thinklab.domain.model.User;
import com.thinklab.domain.valueobject.UserLevel;
import com.thinklab.domain.valueobject.UserProfile;
import com.thinklab.domain.valueobject.UserStatus;
import com.thinklab.infrastructure.adapter.in.web.dto.response.UserResponse;
import com.thinklab.infrastructure.adapter.out.mongo.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Infrastructure Test: Validates the high-fidelity mapping of the {@link UserMapper}.
 * This suite ensures that the Anti-Corruption Layer (ACL) correctly translates
 * data between the Pure Domain, MongoDB Persistence Entities, and public Web DTOs,
 * preserving state integrity and security boundaries.
 */
@DisplayName("Infrastructure: User Mapper (ACL)")
class UserMapperTest {

    private UserMapper mapper;
    private final UUID tenantId = UUID.randomUUID();
    private final UserProfile profile = new UserProfile(
            "Staff Engineer",
            "staff@thinklab.com",
            "5511999998888",
            "pt-br",
            "America/Sao_Paulo"
    );

    @BeforeEach
    void setUp() {
        this.mapper = new UserMapper();
    }

    @Test
    @DisplayName("Should accurately project Domain Model to Persistence Entity")
    void shouldMapDomainToEntity() {
        // Given
        User domain = User.provision(tenantId, null, "john.doe", UserLevel.OPERATOR, profile, "admin-01");

        // When
        UserEntity entity = mapper.toEntity(domain);

        // Then
        assertAll("Entity Mapping Verification",
                () -> assertEquals(domain.id(), entity.id()),
                () -> assertEquals(domain.tenantId(), entity.tenantId()),
                () -> assertEquals(domain.username(), entity.username()),
                () -> assertEquals(domain.status(), entity.status()),
                () -> assertEquals(domain.profile().corporateEmail(), entity.corporateEmail()),
                () -> assertEquals(domain.version(), entity.version())
        );
    }

    @Test
    @DisplayName("Should restore Domain Model from Persistence Entity with high fidelity")
    void shouldMapEntityToDomain() {
        // Given
        UUID userId = UUID.randomUUID();
        UserEntity entity = new UserEntity(
                userId, tenantId, null, "john.doe", "john.doe@thinklab.com",
                UserStatus.ACTIVE, UserLevel.OPERATOR, profile, List.of("ROLE_USER"),
                0, true, "system", Instant.now(), "system", Instant.now(), 1L
        );

        // When
        User domain = mapper.toDomain(entity);

        // Then
        assertAll("Domain Restoration Verification",
                () -> assertEquals(entity.id(), domain.id()),
                () -> assertEquals(entity.tenantId(), domain.tenantId()),
                () -> assertEquals(entity.status(), domain.status()),
                () -> assertEquals(entity.profile().fullName(), domain.profile().fullName()),
                () -> assertEquals(entity.version(), domain.version())
        );
    }

    @Test
    @DisplayName("Should project Domain Model to sanitized Public Response DTO")
    void shouldMapDomainToResponse() {
        // Given
        User domain = User.provision(tenantId, null, "john.doe", UserLevel.OPERATOR, profile, "admin-01");

        // When
        UserResponse response = mapper.toResponse(domain);

        // Then
        assertAll("Response Projection Verification",
                () -> assertEquals(domain.id(), response.id()),
                () -> assertEquals(domain.username(), response.username()),
                () -> assertEquals(domain.profile().fullName(), response.profile().fullName()),
                () -> assertEquals(domain.version(), response.version())
        );
    }

    @Test
    @DisplayName("Should throw NullPointerException when mapping null domain")
    void shouldFailOnNullDomain() {
        assertThrows(NullPointerException.class, () -> mapper.toEntity(null));
        assertThrows(NullPointerException.class, () -> mapper.toResponse(null));
    }
}