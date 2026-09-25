package com.foodie.repository;

import com.foodie.model.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional; // ✅ ఈ ఇంపోర్ట్ యాడ్ చేయాలి

public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {
    List<PromoCode> findByIsActive(boolean isActive);
    Optional<PromoCode> findByCode(String code);
}