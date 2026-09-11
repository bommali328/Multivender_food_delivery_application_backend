package com.foodie.repository;

import com.foodie.model.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {
    List<PromoCode> findByIsActive(boolean isActive);
}