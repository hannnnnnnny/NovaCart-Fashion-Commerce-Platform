package com.novacart.store.service;

import com.novacart.store.dto.ListingDtos;
import com.novacart.store.dto.PageResponse;
import com.novacart.store.entity.Category;
import com.novacart.store.entity.Favorite;
import com.novacart.store.entity.Listing;
import com.novacart.store.entity.ListingCondition;
import com.novacart.store.entity.ListingStatus;
import com.novacart.store.entity.User;
import com.novacart.store.exception.BusinessRuleException;
import com.novacart.store.exception.ConflictException;
import com.novacart.store.exception.ForbiddenOperationException;
import com.novacart.store.exception.ResourceNotFoundException;
import com.novacart.store.repository.FavoriteRepository;
import com.novacart.store.repository.ListingRepository;
import com.novacart.store.security.CurrentUserService;
import com.novacart.store.util.EnumParsers;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListingService {

    private final ListingRepository listingRepository;
    private final FavoriteRepository favoriteRepository;
    private final CategoryService categoryService;
    private final CurrentUserService currentUserService;

    public ListingService(
            ListingRepository listingRepository,
            FavoriteRepository favoriteRepository,
            CategoryService categoryService,
            CurrentUserService currentUserService
    ) {
        this.listingRepository = listingRepository;
        this.favoriteRepository = favoriteRepository;
        this.categoryService = categoryService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingDtos.ListingSummary> search(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String condition,
            String location,
            String sort,
            int page,
            int size
    ) {
        ListingCondition cond = EnumParsers.optional(ListingCondition.class, condition, "condition");
        Sort sortObj = switch (sort == null ? "" : sort) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "popular" -> Sort.by(Sort.Direction.DESC, "favoriteCount").and(Sort.by(Sort.Direction.DESC, "viewCount"));
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(60, Math.max(1, size)), sortObj);
        Page<Listing> result = listingRepository.search(
                ListingStatus.ACTIVE,
                categoryId,
                minPrice,
                maxPrice,
                cond,
                (keyword == null || keyword.isBlank()) ? null : keyword.trim(),
                (location == null || location.isBlank()) ? null : location.trim(),
                pageable
        );
        return PageResponse.from(result.map(ListingDtos.ListingSummary::from));
    }

    @Transactional
    public ListingDtos.ListingDetail get(Long id) {
        Listing listing = requireById(id);
        listing.setViewCount(listing.getViewCount() + 1);
        User current = currentUserService.getCurrentUserOrNull();
        boolean favorited = current != null && favoriteRepository.existsByUserAndListing(current, listing);
        return ListingDtos.ListingDetail.from(listing, favorited);
    }

    @Transactional
    public ListingDtos.ListingDetail create(ListingDtos.ListingCreateRequest request) {
        User seller = currentUserService.requireCurrentUser();
        Category category = categoryService.requireById(request.categoryId());

        Listing listing = new Listing();
        listing.setSeller(seller);
        listing.setCategory(category);
        listing.setTitle(request.title().trim());
        listing.setDescription(request.description().trim());
        listing.setPrice(request.price());
        listing.setOriginalPrice(request.originalPrice());
        listing.setCondition(EnumParsers.required(ListingCondition.class, request.condition(), "condition"));
        listing.setLocation(request.location());
        listing.setNegotiable(request.negotiable());
        listing.setShippingFee(request.shippingFee() == null ? BigDecimal.ZERO : request.shippingFee());
        listing.setImageUrls(new ArrayList<>(request.imageUrls()));
        listing.setStatus(ListingStatus.ACTIVE);
        listing.setCreatedAt(Instant.now());
        listing.setUpdatedAt(Instant.now());
        Listing saved = listingRepository.save(listing);
        return ListingDtos.ListingDetail.from(saved, false);
    }

    @Transactional
    public ListingDtos.ListingDetail update(Long id, ListingDtos.ListingUpdateRequest request) {
        User current = currentUserService.requireCurrentUser();
        Listing listing = requireById(id);
        if (!listing.getSeller().getId().equals(current.getId())) {
            throw new ForbiddenOperationException("You can only edit your own listings.");
        }
        if (listing.getStatus() == ListingStatus.SOLD) {
            throw new BusinessRuleException("Sold listings cannot be edited.");
        }
        if (listing.getStatus() == ListingStatus.RESERVED) {
            // An order is in flight; editing (esp. re-activating) could enable a double-sale.
            throw new BusinessRuleException("This listing has an order in progress and cannot be edited.");
        }
        if (request.title() != null) listing.setTitle(request.title().trim());
        if (request.description() != null) listing.setDescription(request.description().trim());
        if (request.price() != null) listing.setPrice(request.price());
        if (request.originalPrice() != null) listing.setOriginalPrice(request.originalPrice());
        if (request.condition() != null) listing.setCondition(EnumParsers.required(ListingCondition.class, request.condition(), "condition"));
        if (request.categoryId() != null) listing.setCategory(categoryService.requireById(request.categoryId()));
        if (request.location() != null) listing.setLocation(request.location());
        if (request.negotiable() != null) listing.setNegotiable(request.negotiable());
        if (request.shippingFee() != null) listing.setShippingFee(request.shippingFee());
        if (request.imageUrls() != null && !request.imageUrls().isEmpty()) {
            listing.getImageUrls().clear();
            listing.getImageUrls().addAll(request.imageUrls());
        }
        if (request.status() != null) {
            ListingStatus next = EnumParsers.required(ListingStatus.class, request.status(), "status");
            // Sellers may only toggle between ACTIVE and REMOVED. SOLD is set by
            // order completion; RESERVED by an accepted offer / pending order.
            // Anything else (e.g. REMOVED->ACTIVE to re-list a sold item, or
            // forcing RESERVED) is rejected.
            if (next != ListingStatus.ACTIVE && next != ListingStatus.REMOVED) {
                throw new BusinessRuleException("A listing can only be set to active or removed.");
            }
            listing.setStatus(next);
        }
        listing.setUpdatedAt(Instant.now());
        User current2 = currentUserService.getCurrentUserOrNull();
        boolean favorited = current2 != null && favoriteRepository.existsByUserAndListing(current2, listing);
        return ListingDtos.ListingDetail.from(listing, favorited);
    }

    @Transactional
    public void remove(Long id) {
        User current = currentUserService.requireCurrentUser();
        Listing listing = requireById(id);
        if (!listing.getSeller().getId().equals(current.getId())) {
            throw new ForbiddenOperationException("You can only remove your own listings.");
        }
        if (listing.getStatus() == ListingStatus.SOLD) {
            throw new BusinessRuleException("Sold listings cannot be removed.");
        }
        if (listing.getStatus() == ListingStatus.RESERVED) {
            throw new BusinessRuleException("This listing has an order in progress and cannot be removed.");
        }
        listing.setStatus(ListingStatus.REMOVED);
        listing.setUpdatedAt(Instant.now());
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingDtos.ListingSummary> mySellerListings(int page, int size) {
        User seller = currentUserService.requireCurrentUser();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(60, Math.max(1, size)));
        Page<Listing> result = listingRepository.findBySellerOrderByCreatedAtDesc(seller, pageable);
        return PageResponse.from(result.map(ListingDtos.ListingSummary::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingDtos.ListingSummary> publicSellerListings(User seller, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(60, Math.max(1, size)));
        Page<Listing> result = listingRepository.findBySellerAndStatusOrderByCreatedAtDesc(seller, ListingStatus.ACTIVE, pageable);
        return PageResponse.from(result.map(ListingDtos.ListingSummary::from));
    }

    @Transactional
    public void toggleFavorite(Long id) {
        User current = currentUserService.requireCurrentUser();
        Listing listing = requireById(id);
        var existing = favoriteRepository.findByUserAndListing(current, listing);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            listing.setFavoriteCount(Math.max(0, listing.getFavoriteCount() - 1));
        } else {
            favoriteRepository.save(new Favorite(current, listing));
            listing.setFavoriteCount(listing.getFavoriteCount() + 1);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingDtos.ListingSummary> myFavorites(int page, int size) {
        User current = currentUserService.requireCurrentUser();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(60, Math.max(1, size)));
        return PageResponse.from(
                favoriteRepository.findByUserOrderByCreatedAtDesc(current, pageable)
                        .map(f -> ListingDtos.ListingSummary.from(f.getListing()))
        );
    }

    @Transactional(readOnly = true)
    public Listing requireById(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found."));
    }

    @Transactional
    public void markSold(Listing listing) {
        listing.setStatus(ListingStatus.SOLD);
        listing.setSoldAt(Instant.now());
        listing.setUpdatedAt(Instant.now());
    }

    /**
     * Reserve an ACTIVE listing (offer accepted or buy-now checkout). Refuses
     * to reserve anything not ACTIVE, so a leftover offer can't resurrect a
     * SOLD/REMOVED item and two concurrent reservations can't both win (the
     * loser also trips @Version → 409).
     */
    @Transactional
    public void markReserved(Listing listing) {
        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new ConflictException("This item is no longer available.");
        }
        listing.setStatus(ListingStatus.RESERVED);
        listing.setUpdatedAt(Instant.now());
    }

    /**
     * Called when a buyer checks out against an already-accepted offer (the
     * listing is RESERVED). Bumps the row so two concurrent checkouts of the
     * same offer collide on @Version and one gets 409.
     */
    @Transactional
    public void touchForCheckout(Listing listing) {
        if (listing.getStatus() != ListingStatus.RESERVED) {
            throw new ConflictException("This item is no longer available.");
        }
        listing.setUpdatedAt(Instant.now());
    }

    @Transactional
    public void releaseReservation(Listing listing) {
        if (listing.getStatus() == ListingStatus.RESERVED) {
            listing.setStatus(ListingStatus.ACTIVE);
            listing.setUpdatedAt(Instant.now());
        }
    }
}
