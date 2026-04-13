package itis.boardgametracker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BoardgameTrackerApplication

fun main(args: Array<String>) {
    runApplication<BoardgameTrackerApplication>(*args)
}
