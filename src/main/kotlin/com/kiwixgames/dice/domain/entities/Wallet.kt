package com.kiwixgames.dice.domain.entities

import com.kiwixgames.dice.domain.model.common.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "wallet",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id"])]
)
class Wallet(
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(nullable = false)
    var balanceGold: Long = 0
) : BaseEntity() {

    @Version
    var version: Long? = null
}
