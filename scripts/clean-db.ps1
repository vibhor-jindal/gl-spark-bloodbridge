<#
.SYNOPSIS
  Cleans BloodBridge Postgres databases: keeps requirement-valid rows, drops bad/test junk.
  Loads DB password from scripts\local.ps1 when present.
#>
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$local = Join-Path $PSScriptRoot "local.ps1"
if (Test-Path $local) { . $local }

$PgPassword = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "postgres" }
$env:PGPASSWORD = $PgPassword
$psql = "C:\Program Files\PostgreSQL\16\bin\psql.exe"
if (-not (Test-Path $psql)) { throw "psql not found at $psql" }

function Exec-Sql([string]$Db, [string]$Sql) {
    & $psql -U postgres -h 127.0.0.1 -d $Db -v ON_ERROR_STOP=1 -c $Sql
}

Write-Host "Cleaning BloodBridge databases..." -ForegroundColor Cyan

# Align CHECK constraints with current Java enums (Hibernate does not update these)
Exec-Sql "auth_db" @"
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check
  CHECK (role::text = ANY (ARRAY['DONOR','REQUESTER','BLOOD_BANK','ADMIN']::text[]));
"@
Exec-Sql "notification_db" @"
ALTER TABLE notification_logs DROP CONSTRAINT IF EXISTS notification_logs_recipient_type_check;
ALTER TABLE notification_logs ADD CONSTRAINT notification_logs_recipient_type_check
  CHECK (recipient_type::text = ANY (ARRAY['DONOR','REQUESTER','BLOOD_BANK']::text[]));
"@

# rewards_db: legacy columns block inserts (entity uses donation_count, not total_donations)
Exec-Sql "rewards_db" @"
ALTER TABLE reward_accounts ALTER COLUMN total_donations DROP NOT NULL;
ALTER TABLE reward_accounts ALTER COLUMN total_donations SET DEFAULT 0;
UPDATE reward_accounts SET total_donations = COALESCE(total_donations, donation_count, 0) WHERE total_donations IS NULL;
"@

# --- donor_db: phone optional + remove invalid donors ---
Exec-Sql "donor_db" @"
ALTER TABLE donors ALTER COLUMN phone DROP NOT NULL;

DELETE FROM donors
WHERE blood_group IS NULL
   OR blood_group !~ '^(A|B|AB|O)[+-]$'
   OR city IS NULL OR btrim(city) = ''
   OR name IS NULL OR btrim(name) = ''
   OR (phone IS NOT NULL AND phone !~ '^[6-9][0-9]{9}$')
   OR latitude IS NULL OR longitude IS NULL
   OR latitude < -90 OR latitude > 90
   OR longitude < -180 OR longitude > 180;
"@

# --- auth_db: keep valid roles/emails/names ---
Exec-Sql "auth_db" @"
DELETE FROM users
WHERE full_name IS NULL OR btrim(full_name) = ''
   OR email IS NULL OR email !~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'
   OR role IS NULL OR role NOT IN ('DONOR','REQUESTER','BLOOD_BANK','ADMIN');
"@

# --- request_db: valid blood requests only ---
Exec-Sql "request_db" @"
DELETE FROM blood_requests
WHERE patient_name IS NULL OR btrim(patient_name) = ''
   OR blood_group IS NULL OR blood_group !~ '^(A|B|AB|O)[+-]$'
   OR units_needed IS NULL OR units_needed < 1
   OR hospital_name IS NULL OR btrim(hospital_name) = ''
   OR city IS NULL OR btrim(city) = ''
   OR status IS NULL OR status NOT IN (
        'PENDING','MATCHED','CONFIRMED','BANK_RESERVED','OUT_FOR_DELIVERY',
        'FULFILLED','NO_DONORS_FOUND','CANCELLED'
      )
   OR urgency IS NULL OR urgency NOT IN ('CRITICAL','HIGH','NORMAL');
"@

# --- inventory_db: valid stock batches ---
Exec-Sql "inventory_db" @"
DELETE FROM inventory_batches
WHERE blood_bank_name IS NULL OR btrim(blood_bank_name) = ''
   OR city IS NULL OR btrim(city) = ''
   OR blood_group IS NULL OR blood_group !~ '^(A|B|AB|O)[+-]$'
   OR units_available IS NULL OR units_available < 0
   OR collected_date IS NULL OR expiry_date IS NULL
   OR expiry_date < collected_date
   OR status IS NULL OR status NOT IN ('ACTIVE','EXPIRED','DEPLETED');
"@

# --- matching_db: drop orphan / invalid matches ---
Exec-Sql "matching_db" @"
DELETE FROM matches
WHERE request_id IS NULL OR donor_id IS NULL
   OR response_status IS NULL
   OR response_status NOT IN ('PENDING','ACCEPTED','DECLINED','TIMED_OUT');
"@

# --- notification logs are operational noise; clear failed junk subjects ---
Exec-Sql "notification_db" @"
DELETE FROM notification_logs
WHERE subject IS NULL OR btrim(subject) = ''
   OR channel IS NULL OR channel NOT IN ('EMAIL','SMS','PUSH')
   OR status IS NULL OR status NOT IN ('SENT','FAILED');
"@

# --- analytics: drop metrics with invalid blood group / status ---
$analyticsTables = & $psql -U postgres -h 127.0.0.1 -d analytics_db -tAc "SELECT tablename FROM pg_tables WHERE schemaname='public';"
if ($analyticsTables -match "request_metric") {
    Exec-Sql "analytics_db" @"
DELETE FROM request_metrics
WHERE blood_group IS NOT NULL AND blood_group !~ '^(A|B|AB|O)[+-]$'
   OR status IS NOT NULL AND status NOT IN (
        'PENDING','MATCHED','CONFIRMED','BANK_RESERVED','OUT_FOR_DELIVERY',
        'FULFILLED','NO_DONORS_FOUND','CANCELLED'
      );
"@
}

Write-Host ""
Write-Host "Post-clean counts:" -ForegroundColor Green
foreach ($pair in @(
    @{db='auth_db'; sql='SELECT COUNT(*) FROM users'},
    @{db='donor_db'; sql='SELECT COUNT(*) FROM donors'},
    @{db='request_db'; sql='SELECT COUNT(*) FROM blood_requests'},
    @{db='inventory_db'; sql='SELECT COUNT(*) FROM inventory_batches'},
    @{db='matching_db'; sql='SELECT COUNT(*) FROM matches'},
    @{db='notification_db'; sql='SELECT COUNT(*) FROM notification_logs'}
)) {
    $n = & $psql -U postgres -h 127.0.0.1 -d $pair.db -tAc $pair.sql
    Write-Host ("  {0,-16} {1}" -f $pair.db, $n.Trim())
}

Write-Host ""
Write-Host "Done. Phone column is now nullable on donors." -ForegroundColor Green
