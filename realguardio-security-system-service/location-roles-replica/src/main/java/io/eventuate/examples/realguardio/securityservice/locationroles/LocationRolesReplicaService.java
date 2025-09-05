package io.eventuate.examples.realguardio.securityservice.locationroles;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class LocationRolesReplicaService {
    
    private final JdbcTemplate jdbcTemplate;
    
    @Autowired
    public LocationRolesReplicaService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public void saveLocationRole(String userName, Long locationId, String roleName) {
        String sql = "INSERT INTO customer_employee_location_role (user_name, location_id, role_name) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, userName, locationId, roleName);
    }
    
    public List<Map<String, Object>> findLocationRoles(String userName, Long locationId) {
        String sql = "SELECT * FROM customer_employee_location_role WHERE user_name = ? AND location_id = ?";
        return jdbcTemplate.queryForList(sql, userName, locationId);
    }
}