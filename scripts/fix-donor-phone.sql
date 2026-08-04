-- Make donor phone optional (fixes 500 when registering without phone on old schema)
ALTER TABLE donors ALTER COLUMN phone DROP NOT NULL;
