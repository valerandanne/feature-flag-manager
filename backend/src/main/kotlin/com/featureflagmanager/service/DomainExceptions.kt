package com.featureflagmanager.service

class NotFoundException(message: String) : RuntimeException(message)

class InvalidRequestException(message: String) : RuntimeException(message)

class UnauthorizedException(message: String) : RuntimeException(message)

class VersionConflictException(
    message: String,
    val current: Any,
) : RuntimeException(message)
