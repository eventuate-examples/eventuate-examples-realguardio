package io.eventuate.examples.realguardio.customerservice.organizationmanagement;

import io.eventuate.examples.realguardio.customerservice.customermanagement.common.EmailAddress;
import io.eventuate.examples.realguardio.customerservice.customermanagement.common.PersonDetails;
import io.eventuate.examples.realguardio.customerservice.customermanagement.common.PersonName;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.domain.Member;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.domain.MemberRole;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.domain.Organization;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.service.MemberService;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.service.OrganizationService;
import io.eventuate.examples.realguardio.customerservice.security.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class RepositoriesTest {

    @Configuration
    @Import(OrganizationManagementConfiguration.class)
    @EnableAutoConfiguration
    public static class Config {
        // Configuration for the test context
    }

    @MockBean
    private UserService userService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private MemberService memberService;

    @Test
    public void testCreateOrganizationWithMembers() {
        // Create Acme Inc organization
        String organizationName = "Acme Inc" + System.currentTimeMillis();
        Organization acmeInc = organizationService.createOrganization(organizationName);

        // Create Fred member with Admin role
        Member fred = memberService.createMember(new PersonDetails(
                new PersonName("Fred", "Smith"), 
                new EmailAddress("fred.smith@realguard.io")));

        organizationService.assignRole(acmeInc.getId(), fred.getId(), "Admin");

        // Create Mary member with Buyer role
        Member mary = memberService.createMember(new PersonDetails(
                new PersonName("Mary", "Jones"), 
                new EmailAddress("mary.jones@realguard.io")));

        organizationService.assignRole(acmeInc.getId(), mary.getId(), "Buyer");

        // Verify the data was saved correctly
        List<Organization> organizations = organizationService.findOrganizationsByName(organizationName);
        assertThat(organizations).hasSize(1);

        Organization savedOrg = organizations.get(0);
        assertThat(savedOrg.getId()).isNotNull();
        assertThat(savedOrg.getName()).isEqualTo(organizationName);

        List<Member> members = memberService.findAllMembers();
        assertThat(members).hasSize(2);

        List<MemberRole> roles = organizationService.findRolesByOrganizationId(savedOrg.getId());
        assertThat(roles).hasSize(2);

        // Verify Fred has Admin role
        List<MemberRole> fredRoles = organizationService.findRolesByMemberIdAndOrganizationId(fred.getId(), savedOrg.getId());
        assertThat(fredRoles).hasSize(1);
        assertThat(fredRoles.get(0).getName()).isEqualTo("Admin");

        // Verify Mary has Buyer role
        List<MemberRole> maryRoles = organizationService.findRolesByMemberIdAndOrganizationId(mary.getId(), savedOrg.getId());
        assertThat(maryRoles).hasSize(1);
        assertThat(maryRoles.get(0).getName()).isEqualTo("Buyer");
    }
}
