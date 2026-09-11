package com.foodie.service;

import com.foodie.model.Address;
import com.foodie.repository.AddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {

    @Autowired
    private AddressRepository addressRepository;

    public Address saveAddress(Address address) {
        return addressRepository.save(address);
    }

    public List<Address> getAddressesByMobile(String mobile) {
        return addressRepository.findByCustomerMobile(mobile);
    }
}