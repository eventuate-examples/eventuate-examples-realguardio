package io.eventuate.examples.realguardio.customerservice.restapi;

public record AssignLocationRoleRequest(Long employeeId, Long locationId, String roleName) {
}