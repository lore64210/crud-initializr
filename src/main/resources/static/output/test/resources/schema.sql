DROP TABLE IF EXISTS entidad_uno;
DROP TABLE IF EXISTS entidad_dos;
DROP TABLE IF EXISTS entidad_tres;


CREATE TABLE IF NOT EXISTS entidad_uno(
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	fecha_creacion DATETIME,
	atributo_uno VARCHAR(255)
	);

CREATE TABLE IF NOT EXISTS entidad_dos(
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	fecha_creacion DATETIME,
	entidad_uno_id BIGINT,
	CONSTRAINT fk_entidad_uno_entidad_dos FOREIGN KEY (entidad_uno_id) REFERENCES entidad_uno(id),
	entidad_tres_id BIGINT,
	CONSTRAINT fk_atributo_tres_entidad_tres FOREIGN KEY (entidad_tres_id) REFERENCES entidad_tres(id)
	);

CREATE TABLE IF NOT EXISTS entidad_tres(
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	fecha_creacion DATETIME,
	entidad_uno_id BIGINT,
	CONSTRAINT fk_entidad_uno_entidad_tres FOREIGN KEY (entidad_uno_id) REFERENCES entidad_uno(id)
	);

