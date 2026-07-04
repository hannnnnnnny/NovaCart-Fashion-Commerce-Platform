package com.novacart.store.dto;

import com.novacart.store.entity.Offer;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class OfferDtos {

    private OfferDtos() {}

    public record OfferCreateRequest(
            @NotNull Long listingId,
            @NotNull @DecimalMin(value = "0.01") @jakarta.validation.constraints.Digits(integer = 10, fraction = 2) BigDecimal amount,
            @Size(max = 500) String message
    ) {}

    public record OfferCounterRequest(
            @NotNull @DecimalMin(value = "0.01") @jakarta.validation.constraints.Digits(integer = 10, fraction = 2) BigDecimal amount,
            @Size(max = 500) String message
    ) {}

    public record OfferResponse(
            Long id,
            Long listingId,
            String listingTitle,
            String listingCoverImageUrl,
            UserDtos.PublicUser buyer,
            UserDtos.PublicUser seller,
            BigDecimal amount,
            String message,
            String status,
            boolean fromSeller,
            Long parentOfferId,
            Instant createdAt,
            Instant respondedAt
    ) {
        public static OfferResponse from(Offer o) {
            String cover = o.getListing().getImageUrls() != null && !o.getListing().getImageUrls().isEmpty()
                    ? o.getListing().getImageUrls().get(0) : null;
            return new OfferResponse(
                    o.getId(),
                    o.getListing().getId(),
                    o.getListing().getTitle(),
                    cover,
                    UserDtos.PublicUser.from(o.getBuyer()),
                    UserDtos.PublicUser.from(o.getListing().getSeller()),
                    o.getAmount(),
                    o.getMessage(),
                    o.getStatus().name(),
                    o.isFromSeller(),
                    o.getParentOffer() != null ? o.getParentOffer().getId() : null,
                    o.getCreatedAt(),
                    o.getRespondedAt()
            );
        }
    }
}
