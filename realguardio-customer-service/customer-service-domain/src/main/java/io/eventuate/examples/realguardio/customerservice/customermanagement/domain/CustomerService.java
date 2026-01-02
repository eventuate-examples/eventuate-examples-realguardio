package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.examples.realguardio.customerservice.commondomain.EmailAddress;
import io.eventuate.examples.realguardio.customerservice.commondomain.PersonDetails;
import io.eventuate.examples.realguardio.customerservice.domain.CustomerEmployeeAssignedLocationRole;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.domain.Member;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.domain.MemberRole;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.domain.Organization;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.exception.NotAuthorizedException;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.service.MemberService;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.service.OrganizationService;
import io.eventuate.examples.realguardio.customerservice.security.UserNameSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerEmployeeRepository customerEmployeeRepository;
    private final LocationRepository locationRepository;
    private final CustomerEmployeeLocationRoleRepository customerEmployeeLocationRoleRepository;
    private final TeamRepository teamRepository;
    private final TeamLocationRoleRepository teamLocationRoleRepository;
    private final OrganizationService organizationService;
    private final MemberService memberService;
    private final UserNameSupplier userNameSupplier;
    private final CustomerEventPublisher customerEventPublisher;
    private final CustomerActionAuthorizer customerActionAuthorizer;

    @Autowired
    public CustomerService(CustomerRepository customerRepository,
                          CustomerEmployeeRepository customerEmployeeRepository,
                          LocationRepository locationRepository,
                          CustomerEmployeeLocationRoleRepository customerEmployeeLocationRoleRepository,
                          TeamRepository teamRepository,
                          TeamLocationRoleRepository teamLocationRoleRepository,
                          OrganizationService organizationService,
                          MemberService memberService,
                          UserNameSupplier userNameSupplier,
                          CustomerEventPublisher customerEventPublisher, CustomerActionAuthorizer customerActionAuthorizer) {
        this.customerRepository = customerRepository;
        this.customerEmployeeRepository = customerEmployeeRepository;
        this.locationRepository = locationRepository;
        this.customerEmployeeLocationRoleRepository = customerEmployeeLocationRoleRepository;
        this.teamRepository = teamRepository;
        this.teamLocationRoleRepository = teamLocationRoleRepository;
        this.organizationService = organizationService;
        this.memberService = memberService;
        this.userNameSupplier = userNameSupplier;
        this.customerEventPublisher = customerEventPublisher;
        this.customerActionAuthorizer = customerActionAuthorizer;
    }

    /**
     * Create a new customer with the given name and initial administrator.
     *
     * @param name the name of the customer to create
     * @param initialAdministrator the details of the initial administrator
     * @return the created customer and its initial administrator
     */
    @PreAuthorize("hasRole('REALGUARDIO_ADMIN')")
    public CustomerAndCustomerEmployee createCustomer(String name, PersonDetails initialAdministrator) {
        Organization organization = organizationService.createOrganization(name);

        Customer customer = new Customer(name, organization.getId());
        customer = customerRepository.save(customer);
        
        // Create initial admin without authorization check
        CustomerEmployee admin = createCustomerEmployeeInternal(customer.getId(), initialAdministrator);
        
        assignRoleInternal(customer.getId(), admin.getId(), RolesAndPermissions.COMPANY_ROLE_ADMIN);
        
        return new CustomerAndCustomerEmployee(customer, admin);
    }
    
    private CustomerEmployee createCustomerEmployeeInternal(Long customerId, PersonDetails personDetails) {
        Member member = memberService.createMember(personDetails);
        CustomerEmployee customerEmployee = new CustomerEmployee(customerId, member.getId());
        return customerEmployeeRepository.save(customerEmployee);
    }

    /**
     * Create a new customer employee with the given details.
     *
     * @param customerId the ID of the customer
     * @param personDetails the details of the employee
     * @return the created customer employee
     */
    @PreAuthorize("hasRole('REALGUARDIO_CUSTOMER_EMPLOYEE')")
    public CustomerEmployee createCustomerEmployee(Long customerId, PersonDetails personDetails) {
        Customer customer = customerRepository.findRequiredById(customerId);

        customerActionAuthorizer.verifyCanDo(customerId, RolesAndPermissions.CREATE_CUSTOMER_EMPLOYEE);

        return createCustomerEmployeeInternal(customerId, personDetails);
    }

    private Member requireCustomerAdminRole(Long customerId) {
        String currentUserEmail = userNameSupplier.getCurrentUserEmail();
        Member currentMember = memberService.findMemberByEmail(new EmailAddress(currentUserEmail))
                .orElseThrow(() -> new IllegalArgumentException("Current user member not found: %s".formatted(currentUserEmail)));

        Set<String> currentUserRoles = customerEmployeeRepository.findRolesInCustomer(customerId, currentUserEmail);

        if (!currentUserRoles.contains(RolesAndPermissions.COMPANY_ROLE_ADMIN)) {
            throw new NotAuthorizedException("Only company admins can create new employees");
        }
        return currentMember;
    }

    /**
     * Assign a role to a customer employee.
     *
     * @param customerId the ID of the customer
     * @param customerEmployeeId the ID of the customer employee
     * @param roleName the name of the role to assign
     * @return the created member role
     */
    public MemberRole assignRole(Long customerId, Long customerEmployeeId, String roleName) {
        Customer customer = customerRepository.findRequiredById(customerId);

        requireCustomerAdminRole(customerId);

        return assignRoleInternal(customerId, customerEmployeeId, roleName);
    }

    public MemberRole assignRoleInternal(Long customerId, Long customerEmployeeId, String roleName) {
        Customer customer = customerRepository.findRequiredById(customerId);

        CustomerEmployee customerEmployee = customerEmployeeRepository.findRequiredById(customerEmployeeId);

        if (!customerId.equals(customerEmployee.getCustomerId())) {
            throw new IllegalArgumentException("Customer employee does not belong to the specified customer");
        }

        Member member = memberService.findMemberById(customerEmployee.getMemberId());
        String userName = member.getEmailAddress().email();

        customerEventPublisher.publish(customer,
            new CustomerEmployeeAssignedCustomerRole(customerEmployeeId, userName, roleName));

        return organizationService.assignRole(customer.getOrganizationId(), customerEmployee.getMemberId(), roleName);
    }

    /**
     * Create a new location for a customer.
     *
     * @param customerId the ID of the customer
     * @param name the name of the location
     * @return the created location
     */
    @PreAuthorize("hasRole('REALGUARDIO_CUSTOMER_EMPLOYEE')")
    public Location createLocationForCustomer(Long customerId, String name) {
        customerActionAuthorizer.verifyCanDo(customerId, RolesAndPermissions.CREATE_LOCATION);
        return createLocation(customerId, name);
    }

    /**
     * Create a new location for a customer (internal method).
     *
     * @param customerId the ID of the customer
     * @param name the name of the location
     * @return the created location
     */
    public Location createLocation(Long customerId, String name) {
        Customer customer = customerRepository.findRequiredById(customerId);

        // Security-todo Verify that caller has COMPANY_ROLE_ADMIN

        Location location = new Location(name, customerId);


        Location savedLocation = locationRepository.save(location);

        customerEventPublisher.publish(customer,
            new LocationCreatedForCustomer(savedLocation.getId()));

        return savedLocation;
    }

    public void addSecuritySystemToLocation(Long locationId, Long securitySystemId) {
        // Security-todo Verify that caller has COMPANY_ROLE_ADMIN

        Location location = locationRepository.findRequiredById(locationId);
        location.addSecuritySystem(securitySystemId);

        Customer customer = customerRepository.findRequiredById(location.getCustomerId());

        customerEventPublisher.publish(customer,
            new SecuritySystemAssignedToLocation(locationId, securitySystemId));

    }

    /**
     * Find a location by its ID.
     *
     * @param locationId the ID of the location to find
     * @return the location if found, null otherwise
     */
    public Location findLocationById(Long locationId) {
        return locationRepository.findById(locationId).orElse(null);
    }
    
    /**
     * Create or update a location with a security system.
     * If the location doesn't exist, create it.
     * If it exists without a security system, add the security system.
     * If it already has a security system, throw an exception.
     *
     * @param customerId the ID of the customer
     * @param locationName the name of the location
     * @param securitySystemId the ID of the security system
     * @return the ID of the location
     * @throws CustomerNotFoundException if customer doesn't exist
     * @throws LocationAlreadyHasSecuritySystemException if location already has a security system
     */
    public Long createLocationWithSecuritySystem(Long customerId, String locationName, Long securitySystemId) {
        // Verify customer exists
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));
        
        // Find or create location
        Location location = locationRepository.findByCustomerIdAndName(customerId, locationName)
            .orElseGet(() -> {
                Location newLocation = new Location(locationName, customerId);
                return locationRepository.save(newLocation);
            });
        
        // Add security system to location
        location.addSecuritySystem(securitySystemId);
        locationRepository.save(location);

        customerEventPublisher.publish(customer,
            new SecuritySystemAssignedToLocation(location.getId(), securitySystemId));

        return location.getId();
    }

    /**
     * Verify if an employee is authorized to disarm a security system.
     * An employee is authorized if they have the required role at the customer level,
     * if they have the required role for the specific location where the security system is installed,
     * or if they are a member of a team that has the required role for the location.
     * Throws NotAuthorizedException if the employee is not authorized.
     *
     * @param securitySystemId the ID of the security system
     * @param employeeId the ID of the employee
     * @param role the role required for authorization
     * @throws NotAuthorizedException if the employee is not authorized to disarm the security system
     */
    public void verifyEmployeeHasSecuritySystemRole(long securitySystemId, long employeeId, String role) {
        Long customerRoleCount = customerEmployeeRepository.countEmployeeWithSecuritySystemRole(securitySystemId, employeeId, role);

        Long locationRoleCount = customerEmployeeLocationRoleRepository.countEmployeeWithSecuritySystemLocationRole(securitySystemId, employeeId, role);

        Long teamRoleCount = teamRepository.countEmployeeWithTeamSecuritySystemRole(securitySystemId, employeeId, role);

        if (customerRoleCount == 0 && locationRoleCount == 0 && teamRoleCount == 0) {
            throw new NotAuthorizedException("Employee is not authorized to perform this action on the security system");
        }
    }

    /**
     * Assign a location-specific role to a customer employee.
     *
     * @param customerId the ID of the customer
     * @param customerEmployeeId the ID of the customer employee
     * @param locationId the ID of the location
     * @param roleName the name of the role to assign
     * @return the created customer employee location role
     */
    @Transactional
    public CustomerEmployeeLocationRole assignLocationRole(Long customerId, Long customerEmployeeId, Long locationId, String roleName) {
        Customer customer = customerRepository.findRequiredById(customerId);

        CustomerEmployee customerEmployee = customerEmployeeRepository.findRequiredById(customerEmployeeId);

        Location location = locationRepository.findRequiredById(locationId);

        // Security-todo Verify that caller has COMPANY_ROLE_ADMIN
        // And that team and employee belong to the same customer

        CustomerEmployeeLocationRole role = new CustomerEmployeeLocationRole(customerId, customerEmployeeId, locationId, roleName);
        CustomerEmployeeLocationRole savedRole = customerEmployeeLocationRoleRepository.save(role);
        
        // Get the member's email address to use as userName
        Member member = memberService.findMemberById(customerEmployee.getMemberId());
        String userName = member.getEmailAddress().email();

        // Publish the event
        customerEventPublisher.publish(customer,
            new CustomerEmployeeAssignedLocationRole(userName, locationId, roleName));

        return savedRole;
    }

    /**
     * Create a new team for a customer.
     *
     * @param customerId the ID of the customer
     * @param name the name of the team
     * @return the created team
     */
    public Team createTeam(Long customerId, String name) {
        Customer customer = customerRepository.findRequiredById(customerId);

        // Security-todo Verify that caller has COMPANY_ROLE_ADMIN

        Team team = new Team(name, customerId);
        return teamRepository.save(team);
    }

    /**
     * Add a customer employee to a team.
     *
     * @param teamId the ID of the team
     * @param customerEmployeeId the ID of the customer employee
     * @return the updated team
     */
    public Team addTeamMember(Long teamId, Long customerEmployeeId) {
        Team team = teamRepository.findRequiredById(teamId);

        CustomerEmployee customerEmployee = customerEmployeeRepository.findRequiredById(customerEmployeeId);

        // Security-todo Verify that caller has COMPANY_ROLE_ADMIN
        // And that team and employee belong to the same customer

        team.addMember(customerEmployeeId);

        // Add the team ID to the customer employee
        customerEmployee.getTeamIds().add(teamId);

        // Save both entities
        customerEmployeeRepository.save(customerEmployee);
        Team savedTeam = teamRepository.save(team);

        // Publish event using type-safe publisher
        Customer customer = customerRepository.findRequiredById(team.getCustomerId());
        customerEventPublisher.publish(customer,
            new TeamMemberAdded(teamId, customerEmployeeId)
        );

        return savedTeam;
    }

    /**
     * Get all roles for a customer employee.
     *
     * @param customerId the ID of the customer
     * @param employeeId the ID of the customer employee
     * @return set of role names
     */
    public Set<String> getCustomerEmployeeRoles(Long customerId, Long employeeId) {
        Customer customer = customerRepository.findRequiredById(customerId);
        
        CustomerEmployee employee = customerEmployeeRepository.findRequiredById(employeeId);
        
        return organizationService.findRolesByMemberIdAndOrganizationId(
                employee.getMemberId(), customer.getOrganizationId())
                .stream()
                .map(MemberRole::getName)
                .collect(Collectors.toSet());
    }
    
    /**
     * Get location-specific roles for a customer employee.
     *
     * @param userName email address
     * @param locationId the ID of the location
     * @return set of role names for the location
     */
    public Set<String> getCustomerEmployeeLocationRoles(String userName, Long locationId) {

        locationRepository.findRequiredById(locationId);
        
        return new HashSet<>(customerEmployeeLocationRoleRepository
                .findRoleNamesByUserNameAndLocationId(userName, locationId));
    }
    
    /**
     * Find employee details (name and email) for a customer employee.
     *
     * @param customerId the ID of the customer
     * @param employeeId the ID of the customer employee
     * @return employee details containing name and email address
     */
    public PersonDetails findEmployeeDetails(Long customerId, Long employeeId) {
        Customer customer = customerRepository.findRequiredById(customerId);
        
        CustomerEmployee employee = customerEmployeeRepository.findRequiredById(employeeId);
        
        // Verify the employee belongs to the customer
        if (!employee.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Employee does not belong to customer");
        }
        
        Member member = memberService.findMemberById(employee.getMemberId());
        return new PersonDetails(member.getName(), member.getEmailAddress());
    }
    
    /**
     * Assign a location-specific role to a team.
     *
     * @param teamId the ID of the team
     * @param locationId the ID of the location
     * @param roleName the name of the role to assign
     * @return the created team location role
     */
    public TeamLocationRole assignTeamRole(Long teamId, Long locationId, String roleName) {
        // Security-todo Verify that caller has COMPANY_ROLE_ADMIN

        Team team = teamRepository.findRequiredById(teamId);

        Location location = locationRepository.findRequiredById(locationId);

        TeamLocationRole role = new TeamLocationRole(team, locationId, roleName);
        team.addRole(role);
        TeamLocationRole savedRole = teamLocationRoleRepository.save(role);

        Customer customer = customerRepository.findRequiredById(team.getCustomerId());
        customerEventPublisher.publish(customer,
            new TeamAssignedLocationRole(teamId, locationId, roleName)
        );

        return savedRole;
    }

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }
}
