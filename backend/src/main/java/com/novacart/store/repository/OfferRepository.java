package com.novacart.store.repository;

import com.novacart.store.entity.Listing;
import com.novacart.store.entity.Offer;
import com.novacart.store.entity.OfferStatus;
import com.novacart.store.entity.User;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    Page<Offer> findByListing_SellerOrderByCreatedAtDesc(User seller, Pageable pageable);

    Page<Offer> findByBuyerOrderByCreatedAtDesc(User buyer, Pageable pageable);

    List<Offer> findByListingOrderByCreatedAtDesc(Listing listing);

    long countByListing_SellerAndStatus(User seller, OfferStatus status);

    /**
     * Bulk-reject every still-pending offer on a listing except the one being
     * accepted, so a seller can't accept multiple offers on the same item.
     */
    // flushAutomatically writes the just-accepted offer first; we deliberately
    // do NOT clearAutomatically, so the caller's managed listing/offer stay
    // attached for the subsequent markReserved() write.
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE Offer o SET o.status = com.novacart.store.entity.OfferStatus.REJECTED,
                               o.respondedAt = :now
            WHERE o.listing = :listing
              AND o.id <> :keepOfferId
              AND o.status = com.novacart.store.entity.OfferStatus.PENDING
            """)
    int rejectOtherPendingOffers(@Param("listing") Listing listing,
                                 @Param("keepOfferId") Long keepOfferId,
                                 @Param("now") Instant now);
}
