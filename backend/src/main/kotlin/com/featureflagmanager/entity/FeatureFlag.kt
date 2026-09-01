package com.featureflagmanager.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "feature_flag", uniqueConstraints = [UniqueConstraint(columnNames = ["project_id", "name"])])
class FeatureFlag(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    var project: Project,

    @Column(nullable = false)
    var name: String,

    @Column
    var description: String? = null,
) {
    @Id
    @GeneratedValue
    var id: UUID? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}
