DROP TABLE IF EXISTS entrevista_principal;
DROP TABLE IF EXISTS usuario_papurro;


CREATE TABLE IF NOT EXISTS entrevista_principal(
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	fecha_creacion DATETIME,
	titulo_chevere VARCHAR(255),
	usuario_papurro_id BIGINT,
	CONSTRAINT fk_usuario_papurro FOREIGN KEY (usuario_papurro_id) REFERENCES usuario_papurro(id)
	);

CREATE TABLE IF NOT EXISTS usuario_papurro(
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	fecha_creacion DATETIME,
	ni_idea BIGINT
	);

