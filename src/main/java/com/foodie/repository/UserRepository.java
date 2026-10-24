package com.foodie.repository;

import com.foodie.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    User findByMobile(String mobile);
    User findFirstByMobile(String mobile);
}