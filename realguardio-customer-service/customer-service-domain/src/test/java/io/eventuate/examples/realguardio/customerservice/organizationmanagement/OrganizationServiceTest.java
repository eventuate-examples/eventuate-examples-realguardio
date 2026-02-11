package io.eventuate.examples.realguardio.customerservice.organizationmanagement;

import io.eventuate.examples.realguardio.customerservice.commondomain.EmailAddress;
import io.eventuate.examples.realguardio.customerservice.commondomain.PersonDetails;
import io.eventuate.examples.realguardio.customerservice.commondomain.PersonName;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.domain.Member;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.domain.Organization;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.service.MemberService;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.service.OrganizationService;
import io.eventuate.examples.realguardio.customerservice.security.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
public class OrganizationServiceTest {

  @Configuration
  @EnableAutoConfiguration
  @Import({OrganizationService.class, MemberService.class})
  public static class Config {
    // Configuration for the test context
  }

  @MockitoBean
  private UserService userService;

  @Autowired
  private OrganizationService organizationService;

  @Autowired
  private MemberService memberService;


  private Member fred;
  private Member mary;
  private Organization acmeInc;
  private Member harry;
  private Organization otherOrg;
  private Member otherBuyer;

  @BeforeEach
  public void setUp() {
    acmeInc = organizationService.createOrganization("Acme Inc");
    fred = memberService.createMember(new PersonDetails(
            new PersonName("Fred", "Smith"), new EmailAddress("fred.smith@realguard.io")));
    organizationService.assignRole(acmeInc.getId(), fred.getId(), "Approver");
    mary = memberService.createMember(new PersonDetails(
            new PersonName("Mary", "Jones"), new EmailAddress("mary.jones@realguard.io")));
    organizationService.assignRole(acmeInc.getId(), mary.getId(), "Buyer");
    harry = memberService.createMember(new PersonDetails(
            new PersonName("Harry", "Doe"), new EmailAddress("harry.doe@realguard.io")));
    organizationService.assignRole(acmeInc.getId(), harry.getId(), "Buyer");

    otherOrg = organizationService.createOrganization("Other Org");
    otherBuyer = memberService.createMember(new PersonDetails(
            new PersonName("Other", "Buyer"), new EmailAddress("other.buyer@realguard.io")));
    organizationService.assignRole(otherOrg.getId(), otherBuyer.getId(), "Buyer");
  }

  @Test
  public void should() {

  }


}
