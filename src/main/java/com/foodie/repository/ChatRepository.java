package com.foodie.repository;

import com.foodie.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<ChatMessage, Long> {

    // 1. ఒక నిర్దిష్ట ఆర్డర్ ఐడీ ఆధారంగా చాట్ హిస్టరీని టైమ్ ఆర్డర్‌లో పొందడానికి
    List<ChatMessage> findByOrderIdOrderByTimestampAsc(String orderId);
    
    // 2. ఒక నిర్దిష్ట కస్టమర్ మొబైల్ నంబర్ ఆధారంగా చాట్ హిస్టరీని పొందడానికి (అవసరమైతే)
    List<ChatMessage> findBySenderMobileOrderByTimestampAsc(String senderMobile);
    
}