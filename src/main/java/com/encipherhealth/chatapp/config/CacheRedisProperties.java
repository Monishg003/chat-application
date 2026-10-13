package com.encipherhealth.chatapp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;

import static java.time.temporal.ChronoUnit.MINUTES;

@Setter
@Getter
@ConfigurationProperties(prefix = "cache.redis")
public class CacheRedisProperties
{
    @DurationUnit(MINUTES)
    private Duration ttl = Duration.ofMinutes(15);
    private String keyPrefix = "chatapp:";
    private boolean cacheNullValues = false;
}
