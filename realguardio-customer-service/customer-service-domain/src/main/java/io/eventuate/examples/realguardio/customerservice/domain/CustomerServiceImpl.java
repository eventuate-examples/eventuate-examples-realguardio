package io.eventuate.examples.realguardio.customerservice.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {
    
    private final CustomerRepository customerRepository;
    
    public CustomerServiceImpl(CustomerRepository customerRepository) {
        if (customerRepository == null) {
            throw new IllegalArgumentException("customerRepository cannot be null");
        }
        this.customerRepository = customerRepository;
    }
    
    @Override
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }
}