UPDATE wager_lock
SET status = 'LOCKED'
WHERE status IN ('0','1','2');
