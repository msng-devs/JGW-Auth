package com.jaramgroupware.auth.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaramgroupware.auth.dto.member.serviceDto.MemberResponseServiceDto;
import com.jaramgroupware.auth.dto.token.serviceDto.PublishAccessTokenResponseServiceDto;
import com.jaramgroupware.auth.dto.token.serviceDto.PublishTokenRequestServiceDto;
import com.jaramgroupware.auth.dto.token.serviceDto.PublishTokenResponseServiceDto;
import com.jaramgroupware.auth.exceptions.firebase.FireBaseErrorCode;
import com.jaramgroupware.auth.exceptions.firebase.FirebaseApiException;
import com.jaramgroupware.auth.exceptions.jgwauth.JGWAuthErrorCode;
import com.jaramgroupware.auth.exceptions.jgwauth.JGWAuthException;
import com.jaramgroupware.auth.firebase.FireBaseApiImpl;
import com.jaramgroupware.auth.firebase.FireBaseTokenInfo;
import com.jaramgroupware.auth.service.MemberServiceImpl;
import com.jaramgroupware.auth.service.TokenServiceImpl;
import com.jaramgroupware.auth.testConfig.TestJackson2ObjectConfig;
import com.jaramgroupware.auth.testConfig.TestRSAConfig;
import com.jaramgroupware.auth.testUtils.TestUtils;
import com.jaramgroupware.auth.utlis.jwt.JwtTokenInfo;
import com.jaramgroupware.auth.utlis.jwt.JwtTokenVerifyInfo;
import com.jaramgroupware.auth.utlis.jwt.TokenManagerImpl;
import jakarta.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Description;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.restdocs.cookies.CookieDocumentation.responseCookies;
import static org.springframework.restdocs.cookies.CookieDocumentation.cookieWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@ComponentScan
@AutoConfigureRestDocs(uriScheme = "https", uriHost = "docs.jaram.api")
@AutoConfigureMockMvc
@Import({TestRSAConfig.class, TestJackson2ObjectConfig.class})
@WebMvcTest(AuthApiController.class)
class AuthApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private TokenManagerImpl tokenManager;

    @MockBean
    private TokenServiceImpl tokenService;

    @MockBean
    private MemberServiceImpl memberService;

    @MockBean
    private FireBaseApiImpl fireBaseApi;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final TestUtils testUtils = new TestUtils();
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    @BeforeEach
    void setUp() {

    }

    @Test
    @Description("authorizationIdTokenAndPublishTokens - 올바른 firebase id token이 주어지면, 신규 토큰을 발급하고 해당 정보를 리턴한다.")
    void authorizationIdTokenAndPublishTokens() throws Exception {
        //given
        var testUser = testUtils.getTestMember();
        var testIdToken = "thisIsValidToken";


        var testUid = testUser.getId();
        var testFireBaseTokenInfo = FireBaseTokenInfo.builder()
                .idToken(testIdToken)
                .uid(testUid)
                .expireDateTime(LocalDateTime.now())
                .build();
        var testPublishTokenRequestDto = PublishTokenRequestServiceDto.builder()
                .email(testUser.getEmail())
                .roleID(testUser.getRole())
                .userUID(testUser.getId())
                .build();

        var testAccessToken = "thisIsTestAccessToken";
        var testRefreshToken = "thisIsTestRefreshToken";
        var testDate = new Date();

        var testTokens = PublishTokenResponseServiceDto.builder()
                .accessToken(testAccessToken)
                .accessTokenExpired(testDate)
                .refreshToken(testRefreshToken)
                .refreshTokenExpired(testDate)
                .build();

        doReturn(testFireBaseTokenInfo).when(fireBaseApi).checkToken(testIdToken);
        doReturn(MemberResponseServiceDto.builder()
                .uid(testUser.getId())
                .email(testUser.getEmail())
                .roleId(testUser.getRole()).build()).when(memberService).findUserByUid(testUid);
        doReturn(testTokens).when(tokenService).publishToken(testPublishTokenRequestDto);

        //when
        ResultActions result = mvc.perform(
                post("/api/v2/auth/authorization")
                        .queryParam("idToken",testIdToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andDo(document("authorization-success",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("access_token").description("새롭게 발급된 access token 입니다."),
                                fieldWithPath("access_token_expired").description("발급된 access token이 만료되는 시간 입니다."),
                                fieldWithPath("refresh_token_expired").description("발급된 refresh token이 만료되는 시간 입니다.")

                        ),
                        responseCookies(
                                cookieWithName("jgwrefresh").description("새롭게 발급된 refresh token은 HttpOnly Secure가 지정되어 쿠키로 저장됩니다.")
                        )
                ));

        //then
        result.andExpect(status().isOk())
                .andExpect(cookie().exists("jgwrefresh"))
                .andExpect(jsonPath("$.access_token").value(testAccessToken))
                .andExpect(jsonPath("$.access_token_expired").value(simpleDateFormat.format(testDate)))
                .andExpect(jsonPath("$.refresh_token_expired").value(simpleDateFormat.format(testDate)));

        verify(fireBaseApi).checkToken(testIdToken);
        verify(fireBaseApi).revokeToken(testUid);
        verify(tokenService).blockFirebaseIdToken(testFireBaseTokenInfo);
        verify(memberService).findUserByUid(testUid);
        verify(tokenService).publishToken(testPublishTokenRequestDto);
    }

    @Test
    @Description("authorizationIdTokenAndPublishTokens - 올바르지 않은 firebase id token이 주어지면, FireBaseApiException(NOT_VALID_TOKEN)을 발생시키고 403을 리턴한다.")
    void authorizationIdTokenAndPublishTokens2() throws Exception {
        //given
        var testIdToken = "thisIsNotValidToken";
        doThrow(new FirebaseApiException(FireBaseErrorCode.NOT_VALID_TOKEN)).when(fireBaseApi).checkToken(testIdToken);

        //when
        ResultActions result = mvc.perform(
                post("/api/v2/auth/authorization")
                        .queryParam("idToken",testIdToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andDo(document("authorization-fail-NOT_VALID_TOKEN",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("status").description("HTTP code"),
                                fieldWithPath("title").description("오류 제목"),
                                fieldWithPath("detail").description("오류 상세 설명")

                        )
                ));

        //then
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value(FireBaseErrorCode.NOT_VALID_TOKEN.getTitle()));
        verify(fireBaseApi).checkToken(testIdToken);
    }

    @Test
    @Description("authorizationIdTokenAndPublishTokens - 올바른 firebase id token이지만, 이메일 인증이 되지 않았다면 FireBaseApiException(NOT_VERIFIED_EMAIL)을 발생시키고 403을 리턴한다.")
    void authorizationIdTokenAndPublishTokens3() throws Exception {
        //given
        var testIdToken = "thisIsNotValidToken";
        doThrow(new FirebaseApiException(FireBaseErrorCode.NOT_VERIFIED_EMAIL)).when(fireBaseApi).checkToken(testIdToken);

        //when
        ResultActions result = mvc.perform(
                post("/api/v2/auth/authorization")
                        .queryParam("idToken",testIdToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andDo(document("authorization-fail-NOT_VERIFIED_EMAIL",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("status").description("HTTP code"),
                                fieldWithPath("title").description("오류 제목"),
                                fieldWithPath("detail").description("오류 상세 설명")

                        )
                ));

        //then
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value(FireBaseErrorCode.NOT_VERIFIED_EMAIL.getTitle()));
        verify(fireBaseApi).checkToken(testIdToken);
    }

    @Test
    @Description("authorizationIdTokenAndPublishTokens - 올바른 firebase id token이지만 DB에 해당 유저 정보가 존재하지 않다면, JGWAuthException(NOT_FOUND_USER)을 발생시키고 403을 리턴한다.")
    void authorizationIdTokenAndPublishTokens4() throws Exception {
        //given
        var testUser = testUtils.getTestMember();
        var testIdToken = "thisIsValidToken";


        var testUid = testUser.getId();
        var testFireBaseTokenInfo = FireBaseTokenInfo.builder()
                .idToken(testIdToken)
                .uid(testUid)
                .expireDateTime(LocalDateTime.now())
                .build();


        doReturn(testFireBaseTokenInfo).when(fireBaseApi).checkToken(testIdToken);
        doThrow(new JGWAuthException(JGWAuthErrorCode.NOT_FOUND_USER,"해당 유저 정보 조회에 실패했습니다. 회원가입 절차를 완료해주세요.")).when(memberService).findUserByUid(testUid);


        //when
        ResultActions result = mvc.perform(
                post("/api/v2/auth/authorization")
                        .queryParam("idToken",testIdToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andDo(document("authorization-fail-NOT_FOUND_USER",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("status").description("HTTP code"),
                                fieldWithPath("title").description("오류 제목"),
                                fieldWithPath("detail").description("오류 상세 설명")

                        )
                ));

        //then
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value(JGWAuthErrorCode.NOT_FOUND_USER.getTitle()));

        verify(fireBaseApi).checkToken(testIdToken);
        verify(fireBaseApi).revokeToken(testUid);
        verify(tokenService).blockFirebaseIdToken(testFireBaseTokenInfo);
        verify(memberService).findUserByUid(testUid);
    }

    @Test
    @Description("publishAccessToken - 올바른 refresh token이라면, AccessToken을 발급한다.")
    void testPublishAccessToken() throws Exception {
        //given
        var testDate = new Date();

        var testMember = testUtils.getTestMember();

        var jwtTokenInfo = JwtTokenInfo
                .builder()
                .isAccessToken(false)
                .email(testMember.getEmail())
                .uid(testMember.getId())
                .expiredAt(testDate)
                .role(testMember.getRole())
                .build();

        String testRefreshToken = "thisIsValidRefreshToken!";

        String testNewAccessToken = "thisIsNewAccessToken!";

        var cookie = new Cookie("jgwrefresh",testRefreshToken);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath("/");

        var testAccessTokenResult = PublishAccessTokenResponseServiceDto.builder()
                .accessToken(testNewAccessToken)
                .accessTokenExpired(testDate)
                .build();

        doReturn(testMember.getId()).when(tokenService).checkRefreshToken(testRefreshToken);
        doReturn(jwtTokenInfo).when(tokenManager).decodeToken(testRefreshToken);
        doReturn(testAccessTokenResult).when(tokenService).publishAccessToken(
                PublishTokenRequestServiceDto.builder()
                        .userUID(jwtTokenInfo.getUid())
                        .roleID(jwtTokenInfo.getRole())
                        .email(jwtTokenInfo.getEmail())
                        .build());

        //when
        ResultActions result = mvc.perform(
                post("/api/v2/auth/accessToken")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andDo(document("accessToken-success",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("access_token").description("신규 발급된 AccessToken"),
                                fieldWithPath("access_token_expired").description("발급된 AccessToken의 유효시간")

                        )
                ));
        //then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value(testNewAccessToken))
                .andExpect(jsonPath("$.access_token_expired").value(simpleDateFormat.format(testDate)));

        verify(tokenService).checkRefreshToken(testRefreshToken);
        verify(tokenManager).decodeToken(testRefreshToken);
        verify(tokenService).publishAccessToken(
                PublishTokenRequestServiceDto.builder()
                        .userUID(jwtTokenInfo.getUid())
                        .roleID(jwtTokenInfo.getRole())
                        .email(jwtTokenInfo.getEmail())
                        .build());
    }

    @Test
    @Description("publishAccessToken - DB에 존재하지 않는 refresh token이라면,NOT_VALID_TOKEN을 리턴한다.")
    void testPublishAccessToken2() throws Exception {
        //given
        String testRefreshToken = "thisIsValidRefreshToken!";

        var testDate = new Date();

        var testMember = testUtils.getTestMember();

        var jwtTokenInfo = JwtTokenInfo
                .builder()
                .isAccessToken(false)
                .email(testMember.getEmail())
                .uid(testMember.getId())
                .expiredAt(testDate)
                .role(testMember.getRole())
                .build();


        var cookie = new Cookie("jgwrefresh",testRefreshToken);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath("/");

        doReturn(jwtTokenInfo).when(tokenManager).decodeToken(testRefreshToken);
        doThrow(new JGWAuthException(JGWAuthErrorCode.NOT_VALID_TOKEN,"주어진 refresh 토큰이 유효하지 않습니다. 다시 로그인해주세요.")).when(tokenService).checkRefreshToken(testRefreshToken);

        //when
        ResultActions result = mvc.perform(
                post("/api/v2/auth/accessToken")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andDo(document("accessToken-fail-NOT_VALID_TOKEN",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("status").description("HTTP code"),
                                fieldWithPath("title").description("오류 제목"),
                                fieldWithPath("detail").description("오류 상세 설명")

                        )
                ));
        //then
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value(JGWAuthErrorCode.NOT_VALID_TOKEN.getTitle()));
        verify(tokenService).checkRefreshToken(testRefreshToken);
    }

    @Test
    @Description("publishAccessToken - 해당 refresh token이 valid 하지 않다면,NOT_VALID_TOKEN을 리턴한다.")
    void testPublishAccessToken3() throws Exception {
        //given
        String testRefreshToken = "thisIsValidRefreshToken!";


        var cookie = new Cookie("jgwrefresh",testRefreshToken);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath("/");

        doThrow(new JGWAuthException(JGWAuthErrorCode.NOT_VALID_TOKEN,"주어진 refresh 토큰이 유효하지 않습니다. 다시 로그인해주세요.")).when(tokenManager).decodeToken(testRefreshToken);

        //when
        ResultActions result = mvc.perform(
                post("/api/v2/auth/accessToken")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andDo(document("accessToken-fail-NOT_VALID_TOKEN2",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("status").description("HTTP code"),
                                fieldWithPath("title").description("오류 제목"),
                                fieldWithPath("detail").description("오류 상세 설명")

                        )
                ));
        //then
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value(JGWAuthErrorCode.NOT_VALID_TOKEN.getTitle()));
        verify(tokenManager).decodeToken(testRefreshToken);
    }

    @Test
    @Description("checkToken - valid한 accessToken이 주어졌다면, 해당 인증 정보를 리턴한다.")
    void testCheckToken() throws Exception {
        //given
        String testAccessToken = "thisIs.ValidAccess.Token";
        var testDate = new Date();
        var testMember = testUtils.getTestMember();
        var jwtTokenInfo = JwtTokenInfo
                .builder()
                .isAccessToken(false)
                .email(testMember.getEmail())
                .uid(testMember.getId())
                .expiredAt(testDate)
                .role(testMember.getRole())
                .build();

        doReturn(jwtTokenInfo).when(tokenManager).decodeToken(testAccessToken);
        //when
        ResultActions result = mvc.perform(
                get("/api/v2/auth/checkAccessToken")
                        .queryParam("accessToken",testAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andDo(document("checkAccessToken-success",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("uid").description("해당 토큰 소유자의 uid, 유효하지 않으면 null 리턴"),
                                fieldWithPath("role_id").description("해당 토큰의 role 정보, 유효하지 않으면 null 리턴"),
                                fieldWithPath("valid").description("유효한지 여부")

                        )
                ));
        //then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value(testMember.getId()))
                .andExpect(jsonPath("$.role_id").value(testMember.getRole()))
                .andExpect(jsonPath("$.valid").value(true));
        verify(tokenManager).decodeToken(testAccessToken);
        verify(tokenService).checkAccessToken(testAccessToken);
    }

    @Test
    @Description("checkToken - decode 불가능한 토큰일경우 , NOT_VALID_TOKEN가 발생한다.")
    void testCheckToken2() throws Exception {
        //given
        String testAccessToken = "thisIs.Cannotdecode.Token";

        doThrow(new JGWAuthException(JGWAuthErrorCode.NOT_VALID_TOKEN,"유효하지 않은 토큰입니다. 다시 로그인해주세요.")).when(tokenManager).decodeToken(testAccessToken);

        //when
        ResultActions result = mvc.perform(
                get("/api/v2/auth/checkAccessToken")
                        .queryParam("accessToken",testAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andDo(document("checkAccessToken-fail-NOT_VALID_TOKEN",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("uid").description("해당 토큰 소유자의 uid, 유효하지 않으면 null 리턴"),
                                fieldWithPath("role_id").description("해당 토큰의 role 정보, 유효하지 않으면 null 리턴"),
                                fieldWithPath("valid").description("유효한지 여부")

                        )
                ));
        //then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
        verify(tokenManager).decodeToken(testAccessToken);
    }

    @Test
    @Description("checkToken - black list에 등록된 access token이 주어지면, NOT_VALID_TOKEN을 리턴한다.")
    void testCheckToken3() throws Exception {
        //given
        String testAccessToken = "thisIs.ValidAccess.Token";
        var testDate = new Date();
        var testMember = testUtils.getTestMember();
        var jwtTokenInfo = JwtTokenInfo
                .builder()
                .isAccessToken(false)
                .email(testMember.getEmail())
                .uid(testMember.getId())
                .expiredAt(testDate)
                .role(testMember.getRole())
                .build();

        doReturn(jwtTokenInfo).when(tokenManager).decodeToken(testAccessToken);
        doThrow(new JGWAuthException(JGWAuthErrorCode.NOT_VALID_TOKEN,"사용 불가능한 토큰입니다. 다시 로그인해주세요.")).when(tokenService).checkAccessToken(testAccessToken);
        //when
        ResultActions result = mvc.perform(
                get("/api/v2/auth/checkAccessToken")
                        .queryParam("accessToken",testAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print());
        //then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
        verify(tokenManager).decodeToken(testAccessToken);
        verify(tokenService).checkAccessToken(testAccessToken);
    }

    @Test
    @Description("revokeTokens - 정상적인 refresh token과 accss token이 주어지면, 성공적으로 삭제했다는 메시지를 리턴한다.")
    void testRevokeTokens() throws Exception {
        //given
        String testAccessToken = "thisIs.ValidAccess.Token";
        String testRefreshToken = "thisIs.ValidRefresh.Token";
        var cookie = new Cookie("jgwrefresh",testRefreshToken);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        var testDate = new Date();
        var testMember = testUtils.getTestMember();

        var jwtTokenInfo = JwtTokenInfo.builder()
                .isAccessToken(false)
                .email(testMember.getEmail())
                .uid(testMember.getId())
                .expiredAt(testDate)
                .role(testMember.getRole())
                .build();


        doReturn(jwtTokenInfo).when(tokenManager).verifyToken(testRefreshToken,false);
        doReturn(true).when(tokenService).revokeRefreshToken(testRefreshToken);
        doReturn(jwtTokenInfo).when(tokenManager).verifyToken(testAccessToken, JwtTokenVerifyInfo.builder()
                        .uid(jwtTokenInfo.getUid())
                        .email(jwtTokenInfo.getEmail())
                        .isAccessToken(true)
                        .build());

        //when
        ResultActions result = mvc.perform(
                delete("/api/v2/auth/revoke")
                        .queryParam("accessToken",testAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .cookie(cookie))
                .andDo(print())
                .andDo(document("revokeTokens-success",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("message").description("처리 결과")
                        )
                ));
        //then
        result.andExpect(status().isOk())
                .andExpect(cookie().maxAge("jgwrefresh",0))
                .andExpect(jsonPath("$.message").value("성공적으로 모든 토큰을 삭제했습니다."));

        verify(tokenManager).verifyToken(testRefreshToken,false);
        verify(tokenService).revokeRefreshToken(testRefreshToken);
        verify(tokenManager).verifyToken(testAccessToken, JwtTokenVerifyInfo.builder()
                .uid(jwtTokenInfo.getUid())
                .email(jwtTokenInfo.getEmail())
                .isAccessToken(true)
                .build());
        verify(tokenService).revokeAccessToken(testAccessToken,jwtTokenInfo.getUid(),jwtTokenInfo.getExpiredAt());
    }

    @Test
    @Description("revokeTokens - 정상적인 refresh token과 비정상적인 accss token이 주어지면, 일부 토큰만 삭제했다는 메시지를 리턴한다.")
    void testRevokeTokens2() throws Exception {
        //given
        String testAccessToken = "thisIs.NotValidAccess.Token";
        String testRefreshToken = "thisIs.ValidRefresh.Token";
        var cookie = new Cookie("jgwrefresh",testRefreshToken);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        var testDate = new Date();
        var testMember = testUtils.getTestMember();

        var jwtTokenInfo = JwtTokenInfo.builder()
                .isAccessToken(false)
                .email(testMember.getEmail())
                .uid(testMember.getId())
                .expiredAt(testDate)
                .role(testMember.getRole())
                .build();


        doReturn(jwtTokenInfo).when(tokenManager).verifyToken(testRefreshToken,false);
        doReturn(true).when(tokenService).revokeRefreshToken(testRefreshToken);
        doThrow(new JGWAuthException(JGWAuthErrorCode.NOT_VALID_TOKEN,"유효하지 않은 토큰입니다. 다시 로그인해주세요."))
                .when(tokenManager).verifyToken(testAccessToken, JwtTokenVerifyInfo.builder()
                .uid(jwtTokenInfo.getUid())
                .email(jwtTokenInfo.getEmail())
                .isAccessToken(true)
                .build());

        //when
        ResultActions result = mvc.perform(
                delete("/api/v2/auth/revoke")
                        .queryParam("accessToken",testAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .cookie(cookie))
                .andDo(print())
                .andDo(document("revokeTokens-half-success",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("message").description("처리 결과")
                        )
                ));

        //then
        result.andExpect(status().is(203))
                .andExpect(cookie().maxAge("jgwrefresh",0))
                .andExpect(jsonPath("$.message").value("일부 토큰을 성공적으로 취소 했습니다."));

        verify(tokenManager).verifyToken(testRefreshToken,false);
        verify(tokenService).revokeRefreshToken(testRefreshToken);
        verify(tokenManager).verifyToken(testAccessToken, JwtTokenVerifyInfo.builder()
                .uid(jwtTokenInfo.getUid())
                .email(jwtTokenInfo.getEmail())
                .isAccessToken(true)
                .build());
    }

    @Test
    @Description("revokeTokens - 비정상적인 refresh token이 주어지면, AccessToken의 valid 여부와 상관 없이 유효하지 않은 토큰이라는 메시지를 리턴한다.")
    void testRevokeTokens3() throws Exception {
        //given
        String testAccessToken = "thisIs.NotValidAccess.Token";
        String testRefreshToken = "thisIs.NotValidRefresh.Token";

        var cookie = new Cookie("jgwrefresh",testRefreshToken);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath("/");

        doThrow(new JGWAuthException(JGWAuthErrorCode.NOT_VALID_TOKEN,"유효하지 않은 토큰입니다. 다시 로그인해주세요.")).when(tokenManager).verifyToken(testRefreshToken,false);

        //when
        ResultActions result = mvc.perform(
                delete("/api/v2/auth/revoke")
                        .queryParam("accessToken",testAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .cookie(cookie))
                .andDo(print())
                .andDo(document("revokeTokens-fail",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("message").description("처리 결과")
                        )
                ));

        //then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("유효하지 않은 토큰입니다."));


        verify(tokenManager).verifyToken(testRefreshToken,false);
    }

    @Test
    @Description("checkIdToken - 올바른 id token이 주어지면, 해당 uid를 리턴한다.")
    void checkIdToken() throws Exception {
        //given
        var testUser = testUtils.getTestMember();
        var testIdToken = "thisIsValidToken";


        var testUid = testUser.getId();
        var testFireBaseTokenInfo = FireBaseTokenInfo.builder()
                .idToken(testIdToken)
                .uid(testUid)
                .expireDateTime(LocalDateTime.now())
                .build();

        doReturn(testFireBaseTokenInfo).when(fireBaseApi).checkToken(testIdToken);

        //when
        ResultActions result = mvc.perform(
                get("/api/v2/auth/checkIdToken")
                        .queryParam("idToken",testIdToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andDo(document("checkIdToken-success",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("uid").description("해당 id token의 uid (유효하지 않을 경우 null 리턴)"),
                                fieldWithPath("valid").description("유효한지 여부")
                        )
                ));
        //then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value(testUid));
        verify(fireBaseApi).checkToken(testIdToken);
    }

    @Test
    @Description("checkIdToken - 올바르지 않은 id token이 주어지면, JGWAuthException이 발생한다.")
    void checkIdToken2() throws Exception {
        //given

        var testIdToken = "thisIsNotValidToken";

        doThrow(new FirebaseApiException(FireBaseErrorCode.NOT_VALID_TOKEN)).when(fireBaseApi).checkToken(testIdToken);

        //when
        ResultActions result = mvc.perform(
                get("/api/v2/auth/checkIdToken")
                        .queryParam("idToken",testIdToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andDo(document("checkIdToken-fail",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("uid").description("해당 id token의 uid (유효하지 않을 경우 null 리턴)"),
                                fieldWithPath("valid").description("유효한지 여부")
                        )
                ));
        //then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
        verify(fireBaseApi).checkToken(testIdToken);
    }
}