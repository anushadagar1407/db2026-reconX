package com.dbtraining.reconx.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/** TICKET-ADV081 — enables annotation-driven caching for service lookups. */
@Configuration
@EnableCaching
public class CacheConfig {
}
