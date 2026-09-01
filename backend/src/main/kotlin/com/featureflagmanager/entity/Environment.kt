package com.featureflagmanager.entity

import jakarta.persistence.*

@Entity
@Table(name = "environment")
class Environment(
    @Column(nullable = false, unique = true)
    var name: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
