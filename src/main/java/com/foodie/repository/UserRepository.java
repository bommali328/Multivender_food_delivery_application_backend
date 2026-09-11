package com.foodie.repository;

import com.foodie.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> { // 👈 ఇక్కడ String ఉండాలి (ఎందుకంటే mobile టైప్ String కాబట్టి)
    
    // ఒకవేళ టేబుల్‌లో ఎక్కడైనా డూప్లికేట్స్ ఉన్నా క్రాష్ అవ్వకుండా ఉండటానికి
    User findFirstByMobile(String mobile);
}