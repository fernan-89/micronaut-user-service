package com.thinklab.infrastructure.adapter.in.web.dto.response;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Infrastructure DTO: Standardized wrapper for paginated user identity results.
 * This record follows the Collection Projection pattern to provide a consistent
 * structure for API consumers, including both the sanitized payload and the
 * pagination metadata required for advanced UI navigation and grid management.
 *
 * @param content       The sanitized list of user records for the current page.
 * @param totalElements The total count of identities matching the filter criteria.
 * @param page          The current zero-indexed page number.
 * @param size          The total number of items per page (limit).
 */
@Serdeable
@Introspected
@Schema(
        name = "PagedUserResponse",
        description = "Paginated container representing a subset of the Enterprise Identity registry with navigation metadata."
)
public record PagedUserResponse(
        @Schema(description = "The list of sanitized user identities for the current page")
        List<UserResponse> content,

        @Schema(description = "The total count of identities matching the criteria across all pages", example = "1250")
        Long totalElements,

        @Schema(description = "The current zero-indexed page number", example = "0")
        Integer page,

        @Schema(description = "The maximum number of items requested for this page", example = "20")
        Integer size
) {

    /**
     * Static Factory: Constructs a paginated response from a list of projected users.
     * Enforces immutability by creating a defensive copy of the content list.
     *
     * @param content Projected user response list.
     * @param total   Total count from the database.
     * @param page    Current page index.
     * @param size    Current page size.
     * @return A fully initialized {@link PagedUserResponse}.
     */
    @Nonnull
    public static PagedUserResponse of(@Nonnull List<UserResponse> content, long total, int page, int size) {
        return new PagedUserResponse(
                List.copyOf(content),
                total,
                page,
                size
        );
    }
}