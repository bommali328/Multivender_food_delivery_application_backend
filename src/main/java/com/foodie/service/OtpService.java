package com.foodie.service;

import com.foodie.model.OtpToken;
import com.foodie.repository.OtpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class OtpService {

    @Autowired
    private OtpRepository otpRepository;

    // 6-డిజిట్ OTP ని జనరేట్ చేసి సేవ్ చేసే మెథడ్
    public String generateAndSaveOtp(String mobile) {
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(5); // 5 నిమిషాల వాలిడిటీ

        OtpToken existing = otpRepository.findByMobile(mobile).orElse(null);
        if (existing != null) {
            existing.setOtp(otp);
            existing.setExpiryTime(expiryTime);
            otpRepository.save(existing);
        } else {
            OtpToken newToken = new OtpToken(mobile, otp, expiryTime);
            otpRepository.save(newToken);
        }

        // ఇక్కడ మీరు వాట్సాప్ API (లేదా టెక్స్ట్ లాగ్) కాల్ చేసుకోవచ్చు
        System.out.println("WhatsApp OTP for " + mobile + " is: " + otp);
        return otp;
    }

    // OTP ని వెరిఫై చేసే మెథడ్
    public boolean verifyOtp(String mobile, String enteredOtp) {
        Optional<OtpToken> optionalOtp = otpRepository.findByMobile(mobile);
        if (optionalOtp.isPresent()) {
            OtpToken otpToken = optionalOtp.get();
            
            // OTP మ్యాచ్ అయ్యిందా మరియు టైమ్ ఎక్స్‌పైర్ అవ్వలేదా అని చెక్ చేయడం
            if (otpToken.getOtp().equals(enteredOtp) && LocalDateTime.now().isBefore(otpToken.getExpiryTime())) {
                otpRepository.delete(otpToken); // ఒకసారి వాడాక OTP ని డిలీట్ చేయడం
                return true;
            }
        }
        return false;
    }
}