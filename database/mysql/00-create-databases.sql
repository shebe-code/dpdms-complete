CREATE DATABASE IF NOT EXISTS dpdms_auth;
CREATE DATABASE IF NOT EXISTS dpdms_flood;
CREATE DATABASE IF NOT EXISTS dpdms_drought;
CREATE DATABASE IF NOT EXISTS dpdms_fire;
CREATE DATABASE IF NOT EXISTS dpdms_zoonotic;
CREATE DATABASE IF NOT EXISTS dpdms_mining;
CREATE DATABASE IF NOT EXISTS dpdms_alerts;

GRANT ALL PRIVILEGES ON dpdms_auth.* TO 'dpdms'@'%';
GRANT ALL PRIVILEGES ON dpdms_flood.* TO 'dpdms'@'%';
GRANT ALL PRIVILEGES ON dpdms_drought.* TO 'dpdms'@'%';
GRANT ALL PRIVILEGES ON dpdms_fire.* TO 'dpdms'@'%';
GRANT ALL PRIVILEGES ON dpdms_zoonotic.* TO 'dpdms'@'%';
GRANT ALL PRIVILEGES ON dpdms_mining.* TO 'dpdms'@'%';
GRANT ALL PRIVILEGES ON dpdms_alerts.* TO 'dpdms'@'%';

FLUSH PRIVILEGES;
      