package com.codebaseqa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Token bucket rate limiting using Redis.
 * Limits queries per user per hour.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String RATE_LIMIT_PREFIX = "ratelimit:query:";
    private static final int MAX_QUERIES_PER_HOUR = 20;
    private static final Duration WINDOW_DURATION = Duration.ofHours(1);

    /**
     * Check if the user has exceeded the rate limit.
     *
     * @param userId The user ID
     * @return true if rate limit is exceeded, false otherwise
     */
    public boolean isRateLimited(UUID userId) {
        String key = RATE_LIMIT_PREFIX + userId;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == null) {
            return false;
        }

        // Set expiry on first request
        if (count == 1) {
            redisTemplate.expire(key, WINDOW_DURATION);
        }

        boolean limited = count > MAX_QUERIES_PER_HOUR;
        if (limited) {
            log.warn("Rate limit exceeded for user {}: {} queries", userId, count);
        }

        return limited;
    }

    /**
     * Get the number of remaining queries for the user.
     *
     * @param userId The user ID
     * @return Number of remaining queries
     */
    public int getRemainingQueries(UUID userId) {
        String key = RATE_LIMIT_PREFIX + userId;
        Long count = (Long) redisTemplate.opsForValue().get(key);

        if (count == null) {
            return MAX_QUERIES_PER_HOUR;
        }

        return Math.max(0, MAX_QUERIES_PER_HOUR - count.intValue());
    }

    /**
     * Get the time until the rate limit resets (in seconds).
     *
     * @param userId The user ID
     * @return Seconds until reset, or 0 if no limit is active
     */
    public long getResetTime(UUID userId) {
        String key = RATE_LIMIT_PREFIX + userId;
        Long ttl = redisTemplate.getExpire(key);

        if (ttl == null || ttl < 0) {
            return 0;
        }

        return ttl;
    }
}
