package io.eventuate.examples.realguardio.customerservice.iamintegration;

import io.eventuate.examples.realguardio.customerservice.security.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserServiceImpl implements UserService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public void createCustomerEmployeeUser(String email) {
        logger.warn("Creating user with email: {}", email);
    }
    

}