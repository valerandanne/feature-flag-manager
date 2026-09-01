package com.featureflagmanager.service

import com.featureflagmanager.entity.Project
import com.featureflagmanager.repository.ProjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
) {
    @Transactional
    fun create(key: String, name: String): Project {
        if (projectRepository.findByKey(key) != null) {
            throw InvalidRequestException("project with key '$key' already exists")
        }
        return projectRepository.save(Project(key = key, name = name))
    }

    fun list(): List<Project> = projectRepository.findAll()

    fun getById(projectId: UUID): Project =
        projectRepository.findById(projectId)
            .orElseThrow { NotFoundException("project not found") }
}
