package com.foodie.service;

import com.foodie.model.UserWallet;
import com.foodie.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    // మొబైల్ నంబర్ ద్వారా వాలెట్ తెచ్చుకోవడం (లేకపోతే కొత్తది క్రియేట్ చేయడం)
    public UserWallet getWalletByMobile(String mobile) {
        Optional<UserWallet> optionalWallet = walletRepository.findByMobile(mobile);
        if (optionalWallet.isPresent()) {
            return optionalWallet.get();
        } else {
            UserWallet newWallet = new UserWallet();
            newWallet.setMobile(mobile);
            newWallet.setWalletBalance(0.0);
            return walletRepository.save(newWallet);
        }
    }

    // వాలెట్ బ్యాలెన్స్ మరియు ట్రాన్సాక్షన్ అప్‌డేట్ చేయడం
    public UserWallet updateWallet(String mobile, double amount, String transactionDetail) {
        UserWallet wallet = getWalletByMobile(mobile);
        
        // బ్యాలెన్స్ అప్‌డేట్
        wallet.setWalletBalance(wallet.getWalletBalance() + amount);
        
        // ట్రాన్సాక్షన్ హిస్టరీకి యాడ్ చేయడం
        if (transactionDetail != null && !transactionDetail.isEmpty()) {
            wallet.getTransactions().add(transactionDetail);
        }
        
        return walletRepository.save(wallet);
    }
}