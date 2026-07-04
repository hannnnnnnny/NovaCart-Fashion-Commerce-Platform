package com.novacart.store.dto;

import com.novacart.store.entity.User;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class UserDtos {

    private UserDtos() {}

    public record UserSummary(
            Long id,
            String email,
            String displayName,
            String avatarUrl,
            String location,
            String role
    ) {
        public static UserSummary from(User u) {
            return new UserSummary(
                    u.getId(),
                    u.getEmail(),
                    u.getDisplayName(),
                    u.getAvatarUrl(),
                    u.getLocation(),
                    u.getRole().name()
            );
        }
    }

    public record PublicUser(
            Long id,
            String displayName,
            String avatarUrl,
            String bio,
            String location,
            double averageRating,
            int ratingCount,
            Instant memberSince
    ) {
        public static PublicUser from(User u) {
            return new PublicUser(
                    u.getId(),
                    u.getDisplayName(),
                    u.getAvatarUrl(),
                    u.getBio(),
                    u.getLocation(),
                    u.averageRating(),
                    u.getRatingCount(),
                    u.getCreatedAt()
            );
        }
    }

    public record UpdateProfileRequest(
            @Size(min = 2, max = 80) String displayName,
            @Size(max = 500)
            @Pattern(regexp = "^$|^(https?://|/uploads/).+", message = "must be an http(s) URL or an /uploads/ path")
            String avatarUrl,
            @Size(max = 600) String bio,
            @Size(max = 120) String location
    ) {}
}
