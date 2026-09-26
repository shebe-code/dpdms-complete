USE dpdms_mining;
CREATE TABLE IF NOT EXISTS mining_incidents (
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
  mine_name VARCHAR(150) NOT NULL,
  mine_type VARCHAR(40) NOT NULL,
  accident_type VARCHAR(80) NOT NULL,
  trapped_or_injured_miners INT NOT NULL,
  fatalities INT NOT NULL,
  rescue_ongoing BOOLEAN NOT NULL

);
CREATE TABLE IF NOT EXISTS mining_audit (
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
