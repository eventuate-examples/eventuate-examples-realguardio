package io.eventuate.examples.realguardio.securitysystemservice.osointegration;

import io.eventuate.examples.realguardio.securitysystemservice.domain.ForbiddenException;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemActionAuthorizer;
import io.eventuate.examples.realguardio.securitysystemservice.domain.UserNameSupplier;
import io.realguardio.osointegration.ososervice.RealGuardOsoAuthorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

public class OsoLocalSecuritySystemActionAuthorizer implements SecuritySystemActionAuthorizer {

    private static final Logger logger = LoggerFactory.getLogger(OsoLocalSecuritySystemActionAuthorizer.class);

    private final UserNameSupplier userNameSupplier;
    private final RealGuardOsoAuthorizer realGuardOsoAuthorizer;
    private final JdbcTemplate jdbcTemplate;

    public OsoLocalSecuritySystemActionAuthorizer(UserNameSupplier userNameSupplier,
                                                   RealGuardOsoAuthorizer realGuardOsoAuthorizer,
                                                   JdbcTemplate jdbcTemplate) {
        this.userNameSupplier = userNameSupplier;
        this.realGuardOsoAuthorizer = realGuardOsoAuthorizer;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void verifyCanDo(long securitySystemId, String permission) {
        String userId = userNameSupplier.getCurrentUserName();
        String sqlQuery = realGuardOsoAuthorizer.authorizeLocal(userId, permission, "SecuritySystem", String.valueOf(securitySystemId));

        logger.debug("Executing local authorization query: {}", sqlQuery);

        Boolean allowed = jdbcTemplate.queryForObject(sqlQuery, Boolean.class);

        if (!Boolean.TRUE.equals(allowed)) {
            logger.warn("User {} lacks {} permission for securitySystemId {}", userId, permission, securitySystemId);
            throw new ForbiddenException(
                    String.format("User %s is not authorized to %s security system %d",
                            userId, permission, securitySystemId)
            );
        }
    }
}
