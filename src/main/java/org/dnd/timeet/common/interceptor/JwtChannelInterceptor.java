package org.dnd.timeet.common.interceptor;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dnd.timeet.common.security.CustomUserDetails;
import org.dnd.timeet.common.security.JwtProvider;
import org.dnd.timeet.member.application.MemberFindService;
import org.dnd.timeet.member.domain.Member;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * WebSocket 채널에 JWT 검증하는 인터셉터
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final MemberFindService userUtilityService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        // 연결 요청시 JWT 검증
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Authorization 헤더 추출
            List<String> authorization = accessor.getNativeHeader(JwtProvider.HEADER);
            if (authorization != null && !authorization.isEmpty()) {
                String jwt = authorization.get(0).substring(JwtProvider.TOKEN_PREFIX.length());
                try {
                    // JWT 토큰 검증
                    DecodedJWT decodedJWT = JwtProvider.verify(jwt);
                    Long id = decodedJWT.getClaim("id").asLong();
                    // 사용자 정보 조회
                    Member member = userUtilityService.getUserById(id);

                    // 사용자 인증 정보 설정
                    CustomUserDetails userDetails = new CustomUserDetails(member);
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (JWTVerificationException e) {
                    log.error("JWT Verification Failed: " + e.getMessage());
                    return null;
                } catch (Exception e) {
                    log.error("An unexpected error occurred: " + e.getMessage());
                    return null;
                }
            } else {
                // 클라이언트 측 타임아웃 처리
                log.error("Authorization header is not found");
                return null;
            }
        }
        return message;
    }
}


