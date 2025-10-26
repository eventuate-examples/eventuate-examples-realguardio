package io.eventuate.examples.realguardio.securitysystemservice.persistence.finder.oso;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemProjection;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemRepositoryWithOso;
import io.realguardio.osointegration.ososervice.RealGuardOsoAuthorizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class SecuritySystemRepositoryWithOsoImpl implements SecuritySystemRepositoryWithOso {

    @Autowired
    private RealGuardOsoAuthorizer realGuardOsoAuthorizer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<SecuritySystemProjection> findAllAccessible(String userName) {
        String filterSql = realGuardOsoAuthorizer.listLocal(userName, "arm", "ss.id");

        String query = """
                SELECT
                  ss.id                                   AS id,
                  ss.location_name                        AS locationName,
                  ss.state                                AS state,
                  ss.location_id                          AS locationId,
                  ss.rejection_reason                     AS rejectionReason,
                  ss.version                              AS version
                FROM security_system ss
                WHERE
                """ + filterSql;

        return jdbcTemplate.query(query,
            (rs, rowNum) -> new SecuritySystemProjectionImpl(
                rs.getLong("id"),
                rs.getString("locationName"),
                rs.getString("state"),
                rs.getLong("locationId"),
                rs.getString("rejectionReason"),
                rs.getLong("version"),
                new String[0] // TODO empty
            ));
    }
}
