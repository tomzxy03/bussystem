package com.tomzxy.busozy.repository;

import com.tomzxy.busozy.entity.TripSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripSegmentRepository extends JpaRepository<TripSegment, Long> {

    List<TripSegment> findByTripIdOrderByPickupOrderAscDropoffOrderAsc(Long tripId);
}
