package com.jaramgroupware.jgwauth.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jaramgroupware.jgwauth.dto.memberCache.servcieDto.MemberAuthAddRequestDto;
import com.jaramgroupware.jgwauth.dto.memberCache.servcieDto.MemberAuthResponseDto;
import com.jaramgroupware.jgwauth.service.MemberAuthServiceImpl;
import com.jaramgroupware.jgwauth.service.MemberServiceImpl;
import com.jaramgroupware.jgwauth.testConfig.EmbeddedRedisConfig;
import com.jaramgroupware.jgwauth.testConfig.TestFireBaseConfig;
import com.jaramgroupware.jgwauth.testConfig.TestRedisConfig;
import com.jaramgroupware.jgwauth.utils.TestUtils;
import com.jaramgroupware.jgwauth.dto.auth.controllerDto.AuthResponseDto;
import com.jaramgroupware.jgwauth.dto.member.serviceDto.MemberResponseServiceDto;
import com.jaramgroupware.jgwauth.utils.firebase.FireBaseClientImpl;
import com.jaramgroupware.jgwauth.utils.firebase.FireBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Optional;

import static com.jaramgroupware.jgwauth.testConfig.RestDocsConfig.field;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureRestDocs(uriScheme = "https", uriHost = "docs.api.com")
@SpringBootTest
class MemberApiControllerTest {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockBean
    private MemberServiceImpl memberService;

    @MockBean
    private MemberAuthServiceImpl memberAuthService;

    @MockBean
    private FireBaseClientImpl fireBaseClient;

    private final TestUtils testUtils = new TestUtils();

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @AfterEach
    void tearDown() {
    }

    @DisplayName("Redis에 캐싱 되어있지 않은 유저 인증")
    @Test
    void authWithNoCache() throws Exception {
        //given

        MemberResponseServiceDto targetMemberDto = new MemberResponseServiceDto(testUtils.getTestMember());

        AuthResponseDto testRes = AuthResponseDto.builder()
                .isValid(true)
                .roleID(targetMemberDto.getRoleID())
                .uid(targetMemberDto.getId())
                .build();
        MemberAuthAddRequestDto memberAuthAddRequestDto = MemberAuthAddRequestDto.builder()
                .isValid(true)
                .member(testUtils.getTestMember())
                .token(testUtils.getTestToken())
                .ttl(10L)
                .build();
        FireBaseResult fireBaseResult = FireBaseResult.builder()
                .uid(testUtils.getTestUid())
                .ttl(10L)
                .build();

        //redis에서 miss,
        Mockito.doReturn(Optional.empty()).when(memberAuthService).find(testUtils.getTestToken());

        //token 인증 후에,
        Mockito.doReturn(fireBaseResult).when(fireBaseClient).checkToken(testUtils.getTestToken());

        //db에서 찾기
        Mockito.doReturn(testUtils.getTestMember()).when(memberService).findById(targetMemberDto.getId());

        //캐싱
        Mockito.doReturn(true).when(memberAuthService).add(memberAuthAddRequestDto);

        //when
        ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.get("/api/v1/auth/{token}",testUtils.getTestToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andDo(document("auth",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("token").description("인증할 firebase token")
                        ),
                        responseFields(
                                fieldWithPath("valid").description("인증 여부").attributes(field("constraints", "True면 인증에 성공한 것이고, False면 인증에 실패했거나, Not valid한 Token임")),
                                fieldWithPath("role_id").description("해당 유저의 Role id"),
                                fieldWithPath("uid").description("해당 유저의 firebase uid")
                        ))
                );

        //then
        result.andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(testRes)));

        verify(memberAuthService).find(testUtils.getTestToken());
        verify(fireBaseClient).checkToken(testUtils.getTestToken());
        verify(memberService).findById(targetMemberDto.getId());
        verify(memberAuthService).add(memberAuthAddRequestDto);
    }

    @DisplayName("Redis에 캐싱 되어 있는 유저 인증")
    @Test
    void authWithCache() throws Exception {
        //given

        MemberResponseServiceDto targetMemberDto = new MemberResponseServiceDto(testUtils.getTestMember());

        MemberAuthResponseDto testTarget = MemberAuthResponseDto.builder()
                .isValid(true)
                .member(objectMapper.writeValueAsString(testUtils.getTestMember()))
                .ttl(10L)
                .token(testUtils.getTestToken())
                .build();

        AuthResponseDto testRes = AuthResponseDto.builder()
                .isValid(true)
                .roleID(targetMemberDto.getRoleID())
                .uid(targetMemberDto.getId())
                .build();

        //redis에서 hit,
        Mockito.doReturn(Optional.of(testTarget)).when(memberAuthService).find(testUtils.getTestToken());

        //when
        ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.get("/api/v1/auth/{token}",testUtils.getTestToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print());

        //then
        result.andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(testRes)));

        verify(memberAuthService).find(testUtils.getTestToken());
    }

    @Test
    void revoke() throws Exception {

        //given
        Mockito.doReturn(true).when(memberAuthService).revoke(testUtils.getTestToken());

        //when
        ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.delete("/api/v1/auth/{token}",testUtils.getTestToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andDo(document("revoke",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("token").description("취소할 firebase token")
                        ),
                        responseFields(
                                fieldWithPath("message").description("완료 여부")
                        ))
                );

        //then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("ok"));
        verify(memberAuthService).revoke(testUtils.getTestToken());
    }
}