package io.eventuate.examples.realguardio.customerservice.organizationmanagement.service;

import io.eventuate.examples.realguardio.customerservice.commondomain.EmailAddress;
import io.eventuate.examples.realguardio.customerservice.commondomain.PersonDetails;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.domain.Member;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.repository.MemberRepository;
import io.eventuate.examples.realguardio.customerservice.security.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for managing Member entities.
 */
@Service
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final UserService userService;

    @Autowired
    public MemberService(MemberRepository memberRepository, UserService userService) {
        this.memberRepository = memberRepository;
        this.userService = userService;
    }

    /**
     * Create a new member with the given details.
     *
     * @param personDetails the details of the member to create
     * @return the created member
     */
    public Member createMember(PersonDetails personDetails) {
        Member member = new Member(personDetails.name(), personDetails.emailAddress());
        Member savedMember = memberRepository.save(member);
        userService.createCustomerEmployeeUser(personDetails.emailAddress().email());
        return savedMember;
    }


    /**
     * Find all members.
     *
     * @return a list of all members
     */
    public List<Member> findAllMembers() {
        return memberRepository.findAll();
    }
    
    /**
     * Find a member by ID.
     *
     * @param memberId the ID of the member
     * @return the member
     */
    public Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with ID: " + memberId));
    }
    
    /**
     * Find a member by email address.
     *
     * @param emailAddress the email address of the member
     * @return an optional containing the member if found
     */
    public Optional<Member> findMemberByEmail(EmailAddress emailAddress) {
        return memberRepository.findByEmailAddress(emailAddress);
    }
}
