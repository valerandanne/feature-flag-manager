package com.featureflagmanager.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "environment")
class Environment(
    @Column(nullable = false, unique = true)
    var name: String,
) {
    @Id
    @GeneratedValue
    var id: UUID? = null
}
