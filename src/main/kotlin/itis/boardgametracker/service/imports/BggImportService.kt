package itis.boardgametracker.service.imports

import itis.boardgametracker.api.dto.BggImportJobStatus
import itis.boardgametracker.exception.ImportJobAlreadyRunningException
import itis.boardgametracker.exception.NotFoundException
import itis.boardgametracker.repository.BoardGameRepository
import org.slf4j.LoggerFactory
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readLines

@Service
class BggImportService(
    private val bggThingClient: BggThingClient,
    private val boardGameRepository: BoardGameRepository,
    private val taskExecutor: TaskExecutor
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val running = AtomicBoolean(false)
    private val jobs = ConcurrentHashMap<String, BggImportJobState>()

    @Volatile
    private var latestJobId: String? = null

    fun startImport(startLine: Int): BggImportJobStatus {
        if (!running.compareAndSet(false, true)) {
            throw ImportJobAlreadyRunningException()
        }

        val jobId = UUID.randomUUID().toString()
        val jobState = BggImportJobState(
            jobId = jobId,
            startLine = startLine,
            startedAt = Instant.now()
        )
        jobs[jobId] = jobState
        latestJobId = jobId

        taskExecutor.execute { runImportInBackground(jobState) }
        return map(jobState)
    }

    fun status(jobId: String?): BggImportJobStatus {
        val actualJobId = jobId ?: latestJobId ?: throw NotFoundException()
        val jobState = jobs[actualJobId] ?: throw NotFoundException()
        return map(jobState)
    }

    private fun runImportInBackground(jobState: BggImportJobState) {
        try {
            runImport(jobState)
            jobState.status = BggImportJobState.Status.COMPLETED
            jobState.message = "Import completed"
        } catch (exception: Exception) {
            jobState.status = BggImportJobState.Status.FAILED
            jobState.message = exception.message ?: "Import failed"
            log.atError()
                .setCause(exception)
                .addKeyValue("jobId", jobState.jobId)
                .log("BGG import failed")
        } finally {
            jobState.finishedAt = Instant.now()
            running.set(false)
        }
    }

    private fun runImport(jobState: BggImportJobState) {
        val allIds = loadIdsFromFile(jobState.startLine, jobState)
        val chunks = allIds.chunked(20)
        for (chunk in chunks) {
            Thread.sleep(5500)
            val boardGames = bggThingClient.fetchThings(chunk)
            if (boardGames.isEmpty()) {
                chunk.forEach { _ ->
                    jobState.incProcessed()
                    jobState.incErrors()
                }
                continue
            }

            val receivedById = boardGames.associateBy { it.bggId }
            chunk.forEach { requestedId ->
                jobState.incProcessed()
                val boardGame = receivedById[requestedId]
                if (boardGame == null) {
                    jobState.incErrors()
                } else {
                    val inserted = boardGameRepository.createImportedIgnoringDuplicates(boardGame)
                    if (inserted) {
                        jobState.incSaved()
                    } else {
                        jobState.incSkippedDuplicate()
                    }
                }
            }
        }
    }

    private fun loadIdsFromFile(startLine: Int, jobState: BggImportJobState): List<Long> {
        val file = Path("top10000.txt")
        val lines: List<String> = when {
            file.exists() -> file.readLines()
            else -> {
                val resourceStream = javaClass.classLoader.getResourceAsStream("top10000.txt")
                    ?: throw IllegalStateException(
                        "top10000.txt not found (checked filesystem path './top10000.txt' and classpath resource 'top10000.txt')"
                    )
                resourceStream.bufferedReader().use { it.readLines() }
            }
        }

        val unique = linkedSetOf<Long>()
        lines.asSequence()
            .drop(startLine - 1)
            .forEach { line ->
                line.trim()
                    .split(Regex("[,\\s]+"))
                    .asSequence()
                    .filter { it.isNotBlank() }
                    .forEach { token ->
                        val parsed = token.toLongOrNull()
                        if (parsed == null) {
                            jobState.incSkippedInvalid()
                        } else {
                            unique.add(parsed)
                        }
                    }
            }
        return unique.toList()
    }

    private fun map(jobState: BggImportJobState): BggImportJobStatus {
        return BggImportJobStatus(
            jobId = jobState.jobId,
            status = when (jobState.status) {
                BggImportJobState.Status.RUNNING -> BggImportJobStatus.Status.RUNNING
                BggImportJobState.Status.COMPLETED -> BggImportJobStatus.Status.COMPLETED
                BggImportJobState.Status.FAILED -> BggImportJobStatus.Status.FAILED
            },
            startLine = jobState.startLine,
            startedAt = OffsetDateTime.ofInstant(jobState.startedAt, ZoneOffset.UTC),
            processed = jobState.processed(),
            saved = jobState.saved(),
            skippedInvalid = jobState.skippedInvalid(),
            skippedDuplicate = jobState.skippedDuplicate(),
            errors = jobState.errors(),
            finishedAt = jobState.finishedAt?.let { OffsetDateTime.ofInstant(it, ZoneOffset.UTC) },
            message = jobState.message
        )
    }
}
