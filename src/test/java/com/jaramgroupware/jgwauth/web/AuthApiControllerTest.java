package com.jaramgroupware.jgwauth.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.firebase.ErrorCode;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuthException;
import com.jaramgroupware.jgwauth.dto.auth.controllerDto.AuthFullResponseDto;
import com.jaramgroupware.jgwauth.dto.auth.controllerDto.AuthTinyResponseDto;
import com.jaramgroupware.jgwauth.dto.memberCache.servcieDto.MemberAuthAddRequestDto;
import com.jaramgroupware.jgwauth.dto.memberCache.servcieDto.MemberAuthResponseDto;
import com.jaramgroupware.jgwauth.dto.memberCache.servcieDto.TokenAuthAddRequestDto;
import com.jaramgroupware.jgwauth.service.MemberAuthServiceImpl;
import com.jaramgroupware.jgwauth.service.MemberServiceImpl;
import com.jaramgroupware.jgwauth.testConfig.EmbeddedRedisConfig;
import com.jaramgroupware.jgwauth.testConfig.TestFireBaseConfig;
import com.jaramgroupware.jgwauth.testConfig.TestRedisConfig;
import com.jaramgroupware.jgwauth.utils.TestUtils;
import com.jaramgroupware.jgwauth.dto.auth.controllerDto.AuthResponseDto;
import com.jaramgroupware.jgwauth.dto.member.serviceDto.MemberResponseServiceDto;
import com.jaramgroupware.jgwauth.utils.exception.CustomException;
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
@ContextConfiguration(classes = {TestRedisConfig.class})
@AutoConfigureRestDocs(uriScheme = "https", uriHost = "docs.api.com")
@SpringBootTest
class AuthApiControllerTest {

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

