package com.kiwixgames.dice.domain.entities

import com.kiwixgames.dice.domain.enums.GameStatus
import com.kiwixgames.dice.domain.model.common.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "games")
class Game(
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false, unique = true)
    var table: GameTable,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: GameStatus = GameStatus.IN_PROGRESS,

    @Column(name = "target_score", nullable = false)
    var targetScore: Int,

    @Column(name = "winner_seat")
    var winnerSeat: Int? = null,

    @Column(name = "finished_at")
    var finishedAt: LocalDateTime? = null,

    @Column(name = "state_json", columnDefinition = "TEXT")
    var stateJson: String? = null,

    @Version
    var version: Long? = null
) : BaseEntity()
