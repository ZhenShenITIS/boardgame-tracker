package itis.boardgametracker.service.imports

import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

class BggImportJobState(
    val jobId: String,
    val startLine: Int,
    val startedAt: Instant
) {
    enum class Status {
        RUNNING,
        COMPLETED,
        FAILED
    }

    private val processedCounter = AtomicInteger(0)
    private val savedCounter = AtomicInteger(0)
    private val skippedInvalidCounter = AtomicInteger(0)
    private val skippedDuplicateCounter = AtomicInteger(0)
    private val errorsCounter = AtomicInteger(0)

    @Volatile
    var status: Status = Status.RUNNING

    @Volatile
    var finishedAt: Instant? = null

    @Volatile
    var message: String? = null

    fun processed(): Int = processedCounter.get()
    fun saved(): Int = savedCounter.get()
    fun skippedInvalid(): Int = skippedInvalidCounter.get()
    fun skippedDuplicate(): Int = skippedDuplicateCounter.get()
    fun errors(): Int = errorsCounter.get()

    fun incProcessed() = processedCounter.incrementAndGet()
    fun incSaved() = savedCounter.incrementAndGet()
    fun incSkippedInvalid() = skippedInvalidCounter.incrementAndGet()
    fun incSkippedDuplicate() = skippedDuplicateCounter.incrementAndGet()
    fun incErrors() = errorsCounter.incrementAndGet()
}
