package com.codebaseqa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REPO_CACHE_PREFIX = "repo:";
    private static final String QUERY_CACHE_PREFIX = "query:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    /**
     * Invalidate all cache entries for a specific repo.
     */
    public void invalidateRepoCache(UUID repoId) {
        try {
            String pattern = REPO_CACHE_PREFIX + repoId + ":*";
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Invalidated {} cache entries for repo {}", keys.size(), repoId);
            }
        } catch (Exception e) {
            log.warn("Failed to invalidate cache for repo {}: {}", repoId, e.getMessage());
        }
    }

    /**
     * Cache a query result.
     */
    public void cacheQueryResult(UUID repoId, String query, Object result) {
        try {
            String key = QUERY_CACHE_PREFIX + repoId + ":" + query.hashCode();
            redisTemplate.opsForValue().set(key, result, CACHE_TTL);
            log.debug("Cached query result for repo {}", repoId);
        } catch (Exception e) {
            log.warn("Failed to cache query result: {}", e.getMessage());
        }
    }

    /**
     * Get cached query result.
     */
    public Object getCachedQueryResult(UUID repoId, String query) {
        try {
            String key = QUERY_CACHE_PREFIX + repoId + ":" + query.hashCode();
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Failed to get cached query result: {}", e.getMessage());
            return null;
        }
    }
}
