package com.featureflagmanager.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "project")
class Project(
    @Column(name = "`key`", nullable = false, unique = true)
    var key: String,

    @Column(nullable = false)
    var name: String,
) {
    @Id
    @GeneratedValue
    var id: UUID? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
}
