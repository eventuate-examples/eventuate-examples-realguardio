actor CustomerEmployee {}

resource Customer {
  roles = ["DISARM"];               # role at the organization
  permissions = ["disarm"];

  "disarm" if "DISARM";             # org DISARM role grants disarm
}

resource Team {
  relations = { members: CustomerEmployee };
  roles = ["member"];     # optional convenience role

  # optional: give users the 'member' role on the Team if they're in the members relation
  #"member" if "members" on resource;
}

# ── Bridge: team membership + team role-at-location → user role-at-location ──
has_role(u: CustomerEmployee, "DISARM", loc: Location) if
  team matches Team and
  has_relation(team, "members", u) and
  has_role(team, "DISARM", loc);

resource Location {
  relations = { customer: Customer };
  roles = ["DISARM"];               # role at the location

  # Inherit the DISARM role from the owning Customer (org)
  "DISARM" if "DISARM" on "customer";
}

resource SecuritySystem {
  relations = { location: Location };
  permissions = ["disarm"];

  # (a) DISARM role at the system’s Location → can disarm
  "disarm" if "DISARM" on "location";

  # (b) DISARM role at the Customer that owns that Location → can disarm
  # (via the Location role inheritance above)
}
