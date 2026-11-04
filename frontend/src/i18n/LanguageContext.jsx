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
  LANGUAGE_URL_KEYS,
  LANGUAGES,
  detectLanguage,
  readLanguageFromUrl,
  translate,
} from "./translations";

const LanguageContext = createContext(null);

const PATH_LANGUAGE_PATTERN = /^\/(en|es)(?=\/|$)/;

/**
 * Keeps the selected language in the URL so the current view can be linked or
 * embedded from the portfolio, e.g. {@code /?lang=en} or {@code /en}.
 */
const syncLanguageInUrl = (language) => {
  if (typeof window === "undefined") {
    return;
  }

  try {
    const url = new URL(window.location.href);
    const pathMatch = PATH_LANGUAGE_PATTERN.test(url.pathname);
    const queryKey = LANGUAGE_URL_KEYS.find((key) => url.searchParams.has(key));
    const hashParams = new URLSearchParams(url.hash.replace(/^#/, ""));
    const hashKey = LANGUAGE_URL_KEYS.find((key) => hashParams.has(key));

    if (pathMatch) {
      url.pathname = url.pathname.replace(PATH_LANGUAGE_PATTERN, `/${language}`);
    } else if (queryKey) {
      url.searchParams.set(queryKey, language);
    } else if (hashKey) {
      hashParams.set(hashKey, language);
      url.hash = hashParams.toString();
    } else if (language !== DEFAULT_LANGUAGE) {
      url.searchParams.set("lang", language);
    } else {
      return;
    }

    window.history.replaceState(window.history.state, "", url.toString());
  } catch {
    // The URL is only a convenience; failing to update it must not break the UI.
  }
};

export function LanguageProvider({ children }) {
  const [language, setLanguage] = useState(detectLanguage);

  useEffect(() => {
    try {
      window.localStorage.setItem(LANGUAGE_STORAGE_KEY, language);
    } catch {
      // Persisting the preference is best-effort.
    }
    document.documentElement.lang = language;
    syncLanguageInUrl(language);
  }, [language]);

  useEffect(() => {
    const handlePopState = () => {
      const fromUrl = readLanguageFromUrl();
      if (fromUrl) {
        setLanguage(fromUrl);
      }
    };

    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

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
