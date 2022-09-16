package com.jaramgroupware.jgwauth.domain.jpa.member;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member,String>{
    Optional<Member> findMemberById(String id);
}
