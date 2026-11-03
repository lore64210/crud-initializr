import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import {
  DEFAULT_LANGUAGE,
  LANGUAGE_STORAGE_KEY,
  LANGUAGES,
  detectLanguage,
  translate,
} from "./translations";

const LanguageContext = createContext(null);

export function LanguageProvider({ children }) {
  const [language, setLanguage] = useState(detectLanguage);

  useEffect(() => {
    try {
      window.localStorage.setItem(LANGUAGE_STORAGE_KEY, language);
    } catch {
      // Persisting the preference is best-effort.
    }
    document.documentElement.lang = language;
  }, [language]);

  const t = useCallback(
    (key, params) => translate(language, key, params),
    [language],
  );

  const changeLanguage = useCallback((nextLanguage) => {
    setLanguage(LANGUAGES.includes(nextLanguage) ? nextLanguage : DEFAULT_LANGUAGE);
  }, []);

  const toggleLanguage = useCallback(() => {
    setLanguage((current) => (current === "es" ? "en" : "es"));
  }, []);

  const value = useMemo(
    () => ({ language, setLanguage: changeLanguage, toggleLanguage, t }),
    [language, changeLanguage, toggleLanguage, t],
  );

  return (
    <LanguageContext.Provider value={value}>
      {children}
    </LanguageContext.Provider>
  );
}

export function useLanguage() {
  const context = useContext(LanguageContext);
  if (!context) {
    throw new Error("useLanguage must be used inside a LanguageProvider");
  }
  return context;
}
