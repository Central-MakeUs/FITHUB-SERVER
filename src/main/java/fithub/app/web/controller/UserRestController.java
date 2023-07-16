package fithub.app.web.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import fithub.app.base.Code;
import fithub.app.base.ResponseDto;
import fithub.app.converter.ExerciseCategoryConverter;

import fithub.app.converter.UserConverter;
import fithub.app.converter.common.BaseConverter;
import fithub.app.domain.ExerciseCategory;
import fithub.app.domain.User;
import fithub.app.service.AppleService;
import fithub.app.service.UserService;
import fithub.app.sms.dto.SmsResponseDto;
import fithub.app.sms.service.SmsService;
import fithub.app.utils.OAuthResult;
import fithub.app.utils.ResponseCode;
import fithub.app.web.dto.requestDto.UserRequestDto;
import fithub.app.web.dto.common.BaseDto;
import fithub.app.web.dto.responseDto.UserResponseDto;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

@RestController
@Validated
@RequiredArgsConstructor
public class UserRestController {

    private static final Logger logger = LoggerFactory.getLogger(UserRestController.class);

    private final UserService userService;

    private final AppleService appleService;

    private final SmsService smsService;

    @Operation(summary = "카카오 소셜 로그인", description = "카카오 소셜 로그인 API 입니다.")
    @Parameters({
            @Parameter(name = "member", hidden = true)
    })
    @ApiResponses({
            @ApiResponse(responseCode = "2004", description = "OK : 정상응답, 로그인 프로세스"),
            @ApiResponse(responseCode = "2005", description = "OK : 정상응답, 회원가입 프로세스"),
            @ApiResponse(responseCode = "5000", description = "Server Error : 똘이에게 알려주세요",content =@Content(schema =  @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/users/login/social/kakao")
    public ResponseDto<OAuthResult.OAuthResultDto> kakaoOauth(@RequestBody UserRequestDto.socialDto request){

        logger.info("/login/social/kakao 넘겨 받은  body : {}",request.toString());

        OAuthResult.OAuthResultDto result = userService.kakaoOAuth(request.getSocialId());

        Code responseCode;

        if (result.getIsLogin())
            responseCode = Code.KAKAO_OAUTH_LOGIN;
        else
            responseCode = Code.KAKAO_OAUTH_JOIN;

        return ResponseDto.of(responseCode, result);
    }

    @Operation(summary = "애플 소셜 로그인", description = "애플 소셜 로그인 API 입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "2004", description = "OK : 정상응답, 로그인 프로세스"),
            @ApiResponse(responseCode = "2005", description = "OK : 정상응답, 회원가입 프로세스"),
            @ApiResponse(responseCode = "5000", description = "Server Error : 똘이에게 알려주세요",content =@Content(schema =  @Schema(implementation = ResponseDto.class)))

    })
    @PostMapping("/users/login/social/apple")
    public ResponseDto<OAuthResult.OAuthResultDto> appleOauth(@RequestBody UserRequestDto.AppleSocialDto request) throws IOException {


        logger.info("/login/social/apple 넘겨 받은 body : {}", request.getIdentityToken());

        String identityToken = request.getIdentityToken();
        String socialId;

        socialId = appleService.userIdFromApple(identityToken);

        logger.info("userId from apple service : {}", socialId);

        OAuthResult.OAuthResultDto result = userService.appleOAuth(socialId);

        Code responseCode;

        if (result.getIsLogin())
            responseCode = Code.APPLE_OAUTH_LOGIN;
        else
            responseCode = Code.APPLE_OAUTH_JOIN;

        logger.info("애플 소셜로그인의 결과 : {}", responseCode);
        return ResponseDto.of(responseCode, result);
    }


    @Operation(summary = "닉네임 중복 검사", description = "닉네임 중복검사 API 입니다.")
    @GetMapping("/users/exist-nickname")
    @ApiResponses({
            @ApiResponse(responseCode = "2010", description = "OK : 정상응답, 닉네임이 이미 사용중인 경우!"),
            @ApiResponse(responseCode = "2011", description = "OK : 정상응답, 닉네임 사용이 가능함!"),
            @ApiResponse(responseCode = "5000", description = "Server Error : 똘이에게 알려주세요",content =@Content(schema =  @Schema(implementation = ResponseDto.class)))
    })
    public ResponseDto<String> getExistNickname(
            @ApiParam (value = "검사를 원하는 닉네임", readOnly = true) @RequestParam String nickname
    ){

        logger.info("passed nickname from front : {}", nickname);

        Code responseCode= null;

        Optional<User> user = userService.checkExistNickname(nickname);

        if (user.isPresent())
            responseCode = Code.NICKNAME_EXIST;
        else
            responseCode = Code.NICKNAME_OK;

        return ResponseDto.of(responseCode, nickname);
    }

    @Operation(summary = "운동 카테고리 리스트 API", description = "회원가입 시 선호하는 카테고리를 선택할 때 목록을 받아오기 위해 사용됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "2000", description = "OK : 정상응답")
    })
    @GetMapping("/users/exercise-category")
    public ResponseDto<List<ExerciseCategory>> getExerciseCategoryList(){
        List<ExerciseCategory> exerciseList = userService.getExerciseList();

        logger.info("운동 카테고리 리스트 : {}", exerciseList);

        return ResponseDto.of(exerciseList);
    }

    @PostMapping("/users/sign-up")
    @Operation(summary = "핸드폰 번호를 이용한 회원가입 완료 API", description = "핸드폰 번호를 이용한 회원가입 시 사용됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "2000", description = "OK : 정상응답, 성공 시 가입 한 사용자의 DB 상 id, nickname이 담긴 result 반환"),
            @ApiResponse(responseCode = "5000", description = "Server Error : 똘이에게 알려주세요",content =@Content(schema =  @Schema(implementation = ResponseDto.class)))
    })
    public ResponseDto<UserResponseDto.JoinUserDto> signUpByPhoneNum(@RequestBody UserRequestDto.UserInfo request){

        logger.info("넘겨 받은 사용자의 정보 : {}", request.toString());

        User user = userService.signUpPhoneNum(request);

        UserResponseDto.JoinUserDto createdUser = UserConverter.toJoinUserDto(user);
        return ResponseDto.of(createdUser);
    }

    @Operation(summary = "소셜로그인 회원정보 최종입력 API", description = "소셜로그인으로 가입 시 회원정보를 최종으로 기입하기 위해 사용됩니다, 토요일에 작업할게용")
    @PatchMapping("/users/sign-up/oauth")
    public ResponseEntity<BaseDto.BaseResponseDto> signUpByOAuth(@RequestBody UserRequestDto.UserOAuthInfo request){
        return null;
    }

    @Operation(summary = "핸드폰으로 전송된 인증 번호 검증 API", description = "body에 사용자의 핸드폰 번호와 인증 번호 2개를 주세요!")
    @ApiResponses({
            @ApiResponse(responseCode = "2000", description = "OK : 인증 성공"),
            @ApiResponse(responseCode = "4014", description = "UNAUTHORIZED 인증 번호가 틀린 경우", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
                @ApiResponse(responseCode = "4015", description = "UNAUTHORIZED 유효시간이 지난 경우", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "4016", description = "BAD REQUEST 인증번호를 받은 번호가 없는 경우", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "5000", description = "Server Error : 똘이에게 알려주세요",content =@Content(schema =  @Schema(implementation = ResponseDto.class)))

    })
    @PostMapping("/users/sms/auth")
    public ResponseDto<SmsResponseDto.AuthNumResultDto> authPhoneNum(@RequestBody UserRequestDto.PhoneNumAuthDto request){
        SmsResponseDto.AuthNumResultDto authNumResultDto = smsService.authNumber(request.getAuthNum(), request.getPhoneNum());
        return ResponseDto.of(authNumResultDto);
    }

    @Operation(summary = "핸드폰 인증번호 생성 API", description = "사용자의 번호를 body에 넘겨 줘 인증번호가 문자로 송신되게 합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "2000", description = "OK : 정상응답"),
            @ApiResponse(responseCode = "5000", description = "Server Error : 똘이에게 알려주세요",content =@Content(schema =  @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/users/sms")
    public ResponseDto<Integer> sendSms(@RequestBody UserRequestDto.SmsRequestDto request) throws JsonProcessingException, RestClientException, URISyntaxException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException
    {

        Integer data = smsService.sendSms(request.getTargetPhoneNum());
        return ResponseDto.of(data);
    }

    @PostMapping("/users/password")
    public ResponseDto<Integer> findPass(){
        return null;
    }

    @PatchMapping("/users/password")
    public ResponseDto<UserResponseDto.PassChangeDto> changePass(){
        return null;
    }
}

