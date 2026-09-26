USE dpdms_zoonotic;
CREATE TABLE IF NOT EXISTS zoonotic_incidents (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,

  ward VARCHAR(100) NOT NULL,
  district VARCHAR(100) NOT NULL,
  province VARCHAR(100) NOT NULL,
  occurrence_at DATETIME NOT NULL,
  reporter VARCHAR(150) NOT NULL,
  severity VARCHAR(20) NOT NULL,
  status VARCHAR(30) NOT NULL,
  latitude DECIMAL(10,7) NOT NULL,
  longitude DECIMAL(10,7) NOT NULL,
  created_at DATETIME,
  updated_at DATETIME,
  pathogen_name VARCHAR(150) NOT NULL,
  animal_species VARCHAR(150) NOT NULL,
  confirmed_human_cases INT NOT NULL,
  confirmed_animal_cases INT NOT NULL,
  event_classification VARCHAR(40) NOT NULL

);
CREATE TABLE IF NOT EXISTS zoonotic_audit (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  incident_id BIGINT NOT NULL,
  actor_username VARCHAR(150),
  actor_role VARCHAR(80),
  action VARCHAR(50),
  old_status VARCHAR(30),
  new_status VARCHAR(30),
  note VARCHAR(500),
  changed_at DATETIME
);
