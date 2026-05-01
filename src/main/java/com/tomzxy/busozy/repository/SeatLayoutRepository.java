package com.tomzxy.busozy.repository;

import com.tomzxy.busozy.entity.SeatLayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeatLayoutRepository extends JpaRepository<SeatLayout, Long> {
}
