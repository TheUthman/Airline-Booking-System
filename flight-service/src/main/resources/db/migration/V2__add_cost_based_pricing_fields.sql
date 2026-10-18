-- New columns are nullable so existing catalogue rows survive the migration.
-- Populate aircraft specifications and airport coordinates before using fare estimates.
ALTER TABLE aircraft ADD COLUMN IF NOT EXISTS fuel_consumption_litres_per_hour NUMERIC(12,3);
ALTER TABLE aircraft ADD COLUMN IF NOT EXISTS cruise_speed_kmh NUMERIC(8,2);
ALTER TABLE aircraft ADD COLUMN IF NOT EXISTS operating_cost_per_hour NUMERIC(12,2);
ALTER TABLE aircraft ADD COLUMN IF NOT EXISTS maintenance_cost_per_hour NUMERIC(12,2);

ALTER TABLE airports ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE airports ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;
ALTER TABLE airports ADD COLUMN IF NOT EXISTS estimated_airport_charge NUMERIC(12,2) DEFAULT 0;
