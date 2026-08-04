-- Align Postgres CHECK constraints with current Java enums.
-- Safe to re-run.

-- auth_db: allow BLOOD_BANK (old schema had HOSPITAL / BLOOD_BANK_COORDINATOR / NGO)
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check
  CHECK (role::text = ANY (ARRAY['DONOR','REQUESTER','BLOOD_BANK','ADMIN']::text[]));

-- notification_db: blood banks receive request alerts
ALTER TABLE notification_logs DROP CONSTRAINT IF EXISTS notification_logs_recipient_type_check;
ALTER TABLE notification_logs ADD CONSTRAINT notification_logs_recipient_type_check
  CHECK (recipient_type::text = ANY (ARRAY['DONOR','REQUESTER','BLOOD_BANK']::text[]));

-- rewards_db: legacy total_donations blocked inserts (entity uses donation_count)
ALTER TABLE reward_accounts ALTER COLUMN total_donations DROP NOT NULL;
ALTER TABLE reward_accounts ALTER COLUMN total_donations SET DEFAULT 0;
UPDATE reward_accounts SET total_donations = COALESCE(total_donations, donation_count, 0) WHERE total_donations IS NULL;
