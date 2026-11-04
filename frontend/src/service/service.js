import { saveAs } from "file-saver";
import { translate } from "../i18n/translations";

const API_BASE_URL = (process.env.REACT_APP_API_URL || "").replace(/\/+$/, "");

export const formatStringToCamelCase = (value) => {
  const words = String(value ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .trim()
    .split(/[^A-Za-z0-9]+/)
    .filter(Boolean);

  if (words.length === 0) {
    return "";
  }

  return words
    .map((word, index) =>
      index === 0
        ? word.charAt(0).toLowerCase() + word.slice(1)
        : word.charAt(0).toUpperCase() + word.slice(1).toLowerCase(),
    )
    .join("");
};

const createRequestBody = (data) => ({
  title: data.businessName.trim(),
  database: data.database || "POSTGRESQL",
  apiType: data.apiType || "REST",
  cache: data.cache || {
    enabled: false,
    prefix: "crud",
    ttlSeconds: 600,
    cacheFindAll: false,
  },
  entities: data.entities.map((entity) => {
    const attributes = {};
    entity.attributes.forEach((attribute) => {
      const name = formatStringToCamelCase(attribute.name);
      if (!name) {
        return;
      }
      attributes[name] = {
        type: attribute.type,
        customType: attribute.customType
          ? formatStringToCamelCase(attribute.customType)
          : "",
        validations: attribute.validations,
        minValue:
          attribute.minValue === "" || attribute.minValue === undefined
            ? null
            : attribute.minValue,
        maxValue:
          attribute.maxValue === "" || attribute.maxValue === undefined
            ? null
            : attribute.maxValue,
      };
    });
    return {
      entityName: formatStringToCamelCase(entity.entityName),
      attributes,
      embedded: !!entity.embedded,
      cacheEnabled: entity.cacheEnabled !== false,
    };
  }),
});

const service = {
  createProject: async (data, language = "es") => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/domain`, {
        method: "POST",
        body: JSON.stringify(createRequestBody(data)),
        headers: {
          "Content-Type": "application/json",
          "Accept-Language": language,
        },
      });

      if (!response.ok) {
        let error = translate(language, "service.createError");
        try {
          const body = await response.json();
          if (body?.error) {
            error = body.error;
          }
        } catch {
          // The error response was not JSON.
        }
        return { ok: false, error };
      }

      const blob = await response.blob();
      const fileName = formatStringToCamelCase(data.businessName) || "project";
      saveAs(blob, `${fileName}.zip`);
      return { ok: true };
    } catch {
      return {
        ok: false,
        error: translate(language, "service.connectionError"),
      };
    }
  },
};

export default service;
