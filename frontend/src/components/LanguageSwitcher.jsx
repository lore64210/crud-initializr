import React from "react";
import { useLanguage } from "../i18n/LanguageContext";
import "../css/app.css";

const OPTIONS = [
  { value: "es", labelKey: "language.es", short: "ES" },
  { value: "en", labelKey: "language.en", short: "EN" },
];

const LanguageSwitcher = () => {
  const { language, setLanguage, t } = useLanguage();
  const current = OPTIONS.find((option) => option.value === language);

  return (
    <div
      className="language-switcher"
      role="group"
      aria-label={t("language.label")}
    >
      {OPTIONS.map((option) => {
        const selected = option.value === language;
        const label = t(option.labelKey);
        return (
          <button
            key={option.value}
            type="button"
            className={`language-switcher__option${
              selected ? " language-switcher__option--selected" : ""
            }`}
            aria-pressed={selected}
            aria-label={t("language.switchTo", { language: label })}
            title={label}
            onClick={() => setLanguage(option.value)}
          >
            {option.short}
          </button>
        );
      })}
      <span className="sr-only" aria-live="polite">
        {t("language.label")}: {current ? t(current.labelKey) : language}
      </span>
    </div>
  );
};

export default LanguageSwitcher;
