package com.jaramgroupware.jgwauth.domain.redis.membeAuth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jaramgroupware.jgwauth.domain.redis.memberCache.MemberAuth;
import com.jaramgroupware.jgwauth.domain.redis.memberCache.MemberAuthRepository;
import com.jaramgroupware.jgwauth.testConfig.EmbeddedRedisConfig;
import com.jaramgroupware.jgwauth.testConfig.TestRedisConfig;
import com.jaramgroupware.jgwauth.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@ContextConfiguration(classes = {EmbeddedRedisConfig.class, TestRedisConfig.class})
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@DataRedisTest
public class MemberAuthRepositoryTest {

    @Autowired
    private MemberAuthRepository memberAuthRepository;

    private final TestUtils testUtils = new TestUtils();

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();


    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @AfterEach
    void tearDownEach(){
        memberAuthRepository.deleteAll();
    }

    @DisplayName("memberAuth 추가")
    @Test
    void save() throws JsonProcessingException {

        //given
        MemberAuth memberCache = MemberAuth.builder()
                .isValid(true)
                .token(testUtils.getTestToken())
                .member(objectMapper.writeValueAsString(testUtils.getTestMember()))
                .ttl(10L)
                .build();
        //when
        memberAuthRepository.save(memberCache);

        //then
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        assertEquals(objectMapper.writeValueAsString(testUtils.getTestMember()), Objects.requireNonNull(hashOperations.get("memberAuth:" + memberCache.getToken(), "member")).toString());
        assertEquals((memberCache.getIsValid()) ? "1" : "0", Objects.requireNonNull(hashOperations.get("memberAuth:" + memberCache.getToken(), "isValid")).toString());
    }

    @DisplayName("memberAuth 삭제")
    @Test
    void delete() throws JsonProcessingException {
        //given
        MemberAuth memberCache = MemberAuth.builder()
                .isValid(true)
                .token(testUtils.getTestToken())
                .member(objectMapper.writeValueAsString(testUtils.getTestMember()))
                .ttl(10L)
                .build();

        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        Map<String,Object> testData = new HashMap<>();
        testData.put("token",memberCache.getToken());
        testData.put("isValid",memberCache.getIsValid().toString());
        testData.put("member",objectMapper.writeValueAsString(testUtils.getTestMember()));
        hashOperations.putAll("memberAuth:" + memberCache.getToken(),testData);

        //when
        memberAuthRepository.delete(memberCache);

        //then
        assertThrows(NullPointerException.class,() -> Objects.requireNonNull(hashOperations.get("memberAuth:" + memberCache.getToken(), "token").toString()));
    }

    @DisplayName("memberID 로 조회")
    @Test
    void findById() throws JsonProcessingException {
        //given
        MemberAuth memberCache = MemberAuth.builder()
                .isValid(true)
                .token(testUtils.getTestToken())
                .member(objectMapper.writeValueAsString(testUtils.getTestMember()))
                .ttl(10L)
                .build();

        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        Map<String,Object> testData = new HashMap<>();
        testData.put("token",memberCache.getToken());
        testData.put("isValid",memberCache.getIsValid().toString());
        testData.put("member",objectMapper.writeValueAsString(testUtils.getTestMember()));
        hashOperations.putAll("memberAuth:" + memberCache.getToken(),testData);

        //when
        MemberAuth result = memberAuthRepository.findById(memberCache.getToken()).orElseThrow();

        //then
        assertThat(result.equalsExcludeTTL(memberCache),is(true));
    }
}
