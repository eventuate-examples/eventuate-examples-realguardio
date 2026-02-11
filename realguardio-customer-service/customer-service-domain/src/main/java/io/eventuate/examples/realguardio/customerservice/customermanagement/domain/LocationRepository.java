package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long>, LocationRepositoryCustom {
    List<Location> findByCustomerId(Long customerId);
    Optional<Location> findByCustomerIdAndName(Long customerId, String name);
}