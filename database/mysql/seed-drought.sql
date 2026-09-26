USE dpdms_drought;
INSERT INTO drought_incidents (ward,district,province,occurrence_at,reporter,severity,status,latitude,longitude,created_at,updated_at,rainfall_deficit_mm,consecutive_dry_days,crop_failure_percentage,people_facing_water_shortages,livestock_mortality_count)
VALUES ('Ward 2','Rushinga','Mashonaland Central',NOW()-INTERVAL 6 DAY,'drought.recorder','CRITICAL','APPROVED',-16.68,31.43,NOW(),NOW(),68.0,44,61.0,780,24);
