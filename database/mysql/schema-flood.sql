USE dpdms_flood;
CREATE TABLE IF NOT EXISTS flood_incidents (
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
  peak_water_level_metres DECIMAL(10,2) NOT NULL,
  river_basin VARCHAR(150) NOT NULL,
  households_displaced INT NOT NULL,
  area_flooded_hectares DECIMAL(12,2) NOT NULL,
  duration_days INT NOT NULL

);
CREATE TABLE IF NOT EXISTS flood_audit (
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
