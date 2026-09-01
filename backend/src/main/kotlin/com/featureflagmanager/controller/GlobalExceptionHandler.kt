package com.featureflagmanager.controller

import com.featureflagmanager.repository.FlagEnvRepository
import com.featureflagmanager.service.InvalidRequestException
import com.featureflagmanager.service.NotFoundException
import com.featureflagmanager.service.VersionConflictException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.UUID

@RestControllerAdvice
class GlobalExceptionHandler(
    private val flagEnvRepository: FlagEnvRepository,
) {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(ex: NotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(error = ex.message ?: "not found", code = ex.code))

    @ExceptionHandler(InvalidRequestException::class)
    fun handleBadRequest(ex: InvalidRequestException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest().body(ErrorResponse(error = ex.message ?: "invalid request", code = ex.code))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest().body(ErrorResponse(error = ex.message ?: "invalid request", code = "VALIDATION_ERROR"))

    @ExceptionHandler(VersionConflictException::class)
    fun handleVersionConflict(ex: VersionConflictException): ResponseEntity<ErrorResponse> {
        logger.warn("Version conflict (stale read): current={}", ex.current)
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse(error = ex.message ?: "version conflict", code = ex.code, current = ex.current))
    }

    /**
     * Thrown by Hibernate's own `@Version` check at flush/commit time when two requests race past
     * the manual version check in FlagService (both read the same version before either commits).
     * The manual check handles the common "stale read" case; this is the safety net for a true
     * simultaneous write-write race. Re-fetches the row fresh (the transaction that threw this is
     * already rolled back) so `current` reflects whichever request actually won.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException::class)
    fun handleConcurrentModification(ex: ObjectOptimisticLockingFailureException): ResponseEntity<ErrorResponse> {
        val id = ex.identifier as? UUID
        val current = id?.let { flagEnvRepository.findById(it).orElse(null) }?.let {
            mapOf(
                "flagName" to it.flag.name,
                "env" to it.env.name,
                "enabled" to it.enabled,
                "rollout" to it.rollout,
                "version" to it.version,
            )
        }
        logger.warn("Version conflict (simultaneous write race) on FlagEnv id={}: current={}", id, current)
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(error = "version conflict", code = "VERSION_CONFLICT", current = current))
    }
}
