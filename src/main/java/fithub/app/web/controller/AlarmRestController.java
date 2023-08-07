package fithub.app.web.controller;

import fithub.app.auth.handler.annotation.AuthUser;
import fithub.app.base.ResponseDto;
import fithub.app.domain.Notification;
import fithub.app.domain.User;
import fithub.app.firebase.service.FireBaseService;
import fithub.app.service.NotificationService;
import fithub.app.utils.FCMType;
import fithub.app.web.dto.requestDto.ArticleRequestDto;
import fithub.app.web.dto.responseDto.NotificationResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Tag(name = "알람 API", description = "푸쉬 알람 API, 임시!!!!!!")
@RestController
@RequiredArgsConstructor
public class AlarmRestController {

    private final FireBaseService fireBaseService;

    private final NotificationService notificationService;


    @Operation(summary = "<테스트용> 알림 보내보기 API V2 - 인증 아이디2번, 내용이 인증 1인 인증으로 이동  + 사진도 포함  ✔️🔑",description = "테스트임다.")
    @ApiResponses({
            @ApiResponse(responseCode = "2000", description = "OK : 정상응답"),
            @ApiResponse(responseCode = "4031", description = "NOT_FOUND : 게시글이 없습니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "5000", description = "Server Error : 똘이에게 알려주세요",content =@Content(schema =  @Schema(implementation = ResponseDto.class)))
    })
    @Parameters({
            @Parameter(name = "user", hidden = true),
    })
    @PostMapping("/test-alarm/v2")
    public String testFCMV3(@RequestBody ArticleRequestDto.ArticleLikeAlarmDto request) throws IOException
    {
        fireBaseService.sendMessageToV2(request.getToken(),"test record","운동인증으로 이동 테스트 알림", FCMType.RECORD.toString(),"2");
        return null;
    }
//
//    @Operation(summary = "내 알림목록 조회 API ✔️🔑",description = "pageIndex로 페이징")
//    @ApiResponses({
//            @ApiResponse(responseCode = "2000", description = "OK : 정상응답"),
//            @ApiResponse(responseCode = "5000", description = "Server Error : 똘이에게 알려주세요",content =@Content(schema =  @Schema(implementation = ResponseDto.class)))
//    })
//    @Parameters({
//            @Parameter(name = "user", hidden = true),
//    })
//    @GetMapping("/users/alarms")
//    public ResponseDto<NotificationResponseDto.AlarmDto> showAlarm(@AuthUser User user, @RequestParam(name = "pageIndex") Integer pageIndex){
//        Page<Notification> notification = notificationService.getNotification(user, pageIndex);
//
//    }
}
