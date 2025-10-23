package com.sist.backend.jwt;

import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtProvider {
    @Value("${jwt.secret}")
    private String secretKeyCode;
    
    private SecretKey secretKey;

    public SecretKey getSecretKey() {
        if(secretKey == null){
            String encoding = Base64.getEncoder().encodeToString(secretKeyCode.getBytes());
            // secretKey는 숫자와 문자들을 맘대로 길게 기술한 것.(별 의미 없이))
            //그 값을 가지고 jwt키를 만들어야 한다
            //jjwt 라이브러리를 사용하면 편하다
            secretKey = Keys.hmacShaKeyFor(encoding.getBytes());
        }
        return secretKey;
    }

    public String getToken(Map<String, Object> map, int seconds){
        long now = new Date().getTime();

        Date accessTokenExpiresIn = new Date(now+1000L*seconds);
        // 1000L*seconds : 밀리초로 변환
        JwtBuilder jwtBuilder = Jwts.builder().subject("SIST").expiration(accessTokenExpiresIn);
        Set<String> keys = map.keySet();//반복자 처리하기 위해 Set구조화
        Iterator<String> it = keys.iterator();
        while(it.hasNext()){
            String key = it.next();
            Object value = map.get(key);
            jwtBuilder.claim(key, value.toString());
            /*
             * JWT (Json Web Token)은 크게 3가지 영역으로 나눠져있다.
             * 1. Header(알고리즘,타입)
             * 2. Payload(토큰에 담긴 데이터)
             * 3. Singature(서명)
             * 이 중 Payload 안에 들어있는 정보(data)단위를 Claim이라 한다.
             */
        }//반복의 끝
        String jwt = jwtBuilder.signWith(getSecretKey()).compact();

        return jwt;
    }

    public boolean verify(String token){
        boolean value = true;
        try{
            Jwts.parser().verifyWith(getSecretKey()).build().parseSignedClaims(token);
        }catch(Exception e){
            e.printStackTrace();
            //유효기간이 만료되면 예외 발생됨
            value = false;
        }
        return value;
    }

    //토큰에 담긴 사용자 정보(Claim)를 반환한다.
    public Map<String, Object> getClaims(String token){
        return Jwts.parser().verifyWith(getSecretKey()).build().parseSignedClaims(token).getPayload();
    }

    public boolean inspertiontoken(String id ,String accessToken , String refreshToken){
        if(!verify(accessToken)){
            // accessToken 만료되었을 때

            if(!verify(refreshToken)){
                // 두 토큰 다 만료되었을 때
                // 로그인 페이지로 이동
                return false;
            }else{
                // accessToken 만료되었을 때
                // accessToken 재발급
                Object tokenID = getClaims(refreshToken).get("tokenID");
                String tokenIDString = null;
                if(tokenID != null){
                    tokenIDString = tokenID.toString();
                }
            }
            
        }

        return true;
    }
}
