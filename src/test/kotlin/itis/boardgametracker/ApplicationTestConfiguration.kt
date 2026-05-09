package itis.boardgametracker

import itis.boardgametracker.security.config.SecurityConfig
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan


@ConfigurationPropertiesScan(
    basePackageClasses = [BoardgameTrackerApplication::class]
)
@EnableAutoConfiguration
class ApplicationTestConfiguration {
}