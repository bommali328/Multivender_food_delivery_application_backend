package com.foodie.service;

import com.foodie.model.DeliveryPartner;
import com.foodie.repository.DeliveryPartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DeliveryPartnerService {

    @Autowired
    private DeliveryPartnerRepository repository;

    public DeliveryPartner savePartner(DeliveryPartner partner) {
        if (partner.getKycStatus() == null) {
            partner.setKycStatus("Pending");
        }
        return repository.save(partner);
    }

    public List<DeliveryPartner> getAllPartners() {
        return repository.findAll();
    }

    public Optional<DeliveryPartner> getPartnerById(Long id) {
        return repository.findById(id);
    }

    public Optional<DeliveryPartner> getPartnerByMobile(String mobile) {
        return repository.findByMobile(mobile);
    }

    // --- 1. లైవ్ లొకేషన్ మరియు ఆన్‌లైన్/ఆఫ్‌లైన్ స్టేటస్ అప్‌డేట్ చేయడానికి ---
    public void updatePartnerLocationAndStatus(Long partnerId, Double lat, Double lng, boolean isOnline) {
        Optional<DeliveryPartner> optionalPartner = repository.findById(partnerId);
        if (optionalPartner.isPresent()) {
            DeliveryPartner partner = optionalPartner.get();
            partner.setLatitude(lat);
            partner.setLongitude(lng);
            partner.setOnline(isOnline);
            
            // ఒకవేళ ఆన్‌లైన్ అయితే, onlineStartTime లేకపోతే సెట్ చేయడం
            if (isOnline && partner.getOnlineStartTime() == null) {
                partner.setOnlineStartTime(LocalDateTime.now());
            }
            repository.save(partner);
        }
    }

    // --- 2. స్మార్ట్ అసైన్‌మెంట్ అల్గోరిథం (FIFO, Idle Time & Rating ఆధారంగా ఉత్తమ పార్ట్‌నర్‌ను ఎంచుకోవడం) ---
    public DeliveryPartner findBestPartnerForOrder(Double shopLat, Double shopLng) {
        // ఉదాహరణకు 5 కి.మీ రేడియస్ లో ఆన్‌లైన్‌లో ఉండి, బిజీగా లేని పార్ట్‌నర్లను తీసుకోవడం
        List<DeliveryPartner> availablePartners = repository.findAvailablePartnersNearby(shopLat, shopLng, 5.0);

        // ఒకవేళ రేడియస్ లో ఎవరు లేకపోతే, అప్పుడు సాధారణంగా ఆన్‌లైన్‌లో ఉన్న ఫ్రీ పార్ట్‌నర్లని తీసుకోవడం
        if (availablePartners.isEmpty()) {
            availablePartners = repository.findByIsOnlineTrueAndIsBusyFalse();
        }

        if (availablePartners.isEmpty()) {
            return null; // ప్రస్తుతం ఎవరూ అందుబాటులో లేరు
        }

        // స్మార్ట్ సార్టింగ్ రూల్స్ (ఎప్లిసిట్ టైప్స్ తో సరిదిద్దబడిన కోడ్):
        availablePartners.sort((DeliveryPartner p1, DeliveryPartner p2) -> {
            // నియమం 1: చివరిసారి ఆర్డర్ తీసుకున్న సమయం కంపేర్ చేయడం (FIFO / Idle Time)
            LocalDateTime t1 = p1.getLastOrderAcceptedTime();
            LocalDateTime t2 = p2.getLastOrderAcceptedTime();
            
            if (t1 != null && t2 != null) {
                int timeCompare = t1.compareTo(t2);
                if (timeCompare != 0) {
                    return timeCompare; 
                }
            } else if (t1 == null && t2 != null) {
                return -1; 
            } else if (t1 != null && t2 == null) {
                return 1;
            }

            // నియమం 2: రేటింగ్ ఆధారంగా కంపేర్ చేయడం (ఎక్కువ రేటింగ్ ఉన్నవారికి ప్రాధాన్యత)
            int ratingCompare = Double.compare(p2.getPerformanceRating(), p1.getPerformanceRating());
            if (ratingCompare != 0) {
                return ratingCompare;
            }

            // నియమం 3: ఆన్‌లైన్‌కి వచ్చి ఎంతసేపు అయింది (Online Start Time)
            LocalDateTime o1 = p1.getOnlineStartTime();
            LocalDateTime o2 = p2.getOnlineStartTime();
            
            if (o1 != null && o2 != null) {
                int onlineCompare = o1.compareTo(o2);
                if (onlineCompare != 0) {
                    return onlineCompare;
                }
            }

            return 0;
        });

        // లిస్ట్‌లో మొదటి స్థానంలో ఉన్న ఉత్తమ పార్ట్‌నర్‌ను రిటర్న్ చేయడం
        markPartnerAsAssigned(availablePartners.get(0));
        return availablePartners.get(0);
    }

    // --- 3. ఆర్డర్ అసైన్ అయిన తర్వాత పార్ట్‌నర్‌ను 'Busy' గా మార్చడానికి ---
    private void markPartnerAsAssigned(DeliveryPartner partner) {
        partner.setBusy(true);
        partner.setLastOrderAcceptedTime(LocalDateTime.now());
        repository.save(partner);
    }
}