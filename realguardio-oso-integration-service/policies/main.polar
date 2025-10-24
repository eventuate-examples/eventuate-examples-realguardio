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

test "Authz Check" {
  setup {
    has_role(CustomerEmployee{"alice"}, "SECURITY_SYSTEM_DISARMER", Customer{"acme"});
    has_relation(Location{"loc1"}, "customer", Customer{"acme"});
    has_relation(SecuritySystem{"ss1"}, "location", Location{"loc1"});
    has_role(CustomerEmployee{"bob"}, "SECURITY_SYSTEM_DISARMER", Customer{"foo"});
    has_relation(Location{"loc2"}, "customer", Customer{"foo"});
    has_relation(SecuritySystem{"ss2"}, "location", Location{"loc2"});
    has_role(CustomerEmployee{"mary"}, "SECURITY_SYSTEM_DISARMER", Location{"loc3"});
    has_relation(SecuritySystem{"ss3"}, "location", Location{"loc3"});
    has_relation(Team{"ops-t1"}, "members", CustomerEmployee{"charlie"});
    has_role(Team{"ops-t1"}, "SECURITY_SYSTEM_DISARMER", Location{"loc1"});
  }
  assert has_permission(CustomerEmployee{"alice"}, "disarm", SecuritySystem{"ss1"});
  assert_not has_permission(CustomerEmployee{"alice"}, "disarm", SecuritySystem{"ss2"});
  assert has_permission(CustomerEmployee{"bob"}, "disarm", SecuritySystem{"ss2"});
  assert_not has_permission(CustomerEmployee{"bob"}, "disarm", SecuritySystem{"ss1"});
  assert has_permission(CustomerEmployee{"mary"}, "disarm", SecuritySystem{"ss3"});
  assert has_permission(CustomerEmployee{"charlie"}, "disarm", SecuritySystem{"ss1"});
  assert_not has_permission(CustomerEmployee{"charlie"}, "disarm", SecuritySystem{"ss2"});
  assert_not has_permission(CustomerEmployee{"charlie"}, "disarm", SecuritySystem{"ss3"});
}