package com.kiwixgames.dice.domain.entities

import com.kiwixgames.dice.domain.enums.WalletTxnType
import com.kiwixgames.dice.domain.model.common.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "wallet_txn")
class WalletTxn(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(nullable = false)
    var amount: Long, // + / -

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: WalletTxnType,

    @Column(nullable = true)
    var refType: String? = null, // "TABLE" / "GAME"

    @Column(nullable = true)
    var refId: Long? = null,

    @Column(columnDefinition = "TEXT")
    var note: String? = null
) : BaseEntity()
