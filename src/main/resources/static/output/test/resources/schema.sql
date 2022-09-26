DROP TABLE IF EXISTS generic_file;


CREATE TABLE IF NOT EXISTS generic_file(
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	fecha_creacion DATETIME,
	project_owner VARCHAR(255),
	url VARCHAR(255)
	);

