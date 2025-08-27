package io.eventuate.examples.realguardio.customerservice.db;

import io.eventuate.examples.realguardio.customerservice.commondomain.EmailAddress;
import io.eventuate.examples.realguardio.customerservice.commondomain.PersonName;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerEmployee;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerEmployeeRepository;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerRepository;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Location;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.LocationRepository;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Team;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.TeamLocationRole;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.TeamLocationRoleRepository;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.TeamRepository;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.domain.Member;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.domain.MemberRole;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.domain.Organization;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.repository.MemberRepository;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.repository.MemberRoleRepository;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.repository.OrganizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

public class DBInitializer {
    
    private static final Logger logger = LoggerFactory.getLogger(DBInitializer.class);
    
    public static final String LOCATION_OAKLAND_OFFICE = "Oakland office";
    public static final String LOCATION_BERKELEY_OFFICE = "Berkeley office";
    public static final String LOCATION_HAYWARD_OFFICE = "Hayward office";

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerEmployeeRepository customerEmployeeRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamLocationRoleRepository teamLocationRoleRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberRoleRepository memberRoleRepository;

    void createCustomer() {
        // Create Organization
        Organization organization = new Organization("Acme Corporation");
        organization = organizationRepository.save(organization);
        logger.info("Created organization: {} with ID: {}",
            organization.getName(), organization.getId());

        // Create Customer
        Customer customer = new Customer("Acme Corporation", organization.getId());
        customer = customerRepository.save(customer);
        logger.info("Created customer: {} with ID: {}",
            customer.getName(), customer.getId());

        // Create initial admin Member
        Member adminMember = new Member(
            new PersonName("System", "Administrator"),
            new EmailAddress("admin@acme.com")
        );
        adminMember = memberRepository.save(adminMember);
        logger.info("Created admin member with ID: {}", adminMember.getId());

        // Create CustomerEmployee for admin
        CustomerEmployee adminEmployee = new CustomerEmployee(customer.getId(), adminMember.getId());
        adminEmployee = customerEmployeeRepository.save(adminEmployee);
        logger.info("Created admin employee with ID: {}", adminEmployee.getId());

        // Create MemberRole for admin
        MemberRole adminRole = new MemberRole("COMPANY_ROLE_ADMIN", LocalDate.now(), null);
        adminRole.setMember(adminMember);
        adminRole.setOrganization(organization);
        memberRoleRepository.save(adminRole);
        logger.info("Assigned COMPANY_ROLE_ADMIN role to admin");

        // Create locations
        Location oaklandOffice = new Location(LOCATION_OAKLAND_OFFICE, customer.getId());
        oaklandOffice = locationRepository.save(oaklandOffice);
        logger.info("Created location: {} with ID: {}",
            LOCATION_OAKLAND_OFFICE, oaklandOffice.getId());

        Location berkeleyOffice = new Location(LOCATION_BERKELEY_OFFICE, customer.getId());
        berkeleyOffice = locationRepository.save(berkeleyOffice);
        logger.info("Created location: {} with ID: {}",
            LOCATION_BERKELEY_OFFICE, berkeleyOffice.getId());

        Location haywardOffice = new Location(LOCATION_HAYWARD_OFFICE, customer.getId());
        haywardOffice = locationRepository.save(haywardOffice);
        logger.info("Created location: {} with ID: {}",
            LOCATION_HAYWARD_OFFICE, haywardOffice.getId());

        // Create a team
        Team securityTeam = new Team("Security Team", customer.getId());
        securityTeam = teamRepository.save(securityTeam);
        logger.info("Created team: Security Team with ID: {}", securityTeam.getId());

        // Create additional employee Member
        Member employeeMember = new Member(
            new PersonName("John", "Doe"),
            new EmailAddress("john.doe@acme.com")
        );
        employeeMember = memberRepository.save(employeeMember);
        logger.info("Created employee member with ID: {}", employeeMember.getId());

        // Create CustomerEmployee for additional employee
        CustomerEmployee newEmployee = new CustomerEmployee(customer.getId(), employeeMember.getId());
        newEmployee = customerEmployeeRepository.save(newEmployee);
        logger.info("Created additional employee with ID: {}", newEmployee.getId());

        // Add employee to team
        securityTeam.addMember(newEmployee.getId());
        newEmployee.getTeamIds().add(securityTeam.getId());

        // Save both entities
        securityTeam = teamRepository.save(securityTeam);
        newEmployee = customerEmployeeRepository.save(newEmployee);
        logger.info("Added employee {} to Security Team", newEmployee.getId());

        // Create and assign team location role
        TeamLocationRole teamRole = new TeamLocationRole(securityTeam, oaklandOffice.getId(), "SecuritySystemDisarmer");
        teamRole = teamLocationRoleRepository.save(teamRole);

        // Add role to team
        securityTeam.addRole(teamRole);
        teamRepository.save(securityTeam);
        logger.info("Assigned SecuritySystemDisarmer role to Security Team for Oakland office");
    }
}