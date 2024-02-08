package org.dnd.timeet.member.domain;


import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findById(Long id);

    Optional<Member> findByOauthId(Long oauthId);
}
