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
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName

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

    @Autowired
    lateinit var redisTemplate: StringRedisTemplate


    @AfterEach
    fun afterEach() {
        SecurityContextHolder.clearContext()
        testJdbcBoardGameRepository.deleteAll()
        redisTemplate.connectionFactory?.connection?.use { connection ->
            connection.serverCommands().flushDb()
        }
    }

    companion object {
        private var postgresql: PostgreSQLContainer<*> = PostgreSQLContainer<Nothing>("postgres:17")
            .apply {
                withUsername("postgres")
                withPassword("postgres")
                withDatabaseName("boardgame_tracker_db")
                withReuse(true)
            }

        private var redis: GenericContainer<*> = GenericContainer(DockerImageName.parse("redis:7-alpine"))
            .apply {
                withExposedPorts(6379)
                withReuse(true)
            }

        @DynamicPropertySource
        @JvmStatic
        fun configureProperty (registry: DynamicPropertyRegistry) {
            registry.add("POSTGRES_URL") {postgresql.jdbcUrl}
            registry.add("POSTGRES_USER") {postgresql.username}
            registry.add("POSTGRES_PASSWORD") {postgresql.password}
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
        }

        init {
            Startables.deepStart(listOf(postgresql, redis)).join()
        }
    }
}
