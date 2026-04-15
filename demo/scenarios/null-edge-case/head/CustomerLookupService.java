package com.acme.customer;

public class CustomerLookupService {

    private final CustomerRepository customerRepository;

    public CustomerLookupService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer findPreferredCustomer(String customerId) {
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer.getPrimaryEmail() == null) {
            return null;
        }
        return customer;
    }
}
