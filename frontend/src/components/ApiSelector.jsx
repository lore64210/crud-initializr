import React from "react";
import { Typography } from "@mui/material";
import ApiIcon from "@mui/icons-material/Api";
import "../css/app.css";

const APIS = [
  {
    value: "REST",
    label: "REST",
    tag: "HTTP",
    hint: "Controllers REST por entidad",
    note: "Genera un controller REST por entidad con endpoints bajo /api/nombreEntidad y un manejador global de errores.",
  },
  {
    value: "GRAPHQL",
    label: "GraphQL",
    tag: "QUERY",
    hint: "Esquema y controllers GraphQL",
    note: "Genera un esquema GraphQL y un controller por entidad. Incluye GraphiQL en /graphiql para explorar el esquema y probar queries y mutations.",
  },
];

const ApiSelector = ({ value, onChange }) => {
  const selectedApi = APIS.find((api) => api.value === value) ?? APIS[0];

  return (
    <div className="database-selector database-selector--api">
      <div className="database-selector__header">
        <ApiIcon className="database-selector__icon" />
        <div className="database-selector__titles">
          <Typography className="database-selector__title">
            Tipo de API
          </Typography>
          <Typography className="database-selector__subtitle">
            Elegí cómo se exponen las operaciones CRUD de las entidades.
          </Typography>
        </div>
      </div>

      <div
        className="database-selector__options"
        role="radiogroup"
        aria-label="Tipo de API"
      >
        {APIS.map((api) => {
          const selected = value === api.value;
          return (
            <button
              key={api.value}
              type="button"
              role="radio"
              aria-checked={selected}
              aria-label={`${api.label}: ${api.hint}`}
              className={`database-option${
                selected ? " database-option--selected" : ""
              }`}
              onClick={() => onChange(api.value)}
            >
              <span className="database-option__top">
                <span className="database-option__label">{api.label}</span>
                <span className="database-option__tag">{api.tag}</span>
              </span>
              <span className="database-option__hint">{api.hint}</span>
            </button>
          );
        })}
      </div>

      <Typography className="database-selector__note">
        {selectedApi.note}
      </Typography>
    </div>
  );
};

export default ApiSelector;
