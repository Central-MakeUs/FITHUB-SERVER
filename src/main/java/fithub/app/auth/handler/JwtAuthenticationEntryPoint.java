package fithub.app.auth.handler;

import fithub.app.auth.filter.JwtRequestFilter;
import fithub.app.exception.common.ApiErrorResult;
import fithub.app.exception.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        response.setContentType("application/json; charset=UTF-8");
        response.setStatus(401);
        PrintWriter writer = response.getWriter();
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        ApiErrorResult apiErrorResult = ApiErrorResult.builder()
                .code(errorCode)
                .message(errorCode.getMessage())
                .result(JwtRequestFilter.class.getName())
                .build();
        try {
            writer.write(apiErrorResult.toString());
        }catch (NullPointerException e){
            LOGGER.error("응답 메시지 작성 에러", e);
        }finally {
            if(writer != null){
                writer.flush();
                writer.close();
            }
        }
    }
}
