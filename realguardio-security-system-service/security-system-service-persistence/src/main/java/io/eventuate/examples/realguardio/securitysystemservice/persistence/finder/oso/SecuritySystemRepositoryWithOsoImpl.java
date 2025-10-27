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
        String filterSql = realGuardOsoAuthorizer.listLocal(userName, "view", "ss.id");

        String query = """
                SELECT
                  ss.id                                   AS id,
                  ss.location_name                        AS locationName,
                  ss.state                                AS state,
                  ss.location_id                          AS locationId,
                  ss.rejection_reason                     AS rejectionReason,
                  ss.version                              AS version,
                  (SELECT COALESCE(array_agg(DISTINCT role_name), ARRAY[]::VARCHAR[])
                   FROM (
                     -- Direct location roles
                     SELECT role_name
                     FROM customer_employee_location_role
                     WHERE location_id = ss.location_id
                       AND user_name = ?
                     UNION
                     -- Team-based location roles
                     SELECT tlr.role_name
                     FROM team_members tm
                     JOIN team_location_roles tlr ON tm.team_id = tlr.team_id
                     WHERE tlr.location_id = ss.location_id
                       AND tm.customer_employee_id = ?
                     UNION
                     -- Customer-level roles
                     SELECT cecr.role_name
                     FROM customer_employee_customer_roles cecr
                     JOIN locations loc ON cecr.customer_id = loc.customer_id
                     WHERE loc.id = ss.location_id
                       AND cecr.customer_employee_id = ?
                   ) all_roles)                           AS roles
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
                (String[]) rs.getArray("roles").getArray()
            ),
            userName, userName, userName);
    }
}
