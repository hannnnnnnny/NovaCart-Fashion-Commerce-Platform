package com.novacart.store.repository;

import com.novacart.store.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /**
     * Atomically fold a new rating into the reviewee's aggregate, so two
     * concurrent reviews of the same user can't lose one another (read-
     * modify-write would).
     */
    @Modifying
    @Query("""
            UPDATE User u SET u.ratingSum = u.ratingSum + :rating,
                              u.ratingCount = u.ratingCount + 1
            WHERE u.id = :userId
            """)
    void addRating(@Param("userId") Long userId, @Param("rating") int rating);
}
