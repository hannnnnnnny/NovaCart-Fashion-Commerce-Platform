package com.novacart.store.service;

import com.novacart.store.dto.OfferDtos;
import com.novacart.store.dto.PageResponse;
import com.novacart.store.entity.Listing;
import com.novacart.store.entity.ListingStatus;
import com.novacart.store.entity.Offer;
import com.novacart.store.entity.OfferStatus;
import com.novacart.store.entity.User;
import com.novacart.store.exception.BusinessRuleException;
import com.novacart.store.exception.ConflictException;
import com.novacart.store.exception.ForbiddenOperationException;
import com.novacart.store.exception.ResourceNotFoundException;
import com.novacart.store.repository.OfferRepository;
import com.novacart.store.security.CurrentUserService;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfferService {

    private final OfferRepository offerRepository;
    private final ListingService listingService;
    private final CurrentUserService currentUserService;

    public OfferService(OfferRepository offerRepository, ListingService listingService, CurrentUserService currentUserService) {
        this.offerRepository = offerRepository;
        this.listingService = listingService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public OfferDtos.OfferResponse create(OfferDtos.OfferCreateRequest request) {
        User buyer = currentUserService.requireCurrentUser();
        Listing listing = listingService.requireById(request.listingId());
        if (listing.getSeller().getId().equals(buyer.getId())) {
            throw new BusinessRuleException("You cannot make an offer on your own listing.");
        }
        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new BusinessRuleException("This listing is no longer available for offers.");
        }
        if (!listing.isNegotiable()) {
            throw new BusinessRuleException("Seller does not accept offers on this listing.");
        }
        Offer offer = new Offer();
        offer.setListing(listing);
        offer.setBuyer(buyer);
        offer.setAmount(request.amount());
        offer.setMessage(request.message());
        offer.setFromSeller(false);
        offer.setStatus(OfferStatus.PENDING);
        offer.setCreatedAt(Instant.now());
        return OfferDtos.OfferResponse.from(offerRepository.save(offer));
    }

    @Transactional
    public OfferDtos.OfferResponse accept(Long offerId) {
        User current = currentUserService.requireCurrentUser();
        Offer offer = requireById(offerId);
        if (!offer.getListing().getSeller().getId().equals(current.getId())) {
            throw new ForbiddenOperationException("Only the seller can accept this offer.");
        }
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new BusinessRuleException("This offer is not pending.");
        }
        acceptOfferAndReserve(offer);
        return OfferDtos.OfferResponse.from(offer);
    }

    /**
     * Shared accept logic for both a direct offer accept and a buyer accepting
     * a seller counter. Requires the listing to still be ACTIVE (so a leftover
     * offer can't resurrect a SOLD item), reserves it, and rejects every other
     * pending offer on the listing so only one buyer can hold the reservation.
     */
    private void acceptOfferAndReserve(Offer offer) {
        Listing listing = offer.getListing();
        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new ConflictException("This listing is no longer available.");
        }
        offer.setStatus(OfferStatus.ACCEPTED);
        offer.setRespondedAt(Instant.now());
        offerRepository.rejectOtherPendingOffers(listing, offer.getId(), Instant.now());
        listingService.markReserved(listing);
    }

    @Transactional
    public OfferDtos.OfferResponse reject(Long offerId) {
        User current = currentUserService.requireCurrentUser();
        Offer offer = requireById(offerId);
        if (!offer.getListing().getSeller().getId().equals(current.getId())) {
            throw new ForbiddenOperationException("Only the seller can reject this offer.");
        }
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new BusinessRuleException("This offer is not pending.");
        }
        offer.setStatus(OfferStatus.REJECTED);
        offer.setRespondedAt(Instant.now());
        return OfferDtos.OfferResponse.from(offer);
    }

    @Transactional
    public OfferDtos.OfferResponse counter(Long offerId, OfferDtos.OfferCounterRequest request) {
        User current = currentUserService.requireCurrentUser();
        Offer original = requireById(offerId);
        if (!original.getListing().getSeller().getId().equals(current.getId())) {
            throw new ForbiddenOperationException("Only the seller can counter this offer.");
        }
        if (original.getStatus() != OfferStatus.PENDING) {
            throw new BusinessRuleException("This offer is not pending.");
        }
        original.setStatus(OfferStatus.COUNTERED);
        original.setRespondedAt(Instant.now());

        Offer counter = new Offer();
        counter.setListing(original.getListing());
        counter.setBuyer(original.getBuyer());
        counter.setParentOffer(original);
        counter.setAmount(request.amount());
        counter.setMessage(request.message());
        counter.setFromSeller(true);
        counter.setStatus(OfferStatus.PENDING);
        counter.setCreatedAt(Instant.now());
        return OfferDtos.OfferResponse.from(offerRepository.save(counter));
    }

    @Transactional
    public OfferDtos.OfferResponse withdraw(Long offerId) {
        User current = currentUserService.requireCurrentUser();
        Offer offer = requireById(offerId);
        boolean isAuthor = offer.isFromSeller()
                ? offer.getListing().getSeller().getId().equals(current.getId())
                : offer.getBuyer().getId().equals(current.getId());
        if (!isAuthor) {
            throw new ForbiddenOperationException("You can only withdraw your own offers.");
        }
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new BusinessRuleException("Only pending offers can be withdrawn.");
        }
        offer.setStatus(OfferStatus.WITHDRAWN);
        offer.setRespondedAt(Instant.now());
        return OfferDtos.OfferResponse.from(offer);
    }

    @Transactional
    public OfferDtos.OfferResponse acceptCounter(Long offerId) {
        User current = currentUserService.requireCurrentUser();
        Offer offer = requireById(offerId);
        if (!offer.isFromSeller()) {
            throw new BusinessRuleException("Only counter-offers from the seller can be accepted here.");
        }
        if (!offer.getBuyer().getId().equals(current.getId())) {
            throw new ForbiddenOperationException("Only the buyer in this thread can accept the counter.");
        }
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new BusinessRuleException("This counter-offer is no longer pending.");
        }
        acceptOfferAndReserve(offer);
        return OfferDtos.OfferResponse.from(offer);
    }

    @Transactional(readOnly = true)
    public PageResponse<OfferDtos.OfferResponse> receivedOffers(int page, int size) {
        User seller = currentUserService.requireCurrentUser();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(60, Math.max(1, size)));
        Page<Offer> result = offerRepository.findByListing_SellerOrderByCreatedAtDesc(seller, pageable);
        return PageResponse.from(result.map(OfferDtos.OfferResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<OfferDtos.OfferResponse> sentOffers(int page, int size) {
        User buyer = currentUserService.requireCurrentUser();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(60, Math.max(1, size)));
        Page<Offer> result = offerRepository.findByBuyerOrderByCreatedAtDesc(buyer, pageable);
        return PageResponse.from(result.map(OfferDtos.OfferResponse::from));
    }

    @Transactional(readOnly = true)
    public Offer requireById(Long id) {
        return offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found."));
    }
}
