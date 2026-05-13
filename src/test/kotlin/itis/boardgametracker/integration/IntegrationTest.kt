package itis.boardgametracker.integration

import com.fasterxml.jackson.databind.ObjectMapper
import itis.boardgametracker.BoardgameTrackerApplication
import itis.boardgametracker.repository.TestJdbcBoardGameRepository
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.lifecycle.Startables

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [BoardgameTrackerApplication::class]
)
@ConfigurationPropertiesScan(
    basePackageClasses = [BoardgameTrackerApplication::class]
)
@EnableAutoConfiguration
@AutoConfigureMockMvc(addFilters = false)
abstract class IntegrationTest {
    @Autowired
    lateinit var testJdbcBoardGameRepository: TestJdbcBoardGameRepository

    @Autowired
    lateinit var objectMapper: ObjectMapper


    @AfterEach
    fun afterEach() {
        SecurityContextHolder.clearContext()
        testJdbcBoardGameRepository.deleteAll()
    }

    companion object {
        private var postgresql: PostgreSQLContainer<*> = PostgreSQLContainer<Nothing>("postgres:17")
            .apply {
                withUsername("postgres")
                withPassword("postgres")
                withDatabaseName("boardgame_tracker_db")
                withReuse(true)
            }

        @DynamicPropertySource
        @JvmStatic
        fun configureProperty (registry: DynamicPropertyRegistry) {
            registry.add("POSTGRES_URL") {postgresql.jdbcUrl}
            registry.add("POSTGRES_USER") {postgresql.username}
            registry.add("POSTGRES_PASSWORD") {postgresql.password}
        }

        init {
            Startables.deepStart(listOf(postgresql)).join()
        }
    }
}
