package com.featureflagmanager.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "flag_env", uniqueConstraints = [UniqueConstraint(columnNames = ["flag_id", "env_id"])])
class FlagEnv(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flag_id", nullable = false)
    var flag: FeatureFlag,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "env_id", nullable = false)
    var env: Environment,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false)
    var enabled: Boolean = false

    @Version
    @Column(nullable = false)
    var version: Int = 1

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}
