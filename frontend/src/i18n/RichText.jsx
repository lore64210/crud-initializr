import React from "react";

const TOKEN_REGEX = /(\*\*[^*]+\*\*|`[^`]+`)/g;

/**
 * Renders a translated string that may contain **bold** and `code` markers.
 * Keeping the markers in the translation files lets each language phrase the
 * sentence naturally without splitting it into tiny fragments.
 */
export default function RichText({ text }) {
  const parts = String(text ?? "")
    .split(TOKEN_REGEX)
    .filter((part) => part !== "");

  return (
    <>
      {parts.map((part, index) => {
        if (part.startsWith("**") && part.endsWith("**")) {
          return <strong key={index}>{part.slice(2, -2)}</strong>;
        }
        if (part.startsWith("`") && part.endsWith("`")) {
          return <code key={index}>{part.slice(1, -1)}</code>;
        }
        return <React.Fragment key={index}>{part}</React.Fragment>;
      })}
    </>
  );
}
