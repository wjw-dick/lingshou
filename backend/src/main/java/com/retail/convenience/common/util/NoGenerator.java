package com.retail.convenience.common.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Component
public class NoGenerator {

    private final StringRedisTemplate redis;
    private static final DateTimeFormatter SECOND = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public NoGenerator(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 编号 = 前缀 + 秒级时间 + 4 位 Redis 自增序号，避免与历史/种子数据冲突。
     */
    public String next(String prefix) {
        String second = LocalDateTime.now().format(SECOND);
        String key = "seq:" + prefix + ":" + second;
        Long seq = redis.opsForValue().increment(key);
        if (seq == null) {
            seq = 1L;
        }
        if (seq == 1L) {
            redis.expire(key, 2, TimeUnit.MINUTES);
        }
        return prefix + second + String.format("%04d", seq % 10000);
    }
}
