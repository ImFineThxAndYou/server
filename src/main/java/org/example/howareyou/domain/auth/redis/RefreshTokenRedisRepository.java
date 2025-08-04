package org.example.howareyou.domain.auth.redis;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import java.time.Duration;

/** RefreshToken(jti) ↔ userId  저장 · 조회 · 삭제 */
@Repository @RequiredArgsConstructor
public class RefreshTokenRedisRepository {

    private final StringRedisTemplate redis;
    private static final String KEY="rt:";

    @Value("${jwt.secret}")
    private String secretKey;


    private String jti(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey.getBytes())
                .parseClaimsJws(token)
                .getBody();

        return claims.getId(); // jti 값
    }
    public void store(String token,String uid){
        redis.opsForValue().set(KEY+jti(token),uid, Duration.ofDays(14));
    }
    public boolean exists(String token){
        return Boolean.TRUE.equals(redis.hasKey(KEY+jti(token)));
    }
    public void delete(String token){
        redis.delete(KEY+jti(token));
    }
}