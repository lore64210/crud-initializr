import React from "react";
import { Typography } from "@mui/material";
import ApiIcon from "@mui/icons-material/Api";
import { useLanguage } from "../i18n/LanguageContext";
import "../css/app.css";

const APIS = [
  {
    value: "REST",
    label: "REST",
    tag: "HTTP",
    hintKey: "api.rest.hint",
    noteKey: "api.rest.note",
  },
  {
    value: "GRAPHQL",
    label: "GraphQL",
    tag: "QUERY",
    hintKey: "api.graphql.hint",
    noteKey: "api.graphql.note",
  },
];

const ApiSelector = ({ value, onChange }) => {
  const { t } = useLanguage();
  const selectedApi = APIS.find((api) => api.value === value) ?? APIS[0];

  return (
    <div className="database-selector database-selector--api">
      <div className="database-selector__header">
        <ApiIcon className="database-selector__icon" />
        <div className="database-selector__titles">
          <Typography className="database-selector__title">
            {t("api.title")}
          </Typography>
          <Typography className="database-selector__subtitle">
            {t("api.subtitle")}
          </Typography>
        </div>
      </div>

      <div
        className="database-selector__options"
        role="radiogroup"
        aria-label={t("api.aria")}
      >
        {APIS.map((api) => {
          const selected = value === api.value;
          const hint = t(api.hintKey);
          return (
            <button
              key={api.value}
              type="button"
              role="radio"
              aria-checked={selected}
              aria-label={`${api.label}: ${hint}`}
              className={`database-option${
                selected ? " database-option--selected" : ""
              }`}
              onClick={() => onChange(api.value)}
            >
              <span className="database-option__top">
                <span className="database-option__label">{api.label}</span>
                <span className="database-option__tag">{api.tag}</span>
              </span>
              <span className="database-option__hint">{hint}</span>
            </button>
          );
        })}
      </div>

      <Typography className="database-selector__note">
        {t(selectedApi.noteKey)}
      </Typography>
    </div>
  );
};

export default ApiSelector;
