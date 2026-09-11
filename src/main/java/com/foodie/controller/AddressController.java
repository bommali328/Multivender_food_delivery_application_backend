package com.foodie.controller;

import com.foodie.model.Address;
import com.foodie.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/address")
@CrossOrigin(origins = "*")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @PostMapping("/save")
    public ResponseEntity<Address> saveAddress(@RequestBody Address address) {
        Address saved = addressService.saveAddress(address);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/customer/{mobile}")
    public ResponseEntity<List<Address>> getCustomerAddresses(@PathVariable String mobile) {
        List<Address> list = addressService.getAddressesByMobile(mobile);
        return ResponseEntity.ok(list);
    }
}