package com.acme.customer;

import java.util.Optional;

public class CustomerLookupService {

    private final CustomerRepository customerRepository;

    public CustomerLookupService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Optional<Customer> findPreferredCustomer(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return Optional.empty();
        }
        return customerRepository.findById(customerId);
    }
}
