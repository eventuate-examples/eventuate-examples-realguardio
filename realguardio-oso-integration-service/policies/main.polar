actor CustomerEmployee {}

resource Customer {
  roles = ["SECURITY_SYSTEM_ARMER", "SECURITY_SYSTEM_DISARMER", "COMPANY_ROLE_ADMIN", "SECURITY_SYSTEM_VIEWER"];               # role at the organization
  permissions = ["createCustomerEmployee", "createLocation"];

  "createCustomerEmployee" if "COMPANY_ROLE_ADMIN";
  "createLocation" if "COMPANY_ROLE_ADMIN";
}

resource Team {
  relations = { members: CustomerEmployee };
}

# ── Bridge: team membership + team role-at-location → user role-at-location ──

has_role(u: CustomerEmployee, role: String, loc: Location) if
  team matches Team and
  has_relation(team, "members", u) and
  has_role(team, role, loc);


resource Location {
  relations = { customer: Customer };
  roles = ["SECURITY_SYSTEM_ARMER", "SECURITY_SYSTEM_DISARMER", "SECURITY_SYSTEM_VIEWER"];                # role at the location

  # Inherit SECURITY_SYSTEM_XXX roles from the owning Customer (org)

  "SECURITY_SYSTEM_ARMER" if "SECURITY_SYSTEM_ARMER" on "customer";
  "SECURITY_SYSTEM_DISARMER" if "SECURITY_SYSTEM_DISARMER" on "customer";
  "SECURITY_SYSTEM_VIEWER" if "SECURITY_SYSTEM_VIEWER" on "customer";
}

resource SecuritySystem {
  relations = { location: Location };
  permissions = ["arm", "disarm", "view"];

  # (a) SECURITY_SYSTEM_XXX role at the system’s Location → can arm/disarm

  "arm" if "SECURITY_SYSTEM_ARMER" on "location";
  "disarm" if "SECURITY_SYSTEM_DISARMER" on "location";

  "view" if "SECURITY_SYSTEM_ARMER" on "location";
  "view" if "SECURITY_SYSTEM_DISARMER" on "location";
  "view" if "SECURITY_SYSTEM_VIEWER" on "location";

  # (b) SSECURITY_SYSTEM_XXX role at the Customer that owns that Location
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

  assert has_permission(CustomerEmployee{"alice"}, "view", SecuritySystem{"ss1"});
  assert_not has_permission(CustomerEmployee{"alice"}, "view", SecuritySystem{"ss2"});

  assert has_permission(CustomerEmployee{"bob"}, "view", SecuritySystem{"ss2"});
  assert has_permission(CustomerEmployee{"mary"}, "view", SecuritySystem{"ss3"});

}

test "customer admin" {
  setup {
    has_role(CustomerEmployee{"bob"}, "COMPANY_ROLE_ADMIN", Customer{"acme"});
  }
  assert has_permission(CustomerEmployee{"bob"}, "createCustomerEmployee", Customer{"acme"});
  assert has_permission(CustomerEmployee{"bob"}, "createLocation", Customer{"acme"});
}