package com.novacart.store.repository;

import com.novacart.store.entity.Conversation;
import com.novacart.store.entity.Listing;
import com.novacart.store.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByListingAndBuyer(Listing listing, User buyer);

    // Pageable bounds the result — the caller passes a fixed cap so this can't
    // grow unbounded as a heavy user accumulates threads.
    @Query("""
            SELECT c FROM Conversation c
            WHERE c.buyer = :user OR c.seller = :user
            ORDER BY c.lastMessageAt DESC
            """)
    List<Conversation> findAllForUser(@Param("user") User user, Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(CASE WHEN c.buyer = :user THEN c.buyerUnreadCount ELSE c.sellerUnreadCount END), 0)
            FROM Conversation c
            WHERE c.buyer = :user OR c.seller = :user
            """)
    long totalUnreadForUser(@Param("user") User user);
}
