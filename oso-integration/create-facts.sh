#! /bin/bash -e

# has_role(CustomerEmployee{"alice"}, "DISARM", Customer{"acme"})
# has_relation(Location{"loc1"}, "customer", Customer{"acme"})
# has_relation(SecuritySystem{"ss1"}, "location", Location{"loc1"});

source ./set-oso-env.sh

oso-cloud tell has_role CustomerEmployee:alice DISARM Customer:acme
oso-cloud tell has_relation Location:loc1 customer Customer:acme
oso-cloud tell has_relation SecuritySystem:ss1 location Location:loc1

oso-cloud tell has_role CustomerEmployee:bob DISARM Customer:foo
oso-cloud tell has_relation Location:loc2 customer Customer:foo
oso-cloud tell has_relation SecuritySystem:ss2 location Location:loc2

oso-cloud tell has_role CustomerEmployee:mary DISARM Location:loc3
oso-cloud tell has_relation SecuritySystem:ss3 location Location:loc3

oso-cloud tell has_relation Team:ops-t1 members CustomerEmployee:charlie
oso-cloud tell has_role Team:ops-t1 DISARM Location:loc1