package com.featureflagmanager.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "feature_flag")
class FeatureFlag(
    @Column(name = "flag_key", nullable = false, unique = true)
    var key: String,

    @Column(nullable = false, length = 25)
    var name: String,

    @Column
    var description: String? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}
