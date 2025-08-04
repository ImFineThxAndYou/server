package org.example.howareyou.global.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * HTTP 요청에서 User-Agent 및 클라이언트 IP 주소를 추출하는 유틸리티 클래스
 */
public class UserAgentUtils {
    
    // HTTP 헤더 상수
    private static final String[] IP_HEADER_CANDIDATES = {
        "X-Forwarded-For",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED",
        "HTTP_VIA",
        "REMOTE_ADDR"
    };

    private UserAgentUtils() {
        // 인스턴스화 방지
    }

    /**
     * HTTP 요청에서 User-Agent 헤더 값을 가져옵니다.
     *
     * @param request HTTP 요청 객체
     * @return User-Agent 문자열 (없을 경우 "unknown" 반환)
     */
    public static String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return StringUtils.hasText(userAgent) ? userAgent : "unknown";
    }

    /**
     * HTTP 요청에서 클라이언트의 실제 IP 주소를 추출합니다.
     *
     * @param request HTTP 요청 객체
     * @return 클라이언트 IP 주소 (알 수 없는 경우 "unknown" 반환)
     */
    public static String getClientIP(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        // 다양한 헤더에서 IP 주소 시도
        for (String header : IP_HEADER_CANDIDATES) {
            String ipAddress = request.getHeader(header);
            if (StringUtils.hasText(ipAddress) && !"unknown".equalsIgnoreCase(ipAddress)) {
                // X-Forwarded-For의 경우 첫 번째 IP가 실제 클라이언트 IP
                if (ipAddress.contains(",")) {
                    ipAddress = ipAddress.split(",")[0].trim();
                }
                return ipAddress;
            }
        }

        // 모든 헤더에서 IP를 찾지 못한 경우 기본 값 반환
        return request.getRemoteAddr();
    }
}
