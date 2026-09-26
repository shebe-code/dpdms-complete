USE dpdms_alerts;
CREATE TABLE IF NOT EXISTS alert_logs (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 incident_id BIGINT,
 hazard VARCHAR(40),
 channel VARCHAR(20),
 recipient VARCHAR(200),
 delivery_status VARCHAR(30),
 message VARCHAR(1000),
 timestamp DATETIME
);
