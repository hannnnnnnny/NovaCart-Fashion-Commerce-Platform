package com.novacart.store.repository;

import com.novacart.store.entity.Review;
import com.novacart.store.entity.ReviewRole;
import com.novacart.store.entity.TradeOrder;
import com.novacart.store.entity.User;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByRevieweeOrderByCreatedAtDesc(User reviewee, Pageable pageable);

    List<Review> findByOrder(TradeOrder order);

    boolean existsByOrderAndRole(TradeOrder order, ReviewRole role);
}
