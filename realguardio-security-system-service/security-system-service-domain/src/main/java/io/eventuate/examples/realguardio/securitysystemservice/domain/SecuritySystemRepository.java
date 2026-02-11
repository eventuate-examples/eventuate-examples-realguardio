package io.eventuate.examples.realguardio.securitysystemservice.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SecuritySystemRepository extends JpaRepository<SecuritySystem, Long> {

  @Query(value = """
        SELECT
          ss.id                                   AS id,
          ss.location_name                        AS locationName,
          ss.state                                AS state,
          ss.location_id                          AS locationId,
          ss.rejection_reason                     AS rejectionReason,
          ss.version                              AS version,
          ARRAY_AGG(DISTINCT celr.role_name
                    ORDER BY celr.role_name)      AS roleNames
        FROM security_system ss
        JOIN customer_employee_location_role celr
          ON celr.location_id = ss.location_id
        WHERE celr.user_name = :userName
        GROUP BY ss.id
        """, nativeQuery = true)
  List<SecuritySystemProjection> findAllAccessible(@Param("userName") String userName);
}
