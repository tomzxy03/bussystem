package com.tomzxy.busozy.repository;

import com.tomzxy.busozy.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findByIsActiveTrueOrderByIdAsc();

    Optional<PaymentMethod> findByCodeAndIsActiveTrue(String code);
}
