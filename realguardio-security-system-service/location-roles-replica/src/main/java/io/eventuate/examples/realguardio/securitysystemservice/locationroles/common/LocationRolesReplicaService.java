package io.eventuate.examples.realguardio.securitysystemservice.locationroles.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public class LocationRolesReplicaService {
    
    private final JdbcTemplate jdbcTemplate;
    
    private final RowMapper<LocationRole> locationRoleRowMapper = (rs, rowNum) ->
        new LocationRole(
            rs.getLong("id"),
            rs.getString("user_name"),
            rs.getLong("location_id"),
            rs.getString("role_name")
        );
    
    @Autowired
    public LocationRolesReplicaService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public void saveLocationRole(String userName, Long locationId, String roleName) {
        String sql = "INSERT INTO customer_employee_location_role (user_name, location_id, role_name) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, userName, locationId, roleName);
    }
    
    public List<LocationRole> findLocationRoles(String userName, Long locationId) {
        String sql = "SELECT id, user_name, location_id, role_name FROM customer_employee_location_role WHERE user_name = ? AND location_id = ?";
        return jdbcTemplate.query(sql, locationRoleRowMapper, userName, locationId);
    }
}