        AuthFullResponseDto testRes = AuthFullResponseDto.builder()
                .valid(true)
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
                RestDocumentationRequestBuilders.get("/api/v1/auth")
                        .header("Token",testUtils.getTestToken())
                        .queryParam("onlyToken","false")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andDo(document("auth-fully",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("valid").description("해당 토큰이 valid 한지 여부"),
                                fieldWithPath("role_id").description("해당 토큰의 권한 레벨 ID"),
                                fieldWithPath("uid").description("해당 토큰의 유저의 UID(Firebase)")
                        )
                ));

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

        AuthFullResponseDto testRes = AuthFullResponseDto.builder()
                .valid(true)
                .roleID(targetMemberDto.getRoleID())
                .uid(targetMemberDto.getId())
                .build();

        //redis에서 hit,
        Mockito.doReturn(Optional.of(testTarget)).when(memberAuthService).find(testUtils.getTestToken());

        //when
        ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.get("/api/v1/auth")
                        .header("Token",testUtils.getTestToken())
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
                RestDocumentationRequestBuilders.delete("/api/v1/auth")
                        .header("Token",testUtils.getTestToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print());

        //then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("ok"));
        verify(memberAuthService).revoke(testUtils.getTestToken());
    }

    @DisplayName("Redis에 캐싱 되어있지 않은 토큰 인증")
    @Test
    void authTokenWithNoCache() throws Exception {
        //given
        AuthTinyResponseDto testRes = AuthTinyResponseDto.builder()
                .uid(testUtils.getTestUid())
                .valid(true)
                .build();

        FireBaseResult fireBaseResult = FireBaseResult.builder()
                .uid(testUtils.getTestUid())
                .ttl(10L)
                .build();

        //redis에서 miss,
        Mockito.doReturn(Optional.empty()).when(memberAuthService).findOnlyToken(testUtils.getTestToken());

        //token 인증 후에,
        Mockito.doReturn(fireBaseResult).when(fireBaseClient).checkToken(testUtils.getTestToken());

        //캐싱
        Mockito.doReturn(true).when(memberAuthService).add(TokenAuthAddRequestDto
                .builder()
                .token(testUtils.getTestToken())
                .uid(testUtils.getTestUid())
                .ttl(fireBaseResult.getTtl())
                .build());

        //when
        ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.get("/api/v1/auth")
                        .queryParam("onlyToken","true")
                        .header("Token",testUtils.getTestToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andDo(document("auth-only-token",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("valid").description("해당 토큰이 valid 한지 여부"),
                                fieldWithPath("uid").description("해당 토큰의 유저의 UID(Firebase)")
                        )
                ));

        //then
        result.andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(testRes)));

        verify(memberAuthService).findOnlyToken(testUtils.getTestToken());
        verify(fireBaseClient).checkToken(testUtils.getTestToken());
        verify(memberAuthService).add(TokenAuthAddRequestDto
                .builder()
                .token(testUtils.getTestToken())
                .uid(testUtils.getTestUid())
                .ttl(fireBaseResult.getTtl())
                .build());
        verify(fireBaseClient).checkToken(testUtils.getTestToken());

    }

    @DisplayName("Redis에 캐싱 되어있는 토큰 인증")
    @Test
    void authTokenWithCache() throws Exception {
        //given

        AuthTinyResponseDto testRes = AuthTinyResponseDto.builder()
                .uid(testUtils.getTestUid())
                .valid(true)
                .build();

        //redis에서 hit,
        Mockito.doReturn(Optional.of(testUtils.getTestUid())).when(memberAuthService).findOnlyToken(testUtils.getTestToken());


        //when
        ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.get("/api/v1/auth")
                        .header("Token",testUtils.getTestToken())
                        .queryParam("onlyToken","true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print());

        //then
        result.andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(testRes)));

        verify(memberAuthService).findOnlyToken(testUtils.getTestToken());

    }


    @DisplayName("Redis에 캐싱 되어있는 토큰 인증 실패")
    @Test
    void authTokenWithCacheFail() throws Exception {
        //given

        AuthTinyResponseDto testRes = AuthTinyResponseDto.builder()
                .valid(false)
                .uid("")
                .build();

        //redis에서 hit,
        Mockito.doReturn(Optional.of("")).when(memberAuthService).findOnlyToken(testUtils.getTestToken());


        //when
        ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.get("/api/v1/auth")
                        .queryParam("onlyToken","true")
                        .header("Token",testUtils.getTestToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print());

        //then
        result.andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(testRes)));

        verify(memberAuthService).findOnlyToken(testUtils.getTestToken());

    }

    @DisplayName("Redis에 캐싱 되어있지 않은 토큰 인증 실패")
    @Test
    void authTokenWithNoCacheFail() throws Exception {
        //given
        AuthTinyResponseDto testRes = AuthTinyResponseDto.builder()
                .uid("")
                .valid(false)
                .build();

        //redis에서 miss,
        Mockito.doReturn(Optional.empty()).when(memberAuthService).findOnlyToken(testUtils.getTestToken());

        //token 인증 후에,
        Mockito.when(fireBaseClient.checkToken(testUtils.getTestToken()))
                .thenThrow(new FirebaseAuthException(
                        new FirebaseException(ErrorCode.CONFLICT,"error",new CustomException(com.jaramgroupware.jgwauth.utils.exception.ErrorCode.MEMBER_NOT_FOUND))));

        //when
        ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.get("/api/v1/auth")
                        .queryParam("onlyToken","true")
                        .header("Token",testUtils.getTestToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print());

        //then
        result.andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(testRes)));

        verify(memberAuthService).findOnlyToken(testUtils.getTestToken());
        verify(fireBaseClient).checkToken(testUtils.getTestToken());

    }

    @DisplayName("Redis에 캐싱 되어있지 않은 유저 인증 실패")
    @Test
    void authWithNoCacheFail() throws Exception {
        //given

        MemberResponseServiceDto targetMemberDto = new MemberResponseServiceDto(testUtils.getTestMember());

        AuthFullResponseDto testRes = AuthFullResponseDto.builder()
                .valid(false)
                .uid("")
                .roleID(-1)
                .build();



        //redis에서 miss,
        Mockito.doReturn(Optional.empty()).when(memberAuthService).find(testUtils.getTestToken());

        //token 인증 후에,
        Mockito.when(fireBaseClient.checkToken(testUtils.getTestToken()))
                .thenThrow(new FirebaseAuthException(
                        new FirebaseException(ErrorCode.CONFLICT,"error",new CustomException(com.jaramgroupware.jgwauth.utils.exception.ErrorCode.MEMBER_NOT_FOUND))));

        //when
        ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.get("/api/v1/auth")
                        .header("Token",testUtils.getTestToken())
                        .queryParam("onlyToken","false")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print());

        //then
        result.andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(testRes)));

        verify(memberAuthService).find(testUtils.getTestToken());
        verify(fireBaseClient).checkToken(testUtils.getTestToken());
    }

    @DisplayName("Redis에 캐싱 되어 있는 유저 인증 실패")
    @Test
    void authWithCacheFail() throws Exception {
        //given

        MemberAuthResponseDto testTarget = MemberAuthResponseDto.builder()
                .isValid(false)
                .member(null)
                .ttl(10L)
                .token(testUtils.getTestToken())
                .build();

        AuthFullResponseDto testRes = AuthFullResponseDto.builder()
                .valid(false)
                .roleID(-1)
                .uid("")
                .build();

        //redis에서 hit,
        Mockito.doReturn(Optional.of(testTarget)).when(memberAuthService).find(testUtils.getTestToken());

        //when
        ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.get("/api/v1/auth")
                        .header("Token",testUtils.getTestToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print());

        //then
        result.andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(testRes)));

        verify(memberAuthService).find(testUtils.getTestToken());
    }
}