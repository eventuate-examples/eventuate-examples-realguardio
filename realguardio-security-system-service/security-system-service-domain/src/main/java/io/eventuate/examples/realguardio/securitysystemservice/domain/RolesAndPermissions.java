package io.eventuate.examples.realguardio.securitysystemservice.domain;


import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RolesAndPermissions {

    public static final String SECURITY_SYSTEM_ARMER = "SECURITY_SYSTEM_ARMER";
    public static final String SECURITY_SYSTEM_DISARMER = "SECURITY_SYSTEM_DISARMER";
    public static final String SECURITY_SYSTEM_VIEWER = "SECURITY_SYSTEM_VIEWER";

    public static final String ARM = "arm";
    public static final String DISARM = "disarm";
    public static final String VIEW = "view";

    public static final Map<String, Set<String>> rolesToPermissions = Map.of(
            SECURITY_SYSTEM_ARMER, Set.of(ARM, VIEW),
            SECURITY_SYSTEM_DISARMER, Set.of(DISARM, VIEW),
            SECURITY_SYSTEM_VIEWER, Set.of(VIEW)
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
