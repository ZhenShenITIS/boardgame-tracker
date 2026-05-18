package itis.boardgametracker.service.imports

import itis.boardgametracker.constant.BoardGameType
import itis.boardgametracker.model.BoardGame
import itis.boardgametracker.model.Tag
import itis.boardgametracker.properties.BggApiProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.w3c.dom.Element
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import javax.xml.parsers.DocumentBuilderFactory

@Component
class BggThingClient(
    private val bggApiProperties: BggApiProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(bggApiProperties.requestTimeoutSeconds))
        .build()

    fun fetchThings(ids: List<Long>): List<BoardGame> {
        if (ids.isEmpty()) return emptyList()

        val url = buildThingUrl(ids)
        val body = executeXmlRequest(url) ?: return emptyList()
        return runCatching {
            parseBoardGames(body)
        }.getOrElse { exception ->
            log.atWarn()
                .setCause(exception)
                .addKeyValue("url", url)
                .log("BGG thing response parsing failed")
            emptyList()
        }
    }

    private fun buildThingUrl(ids: List<Long>): String {
        val joinedIds = ids.joinToString(",")
        val encodedIds = URLEncoder.encode(joinedIds, StandardCharsets.UTF_8)
        return "${bggApiProperties.baseUrl}/thing?id=$encodedIds&type=boardgame,boardgameexpansion&stats=1"
    }

    private fun executeXmlRequest(url: String): String? {
        var attempt = 1
        var delayMillis = bggApiProperties.minDelayMillis
        while (attempt <= bggApiProperties.retryMaxAttempts) {
            val startedAt = Instant.now()
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(bggApiProperties.requestTimeoutSeconds))
                .header("Authorization", "Bearer ${bggApiProperties.token}")
                .GET()
                .build()

            val response = runCatching {
                httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            }.getOrNull()

            val statusCode = response?.statusCode() ?: 503
            log.atInfo()
                .addKeyValue("url", url)
                .addKeyValue("attempt", attempt)
                .addKeyValue("statusCode", statusCode)
                .addKeyValue("durationMs", Duration.between(startedAt, Instant.now()).toMillis())
                .log("BGG XML request completed")

            if (response != null && statusCode == 200) {
                return response.body()
            }

            if (response != null && statusCode in listOf(401, 403)) {
                val message = "BGG authorization failed with status=$statusCode"
                log.atWarn().addKeyValue("statusCode", statusCode).log(message)
                return null
            }

            if (attempt == bggApiProperties.retryMaxAttempts) {
                return null
            }

            Thread.sleep(delayMillis)
            delayMillis = (delayMillis * 2).coerceAtMost(120_000)
            attempt++
        }
        return null
    }

    private fun parseBoardGames(xml: String): List<BoardGame> {
        val document = parseDocument(xml)
        val items = document.getElementsByTagName("item")

        val result = mutableListOf<BoardGame>()
        for (index in 0 until items.length) {
            val item = items.item(index) as? Element ?: continue
            val type = item.getAttribute("type")
            if (type != "boardgame" && type != "boardgameexpansion") continue

            val bggId = item.getAttribute("id").toLongOrNull() ?: continue
            val originalName = primaryName(item) ?: continue
            val displayName = displayName(item, originalName)
            val boardGameType = if (type == "boardgameexpansion") BoardGameType.EXPANSION else BoardGameType.BOARDGAME

            result.add(
                BoardGame(
                    bggId = bggId,
                    type = boardGameType,
                    originalName = originalName,
                    displayName = displayName,
                    complexity = parseDouble(findNestedValue(item, "statistics", "ratings", "averageweight")),
                    minPlayers = parseInt(directValue(item, "minplayers")),
                    maxPlayers = parseInt(directValue(item, "maxplayers")),
                    playingTime = parseInt(directValue(item, "playingtime")),
                    minPlayTime = parseInt(directValue(item, "minplaytime")),
                    maxPlayTime = parseInt(directValue(item, "maxplaytime")),
                    minAge = parseInt(directValue(item, "minage")),
                    yearPublished = parseInt(directValue(item, "yearpublished")),
                    bggImageUrl = textContent(item, "image"),
                    bggPreviewUrl = textContent(item, "thumbnail"),
                    isCustom = false,
                    createdById = null,
                    tags = parseTags(item)
                )
            )
        }

        return result
    }

    private fun parseDocument(xml: String): org.w3c.dom.Document {
        val builderFactory = DocumentBuilderFactory.newInstance()
        builderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        builderFactory.isExpandEntityReferences = false
        builderFactory.isXIncludeAware = false
        val builder = builderFactory.newDocumentBuilder()
        return builder.parse(xml.byteInputStream())
    }

    private fun primaryName(item: Element): String? {
        val names = item.getElementsByTagName("name")
        for (i in 0 until names.length) {
            val nameNode = names.item(i) as? Element ?: continue
            if (nameNode.getAttribute("type") == "primary") {
                return nameNode.getAttribute("value").takeIf { it.isNotBlank() }
            }
        }
        return null
    }

    private fun displayName(item: Element, originalName: String): String {
        val names = item.getElementsByTagName("name")
        for (i in 0 until names.length) {
            val nameNode = names.item(i) as? Element ?: continue
            val value = nameNode.getAttribute("value").trim()
            if (value.isNotBlank() && value.any { it.code in 0x0400..0x04FF }) {
                return value
            }
        }
        return originalName
    }

    private fun parseTags(item: Element): List<Tag> {
        val links = item.getElementsByTagName("link")
        val tags = linkedMapOf<String, Tag>()

        for (i in 0 until links.length) {
            val link = links.item(i) as? Element ?: continue
            val type = link.getAttribute("type")
            val name = link.getAttribute("value").trim()
            if (name.isBlank()) continue

            val description = when (type) {
                "boardgamecategory" -> "Категория"
                "boardgamemechanic" -> "Механика"
                else -> null
            } ?: continue

            tags.putIfAbsent(
                name.lowercase(),
                Tag(
                    name = name,
                    description = description
                )
            )
        }

        return tags.values.toList()
    }

    private fun directValue(item: Element, tagName: String): String? {
        val nodes = item.getElementsByTagName(tagName)
        val element = nodes.item(0) as? Element ?: return null
        return element.getAttribute("value")
    }

    private fun findNestedValue(item: Element, level1: String, level2: String, level3: String): String? {
        val l1 = item.getElementsByTagName(level1).item(0) as? Element ?: return null
        val l2 = l1.getElementsByTagName(level2).item(0) as? Element ?: return null
        val l3 = l2.getElementsByTagName(level3).item(0) as? Element ?: return null
        return l3.getAttribute("value")
    }

    private fun textContent(item: Element, tagName: String): String? {
        val nodes = item.getElementsByTagName(tagName)
        return nodes.item(0)?.textContent?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun parseInt(value: String?): Int? = value?.toIntOrNull()
    private fun parseDouble(value: String?): Double? = value?.toDoubleOrNull()
}
