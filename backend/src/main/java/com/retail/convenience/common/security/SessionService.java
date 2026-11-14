package com.retail.convenience.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class SessionService {

    private final StringRedisTemplate redis;
    private final JwtService jwtService;

    public SessionService(StringRedisTemplate redis, JwtService jwtService) {
        this.redis = redis;
        this.jwtService = jwtService;
    }

    public void save(LoginUser user, String token) {
        redis.opsForValue().set(key(user), token, jwtService.getExpire());
    }

    public boolean isActive(LoginUser user) {
        return Boolean.TRUE.equals(redis.hasKey(key(user)));
    }

    public void remove(LoginUser user) {
        if (user != null && user.getTokenId() != null) {
            redis.delete(key(user));
        }
    }

    private String key(LoginUser user) {
        return "session:" + user.getUserType() + ":" + user.getUserId() + ":" + user.getTokenId();
    }
}
