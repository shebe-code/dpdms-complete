USE dpdms_zoonotic;
INSERT INTO zoonotic_incidents (ward,district,province,occurrence_at,reporter,severity,status,latitude,longitude,created_at,updated_at,pathogen_name,animal_species,confirmed_human_cases,confirmed_animal_cases,event_classification)
VALUES ('Ward 4','Rushinga','Mashonaland Central',NOW()-INTERVAL 1 DAY,'zoonotic.recorder','CRITICAL','APPROVED',-16.64,31.46,NOW(),NOW(),'Anthrax','Cattle',6,14,'OUTBREAK');
