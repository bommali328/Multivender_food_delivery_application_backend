package com.foodie.service;

import com.foodie.model.MenuItem;
import com.foodie.repository.MenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenuService {

    @Autowired
    private MenuRepository menuRepository;

    // అన్ని మెనూ ఐటమ్స్ తెచ్చేందుకు
    public List<MenuItem> getAllMenuItems() {
        return menuRepository.findAll();
    }

    // ⭐ నిర్దిష్టమైన షాప్ ఐడీ ఆధారంగా మెనూ ఐటమ్స్ తెచ్చేందుకు (ఇది కొత్తగా యాడ్ చేయండి)
    public List<MenuItem> getMenuItemsByShopId(Long shopId) {
        return menuRepository.findByShopId(shopId);
    }

    // కొత్త ఐటమ్ సేవ్ చేసేందుకు
    public MenuItem addMenuItem(MenuItem menuItem) {
        return menuRepository.save(menuItem);
    }
}