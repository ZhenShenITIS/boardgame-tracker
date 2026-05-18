package itis.boardgametracker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
@EnableConfigurationProperties
@ConfigurationPropertiesScan
@EnableAsync
@SpringBootApplication
class BoardgameTrackerApplication

fun main(args: Array<String>) {
    runApplication<BoardgameTrackerApplication>(*args)
}
