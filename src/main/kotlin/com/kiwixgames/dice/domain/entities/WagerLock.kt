package com.kiwixgames.dice.domain.entities

import com.kiwixgames.dice.domain.enums.WagerLockStatus
import com.kiwixgames.dice.domain.model.common.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "wager_lock",
    indexes = [Index(name = "idx_wager_table_user", columnList = "table_id,user_id", unique = true)]
)
class WagerLock(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    var table: GameTable,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(nullable = false)
    var stakeGold: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: WagerLockStatus = WagerLockStatus.LOCKED
) : BaseEntity()
