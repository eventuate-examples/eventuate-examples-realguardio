package io.eventuate.examples.realguardio.customerservice.restapi;

import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerService;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Location;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers")
public class LocationController {

    private final CustomerService customerService;

    public LocationController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/{customerId}/locations")
    @PreAuthorize("hasRole('REALGUARDIO_CUSTOMER_EMPLOYEE')")
    public CreateLocationResponse createLocation(
            @PathVariable Long customerId,
            @RequestBody CreateLocationRequest request) {
        Location location = customerService.createLocationForCustomer(customerId, request.name());
        return new CreateLocationResponse(location.getId());
    }
}
