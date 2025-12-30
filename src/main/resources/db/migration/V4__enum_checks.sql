ALTER TABLE tables
    ADD CONSTRAINT chk_tables_status_enum
        CHECK (status IN ('WAITING','FULL','IN_GAME','FINISHED','CANCELLED'));

ALTER TABLE tables
    ADD CONSTRAINT chk_tables_mode_enum
        CHECK (mode IN ('UNBADGED','BADGED'));

ALTER TABLE tables
    ADD CONSTRAINT chk_tables_badge_tier_enum
        CHECK (badge_tier IS NULL OR badge_tier IN ('TIN','SILVER','GOLD'));

ALTER TABLE wallet_txn
    ADD CONSTRAINT chk_wallet_txn_type_enum
        CHECK (type IN ('WAGER_LOCK','WAGER_REFUND','PAYOUT','ADJUSTMENT'));

ALTER TABLE wager_lock
    ADD CONSTRAINT chk_wager_lock_status_enum
        CHECK (status IN ('LOCKED','REFUNDED','PAID_OUT'));