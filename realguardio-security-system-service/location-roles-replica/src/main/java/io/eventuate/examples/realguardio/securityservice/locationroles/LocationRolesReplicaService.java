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
        StringBuilder sql = new StringBuilder("SELECT * FROM customer_employee_location_role WHERE 1=1");
        
        if (userName != null) {
            sql.append(" AND user_name = ?");
        }
        if (locationId != null) {
            sql.append(" AND location_id = ?");
        }
        
        if (userName != null && locationId != null) {
            return jdbcTemplate.queryForList(sql.toString(), userName, locationId);
        } else if (userName != null) {
            return jdbcTemplate.queryForList(sql.toString(), userName);
        } else if (locationId != null) {
            return jdbcTemplate.queryForList(sql.toString(), locationId);
        } else {
            return jdbcTemplate.queryForList(sql.toString());
        }
    }
}