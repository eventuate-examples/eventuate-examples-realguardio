actor CustomerEmployee {}

resource Customer {
  roles = ["SECURITY_SYSTEM_ARMER", "SECURITY_SYSTEM_DISARMER", "COMPANY_ROLE_ADMIN"];               # role at the organization
  permissions = ["arm", "disarm", "admin"];

  "arm" if "SECURITY_SYSTEM_ARMER";             # org SECURITY_SYSTEM_ARMER role grants arm
  "disarm" if "SECURITY_SYSTEM_DISARMER";             # org SECURITY_SYSTEM_DISARMER role grants disarm
  "admin" if "COMPANY_ROLE_ADMIN";             # org SECURITY_SYSTEM_DISARMER role grants disarm
}

resource Team {
  relations = { members: CustomerEmployee };
  roles = ["member"];     # optional convenience role

  # optional: give users the 'member' role on the Team if they're in the members relation
  #"member" if "members" on resource;
}

# ── Bridge: team membership + team role-at-location → user role-at-location ──

has_role(u: CustomerEmployee, role: String, loc: Location) if
  team matches Team and
  has_relation(team, "members", u) and
  has_role(team, role, loc);


resource Location {
  relations = { customer: Customer };
  roles = ["SECURITY_SYSTEM_ARMER", "SECURITY_SYSTEM_DISARMER"];                # role at the location

  # Inherit the SECURITY_SYSTEM_ARMER/SECURITY_SYSTEM_DISARMER role from the owning Customer (org)
  "SECURITY_SYSTEM_ARMER" if "SECURITY_SYSTEM_ARMER" on "customer";
  "SECURITY_SYSTEM_DISARMER" if "SECURITY_SYSTEM_DISARMER" on "customer";
}

resource SecuritySystem {
  relations = { location: Location };
  permissions = ["arm", "disarm"];

  # (a) SECURITY_SYSTEM_ARMER/SECURITY_SYSTEM_DISARMER role at the system’s Location → can arm/disarm
  "arm" if "SECURITY_SYSTEM_ARMER" on "location";
  "disarm" if "SECURITY_SYSTEM_DISARMER" on "location";

  # (b) SECURITY_SYSTEM_ARMER/SECURITY_SYSTEM_DISARMER role at the Customer that owns that Location → can disarm
  # (via the Location role inheritance above)
}
