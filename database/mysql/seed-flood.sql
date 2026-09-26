USE dpdms_flood;
INSERT INTO flood_incidents (ward,district,province,occurrence_at,reporter,severity,status,latitude,longitude,created_at,updated_at,peak_water_level_metres,river_basin,households_displaced,area_flooded_hectares,duration_days)
VALUES ('Ward 1','Rushinga','Mashonaland Central',NOW()-INTERVAL 3 DAY,'flood.recorder','HIGH','APPROVED',-16.60,31.40,NOW(),NOW(),5.4,'Mazowe Catchment',72,18.5,3);
