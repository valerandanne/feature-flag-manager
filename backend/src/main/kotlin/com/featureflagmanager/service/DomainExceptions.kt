package com.featureflagmanager.service

class NotFoundException(message: String, val code: String) : RuntimeException(message)

class InvalidRequestException(message: String, val code: String) : RuntimeException(message)

class VersionConflictException(
    message: String,
    val current: Any,
    val code: String = "VERSION_CONFLICT",
) : RuntimeException(message)
