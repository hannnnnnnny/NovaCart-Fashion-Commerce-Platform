package com.novacart.store.service;

import com.novacart.store.dto.ReviewDtos;
import com.novacart.store.entity.OrderStatus;
import com.novacart.store.entity.Review;
import com.novacart.store.entity.ReviewRole;
import com.novacart.store.entity.TradeOrder;
import com.novacart.store.entity.User;
import com.novacart.store.exception.BusinessRuleException;
import com.novacart.store.exception.ForbiddenOperationException;
import com.novacart.store.exception.ResourceNotFoundException;
import com.novacart.store.repository.ReviewRepository;
import com.novacart.store.repository.UserRepository;
import com.novacart.store.security.CurrentUserService;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    public ReviewService(
            ReviewRepository reviewRepository,
            UserRepository userRepository,
            OrderService orderService,
            CurrentUserService currentUserService
    ) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public ReviewDtos.ReviewResponse create(ReviewDtos.CreateReviewRequest request) {
        User reviewer = currentUserService.requireCurrentUser();
        TradeOrder order = orderService.requireById(request.orderId());
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessRuleException("You can only review completed orders.");
        }
        ReviewRole role;
        User reviewee;
        if (order.getBuyer().getId().equals(reviewer.getId())) {
            role = ReviewRole.BUYER_REVIEWS_SELLER;
            reviewee = order.getSeller();
        } else if (order.getSeller().getId().equals(reviewer.getId())) {
            role = ReviewRole.SELLER_REVIEWS_BUYER;
            reviewee = order.getBuyer();
        } else {
            throw new ForbiddenOperationException("You cannot review this order.");
        }
        if (reviewRepository.existsByOrderAndRole(order, role)) {
            throw new BusinessRuleException("You have already reviewed this order.");
        }
        Review review = new Review();
        review.setOrder(order);
        review.setReviewer(reviewer);
        review.setReviewee(reviewee);
        review.setRating(request.rating());
        review.setComment(request.comment());
        review.setRole(role);
        review.setCreatedAt(Instant.now());
        reviewRepository.save(review);

        // Atomic increment (avoids a lost update if the counterparty reviews at
        // the same time). The in-memory reviewee aggregate in this immediate
        // response is one behind; the profile page refetches.
        userRepository.addRating(reviewee.getId(), request.rating());

        return ReviewDtos.ReviewResponse.from(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewDtos.ReviewResponse> listForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return reviewRepository.findByRevieweeOrderByCreatedAtDesc(user, PageRequest.of(0, 100)).stream()
                .map(ReviewDtos.ReviewResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewDtos.ReviewResponse> listForOrder(Long orderId) {
        User current = currentUserService.requireCurrentUser();
        TradeOrder order = orderService.requireById(orderId);
        // Only the order's participants can read its reviews (prevents order-id
        // enumeration of review contents).
        if (!order.getBuyer().getId().equals(current.getId())
                && !order.getSeller().getId().equals(current.getId())) {
            throw new ForbiddenOperationException("You cannot view this order's reviews.");
        }
        return reviewRepository.findByOrder(order).stream()
                .map(ReviewDtos.ReviewResponse::from)
                .toList();
    }
}
