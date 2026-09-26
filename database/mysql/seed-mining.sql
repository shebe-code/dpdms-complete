USE dpdms_mining;
INSERT INTO mining_incidents (ward,district,province,occurrence_at,reporter,severity,status,latitude,longitude,created_at,updated_at,mine_name,mine_type,accident_type,trapped_or_injured_miners,fatalities,rescue_ongoing)
VALUES ('Ward 5','Rushinga','Mashonaland Central',NOW()-INTERVAL 4 DAY,'mining.recorder','CRITICAL','APPROVED',-16.70,31.49,NOW(),NOW(),'Rushinga East Mine','ARTISANAL','COLLAPSE',7,1,TRUE);
