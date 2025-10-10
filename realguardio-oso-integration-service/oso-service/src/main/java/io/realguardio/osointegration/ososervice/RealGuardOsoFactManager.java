package io.realguardio.osointegration.ososervice;

import org.springframework.stereotype.Service;

@Service
public class RealGuardOsoFactManager {
  private final OsoService osoService;

  public RealGuardOsoFactManager(OsoService osoService) {
    this.osoService = osoService;
  }

  // LocationCreatedForCustomer

  public void createLocationForCustomer(String location, String customer) {
    osoService.createRelation("Location", location, "customer", "Customer", customer);
  }

  // SecuritySystemAssignedToLocation

  public void assignSecuritySystemToLocation(String securitySystem, String location) {
    osoService.createRelation("SecuritySystem", securitySystem, "location", "Location", location);
  }


  // CustomerEmployeeAssignedCustomerRole

  public void createRoleInCustomer(String user, String company, String role) {
    osoService.createRole("CustomerEmployee", user, role, "Customer", company);
  }

  // CustomerEmployeeAssignedLocationRole

  public void createRoleAtLocation(String user, String location, String role) {
    osoService.createRole("CustomerEmployee", user, role, "Location", location);
  }


  public void createTeamRoleAtLocation(String team, String location, String role) {
    osoService.createRole("Team", team, role, "Location", location);
  }

  public void addToTeam(String user, String team) {
    osoService.createRelation("Team", team, "members", "CustomerEmployee", user);
  }

}
