package com.tomzxy.busozy.repository;

import com.tomzxy.busozy.entity.Review;
import com.tomzxy.busozy.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    interface ReviewRatingSummaryProjection {
        Double getAverageRating();

        Long getTotalReviews();
    }

    interface ReviewRatingCountProjection {
        Integer getRating();

        Long getTotal();
    }

    boolean existsByBookingId(Long bookingId);

    @Query(value = """
            SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END
            FROM reviews
            WHERE booking_id = :bookingId
            """, nativeQuery = true)
    boolean existsAnyByBookingId(@Param("bookingId") Long bookingId);

    Optional<Review> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"user", "trip", "trip.route"})
    Page<Review> findByTripIdOrderByCreatedAtDesc(Long tripId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "trip", "trip.route"})
    Page<Review> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "trip", "trip.route"})
    @Query("SELECT r FROM Review r ORDER BY r.createdAt DESC")
    Page<Review> findAllForAdmin(Pageable pageable);

    @Query("""
            SELECT AVG(r.rating) AS averageRating, COUNT(r) AS totalReviews
            FROM Review r
            WHERE r.trip.id = :tripId
            """)
    ReviewRatingSummaryProjection getRatingSummaryByTripId(@Param("tripId") Long tripId);

    @Query("""
            SELECT r.rating AS rating, COUNT(r) AS total
            FROM Review r
            WHERE r.trip.id = :tripId
            GROUP BY r.rating
            """)
    List<ReviewRatingCountProjection> countRatingsByTripId(@Param("tripId") Long tripId);

    @Modifying
    @Query("""
            UPDATE Review r
            SET r.isVerified = true
            WHERE r.isVerified = false
              AND r.createdAt <= :threshold
            """)
    int autoVerifyOlderThan(@Param("threshold") OffsetDateTime threshold);
}
