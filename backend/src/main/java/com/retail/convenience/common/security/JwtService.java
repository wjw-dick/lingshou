package com.retail.convenience.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtService {

    private final SecretKey key;
    private final Duration expire;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expire-hours:12}") long expireHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expire = Duration.ofHours(expireHours);
    }

    public String create(LoginUser user) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        user.setTokenId(jti);
        Instant now = Instant.now();
        return Jwts.builder()
                .id(jti)
                .subject(String.valueOf(user.getUserId()))
                .claim("username", user.getUsername())
                .claim("realName", user.getRealName())
                .claim("userType", user.getUserType())
                .claim("roleCode", user.getRoleCode())
                .claim("tenantId", user.getTenantId())
                .claim("tenantCode", user.getTenantCode())
                .claim("storeId", user.getStoreId())
                .claim("supplierId", user.getSupplierId())
                .claim("franchiseeId", user.getFranchiseeId())
                .claim("memberId", user.getMemberId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expire)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public Duration getExpire() {
        return expire;
    }

    public LoginUser toLoginUser(Claims c) {
        LoginUser u = new LoginUser();
        u.setUserId(Long.valueOf(c.getSubject()));
        u.setUsername(c.get("username", String.class));
        u.setRealName(c.get("realName", String.class));
        u.setUserType(c.get("userType", String.class));
        u.setRoleCode(c.get("roleCode", String.class));
        u.setTenantId(asLong(c.get("tenantId")));
        u.setTenantCode(c.get("tenantCode", String.class));
        u.setStoreId(asLong(c.get("storeId")));
        u.setSupplierId(asLong(c.get("supplierId")));
        u.setFranchiseeId(asLong(c.get("franchiseeId")));
        u.setMemberId(asLong(c.get("memberId")));
        u.setTokenId(c.getId());
        return u;
    }

    private Long asLong(Object v) {
        if (v == null) {
            return null;
        }
        return v instanceof Number n ? n.longValue() : Long.valueOf(v.toString());
    }
}
