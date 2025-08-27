package io.eventuate.examples.realguardio.customerservice.organizationmanagement.service;

import io.eventuate.examples.realguardio.customerservice.commondomain.EmailAddress;
import io.eventuate.examples.realguardio.customerservice.commondomain.PersonDetails;
import io.eventuate.examples.realguardio.customerservice.commondomain.PersonName;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.domain.Member;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.repository.MemberRepository;
import io.eventuate.examples.realguardio.customerservice.security.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;
    
    @Mock
    private UserService userService;
    
    @InjectMocks
    private MemberService memberService;
    
    @Test
    void shouldCreateMemberAndCallUserService() {
        PersonDetails personDetails = new PersonDetails(
            new PersonName("John", "Doe"),
            new EmailAddress("john.doe@realguard.io"));
        
        Member expectedMember = new Member(personDetails.name(), personDetails.emailAddress());
        
        when(memberRepository.save(any(Member.class))).thenReturn(expectedMember);
        
        Member result = memberService.createMember(personDetails);
        
        assertThat(result).isNotNull();
        assertThat(result.getEmailAddress().email()).isEqualTo("john.doe@realguard.io");
        
        verify(userService).createCustomerEmployeeUser(personDetails.emailAddress().email());
        
        verify(memberRepository).save(any(Member.class));
    }
}