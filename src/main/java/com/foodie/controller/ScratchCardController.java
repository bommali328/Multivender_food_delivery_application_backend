package com.foodie.controller;

import com.foodie.model.ScratchCard;
import com.foodie.repository.ScratchCardRepository;
import com.foodie.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/scratch-card")
@CrossOrigin(origins = "*")
public class ScratchCardController {

    @Autowired
    private ScratchCardRepository scratchCardRepository;
    
    @Autowired
    private UserRepository userRepository;

    // 1. కస్టమర్ యొక్క అన్ని స్క్రాచ్ కార్డ్స్ ఫెచ్ చేయడానికి
 // 1. కస్టమర్ యొక్క అన్ని స్క్రాచ్ కార్డ్స్ ఫెచ్ చేయడానికి
    @GetMapping("/user/{mobile}")
    public ResponseEntity<List<ScratchCard>> getUserScratchCards(@PathVariable String mobile) {
        List<ScratchCard> cards = scratchCardRepository.findByCustomerMobile(mobile);
        return ResponseEntity.ok(cards);
    }

    // 2. ఆర్డర్ కంప్లీట్ అయినప్పుడు కొత్త స్క్రాచ్ కార్డ్ జనరేట్ చేయడానికి (Order Placement లాజిక్ లో దీన్ని కాల్ చేయవచ్చు)
    @PostMapping("/generate")
    public ResponseEntity<?> generateScratchCard(@RequestBody Map<String, String> request) {
        String mobile = request.get("customerMobile");
        String orderId = request.get("orderId");

        // ఈ ఆర్డర్‌కి ఆల్రెడీ కార్డ్ ఉంటే మళ్లీ క్రియేట్ చేయకూడదు
        if (scratchCardRepository.existsByOrderId(orderId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Scratch card already exists for this order"));
        }

        // 5 నుండి 50 రూపాయిల మధ్యలో ర్యాండమ్ అమౌంట్ జనరేట్ చేయడం
        Random random = new Random();
        double randomAmount = 5 + (50 - 5) * random.nextDouble();
        double roundedAmount = Math.round(randomAmount * 10.0) / 10.0;

        ScratchCard card = new ScratchCard();
        card.setCustomerMobile(mobile);
        card.setOrderId(orderId);
        card.setAmount(roundedAmount);
        card.setClaimed(false);
        
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("d/M/yyyy, h:mm:ss a");
        card.setUnlockedAt(dtf.format(LocalDateTime.now()));

        ScratchCard savedCard = scratchCardRepository.save(card);
        return ResponseEntity.ok(savedCard);
    }
 

    // 3. స్క్రాచ్ కార్డ్ క్లెయిమ్ చేసి అమౌంట్ వాలెట్‌కి యాడ్ చేయడానికి
    @PutMapping("/claim/{id}")
    public ResponseEntity<?> claimScratchCard(@PathVariable Long id) {
        ScratchCard card = scratchCardRepository.findById(id).orElse(null);
        if (card == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Card not found"));
        }

        if (card.isClaimed()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Card already claimed"));
        }

        // 1. కార్డ్‌ని క్లెయిమ్ చేసినట్లు మార్చడం
        card.setClaimed(true);
        scratchCardRepository.save(card);

        // 2. కస్టమర్ వాలెట్ బ్యాలెన్స్‌కి అమౌంట్ యాడ్ చేయడం
        com.foodie.model.User customer = userRepository.findFirstByMobile(card.getCustomerMobile());
        if (customer != null) {
            double currentWallet = customer.getWalletBalance() != null ? customer.getWalletBalance() : 0.0;
            double newBalance = currentWallet + card.getAmount();
            
            customer.setWalletBalance(newBalance);
            userRepository.save(customer); // వాలెట్ అప్‌డేట్ సేవ్ అవుతుంది

            return ResponseEntity.ok(Map.of(
                "status", "success", 
                "amount", card.getAmount(),
                "newWalletBalance", newBalance
            ));
        }

        return ResponseEntity.badRequest().body(Map.of("error", "Customer not found for this scratch card"));
    }
}