package com.kiwixgames.dice.domain.entities

import com.kiwixgames.dice.domain.enums.BadgeTier
import com.kiwixgames.dice.domain.enums.TableMode
import com.kiwixgames.dice.domain.enums.TableStatus
import com.kiwixgames.dice.domain.model.common.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "tables")
class GameTable(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    var owner: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: TableStatus = TableStatus.WAITING,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var mode: TableMode,

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    var badgeTier: BadgeTier? = null,

    @Column(nullable = false)
    var stakeGold: Int,

    @Column(nullable = false)
    var targetScore: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat0_user_id", nullable = true)
    var seat0: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat1_user_id", nullable = true)
    var seat1: User? = null,

    @Version
    var version: Long? = null
) : BaseEntity()
