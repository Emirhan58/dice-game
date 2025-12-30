package com.kiwixgames.dice.domain.entities


import com.kiwixgames.dice.domain.enums.Role
import com.kiwixgames.dice.domain.model.common.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "users")
class User(

    @Column(nullable = false, unique = true)
    var username: String,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false)
    var password: String,

    var firstName: String? = null,

    var lastName: String? = null,

    @Column(columnDefinition = "TEXT")
    var bio: String? = null,

    var profileImage: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: Role = Role.USER,

    var isActive: Boolean = true



) : BaseEntity() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (isActive != other.isActive) return false
        if (username != other.username) return false
        if (email != other.email) return false
        if (password != other.password) return false
        if (firstName != other.firstName) return false
        if (lastName != other.lastName) return false
        if (bio != other.bio) return false
        if (profileImage != other.profileImage) return false
        if (role != other.role) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isActive.hashCode()
        result = 31 * result + username.hashCode()
        result = 31 * result + email.hashCode()
        result = 31 * result + password.hashCode()
        result = 31 * result + (firstName?.hashCode() ?: 0)
        result = 31 * result + (lastName?.hashCode() ?: 0)
        result = 31 * result + (bio?.hashCode() ?: 0)
        result = 31 * result + (profileImage?.hashCode() ?: 0)
        result = 31 * result + role.hashCode()
        return result
    }
}
