package com.foodie.controller;

import com.foodie.model.UserWallet;
import com.foodie.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
@CrossOrigin(origins = "*") // అన్ని సోర్సెస్ నుండి రిక్వెస్ట్స్ అలవ్ చేయడానికి
public class WalletController {

    @Autowired
    private WalletService walletService;

    // 1. మొబైల్ నంబర్ ద్వారా వాలెట్ వివరాలు పొందడానికి GET API
    @GetMapping("/{mobile}")
    public ResponseEntity<UserWallet> getWallet(@PathVariable String mobile) {
        UserWallet wallet = walletService.getWalletByMobile(mobile);
        return ResponseEntity.ok(wallet);
    }

    // 2. మనీ యాడ్ చేయడానికి లేదా డిడెక్ట్ చేయడానికి POST API
    @PostMapping("/update")
    public ResponseEntity<UserWallet> updateWallet(@RequestBody WalletRequest request) {
        UserWallet updatedWallet = walletService.updateWallet(
            request.getMobile(), 
            request.getAmount(), 
            request.getTransactionDetails()
        );
        return ResponseEntity.ok(updatedWallet);
    }
}

// DTO Class for Request Body
class WalletRequest {
    private String mobile;
    private double amount;
    private String transactionDetails;

    // Getters and Setters
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    
    public String getTransactionDetails() { return transactionDetails; }
    public void setTransactionDetails(String transactionDetails) { this.transactionDetails = transactionDetails; }
}