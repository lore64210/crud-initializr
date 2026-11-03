import React from "react";
import ReactDOM from "react-dom/client";
import HomePage from "./components/homePage";
import { LanguageProvider } from "./i18n/LanguageContext";
import "./css/app.css";

const root = ReactDOM.createRoot(document.getElementById("root"));

root.render(
  <LanguageProvider>
    <HomePage />
  </LanguageProvider>,
);
