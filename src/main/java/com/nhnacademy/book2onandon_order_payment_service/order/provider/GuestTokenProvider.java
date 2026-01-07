package com.nhnacademy.book2onandon_order_payment_service.order.provider;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GuestTokenProvider {

    private final Key key;

    public GuestTokenProvider(@Value("${guest.token.secret}") String secretKey) {

        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createToken(Long orderId) {
        return Jwts.builder()
                .setSubject("GUEST")
                .claim("orderId", orderId)
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 30)) // 30분
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Long validateTokenAndGetOrderId(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .get("orderId", Long.class);
        } catch (Exception e) {
            return null;
        }
    }
}