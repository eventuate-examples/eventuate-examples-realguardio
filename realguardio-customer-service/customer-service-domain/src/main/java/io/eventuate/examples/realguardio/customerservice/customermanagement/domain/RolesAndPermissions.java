package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;


import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RolesAndPermissions {

    public static class Roles {
        public static final String COMPANY_ROLE_ADMIN = "COMPANY_ROLE_ADMIN";
    }

    public static class Permissions {
        public static final String CREATE_CUSTOMER_EMPLOYEE = "createCustomerEmployee";
        public static final String CREATE_LOCATION = "createLocation";
    }

    public static final Map<String, Set<String>> rolesToPermissions = Map.of(
            Roles.COMPANY_ROLE_ADMIN, Set.of(Permissions.CREATE_CUSTOMER_EMPLOYEE, Permissions.CREATE_LOCATION)
    );

    public static final Map<String, Set<String>> permissionsToRoles = invertMap(rolesToPermissions);

    private static Map<String, Set<String>> invertMap(Map<String, Set<String>> rolesToPermissions) {
        return rolesToPermissions.entrySet().stream()
            .flatMap(entry -> entry.getValue().stream()
                .map(permission -> Map.entry(permission, entry.getKey())))
            .collect(Collectors.groupingBy(
                Map.Entry::getKey,
                Collectors.mapping(Map.Entry::getValue, Collectors.toSet())
            ));
    }

    public static Set<String> rolesForPermission(String permission) {
        return permissionsToRoles.get(permission);
    }
}
