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
        return c;
    }
    
    public static Cookie expire(){
        Cookie c=new Cookie("Refresh",null);
        c.setPath("/"); 
        c.setMaxAge(0);
        return c;
    }
}