package com.featureflagmanager.controller

import com.featureflagmanager.service.InvalidRequestException
import com.featureflagmanager.service.NotFoundException
import com.featureflagmanager.service.UnauthorizedException
import com.featureflagmanager.service.VersionConflictException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(ex: NotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(error = ex.message ?: "not found"))

    @ExceptionHandler(InvalidRequestException::class, MethodArgumentNotValidException::class)
    fun handleBadRequest(ex: Exception): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest().body(ErrorResponse(error = ex.message ?: "invalid request"))

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(ex: UnauthorizedException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse(error = ex.message ?: "unauthorized"))

    @ExceptionHandler(VersionConflictException::class)
    fun handleVersionConflict(ex: VersionConflictException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse(error = ex.message ?: "version conflict", current = ex.current))
}
