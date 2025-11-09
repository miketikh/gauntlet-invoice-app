package com.invoiceme.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration using Caffeine cache provider
 * Enables caching for read-heavy query operations to improve performance
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure Caffeine cache manager with custom settings
     * Different caches can have different TTL and size limits
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "customers",           // 5 min TTL
            "customerList",        // 5 min TTL
            "invoiceStats",        // 1 min TTL
            "paymentStats",        // 1 min TTL
            "dashboardStats"       // 1 min TTL
        );

        cacheManager.setCaffeine(defaultCaffeineConfig());
        return cacheManager;
    }

    /**
     * Default Caffeine configuration
     * - 5 minute expiration after write
     * - Maximum 1000 entries
     * - Record stats for monitoring
     */
    private Caffeine<Object, Object> defaultCaffeineConfig() {
        return Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(1000)
            .recordStats();
    }

    /**
     * Caffeine configuration for short-lived statistics caches (1 minute)
     */
    @Bean
    public Caffeine<Object, Object> statsCaffeineConfig() {
        return Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(100)
            .recordStats();
    }
}
