package com.foodie.service;

import com.foodie.model.Shop;
import com.foodie.repository.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ShopService {

    @Autowired
    private ShopRepository shopRepository;

    public Shop registerShop(Shop shop) {
        return shopRepository.save(shop);
    }

    public Shop updateBankDetails(Long id, Shop bankDetails) {
        Optional<Shop> optionalShop = shopRepository.findById(id);
        if (optionalShop.isPresent()) {
            Shop shop = optionalShop.get();
            shop.setUpiId(bankDetails.getUpiId());
            shop.setAccountNumber(bankDetails.getAccountNumber());
            shop.setIfscCode(bankDetails.getIfscCode());
            shop.setBankName(bankDetails.getBankName());
            return shopRepository.save(shop);
        }
        return null;
    }

    public Optional<Shop> getShopById(Long id) {
        return shopRepository.findById(id);
    }
}