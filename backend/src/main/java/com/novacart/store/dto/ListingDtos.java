package com.novacart.store.dto;

import com.novacart.store.entity.Listing;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ListingDtos {

    private ListingDtos() {}

    /**
     * Accepted image URL shapes: an https(?) URL or an internal /uploads/ path
     * (what the upload endpoint returns). Rejects things like {@code javascript:}
     * or {@code data:} that shouldn't be stored as a listing image source.
     */
    public static final String IMAGE_URL_PATTERN = "^(https?://|/uploads/).+";

    public record ListingCreateRequest(
            @NotBlank @Size(max = 140) String title,
            @NotBlank @Size(max = 4000) String description,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
            @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2) BigDecimal originalPrice,
            @NotBlank String condition,
            @NotNull Long categoryId,
            @Size(max = 120) String location,
            boolean negotiable,
            @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2) BigDecimal shippingFee,
            @NotNull @Size(min = 1, max = 8)
            List<@NotBlank @Size(max = 500) @Pattern(regexp = IMAGE_URL_PATTERN, message = "must be an http(s) URL or an /uploads/ path") String> imageUrls
    ) {}

    public record ListingUpdateRequest(
            @Size(max = 140) String title,
            @Size(max = 4000) String description,
            @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
            @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2) BigDecimal originalPrice,
            String condition,
            Long categoryId,
            @Size(max = 120) String location,
            Boolean negotiable,
            @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2) BigDecimal shippingFee,
            List<@NotBlank @Size(max = 500) @Pattern(regexp = IMAGE_URL_PATTERN, message = "must be an http(s) URL or an /uploads/ path") String> imageUrls,
            String status
    ) {}

    public record ListingSummary(
            Long id,
            String title,
            BigDecimal price,
            BigDecimal originalPrice,
            String condition,
            String location,
            boolean negotiable,
            String coverImageUrl,
            String status,
            int viewCount,
            int favoriteCount,
            Instant createdAt,
            CategoryDtos.CategoryResponse category,
            UserDtos.PublicUser seller
    ) {
        public static ListingSummary from(Listing l) {
            String cover = l.getImageUrls() != null && !l.getImageUrls().isEmpty() ? l.getImageUrls().get(0) : null;
            return new ListingSummary(
                    l.getId(),
                    l.getTitle(),
                    l.getPrice(),
                    l.getOriginalPrice(),
                    l.getCondition().name(),
                    l.getLocation(),
                    l.isNegotiable(),
                    cover,
                    l.getStatus().name(),
                    l.getViewCount(),
                    l.getFavoriteCount(),
                    l.getCreatedAt(),
                    CategoryDtos.CategoryResponse.from(l.getCategory()),
                    UserDtos.PublicUser.from(l.getSeller())
            );
        }
    }

    public record ListingDetail(
            Long id,
            String title,
            String description,
            BigDecimal price,
            BigDecimal originalPrice,
            String condition,
            String location,
            boolean negotiable,
            BigDecimal shippingFee,
            List<String> imageUrls,
            String status,
            int viewCount,
            int favoriteCount,
            Instant createdAt,
            Instant updatedAt,
            CategoryDtos.CategoryResponse category,
            UserDtos.PublicUser seller,
            boolean favorited
    ) {
        public static ListingDetail from(Listing l, boolean favorited) {
            return new ListingDetail(
                    l.getId(),
                    l.getTitle(),
                    l.getDescription(),
                    l.getPrice(),
                    l.getOriginalPrice(),
                    l.getCondition().name(),
                    l.getLocation(),
                    l.isNegotiable(),
                    l.getShippingFee(),
                    List.copyOf(l.getImageUrls()),
                    l.getStatus().name(),
                    l.getViewCount(),
                    l.getFavoriteCount(),
                    l.getCreatedAt(),
                    l.getUpdatedAt(),
                    CategoryDtos.CategoryResponse.from(l.getCategory()),
                    UserDtos.PublicUser.from(l.getSeller()),
                    favorited
            );
        }
    }
}
