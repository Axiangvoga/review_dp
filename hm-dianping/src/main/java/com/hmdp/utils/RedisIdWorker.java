package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 实现唯一ID 采用当前时间减去开始时刻左移32位拼接上自增长主键
 */
@Component
public class RedisIdWorker {
    /**
     * 开始时刻的时间戳
     */
    private static final long BEGIN_TIME_STAMP = 1640995200L;
    /**
     * 序列号位数
     */
    private static final int BITS_COUNT = 32;
    private StringRedisTemplate stringRedisTemplate;
    public RedisIdWorker(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate=stringRedisTemplate;
    }
    public long nextId(String prefix){
        LocalDateTime now = LocalDateTime.now();
        long newSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp =newSecond - BEGIN_TIME_STAMP;
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd")); // 获取当前日期，精确到天
        long count = stringRedisTemplate.opsForValue().increment("icr:"+prefix+":"+date);// 自增长主键
        return timeStamp << BITS_COUNT | count; // 当前时间戳左移后拼接上自增长主键
    }
}
