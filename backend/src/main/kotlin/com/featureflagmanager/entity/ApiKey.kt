package com.featureflagmanager.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "api_key")
class ApiKey(
    @Column(name = "`key`", nullable = false, unique = true)
    var key: String,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    var project: Project,

    @Column
    var description: String? = null,
) {
    @Id
    @GeneratedValue
    var id: UUID? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
}
