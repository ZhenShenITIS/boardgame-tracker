package itis.boardgametracker.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.cache")
data class AppCacheProperties(
    val userCollectionStatsTtl: Duration = Duration.ofMinutes(10)
)
