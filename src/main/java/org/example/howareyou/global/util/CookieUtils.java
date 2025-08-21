package org.example.howareyou.global.util;

import jakarta.servlet.http.Cookie;

/** Refresh 쿠키 생성 / 삭제 헬퍼 */
public final class CookieUtils {
    private CookieUtils(){}
    
    public static Cookie refresh(String rt, boolean secure){
        Cookie c=new Cookie("Refresh",rt);
        c.setHttpOnly(false); // JavaScript에서 읽을 수 있도록
        c.setPath("/");
        c.setSecure(secure); 
        c.setMaxAge(14*24*60*60);
        
        // SameSite 설정 추가 (프로덕션에서는 Strict, 개발에서는 Lax)
        if (secure) {
            // 프로덕션 환경: SameSite=Strict (크로스 사이트 요청 차단)
            c.setAttribute("SameSite", "Strict");
        } else {
            // 개발 환경: SameSite=Lax (OAuth2 리다이렉트 허용)
            c.setAttribute("SameSite", "Lax");
        }
        
        return c;
    }
    
    public static Cookie expire(){
        Cookie c=new Cookie("Refresh",null);
        c.setPath("/"); 
        c.setMaxAge(0);
        c.setSecure(false); // 삭제 시에는 secure 해제
        c.setAttribute("SameSite", "Lax"); // 삭제 시에는 Lax로 설정
        return c;
    }
}