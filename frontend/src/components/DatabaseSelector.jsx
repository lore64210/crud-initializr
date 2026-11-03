import React from "react";
import { Typography } from "@mui/material";
import StorageIcon from "@mui/icons-material/Storage";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import { useLanguage } from "../i18n/LanguageContext";
import "../css/app.css";

const DATABASES = [
  {
    value: "POSTGRESQL",
    label: "PostgreSQL",
    tag: "SQL",
    hintKey: "database.postgres.hint",
    noteKey: "database.postgres.note",
  },
  {
    value: "MARIADB",
    label: "MariaDB",
    tag: "SQL",
    hintKey: "database.mariadb.hint",
    noteKey: "database.mariadb.note",
  },
  {
    value: "MYSQL",
    label: "MySQL",
    tag: "SQL",
    hintKey: "database.mysql.hint",
    noteKey: "database.mysql.note",
  },
  {
    value: "SQLITE",
    label: "SQLite",
    tag: "FILE",
    hintKey: "database.sqlite.hint",
    noteKey: "database.sqlite.note",
  },
  {
    value: "H2",
    label: "H2",
    tag: "DEV",
    hintKey: "database.h2.hint",
    noteKey: "database.h2.note",
  },
  {
    value: "MONGODB",
    label: "MongoDB",
    tag: "NoSQL",
    hintKey: "database.mongodb.hint",
    noteKey: "database.mongodb.note",
  },
];

const DatabaseSelector = ({ value, onChange, warning }) => {
  const { t } = useLanguage();
  const selectedDatabase =
    DATABASES.find((database) => database.value === value) ?? DATABASES[0];

  return (
    <div className="database-selector">
      <div className="database-selector__header">
        <StorageIcon className="database-selector__icon" />
        <div className="database-selector__titles">
          <Typography className="database-selector__title">
            {t("database.title")}
          </Typography>
          <Typography className="database-selector__subtitle">
            {t("database.subtitle")}
          </Typography>
        </div>
      </div>

      <div
        className="database-selector__options"
        role="radiogroup"
        aria-label={t("database.aria")}
      >
        {DATABASES.map((database) => {
          const selected = value === database.value;
          const hint = t(database.hintKey);
          return (
            <button
              key={database.value}
              type="button"
              role="radio"
              aria-checked={selected}
              aria-label={`${database.label}: ${hint}`}
              className={`database-option${
                selected ? " database-option--selected" : ""
              }`}
              onClick={() => onChange(database.value)}
            >
              <span className="database-option__top">
                <span className="database-option__label">{database.label}</span>
                <span className="database-option__tag">{database.tag}</span>
              </span>
              <span className="database-option__hint">{hint}</span>
            </button>
          );
        })}
      </div>

      <Typography className="database-selector__note">
        {t(selectedDatabase.noteKey)}
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
