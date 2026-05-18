package itis.boardgametracker.api

import itis.boardgametracker.api.dto.BggImportJobStatus
import itis.boardgametracker.security.CurrentUserProvider
import itis.boardgametracker.service.imports.BggImportService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminController(
    private val currentUserProvider: CurrentUserProvider,
    private val bggImportService: BggImportService
) : AdminApi {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun adminBoardgamesImportBggPost(startLine: Int): ResponseEntity<BggImportJobStatus> {
        val principal = currentUserProvider.currentUser()
        assertAdmin(principal.roles)

        log.atInfo()
            .addKeyValue("userId", principal.userId)
            .addKeyValue("startLine", startLine)
            .log("Received BGG import start request")

        val result = bggImportService.startImport(startLine)
        return ResponseEntity.accepted().body(result)
    }

    override fun adminBoardgamesImportBggStatusGet(jobId: String?): ResponseEntity<BggImportJobStatus> {
        val principal = currentUserProvider.currentUser()
        assertAdmin(principal.roles)

        val result = bggImportService.status(jobId)
        return ResponseEntity.ok(result)
    }

    private fun assertAdmin(roles: List<String>) {
        if (!roles.contains("ADMIN") && !roles.contains("ROLE_ADMIN")) {
            throw AccessDeniedException("Admin role is required")
        }
    }
}
