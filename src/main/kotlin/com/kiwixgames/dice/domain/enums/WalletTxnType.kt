package com.kiwixgames.dice.domain.enums

enum class WalletTxnType {
    WAGER_LOCK,       // masaya girerken emanet
    WAGER_REFUND,     // masa iptal vb.
    PAYOUT,           // kazanana ödeme
    ADJUSTMENT        // admin/ops (opsiyonel)
}