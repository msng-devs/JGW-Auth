package com.jaramgroupware.auth.domain;

import com.jaramgroupware.auth.testUtils.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Description;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SqlGroup({
        @Sql(scripts = "classpath:tableBuild.sql",executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
        @Sql(scripts = "classpath:testDataSet.sql",executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(SpringExtension.class)
@Transactional
@DataJpaTest
class MemberRepositoryTest {

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private MemberRepository memberRepository;

    private final TestUtils testUtils = new TestUtils();

    @Test
    @Description("올바른 유저의 id가 주어졌을 때, 해당 유저를 반환한다.(1)")
    void findById() {
        //given
        Member targetUser = testUtils.getTestMember();

        //when
        Optional<Member> result = memberRepository.findById(targetUser.getId());

        //then
        assertTrue(result.isPresent());
        assertEquals(targetUser.toString(),result.get().toString());
    }

    @Test
    @Description("올바른 유저의 id가 주어졌을 때, 해당 유저를 반환한다.(2)")
    void findById2() {
        //given
        Member targetUser = testUtils.getTestMember2();

        //when
        Optional<Member> result = memberRepository.findById(targetUser.getId());

        //then
        assertTrue(result.isPresent());
        assertEquals(targetUser.toString(),result.get().toString());

    }

    @Test
    @Description("존재하지 않는 유저의 id가 주어졌을 때, 해당 유저를 반환하지 않는다.")
    void findById3() {
        //given
        String testId = "doyouknowherosofthestorm";

        //when
        Optional<Member> result = memberRepository.findById(testId);

        //then
        assertFalse(result.isPresent());
    }
}