import uuid from "react-uuid";

const STORAGE_KEY = "crud-initializr.saved-projects.v1";
const SCHEMA_VERSION = 1;

const defaultAttribute = () => ({
  uuid: uuid(),
  name: "",
  type: "STRING",
  customType: "",
  validations: [],
  minValue: "",
  maxValue: "",
});

const normalizeAttribute = (attribute) => ({
  ...defaultAttribute(),
  ...(attribute || {}),
  uuid: attribute?.uuid || uuid(),
  name: typeof attribute?.name === "string" ? attribute.name : "",
  customType:
    typeof attribute?.customType === "string" ? attribute.customType : "",
  validations: Array.isArray(attribute?.validations)
    ? attribute.validations
    : [],
  minValue: attribute?.minValue ?? "",
  maxValue: attribute?.maxValue ?? "",
});

export const normalizeEntity = (entity) => ({
  uuid: entity?.uuid || uuid(),
  entityName: typeof entity?.entityName === "string" ? entity.entityName : "",
  embedded: !!entity?.embedded,
  cacheEnabled: entity?.cacheEnabled !== false,
  attributes: Array.isArray(entity?.attributes)
    ? entity.attributes.map(normalizeAttribute)
    : [],
});

export const normalizeProject = (project) => {
  const name = String(project?.name ?? "").trim();
  const createdAt = Number(project?.createdAt) || Date.now();
  const updatedAt = Number(project?.updatedAt) || createdAt;
  const cache = project?.cache || {};

  return {
    id: project?.id || uuid(),
    name,
    database: String(project?.database || "POSTGRESQL").toUpperCase(),
    apiType: project?.apiType === "GRAPHQL" ? "GRAPHQL" : "REST",
    cache: {
      enabled: !!cache.enabled,
      prefix: typeof cache.prefix === "string" ? cache.prefix : "crud",
      ttlSeconds: Number(cache.ttlSeconds) > 0 ? Number(cache.ttlSeconds) : 600,
      cacheFindAll: !!cache.cacheFindAll,
    },
    entities: Array.isArray(project?.entities)
      ? project.entities.map(normalizeEntity)
      : [],
    createdAt,
    updatedAt,
  };
};

const readStore = () => {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return [];
    }

    const parsed = JSON.parse(raw);
    const items = Array.isArray(parsed?.items)
      ? parsed.items
      : Array.isArray(parsed)
        ? parsed
        : [];

    return items
      .filter((item) => item && typeof item === "object")
      .map(normalizeProject)
      .filter((item) => item.name)
      .sort((a, b) => b.updatedAt - a.updatedAt);
  } catch {
    return [];
  }
};

const writeStore = (projects) => {
  const payload = {
    version: SCHEMA_VERSION,
    items: projects,
  };
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
};

export const listProjects = () => readStore();

export const saveProject = (project) => {
  const now = Date.now();
  const projects = readStore();
  const normalized = normalizeProject({ ...project, updatedAt: now });
  const existingIndex = projects.findIndex((item) => item.id === normalized.id);
  const saved = {
    ...normalized,
    createdAt:
      existingIndex >= 0 ? projects[existingIndex].createdAt : now,
  };

  if (existingIndex >= 0) {
    projects[existingIndex] = saved;
  } else {
    projects.push(saved);
  }

  const sorted = projects.sort((a, b) => b.updatedAt - a.updatedAt);
  writeStore(sorted);
  return { project: saved, projects: sorted };
};

export const deleteProject = (projectId) => {
  const projects = readStore().filter((project) => project.id !== projectId);
  writeStore(projects);
  return projects;
};

export const projectStorageKey = STORAGE_KEY;
