package com.foodie.repository;

import com.foodie.model.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<UserWallet, Long> {
    Optional<UserWallet> findByMobile(String mobile);
}