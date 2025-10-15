package io.realguardio.osointegration.ososervice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RealGuardOsoFactManager {
  private static final Logger logger = LoggerFactory.getLogger(RealGuardOsoFactManager.class);

  private final OsoService osoService;

  public RealGuardOsoFactManager(OsoService osoService) {
    this.osoService = osoService;
  }

  // LocationCreatedForCustomer

  public void createLocationForCustomer(String location, String customer) {
    logger.info("Creating location {} for customer {}", location, customer);
    osoService.createRelation("Location", location, "customer", "Customer", customer);
  }

  // SecuritySystemAssignedToLocation

  public void assignSecuritySystemToLocation(String securitySystem, String location) {
    logger.info("Assigning security system {} to location {}", securitySystem, location);
    osoService.createRelation("SecuritySystem", securitySystem, "location", "Location", location);
  }


  // CustomerEmployeeAssignedCustomerRole

  public void createRoleInCustomer(String user, String company, String role) {
    logger.info("Creating role {} for user {} in customer {}", role, user, company);
    osoService.createRole("CustomerEmployee", user, role, "Customer", company);
  }

  // CustomerEmployeeAssignedLocationRole

  public void createRoleAtLocation(String user, String location, String role) {
    logger.info("Creating role {} for user {} at location {}", role, user, location);
    osoService.createRole("CustomerEmployee", user, role, "Location", location);
  }


  public void createTeamRoleAtLocation(String team, String location, String role) {
    logger.info("Creating role {} for team {} at location {}", role, team, location);
    osoService.createRole("Team", team, role, "Location", location);
  }

  public void addToTeam(String user, String team) {
    logger.info("Adding user {} to team {}", user, team);
    osoService.createRelation("Team", team, "members", "CustomerEmployee", user);
  }

}
