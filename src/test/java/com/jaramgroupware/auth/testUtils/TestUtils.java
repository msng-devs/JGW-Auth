package com.jaramgroupware.auth.testUtils;

import com.jaramgroupware.auth.domain.Member;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
public class TestUtils {

    private final String testToken = "ThisIsNotRealTokenButFirebaseClientAllowThisToken";
    private final Member testMember;
    private final Member testMember2;
    public final String testUid;

    public boolean isListSame(List<?> targetListA , List<?> targetListB){

        if(targetListA.size() != targetListB.size()) return false;
        for (int i = 0; i < targetListA.size(); i++) {
            try{
                targetListA.indexOf(targetListB.get(i));
            }catch (Exception e){
                log.debug("{}",targetListA.get(i).toString());
                log.debug("{}",targetListB.get(i).toString());
                return false;
            }
        }
        return true;
    }
    public TestUtils(){

        testMember = Member.builder()
                .id("Th1s1sNotRea1U1DDOY0UKNOWH0S")
                .name("황테스트")
                .email("hwangTest@test.com")
                .role(1)
                .isActive(true)
                .build();

        testMember2 = Member.builder()
                .id("ThiS1SNotRea1U1DDOY0UKNOWHoS")
                .name("김테스트")
                .email("kimTest@test.com")
                .role(2)
                .isActive(false)
                .build();

        testUid = testMember.getId();


    }
    public HttpEntity<?> createHttpEntity(Object dto,String userUid){

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add("user_uid",userUid);

        return new HttpEntity<>(dto, headers);
    }
    public Map<String,Object> getString(String arg, Object value) {
        return Collections.singletonMap(arg, value);
    }
}
