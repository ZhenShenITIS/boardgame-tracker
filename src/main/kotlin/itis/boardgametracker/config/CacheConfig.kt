package itis.boardgametracker.config

import com.fasterxml.jackson.databind.ObjectMapper
import itis.boardgametracker.api.dto.UserCollectionStats
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun redisCacheManagerBuilderCustomizer(
        objectMapper: ObjectMapper,
        appCacheProperties: AppCacheProperties
    ): RedisCacheManagerBuilderCustomizer {
        val userCollectionStatsSerializer = Jackson2JsonRedisSerializer(
            objectMapper,
            UserCollectionStats::class.java
        )
        val defaultConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(userCollectionStatsSerializer))

        return RedisCacheManagerBuilderCustomizer { builder ->
            builder.transactionAware()
            builder.withCacheConfiguration(
                CacheCatalog.USER_COLLECTION_STATS,
                defaultConfiguration.entryTtl(appCacheProperties.userCollectionStatsTtl)
            )
        }
    }
}
