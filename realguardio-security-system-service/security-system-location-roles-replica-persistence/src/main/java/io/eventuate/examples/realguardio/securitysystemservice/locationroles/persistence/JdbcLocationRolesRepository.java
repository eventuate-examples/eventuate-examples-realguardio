package io.eventuate.examples.realguardio.securitysystemservice.locationroles.persistence;

import io.eventuate.examples.realguardio.securitysystemservice.locationroles.domain.LocationRole;
import io.eventuate.examples.realguardio.securitysystemservice.locationroles.domain.LocationRolesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public class JdbcLocationRolesRepository implements LocationRolesRepository {

    private static final Logger logger = LoggerFactory.getLogger(JdbcLocationRolesRepository.class);

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<LocationRole> locationRoleRowMapper = (rs, rowNum) ->
        new LocationRole(
            rs.getLong("id"),
            rs.getString("user_name"),
            rs.getLong("location_id"),
            rs.getString("role_name")
        );

    public JdbcLocationRolesRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void saveLocationRole(String userName, Long locationId, String roleName) {
        String sql = "INSERT INTO customer_employee_location_role (user_name, location_id, role_name) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, userName, locationId, roleName);
    }

    @Override
    public void saveTeamMember(String teamId, String customerEmployeeId) {
        String sql = "INSERT INTO team_members (team_id, customer_employee_id) " +
                     "VALUES (?, ?) " +
                     "ON CONFLICT (team_id, customer_employee_id) DO NOTHING";
        jdbcTemplate.update(sql, teamId, customerEmployeeId);
        logger.info("Saved team member: teamId={}, employeeId={}", teamId, customerEmployeeId);
    }

    @Override
    public void saveTeamLocationRole(String teamId, String roleName, Long locationId) {
        String sql = "INSERT INTO team_location_roles (team_id, role_name, location_id) " +
                     "VALUES (?, ?, ?) " +
                     "ON CONFLICT (team_id, role_name, location_id) DO NOTHING";
        jdbcTemplate.update(sql, teamId, roleName, locationId);
        logger.info("Saved team location role: teamId={}, role={}, locationId={}",
                   teamId, roleName, locationId);
    }

    @Override
    public void saveLocation(Long locationId, String customerId) {
        String sql = "INSERT INTO locations (id, customer_id) " +
                     "VALUES (?, ?) " +
                     "ON CONFLICT (id) DO NOTHING";
        jdbcTemplate.update(sql, locationId, customerId);
        logger.info("Saved location: id={}, customerId={}", locationId, customerId);
    }

    @Override
    public List<LocationRole> findLocationRoles(String userName, Long locationId) {
        String sql = "SELECT id, user_name, location_id, role_name FROM customer_employee_location_role WHERE user_name = ? AND location_id = ?";
        return jdbcTemplate.query(sql, locationRoleRowMapper, userName, locationId);
    }
}
