package itis.boardgametracker.service

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import itis.boardgametracker.repository.CollectionItemRepository
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger

@Service
class ShelfOfShameMetricsService(
    private val collectionItemRepository: CollectionItemRepository,
    meterRegistry: MeterRegistry
) {
    private val shelfOfShameSize = AtomicInteger(0)

    init {
        refreshShelfOfShameSize()
        Gauge.builder(MetricsCatalog.SHELF_OF_SHAME_SIZE, shelfOfShameSize) { value -> value.toDouble() }
            .description("Количество элементов на полке позора")
            .register(meterRegistry)
    }

    fun refreshShelfOfShameSize() {
        shelfOfShameSize.set(collectionItemRepository.countGlobalShelfOfShameItems())
    }

    object MetricsCatalog {
        const val SHELF_OF_SHAME_SIZE = "collection_items.shelf_of_shame.size"
    }
}
