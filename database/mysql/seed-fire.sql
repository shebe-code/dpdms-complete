USE dpdms_fire;
INSERT INTO fire_incidents (ward,district,province,occurrence_at,reporter,severity,status,latitude,longitude,created_at,updated_at,area_burned_hectares,suspected_cause,injuries_fatalities,structures_destroyed,active)
VALUES ('Ward 3','Rushinga','Mashonaland Central',NOW()-INTERVAL 2 DAY,'fire.recorder','HIGH','APPROVED',-16.72,31.36,NOW(),NOW(),12.5,'ACCIDENTAL',2,3,TRUE);
