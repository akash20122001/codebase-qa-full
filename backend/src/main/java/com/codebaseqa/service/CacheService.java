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
     * Called after re-indexing to ensure fresh query results.
     */
    public void invalidateRepoCache(UUID repoId) {
        try {
            // Invalidate repo-specific cache
            String repoPattern = REPO_CACHE_PREFIX + repoId + ":*";
            var repoKeys = redisTemplate.keys(repoPattern);
            if (repoKeys != null && !repoKeys.isEmpty()) {
                redisTemplate.delete(repoKeys);
                log.info("Invalidated {} repo cache entries for repo {}", repoKeys.size(), repoId);
            }

            // Invalidate query cache for this repo
            String queryPattern = QUERY_CACHE_PREFIX + repoId + ":*";
            var queryKeys = redisTemplate.keys(queryPattern);
            if (queryKeys != null && !queryKeys.isEmpty()) {
                redisTemplate.delete(queryKeys);
                log.info("Invalidated {} query cache entries for repo {}", queryKeys.size(), repoId);
            }
        } catch (Exception e) {
            log.warn("Failed to invalidate cache for repo {}: {}", repoId, e.getMessage());
            // Don't throw - cache invalidation failure shouldn't break the flow
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
