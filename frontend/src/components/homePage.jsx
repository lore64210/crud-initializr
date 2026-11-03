import React, { useCallback, useState } from "react";
import EntityForm from "../components/EntityForm";
import DatabaseSelector from "../components/DatabaseSelector";
import ApiSelector from "../components/ApiSelector";
import CacheSettings from "../components/CacheSettings";
import ProjectsDrawer from "../components/ProjectsDrawer";
import SaveProjectDialog from "../components/SaveProjectDialog";
import OpenProjectDialog from "../components/OpenProjectDialog";
import LanguageSwitcher from "../components/LanguageSwitcher";
import { RelationshipGraphPanel } from "../components/RelationshipGraph";
import service, { formatStringToCamelCase } from "../service/service";
import {
  deleteProject,
  exportProjectsToFile,
  importProjectsFromText,
  listProjects,
  saveProject,
} from "../service/projectStorage";
import uuid from "react-uuid";
import {
  Snackbar,
  Alert,
  Button,
  TextField,
  Typography,
  IconButton,
  Tooltip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Fab,
  Badge,
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import CloseIcon from "@mui/icons-material/Close";
import DownloadIcon from "@mui/icons-material/Download";
import FolderOpenIcon from "@mui/icons-material/FolderOpen";
import HelpOutlineIcon from "@mui/icons-material/HelpOutline";
import SaveIcon from "@mui/icons-material/Save";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import { useLanguage } from "../i18n/LanguageContext";
import RichText from "../i18n/RichText";

import "../css/app.css";

const createEmptyAttribute = () => ({
  uuid: uuid(),
  name: "",
  type: "STRING",
  customType: "",
  validations: [],
  minValue: "",
  maxValue: "",
});

const SQL_RELATION_TYPES = [
  "CUSTOM",
  "CUSTOM_LIST",
  "CUSTOM_ONE_TO_ONE",
  "CUSTOM_MANY_TO_MANY",
];
const MONGO_COMPOSITION_TYPES = ["COMPOSITION", "COMPOSITION_LIST"];

const isRelationType = (type) => SQL_RELATION_TYPES.includes(type);
const isCompositionType = (type) => MONGO_COMPOSITION_TYPES.includes(type);
const isLinkType = (type) => isRelationType(type) || isCompositionType(type);
const isNumericType = (type) =>
  ["INTEGER", "LONG", "DECIMAL", "DOUBLE"].includes(type);

const createDefaultCache = () => ({
  enabled: false,
  prefix: "crud",
  ttlSeconds: 600,
  cacheFindAll: false,
});

const createEmptyEntity = () => ({
  uuid: uuid(),
  entityName: "",
  attributes: [createEmptyAttribute()],
  embedded: false,
  cacheEnabled: true,
});

const validateForm = (businessName, entities, database, cache, t) => {
  const title = businessName.trim();
  if (!title) {
    return t("validation.projectNameEmpty");
  }
  if (title.length > 30) {
    return t("validation.projectNameTooLong");
  }
  if (!formatStringToCamelCase(title)) {
    return t("validation.projectNameInvalid");
  }

  if (cache?.enabled) {
    const prefix = String(cache.prefix || "").trim();
    if (!prefix) {
      return t("validation.redisPrefixEmpty");
    }
    if (prefix.length > 50) {
      return t("validation.redisPrefixTooLong");
    }
    if (!/^[A-Za-z0-9:_-]+$/.test(prefix)) {
      return t("validation.redisPrefixChars");
    }
    if (!Number.isInteger(Number(cache.ttlSeconds)) || Number(cache.ttlSeconds) <= 0) {
      return t("validation.redisTtl");
    }
  }

  if (entities.length === 0) {
    return t("validation.noEntities");
  }

  const entityNames = new Set();
  for (let index = 0; index < entities.length; index++) {
    const entity = entities[index];
    const entityName = formatStringToCamelCase(entity.entityName);

    if (!entity.entityName.trim()) {
      return t("validation.entityNoName", { index: index + 1 });
    }
    if (!/^[A-Za-z][A-Za-z0-9]*$/.test(entityName)) {
      return t("validation.entityNameInvalid", {
        name: entity.entityName,
      });
    }
    if (entityName.length > 30) {
      return t("validation.entityNameTooLong", {
        name: entity.entityName,
      });
    }
    if (entityNames.has(entityName.toLowerCase())) {
      return t("validation.entityDuplicate", { name: entity.entityName });
    }
    entityNames.add(entityName.toLowerCase());

    const attributeNames = new Set();
    for (const attribute of entity.attributes) {
      const attributeName = formatStringToCamelCase(attribute.name);

      if (!attribute.name.trim()) {
        return t("validation.attributeNoName", {
          entity: entity.entityName,
        });
      }
      if (!/^[A-Za-z][A-Za-z0-9]*$/.test(attributeName)) {
        return t("validation.attributeNameInvalid", {
          name: attribute.name,
        });
      }
      if (attributeName.length > 30) {
        return t("validation.attributeNameTooLong", {
          name: attribute.name,
        });
      }
      if (attributeNames.has(attributeName.toLowerCase())) {
        return t("validation.attributeDuplicate", {
          attribute: attribute.name,
          entity: entity.entityName,
        });
      }
      attributeNames.add(attributeName.toLowerCase());

      if (isLinkType(attribute.type) && !attribute.customType?.trim()) {
        return t("validation.attributeNeedsTarget", {
          name: attribute.name,
        });
      }

      if (isNumericType(attribute.type)) {
        const hasMin = attribute.validations.includes("MIN");
        const hasMax = attribute.validations.includes("MAX");
        if (hasMin && attribute.minValue === "") {
          return t("validation.attributeNeedsMin", {
            name: attribute.name,
          });
        }
        if (hasMax && attribute.maxValue === "") {
          return t("validation.attributeNeedsMax", {
            name: attribute.name,
          });
        }
        if (
          hasMin &&
          hasMax &&
          Number(attribute.minValue) > Number(attribute.maxValue)
        ) {
          return t("validation.attributeMinGreaterThanMax", {
            name: attribute.name,
          });
        }
      }

      if (isLinkType(attribute.type)) {
        const targetName = formatStringToCamelCase(attribute.customType);
        const target = entities.find(
          (candidate) =>
            formatStringToCamelCase(candidate.entityName).toLowerCase() ===
            targetName.toLowerCase(),
        );

        if (!target) {
          return t("validation.attributeMissingTarget", {
            attribute: attribute.name,
            entity: entity.entityName,
            target: attribute.customType,
          });
        }
        if (target.uuid === entity.uuid) {
          return t("validation.attributeSelfReference", {
            attribute: attribute.name,
            entity: entity.entityName,
          });
        }
      }

      if (database === "MONGODB") {
        if (isRelationType(attribute.type)) {
          return t("validation.mongoNoRelations", {
            attribute: attribute.name,
            entity: entity.entityName,
          });
        }
        if (isCompositionType(attribute.type)) {
          if (entity.embedded) {
            return t("validation.embeddedNoComposition", {
              entity: entity.entityName,
            });
          }
          const target = entities.find(
            (candidate) =>
              formatStringToCamelCase(candidate.entityName).toLowerCase() ===
              formatStringToCamelCase(attribute.customType).toLowerCase(),
          );
          if (!target?.embedded) {
            return t("validation.compositionNeedsEmbedded", {
              attribute: attribute.name,
              entity: entity.entityName,
            });
          }
        }
      } else {
        if (entity.embedded) {
          return t("validation.embeddedOnlyMongo", {
            entity: entity.entityName,
          });
        }
        if (isCompositionType(attribute.type)) {
          return t("validation.compositionOnlyMongo", {
            attribute: attribute.name,
            entity: entity.entityName,
          });
        }
      }
    }
  }

  if (cache?.enabled) {
    const cacheableCount = entities.filter(
      (entity) => !entity.embedded && entity.cacheEnabled !== false,
    ).length;
    if (cacheableCount === 0) {
      return t("validation.cacheNoEntities");
    }
  }

  return "";
};

function HomePage() {
  const { t, language } = useLanguage();
  const [projectDownloaded, setProjectDownloaded] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [businessName, setBusinessName] = useState("");
  const [entities, setEntities] = useState(() => [createEmptyEntity()]);
  const [selectedEntityUuid, setSelectedEntityUuid] = useState(
    () => entities[0]?.uuid ?? null,
  );
  const [loading, setLoading] = useState(false);
  const [database, setDatabase] = useState("POSTGRESQL");
  const [apiType, setApiType] = useState("REST");
  const [cache, setCache] = useState(createDefaultCache);
  const [savedProjects, setSavedProjects] = useState(() => listProjects());
  const [currentProjectId, setCurrentProjectId] = useState(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [openProjectDialog, setOpenProjectDialog] = useState(null);
  const [saveDialogOpen, setSaveDialogOpen] = useState(false);
  const [pendingAfterSave, setPendingAfterSave] = useState(null);
  const [saveFeedback, setSaveFeedback] = useState("");
  const [helpOpen, setHelpOpen] = useState(false);
  const [mongoWarningOpen, setMongoWarningOpen] = useState(false);
  const [sqlWarningOpen, setSqlWarningOpen] = useState(false);
  const [pendingDatabase, setPendingDatabase] = useState(null);

  const selectedEntity =
    entities.find((entity) => entity.uuid === selectedEntityUuid) ?? null;

  const linkCount = entities.reduce(
    (total, entity) =>
      total +
      entity.attributes.filter(
        (attribute) =>
          isLinkType(attribute.type) && attribute.customType?.trim(),
      ).length,
    0,
  );

  const relationAttributeCount = entities.reduce(
    (total, entity) =>
      total +
      entity.attributes.filter((attribute) =>
        isRelationType(attribute.type),
      ).length,
    0,
  );

  const compositionAttributeCount = entities.reduce(
    (total, entity) =>
      total +
      entity.attributes.filter((attribute) =>
        isCompositionType(attribute.type),
      ).length,
    0,
  );

  const embeddedEntityCount = entities.filter((entity) => entity.embedded)
    .length;

  const documentMode = database === "MONGODB";
  const graphQlMode = apiType === "GRAPHQL";
  const mongoRelationWarning =
    documentMode && relationAttributeCount > 0
      ? t("home.mongoRelationsWarning", { count: relationAttributeCount })
      : "";
  const sqlCompositionWarning =
    !documentMode && (compositionAttributeCount > 0 || embeddedEntityCount > 0)
      ? t("home.sqlCompositionsWarning", {
          compositions: compositionAttributeCount,
          embedded: embeddedEntityCount,
        })
      : "";

  const changeDatabase = (nextDatabase) => {
    if (nextDatabase === database) {
      return;
    }
    if (nextDatabase === "MONGODB" && relationAttributeCount > 0) {
      setPendingDatabase(nextDatabase);
      setMongoWarningOpen(true);
      return;
    }
    if (
      database === "MONGODB" &&
      nextDatabase !== "MONGODB" &&
      (compositionAttributeCount > 0 || embeddedEntityCount > 0)
    ) {
      setPendingDatabase(nextDatabase);
      setSqlWarningOpen(true);
      return;
    }
    setDatabase(nextDatabase);
  };

  const confirmMongoSelection = () => {
    setDatabase(pendingDatabase || "MONGODB");
    setMongoWarningOpen(false);
    setPendingDatabase(null);
  };

  const confirmSqlSelection = () => {
    setEntities((current) =>
      current.map((entity) => ({ ...entity, embedded: false })),
    );
    setDatabase(pendingDatabase);
    setSqlWarningOpen(false);
    setPendingDatabase(null);
  };

  const createRelation = useCallback((entityUuid, targetName, { name, type }) => {
    setEntities((current) =>
      current.map((entity) =>
        entity.uuid === entityUuid
          ? {
              ...entity,
              attributes: [
                ...entity.attributes,
                {
                  uuid: uuid(),
                  name,
                  type,
                  customType: targetName,
                  validations: [],
                },
              ],
            }
          : entity,
      ),
    );
  }, []);

  const updateRelation = useCallback(
    (entityUuid, attributeUuid, { name, type, customType }) => {
      setEntities((current) =>
        current.map((entity) => {
          if (entity.uuid !== entityUuid) return entity;
          return {
            ...entity,
            attributes: entity.attributes.map((attribute) => {
              if (attribute.uuid !== attributeUuid) return attribute;
              return {
                ...attribute,
                name: name ?? attribute.name,
                type: type ?? attribute.type,
                customType: customType ?? attribute.customType,
                validations:
                  type && type !== attribute.type ? [] : attribute.validations,
              };
            }),
          };
        }),
      );
    },
    [],
  );

  const addEntity = () => {
    const newEntity = createEmptyEntity();
    setEntities((current) => [newEntity, ...current]);
    setSelectedEntityUuid(newEntity.uuid);
  };

  const changeEntityName = (entityUuid, entityName) => {
    setEntities((current) => {
      const previousName = current.find((entity) => entity.uuid === entityUuid)
        ?.entityName;

      return current.map((entity) => {
        const updatedEntity =
          entity.uuid === entityUuid ? { ...entity, entityName } : entity;

        if (!previousName || previousName === entityName) {
          return updatedEntity;
        }

        return {
          ...updatedEntity,
          attributes: updatedEntity.attributes.map((attribute) =>
            isLinkType(attribute.type) &&
            attribute.customType === previousName
              ? { ...attribute, customType: entityName }
              : attribute,
          ),
        };
      });
    });
  };

  const changeEmbedded = (entityUuid, embedded) => {
    setEntities((current) =>
      current.map((entity) =>
        entity.uuid === entityUuid ? { ...entity, embedded } : entity,
      ),
    );
  };

  const changeEntityCache = (entityUuid, cacheEnabled) => {
    setEntities((current) =>
      current.map((entity) =>
        entity.uuid === entityUuid ? { ...entity, cacheEnabled } : entity,
      ),
    );
  };

  const deleteEntity = (entityUuid) => {
    setEntities((current) => {
      const deletedEntity = current.find((entity) => entity.uuid === entityUuid);

      return current
        .filter((entity) => entity.uuid !== entityUuid)
        .map((entity) => ({
          ...entity,
          attributes: entity.attributes.map((attribute) =>
            deletedEntity &&
            isLinkType(attribute.type) &&
            attribute.customType === deletedEntity.entityName
              ? { ...attribute, customType: "" }
              : attribute,
          ),
        }));
    });

    setSelectedEntityUuid((current) => {
      if (current !== entityUuid) return current;
      const remaining = entities.filter((entity) => entity.uuid !== entityUuid);
      return remaining[0]?.uuid ?? null;
    });
  };

  const changeAttribute = (entityUuid, attribute) => {
    setEntities((current) =>
      current.map((entity) =>
        entity.uuid === entityUuid
          ? {
              ...entity,
              attributes: entity.attributes.map((currentAttribute) =>
                currentAttribute.uuid === attribute.uuid
                  ? attribute
                  : currentAttribute,
              ),
            }
          : entity,
      ),
    );
  };

  const addAttribute = (entityUuid) => {
    setEntities((current) =>
      current.map((entity) =>
        entity.uuid === entityUuid
          ? {
              ...entity,
              attributes: [...entity.attributes, createEmptyAttribute()],
            }
          : entity,
      ),
    );
  };

  const deleteAttribute = (entityUuid, attributeUuid) => {
    setEntities((current) =>
      current.map((entity) =>
        entity.uuid === entityUuid
          ? {
              ...entity,
              attributes: entity.attributes.filter(
                (attribute) => attribute.uuid !== attributeUuid,
              ),
            }
          : entity,
      ),
    );
  };

  const persistCurrentProject = (name) => {
    const trimmed = String(name || "").trim();
    if (!trimmed) {
      return false;
    }

    try {
      const { project, projects } = saveProject({
        id: currentProjectId || undefined,
        name: trimmed,
        database,
        apiType,
        cache,
        entities,
      });
      setSavedProjects(projects);
      setCurrentProjectId(project.id);
      setBusinessName(project.name);
      setSaveFeedback(t("home.feedback.saved", { name: project.name }));
      return true;
    } catch {
      setErrorMessage(t("home.feedback.saveError"));
      return false;
    }
  };

  const loadProject = (project) => {
    const loadedEntities =
      project.entities.length > 0
        ? JSON.parse(JSON.stringify(project.entities))
        : [createEmptyEntity()];

    setBusinessName(project.name);
    setDatabase(project.database || "POSTGRESQL");
    setApiType(project.apiType || "REST");
    setCache({ ...createDefaultCache(), ...(project.cache || {}) });
    setEntities(loadedEntities);
    setSelectedEntityUuid(loadedEntities[0]?.uuid ?? null);
    setCurrentProjectId(project.id);
    setDrawerOpen(false);
    setErrorMessage("");
    setProjectDownloaded(false);
  };

  const startNewProject = () => {
    const emptyEntity = createEmptyEntity();

    setBusinessName("");
    setEntities([emptyEntity]);
    setSelectedEntityUuid(emptyEntity.uuid);
    setDatabase("POSTGRESQL");
    setApiType("REST");
    setCache(createDefaultCache());
    setCurrentProjectId(null);
    setDrawerOpen(false);
    setErrorMessage("");
    setProjectDownloaded(false);
    setPendingDatabase(null);
    setMongoWarningOpen(false);
    setSqlWarningOpen(false);
  };

  const handleSaveClick = () => {
    if (!businessName.trim()) {
      setPendingAfterSave(null);
      setSaveDialogOpen(true);
      return;
    }
    persistCurrentProject(businessName);
  };

  const handleSaveDialogConfirm = (name) => {
    if (!persistCurrentProject(name)) {
      return;
    }

    setSaveDialogOpen(false);
    const pending = pendingAfterSave;
    setPendingAfterSave(null);

    if (pending) {
      setOpenProjectDialog(null);
      if (pending.project) {
        loadProject(pending.project);
      } else {
        startNewProject();
      }
    }
  };

  const cancelSaveDialog = () => {
    setSaveDialogOpen(false);
    setPendingAfterSave(null);
  };

  const requestOpenProject = (project) => {
    setOpenProjectDialog({ project });
  };

  const requestNewProject = () => {
    setDrawerOpen(false);
    setOpenProjectDialog({ project: null });
  };

  const handleDiscardAndContinue = () => {
    const target = openProjectDialog?.project ?? null;
    setOpenProjectDialog(null);
    if (target) {
      loadProject(target);
    } else {
      startNewProject();
    }
  };

  const handleSaveAndContinue = () => {
    const target = openProjectDialog?.project ?? null;

    if (!businessName.trim()) {
      setPendingAfterSave({ project: target });
      setOpenProjectDialog(null);
      setSaveDialogOpen(true);
      return;
    }

    if (!persistCurrentProject(businessName)) {
      return;
    }

    setOpenProjectDialog(null);
    if (target) {
      loadProject(target);
    } else {
      startNewProject();
    }
  };

  const handleDeleteProject = (projectId) => {
    try {
      setSavedProjects(deleteProject(projectId));
      if (currentProjectId === projectId) {
        setCurrentProjectId(null);
      }
      setSaveFeedback(t("home.feedback.deleted"));
    } catch {
      setErrorMessage(t("home.feedback.deleteError"));
    }
  };

  const handleExportProjects = () => {
    if (savedProjects.length === 0) {
      return;
    }

    try {
      exportProjectsToFile();
      setSaveFeedback(t("home.feedback.exported"));
    } catch {
      setErrorMessage(t("home.feedback.exportError"));
    }
  };

  const handleImportProjects = (text) => {
    try {
      const { projects, imported } = importProjectsFromText(text, language);
      setSavedProjects(projects);
      setSaveFeedback(
        imported === 1
          ? t("home.feedback.importedOne")
          : t("home.feedback.importedMany", { count: imported }),
      );
    } catch (error) {
      setErrorMessage(error?.message || t("home.feedback.importError"));
    }
  };

  const submit = async () => {
    const validationError = validateForm(
      businessName,
      entities,
      database,
      cache,
      t,
    );
    if (validationError) {
      setErrorMessage(validationError);
      return;
    }

    setErrorMessage("");
    setLoading(true);
    const result = await service.createProject(
      {
        businessName,
        entities,
        database,
        apiType,
        cache,
      },
      language,
    );
    setLoading(false);

    if (!result.ok) {
      setErrorMessage(result.error);
    } else {
      setProjectDownloaded(true);
    }
  };

  return (
    <div className="main">
      <div className="header-container">
        <div className="header">
          <div className="brand">
            <span className="brand__logo">
              <img
                src="/favicon.ico"
                alt="spring-logo"
                className="spring-logo"
              />
            </span>
            <div className="brand__titles">
              <Typography variant="h4" className="title">
                Spring CRUD Initializr
              </Typography>
              <span className="brand__subtitle">{t("home.subtitle")}</span>
            </div>
          </div>
          <div className="header__meta">
            <span className="header__badge">Spring Boot v4.1.2</span>
            <LanguageSwitcher />
          </div>
        </div>
      </div>
      <div className="bussines-name-input-container">
        <div className="project-bar">
          <Tooltip title={t("home.savedProjectsTooltip")} arrow>
            <IconButton
              className="project-bar__projects-button"
              aria-label={t("home.openSavedProjects")}
              onClick={() => setDrawerOpen(true)}
            >
              <Badge
                badgeContent={savedProjects.length}
                color="success"
                overlap="circular"
                invisible={savedProjects.length === 0}
              >
                <FolderOpenIcon />
              </Badge>
            </IconButton>
          </Tooltip>
          <TextField
            color="success"
            className="input project-name-input"
            id="project-name"
            label={t("home.projectName")}
            variant="outlined"
            size="small"
            value={businessName}
            onChange={({ target: { value } }) => setBusinessName(value)}
          />
          <Tooltip title={t("home.createTooltip")} arrow>
            <span className="create-project-tooltip-wrapper">
              <Button
                className="create-project-button"
                variant="contained"
                onClick={submit}
                color="success"
                startIcon={<DownloadIcon />}
                disabled={loading}
              >
                {loading ? t("home.creating") : t("home.create")}
              </Button>
            </span>
          </Tooltip>
          <Tooltip title={t("home.saveTooltip")} arrow>
            <Button
              className="save-project-button"
              variant="outlined"
              color="success"
              startIcon={<SaveIcon />}
              onClick={handleSaveClick}
            >
              {t("home.save")}
            </Button>
          </Tooltip>
        </div>
      </div>
      <DatabaseSelector
        value={database}
        onChange={changeDatabase}
        warning={
          mongoRelationWarning
            ? {
                title: t("home.mongoRelationsTitle"),
                message: mongoRelationWarning,
              }
            : sqlCompositionWarning
              ? {
                  title: t("home.sqlCompositionsTitle"),
                  message: sqlCompositionWarning,
                }
              : null
        }
      />
      <ApiSelector value={apiType} onChange={setApiType} />
      <CacheSettings
        value={cache}
        onChange={setCache}
        entities={entities}
        onToggleEntity={changeEntityCache}
      />
      <div className="workspace">
        <div className="workspace__graph">
          <RelationshipGraphPanel
            entities={entities}
            relationCount={linkCount}
            database={database}
            selectedEntityUuid={selectedEntityUuid}
            onSelectEntity={setSelectedEntityUuid}
            onAddEntity={addEntity}
            onCreateRelation={createRelation}
            onUpdateRelation={updateRelation}
            onDeleteRelation={deleteAttribute}
          />
        </div>
        <aside className="entity-editor" aria-label={t("home.editorEyebrow")}>
          <div className="entity-editor__header">
            <div className="entity-editor__titles">
              <span className="entity-editor__eyebrow">
                {t("home.editorEyebrow")}
              </span>
              <Typography className="entity-editor__title">
                {selectedEntity
                  ? selectedEntity.entityName.trim() || t("home.unnamedEntity")
                  : t("home.noEntitySelected")}
              </Typography>
            </div>
            {selectedEntity && (
              <Tooltip title={t("home.closeEditor")}>
                <IconButton
                  aria-label={t("home.closeEditorAria")}
                  className="entity-editor__close"
                  size="small"
                  onClick={() => setSelectedEntityUuid(null)}
                >
                  <CloseIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            )}
          </div>
          <div className="entity-editor__body">
            {selectedEntity ? (
              <EntityForm
                key={selectedEntity.uuid}
                entity={selectedEntity}
                entities={entities}
                database={database}
                documentMode={documentMode}
                onChangeEntityName={(entityName) =>
                  changeEntityName(selectedEntity.uuid, entityName)
                }
                onChangeEmbedded={(embedded) =>
                  changeEmbedded(selectedEntity.uuid, embedded)
                }
                cacheEnabled={cache.enabled}
                onChangeCacheEnabled={(cacheEnabled) =>
                  changeEntityCache(selectedEntity.uuid, cacheEnabled)
                }
                onChangeAttribute={(attribute) =>
                  changeAttribute(selectedEntity.uuid, attribute)
                }
                onAddAttribute={() => addAttribute(selectedEntity.uuid)}
                onDeleteAttribute={(attributeUuid) =>
                  deleteAttribute(selectedEntity.uuid, attributeUuid)
                }
                onDelete={() => deleteEntity(selectedEntity.uuid)}
              />
            ) : (
              <div className="entity-editor__empty">
                <p>{t("home.emptyEditor")}</p>
                <Button
                  variant="contained"
                  color="success"
                  startIcon={<AddIcon />}
                  onClick={addEntity}
                >
                  {t("home.addEntity")}
                </Button>
              </div>
            )}
          </div>
        </aside>
      </div>
      <Snackbar
        open={!!errorMessage.length}
        autoHideDuration={6000}
        onClose={() => setErrorMessage("")}
      >
        <Alert severity="error" sx={{ width: "100%" }}>
          <div>{errorMessage}</div>
        </Alert>
      </Snackbar>
      <Snackbar
        open={projectDownloaded}
        autoHideDuration={10000}
        onClose={() => setProjectDownloaded(false)}
      >
        <Alert
          severity="success"
          sx={{ width: "95%" }}
        >
          <div>
            {database === "MONGODB"
              ? graphQlMode
                ? t("home.downloaded.mongoGraphql")
                : t("home.downloaded.mongo")
              : graphQlMode
                ? t("home.downloaded.graphql", { database })
                : t("home.downloaded.sql", { database })}
          </div>
        </Alert>
      </Snackbar>
      <Dialog
        open={mongoWarningOpen}
        onClose={() => {
          setMongoWarningOpen(false);
          setPendingDatabase(null);
        }}
        maxWidth="sm"
        fullWidth
        PaperProps={{ className: "compatibility-dialog-paper" }}
      >
        <DialogTitle className="compatibility-dialog__title">
          <span className="compatibility-dialog__eyebrow">
            {t("home.warning.eyebrow")}
          </span>
          <Typography className="compatibility-dialog__heading">
            <WarningAmberIcon className="compatibility-dialog__icon" />
            {t("home.mongoDialog.title")}
          </Typography>
        </DialogTitle>
        <DialogContent
          className="compatibility-dialog__content"
          dividers
        >
          <p className="compatibility-dialog__intro">
            {t("home.mongoDialog.intro", { count: relationAttributeCount })}
          </p>
          <p className="compatibility-dialog__note">
            {t("home.mongoDialog.note")}
          </p>
        </DialogContent>
        <DialogActions className="compatibility-dialog__actions">
          <Button
            className="compatibility-dialog__cancel"
            onClick={() => {
              setMongoWarningOpen(false);
              setPendingDatabase(null);
            }}
          >
            {t("common.cancel")}
          </Button>
          <Button
            className="compatibility-dialog__confirm"
            variant="contained"
            onClick={confirmMongoSelection}
          >
            {t("home.mongoDialog.confirm")}
          </Button>
        </DialogActions>
      </Dialog>
      <Dialog
        open={sqlWarningOpen}
        onClose={() => {
          setSqlWarningOpen(false);
          setPendingDatabase(null);
        }}
        maxWidth="sm"
        fullWidth
        PaperProps={{ className: "compatibility-dialog-paper" }}
      >
        <DialogTitle className="compatibility-dialog__title">
          <span className="compatibility-dialog__eyebrow">
            {t("home.warning.eyebrow")}
          </span>
          <Typography className="compatibility-dialog__heading">
            <WarningAmberIcon className="compatibility-dialog__icon" />
            {t("home.sqlDialog.title")}
          </Typography>
        </DialogTitle>
        <DialogContent
          className="compatibility-dialog__content"
          dividers
        >
          <p className="compatibility-dialog__intro">
            {t("home.sqlDialog.intro", {
              compositions: compositionAttributeCount,
              embedded: embeddedEntityCount,
            })}
          </p>
          <p className="compatibility-dialog__note">
            {t("home.sqlDialog.note")}
          </p>
        </DialogContent>
        <DialogActions className="compatibility-dialog__actions">
          <Button
            className="compatibility-dialog__cancel"
            onClick={() => {
              setSqlWarningOpen(false);
              setPendingDatabase(null);
            }}
          >
            {t("common.cancel")}
          </Button>
          <Button
            className="compatibility-dialog__confirm"
            variant="contained"
            onClick={confirmSqlSelection}
          >
            {t("home.sqlDialog.confirm")}
          </Button>
        </DialogActions>
      </Dialog>
      <Tooltip title={t("home.help.tooltip")} placement="left">
        <Fab
          className="help-fab"
          aria-label={t("home.help.aria")}
          onClick={() => setHelpOpen(true)}
        >
          <HelpOutlineIcon />
        </Fab>
      </Tooltip>
      <Dialog
        open={helpOpen}
        onClose={() => setHelpOpen(false)}
        maxWidth="sm"
        fullWidth
        PaperProps={{ className: "help-dialog-paper" }}
      >
        <DialogTitle className="help-dialog__title">
          <span className="help-dialog__eyebrow">
            {t("home.help.eyebrow")}
          </span>
          <Typography className="help-dialog__heading">
            {t("home.help.title")}
          </Typography>
        </DialogTitle>
        <DialogContent className="help-dialog__content" dividers>
          <p className="help-dialog__intro">
            <RichText text={t("home.help.intro")} />
          </p>

          <section className="help-dialog__section">
            <h3>{t("home.help.base.title")}</h3>
            <ul>
              <li>
                <RichText text={t("home.help.base.pom")} />
              </li>
              <li>
                <RichText
                  text={
                    graphQlMode
                      ? t("home.help.base.mainGraphql")
                      : t("home.help.base.mainRest")
                  }
                />
              </li>
              <li>
                <RichText text={t("home.help.base.compose")} />
              </li>
            </ul>
          </section>

          <section className="help-dialog__section">
            <h3>
              {t("home.help.api.title", {
                api: graphQlMode ? "GraphQL" : "REST",
              })}
            </h3>
            {graphQlMode ? (
              <ul>
                <li>
                  <RichText text={t("home.help.graphql.controller")} />
                </li>
                <li>
                  <RichText text={t("home.help.graphql.schema")} />
                </li>
                <li>
                  <RichText text={t("home.help.graphql.endpoint")} />
                </li>
                <li>
                  <RichText text={t("home.help.graphql.graphiql")} />
                </li>
                <li>
                  <RichText text={t("home.help.graphql.scalars")} />
                </li>
              </ul>
            ) : (
              <ul>
                <li>
                  <RichText text={t("home.help.rest.controller")} />
                </li>
                <li>
                  <RichText text={t("home.help.rest.swagger")} />
                </li>
                <li>
                  <RichText text={t("home.help.rest.errors")} />
                </li>
              </ul>
            )}
            <p className="help-dialog__intro">
              <RichText text={t("home.help.relations")} />
            </p>
          </section>

          <section className="help-dialog__section">
            <h3>{t("home.help.entity.title")}</h3>
            <ul>
              <li>
                <RichText text={t("home.help.entity.domain")} />
              </li>
              <li>
                <RichText text={t("home.help.entity.repository")} />
              </li>
              <li>
                <RichText
                  text={
                    graphQlMode
                      ? t("home.help.entity.controllerGraphql")
                      : t("home.help.entity.controllerRest")
                  }
                />
              </li>
              <li>
                <RichText text={t("home.help.entity.tests")} />
              </li>
            </ul>
          </section>

          <section className="help-dialog__section">
            <h3>{t("home.help.database.title")}</h3>
            <ul>
              <li>
                <RichText text={t("home.help.database.config")} />
              </li>
              <li>
                <RichText text={t("home.help.database.schema")} />
              </li>
              <li>
                <RichText text={t("home.help.database.h2")} />
              </li>
              <li>
                <RichText text={t("home.help.database.types")} />
              </li>
            </ul>
          </section>

          <section className="help-dialog__section">
            <h3>{t("home.help.links.title")}</h3>
            <ul>
              <li>
                <RichText text={t("home.help.links.sql")} />
              </li>
              <li>
                <RichText text={t("home.help.links.mongo")} />
              </li>
            </ul>
          </section>

          <p className="help-dialog__note">
            <RichText text={t("home.help.note")} />
          </p>
        </DialogContent>
        <DialogActions className="help-dialog__actions">
          <Button
            className="help-dialog__accept"
            variant="contained"
            color="success"
            onClick={() => setHelpOpen(false)}
          >
            {t("home.help.accept")}
          </Button>
        </DialogActions>
      </Dialog>

      <ProjectsDrawer
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        projects={savedProjects}
        currentProjectId={currentProjectId}
        onOpenProject={requestOpenProject}
        onDeleteProject={handleDeleteProject}
        onNewProject={requestNewProject}
        onImport={handleImportProjects}
        onExport={handleExportProjects}
      />

      <OpenProjectDialog
        open={!!openProjectDialog}
        project={openProjectDialog?.project ?? null}
        currentName={businessName}
        onCancel={() => setOpenProjectDialog(null)}
        onDiscard={handleDiscardAndContinue}
        onSaveAndContinue={handleSaveAndContinue}
      />

      <SaveProjectDialog
        open={saveDialogOpen}
        initialName={businessName}
        description={
          pendingAfterSave
            ? t("saveDialog.descriptionPending")
            : t("saveDialog.descriptionDefault")
        }
        onCancel={cancelSaveDialog}
        onConfirm={handleSaveDialogConfirm}
      />

      <Snackbar
        open={!!saveFeedback.length}
        autoHideDuration={3000}
        onClose={() => setSaveFeedback("")}
      >
        <Alert severity="success" sx={{ width: "100%" }}>
          {saveFeedback}
        </Alert>
      </Snackbar>
    </div>
  );
}

export default HomePage;
