package com.featureflagmanager.controller

import com.featureflagmanager.entity.Environment
import com.featureflagmanager.entity.FeatureFlag
import com.featureflagmanager.entity.FlagEnv
import com.featureflagmanager.repository.FlagEnvRepository
import com.featureflagmanager.service.InvalidRequestException
import com.featureflagmanager.service.NotFoundException
import com.featureflagmanager.service.VersionConflictException
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import java.util.UUID

class GlobalExceptionHandlerTest {

    private val flagEnvRepository = mockk<FlagEnvRepository>()
    private val handler = GlobalExceptionHandler(flagEnvRepository)

    @Test
    fun `NotFoundException maps to 404 with message and code`() {
        val response = handler.handleNotFound(NotFoundException("flag not found", code = "FLAG_NOT_FOUND"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(response.body?.error).isEqualTo("flag not found")
        assertThat(response.body?.code).isEqualTo("FLAG_NOT_FOUND")
    }

    @Test
    fun `InvalidRequestException maps to 400 with message and code`() {
        val response = handler.handleBadRequest(InvalidRequestException("invalid env", code = "INVALID_ENV"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.error).isEqualTo("invalid env")
        assertThat(response.body?.code).isEqualTo("INVALID_ENV")
    }

    @Test
    fun `MethodArgumentNotValidException maps to 400 with a VALIDATION_ERROR code`() {
        val bindingResult = mockk<BindingResult>()
        every { bindingResult.fieldErrors } returns listOf(FieldError("body", "name", "must not be blank"))
        every { bindingResult.allErrors } returns listOf(FieldError("body", "name", "must not be blank"))
        val ex = mockk<MethodArgumentNotValidException>()
        every { ex.bindingResult } returns bindingResult
        every { ex.message } returns "validation failed"

        val response = handler.handleValidation(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.error).isEqualTo("validation failed")
        assertThat(response.body?.code).isEqualTo("VALIDATION_ERROR")
    }

    @Test
    fun `VersionConflictException maps to 409 with current state and code`() {
        val current = mapOf("version" to 5, "enabled" to true)
        val response = handler.handleVersionConflict(VersionConflictException("version conflict", current))

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body?.error).isEqualTo("version conflict")
        assertThat(response.body?.code).isEqualTo("VERSION_CONFLICT")
        assertThat(response.body?.current).isEqualTo(current)
    }

    @Test
    fun `ObjectOptimisticLockingFailureException maps to 409 with the freshly re-fetched row`() {
        val flag = FeatureFlag(name = "new-checkout").apply { id = UUID.randomUUID() }
        val env = Environment(name = "production").apply { id = UUID.randomUUID() }
        val rowId = UUID.randomUUID()
        val freshRow = FlagEnv(flag = flag, env = env).apply { id = rowId; enabled = true; rollout = 42; version = 7 }

        val ex = mockk<ObjectOptimisticLockingFailureException>()
        every { ex.identifier } returns rowId
        every { flagEnvRepository.findById(rowId) } returns java.util.Optional.of(freshRow)

        val response = handler.handleConcurrentModification(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body?.error).isEqualTo("version conflict")
        @Suppress("UNCHECKED_CAST")
        val current = response.body?.current as Map<String, Any?>
        assertThat(current["version"]).isEqualTo(7)
        assertThat(current["env"]).isEqualTo("production")
        assertThat(current["enabled"]).isEqualTo(true)
        assertThat(current["rollout"]).isEqualTo(42)
    }

    @Test
    fun `ObjectOptimisticLockingFailureException with a non-UUID identifier returns null current`() {
        val ex = mockk<ObjectOptimisticLockingFailureException>()
        every { ex.identifier } returns "not-a-uuid"

        val response = handler.handleConcurrentModification(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body?.current).isNull()
    }

    @Test
    fun `ObjectOptimisticLockingFailureException where the row is gone returns null current`() {
        val rowId = UUID.randomUUID()
        val ex = mockk<ObjectOptimisticLockingFailureException>()
        every { ex.identifier } returns rowId
        every { flagEnvRepository.findById(rowId) } returns java.util.Optional.empty()

        val response = handler.handleConcurrentModification(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body?.current).isNull()
    }
}
