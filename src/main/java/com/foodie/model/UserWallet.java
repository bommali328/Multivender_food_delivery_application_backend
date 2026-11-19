package com.foodie.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_wallet_transactions")
public class UserWallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String mobile; // మొబైల్ నంబర్ (Primary Link)
    
    private double walletBalance = 0.0;
    
    @ElementCollection
    @CollectionTable(name = "wallet_transaction_history", joinColumns = @JoinColumn(name = "wallet_id"))
    @Column(name = "transaction_detail", length = 500)
    private List<String> transactions = new ArrayList<>(); // Transaction IDs & History Timestamps

    // Default Constructor
    public UserWallet() {}

    // Parameterized Constructor
    public UserWallet(String mobile, double walletBalance) {
        this.mobile = mobile;
        this.walletBalance = walletBalance;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public double getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(double walletBalance) {
        this.walletBalance = walletBalance;
    }

    public List<String> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<String> transactions) {
        this.transactions = transactions;
    }
}