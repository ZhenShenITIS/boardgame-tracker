package itis.boardgametracker.integration

import itis.boardgametracker.BoardgameTrackerApplication
import itis.boardgametracker.repository.TestJdbcBoardGameRepository
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.test.context.SpringBootTest
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
abstract class IntegrationTest {
    @Autowired
    lateinit var testJdbcBoardGameRepository: TestJdbcBoardGameRepository


    @AfterEach
    fun afterEach() {
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