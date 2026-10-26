import React from "react";
import { Typography } from "@mui/material";
import StorageIcon from "@mui/icons-material/Storage";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import "../css/app.css";

const DATABASES = [
  {
    value: "POSTGRESQL",
    label: "PostgreSQL",
    tag: "SQL",
    hint: "Recomendada para producción",
    note: "Recomendada para producción. El proyecto incluye Docker Compose y los tests usan H2 en memoria.",
  },
  {
    value: "MARIADB",
    label: "MariaDB",
    tag: "SQL",
    hint: "Compatible con MySQL",
    note: "Compatible con MySQL. El proyecto incluye Docker Compose y los tests usan H2 en memoria.",
  },
  {
    value: "MYSQL",
    label: "MySQL",
    tag: "SQL",
    hint: "Servidor · Docker",
    note: "La opción clásica. El proyecto incluye Docker Compose y los tests usan H2 en memoria.",
  },
  {
    value: "SQLITE",
    label: "SQLite",
    tag: "FILE",
    hint: "Archivo local, sin servidor",
    note: "Archivo local, sin servidor. La conexión habilita foreign_keys y los tests usan H2 en memoria.",
  },
  {
    value: "H2",
    label: "H2",
    tag: "DEV",
    hint: "Embebida, sin servidor",
    note: "Embebida: ideal para probar sin instalar nada. Incluye consola H2 y los tests usan H2 en memoria.",
  },
  {
    value: "MONGODB",
    label: "MongoDB",
    tag: "NoSQL",
    hint: "Documental con composiciones",
    note: "Documental: entidades raíz y composiciones con documentos embebidos. Incluye Docker Compose y tests con Testcontainers.",
  },
];

const DatabaseSelector = ({ value, onChange, warning }) => {
  const selectedDatabase =
    DATABASES.find((database) => database.value === value) ?? DATABASES[0];

  return (
    <div className="database-selector">
      <div className="database-selector__header">
        <StorageIcon className="database-selector__icon" />
        <div className="database-selector__titles">
          <Typography className="database-selector__title">
            Base de datos
          </Typography>
          <Typography className="database-selector__subtitle">
            El proyecto se genera con la configuración y la capa de persistencia
            de este motor.
          </Typography>
        </div>
      </div>

      <div
        className="database-selector__options"
        role="radiogroup"
        aria-label="Base de datos"
      >
        {DATABASES.map((database) => {
          const selected = value === database.value;
          return (
            <button
              key={database.value}
              type="button"
              role="radio"
              aria-checked={selected}
              aria-label={`${database.label}: ${database.hint}`}
              className={`database-option${
                selected ? " database-option--selected" : ""
              }`}
              onClick={() => onChange(database.value)}
            >
              <span className="database-option__top">
                <span className="database-option__label">{database.label}</span>
                <span className="database-option__tag">{database.tag}</span>
              </span>
              <span className="database-option__hint">{database.hint}</span>
            </button>
          );
        })}
      </div>

      <Typography className="database-selector__note">
        {selectedDatabase.note}
      </Typography>

      {warning && (
        <div className="compatibility-warning" role="status">
          <WarningAmberIcon className="compatibility-warning__icon" />
          <div className="compatibility-warning__content">
            {warning.title && (
              <span className="compatibility-warning__title">
                {warning.title}
              </span>
            )}
            <span className="compatibility-warning__text">
              {warning.message ?? warning}
            </span>
          </div>
        </div>
      )}
    </div>
  );
};

export default DatabaseSelector;
