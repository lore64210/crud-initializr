import React, { useCallback, useState } from "react";
import EntityForm from "../components/EntityForm";
import DatabaseSelector from "../components/DatabaseSelector";
import ApiSelector from "../components/ApiSelector";
import { RelationshipGraphPanel } from "../components/RelationshipGraph";
import service, { formatStringToCamelCase } from "../service/service";
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
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import CloseIcon from "@mui/icons-material/Close";
import DownloadIcon from "@mui/icons-material/Download";
import HelpOutlineIcon from "@mui/icons-material/HelpOutline";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";

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
const isListType = (type) =>
  type === "CUSTOM_LIST" ||
  type === "CUSTOM_MANY_TO_MANY" ||
  type === "COMPOSITION_LIST";
const isNumericType = (type) =>
  ["INTEGER", "LONG", "DECIMAL", "DOUBLE"].includes(type);

const createEmptyEntity = () => ({
  uuid: uuid(),
  entityName: "",
  attributes: [createEmptyAttribute()],
  embedded: false,
});

const warningMessage =
  "WARNING: Segun las validaciones que se usen en los atributos de tipo relación o composición, los test pueden fallar. Revisar los builders que se usan al crear instancias genericas y reemplazar por lo que haga falta";

const validateForm = (businessName, entities, database) => {
  const title = businessName.trim();
  if (!title) {
    return "El nombre del proyecto no puede estar vacío";
  }
  if (title.length > 30) {
    return "El nombre del proyecto no puede tener más de 30 caracteres";
  }
  if (!formatStringToCamelCase(title)) {
    return "El nombre del proyecto debe contener al menos una letra o número";
  }

  if (entities.length === 0) {
    return "El proyecto debe tener al menos una entidad";
  }

  const entityNames = new Set();
  for (let index = 0; index < entities.length; index++) {
    const entity = entities[index];
    const entityName = formatStringToCamelCase(entity.entityName);

    if (!entity.entityName.trim()) {
      return `La entidad #${index + 1} no tiene nombre`;
    }
    if (!/^[A-Za-z][A-Za-z0-9]*$/.test(entityName)) {
      return `El nombre de la entidad "${entity.entityName}" no es válido`;
    }
    if (entityName.length > 30) {
      return `El nombre de la entidad "${entity.entityName}" no puede tener más de 30 caracteres`;
    }
    if (entityNames.has(entityName.toLowerCase())) {
      return `La entidad "${entity.entityName}" está repetida`;
    }
    entityNames.add(entityName.toLowerCase());

    const attributeNames = new Set();
    for (const attribute of entity.attributes) {
      const attributeName = formatStringToCamelCase(attribute.name);

      if (!attribute.name.trim()) {
        return `La entidad "${entity.entityName}" tiene un atributo sin nombre`;
      }
      if (!/^[A-Za-z][A-Za-z0-9]*$/.test(attributeName)) {
        return `El nombre del atributo "${attribute.name}" no es válido`;
      }
      if (attributeName.length > 30) {
        return `El nombre del atributo "${attribute.name}" no puede tener más de 30 caracteres`;
      }
      if (attributeNames.has(attributeName.toLowerCase())) {
        return `El atributo "${attribute.name}" está repetido en "${entity.entityName}"`;
      }
      attributeNames.add(attributeName.toLowerCase());

      if (isLinkType(attribute.type) && !attribute.customType?.trim()) {
        return `El atributo "${attribute.name}" necesita una entidad destino seleccionada`;
      }

      if (isNumericType(attribute.type)) {
        const hasMin = attribute.validations.includes("MIN");
        const hasMax = attribute.validations.includes("MAX");
        if (hasMin && attribute.minValue === "") {
          return `El atributo "${attribute.name}" necesita un valor mínimo`;
        }
        if (hasMax && attribute.maxValue === "") {
          return `El atributo "${attribute.name}" necesita un valor máximo`;
        }
        if (
          hasMin &&
          hasMax &&
          Number(attribute.minValue) > Number(attribute.maxValue)
        ) {
          return `El valor mínimo de "${attribute.name}" no puede ser mayor que el máximo`;
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
          return `El atributo "${attribute.name}" de "${entity.entityName}" referencia a una entidad inexistente: "${attribute.customType}"`;
        }
        if (target.uuid === entity.uuid) {
          return `El atributo "${attribute.name}" de "${entity.entityName}" no puede referenciarse a sí mismo`;
        }
      }

      if (database === "MONGODB") {
        if (isRelationType(attribute.type)) {
          return `MongoDB no soporta relaciones SQL en esta versión. El atributo "${attribute.name}" de "${entity.entityName}" es una relación. Cambialo a una composición o elegí una base SQL.`;
        }
        if (isCompositionType(attribute.type)) {
          if (entity.embedded) {
            return `La entidad embebida "${entity.entityName}" no puede contener composiciones en esta versión.`;
          }
          const target = entities.find(
            (candidate) =>
              formatStringToCamelCase(candidate.entityName).toLowerCase() ===
              formatStringToCamelCase(attribute.customType).toLowerCase(),
          );
          if (!target?.embedded) {
            return `La composición "${attribute.name}" de "${entity.entityName}" debe apuntar a un documento embebido.`;
          }
        }
      } else {
        if (entity.embedded) {
          return `La entidad "${entity.entityName}" está marcada como documento embebido y solo es válida para MongoDB.`;
        }
        if (isCompositionType(attribute.type)) {
          return `La composición "${attribute.name}" de "${entity.entityName}" solo es válida para MongoDB.`;
        }
      }
    }
  }

  return "";
};

function HomePage() {
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
      ? `Hay ${relationAttributeCount} ${
          relationAttributeCount === 1 ? "relación" : "relaciones"
        } SQL en el mapa. MongoDB no las soporta: cambialas a composiciones o volvé a una base SQL.`
      : "";
  const sqlCompositionWarning =
    !documentMode && (compositionAttributeCount > 0 || embeddedEntityCount > 0)
      ? `Hay ${compositionAttributeCount} ${
          compositionAttributeCount === 1 ? "composición" : "composiciones"
        } y ${embeddedEntityCount} ${
          embeddedEntityCount === 1
            ? "entidad embebida"
            : "entidades embebidas"
        } de MongoDB. Las bases SQL no las soportan: cambialas o volvé a MongoDB.`
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

  const submit = async () => {
    const validationError = validateForm(businessName, entities, database);
    if (validationError) {
      setErrorMessage(validationError);
      return;
    }

    setErrorMessage("");
    setLoading(true);
    const result = await service.createProject({
      businessName,
      entities,
      database,
      apiType,
    });
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
              <span className="brand__subtitle">
                Proyecto personal de Lorenzo Lopez
              </span>
            </div>
          </div>
          <span className="header__badge">Spring Boot v4.1.2</span>
        </div>
      </div>
      <div className="bussines-name-input-container">
        <div className="project-bar">
          <TextField
            color="success"
            className="input project-name-input"
            id="project-name"
            label="Nombre del proyecto"
            variant="outlined"
            size="small"
            value={businessName}
            onChange={({ target: { value } }) => setBusinessName(value)}
          />
          <Tooltip
            title="Descarga un ZIP con un proyecto Spring Boot listo para ejecutar, incluyendo tus entidades, relaciones y composiciones."
            arrow
          >
            <span className="create-project-tooltip-wrapper">
              <Button
                className="create-project-button"
                variant="contained"
                onClick={submit}
                color="success"
                startIcon={<DownloadIcon />}
                disabled={loading}
              >
                {loading ? "Creando proyecto..." : "Crear proyecto"}
              </Button>
            </span>
          </Tooltip>
        </div>
      </div>
      <DatabaseSelector
        value={database}
        onChange={changeDatabase}
        warning={
          mongoRelationWarning
            ? {
                title: "Relaciones SQL detectadas",
                message: mongoRelationWarning,
              }
            : sqlCompositionWarning
              ? {
                  title: "Composiciones de MongoDB detectadas",
                  message: sqlCompositionWarning,
                }
              : null
        }
      />
      <ApiSelector value={apiType} onChange={setApiType} />
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
        <aside className="entity-editor" aria-label="Editor de entidad">
          <div className="entity-editor__header">
            <div className="entity-editor__titles">
              <span className="entity-editor__eyebrow">EDITOR DE ENTIDAD</span>
              <Typography className="entity-editor__title">
                {selectedEntity
                  ? selectedEntity.entityName.trim() || "Entidad sin nombre"
                  : "Ninguna entidad seleccionada"}
              </Typography>
            </div>
            {selectedEntity && (
              <Tooltip title="Cerrar editor">
                <IconButton
                  aria-label="Cerrar editor de entidad"
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
                <p>
                  Seleccioná una entidad en el mapa para editarla o creá una
                  nueva.
                </p>
                <Button
                  variant="contained"
                  color="success"
                  startIcon={<AddIcon />}
                  onClick={addEntity}
                >
                  Agregar entidad
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
          severity={database === "MONGODB" ? "success" : "warning"}
          sx={{ width: "95%" }}
        >
          <div>
            {database === "MONGODB"
              ? graphQlMode
                ? "Proyecto GraphQL generado con MongoDB. Incluye GraphiQL en /graphiql; los tests usan Testcontainers y requieren Docker."
                : "Proyecto MongoDB generado. Los tests usan Testcontainers y requieren Docker."
              : graphQlMode
                ? `Proyecto GraphQL generado con ${database}. Incluye el esquema y GraphiQL en /graphiql. ${warningMessage}`
                : warningMessage}
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
            Advertencia de compatibilidad
          </span>
          <Typography className="compatibility-dialog__heading">
            <WarningAmberIcon className="compatibility-dialog__icon" />
            MongoDB no soporta relaciones SQL
          </Typography>
        </DialogTitle>
        <DialogContent
          className="compatibility-dialog__content"
          dividers
        >
          <p className="compatibility-dialog__intro">
            Detectamos {relationAttributeCount}{" "}
            {relationAttributeCount === 1 ? "relación" : "relaciones"} SQL en el
            mapa. MongoDB usa composiciones con documentos embebidos, así que
            esas relaciones no serán válidas para este motor.
          </p>
          <p className="compatibility-dialog__note">
            No se eliminarán automáticamente. Podés convertirlas en
            composiciones o volver a una base SQL antes de crear el proyecto.
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
            Cancelar
          </Button>
          <Button
            className="compatibility-dialog__confirm"
            variant="contained"
            onClick={confirmMongoSelection}
          >
            Usar MongoDB igual
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
            Advertencia de compatibilidad
          </span>
          <Typography className="compatibility-dialog__heading">
            <WarningAmberIcon className="compatibility-dialog__icon" />
            Las bases SQL no soportan composiciones
          </Typography>
        </DialogTitle>
        <DialogContent
          className="compatibility-dialog__content"
          dividers
        >
          <p className="compatibility-dialog__intro">
            Detectamos {compositionAttributeCount}{" "}
            {compositionAttributeCount === 1
              ? "composición"
              : "composiciones"}{" "}
            y {embeddedEntityCount}{" "}
            {embeddedEntityCount === 1
              ? "entidad embebida"
              : "entidades embebidas"}{" "}
            de MongoDB.
          </p>
          <p className="compatibility-dialog__note">
            Al continuar, las entidades embebidas volverán a ser entidades raíz.
            Las composiciones no se eliminarán automáticamente y deberás
            convertirlas a relaciones SQL o borrarlas.
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
            Cancelar
          </Button>
          <Button
            className="compatibility-dialog__confirm"
            variant="contained"
            onClick={confirmSqlSelection}
          >
            Usar base SQL igual
          </Button>
        </DialogActions>
      </Dialog>
      <Tooltip title="Ver qué incluye el proyecto generado" placement="left">
        <Fab
          className="help-fab"
          aria-label="Ver detalle del proyecto que se genera"
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
          <span className="help-dialog__eyebrow">QUÉ VAS A DESCARGAR</span>
          <Typography className="help-dialog__heading">
            Detalle del proyecto generado
          </Typography>
        </DialogTitle>
        <DialogContent className="help-dialog__content" dividers>
          <p className="help-dialog__intro">
            Al presionar <strong>Crear proyecto</strong> se descarga un archivo
            ZIP con un proyecto Maven de Spring Boot 4.1.2, generado a partir
            del nombre del proyecto, la base de datos elegida, el tipo de API
            (REST o GraphQL) y las entidades definidas en el mapa.
          </p>

          <section className="help-dialog__section">
            <h3>Proyecto base</h3>
            <ul>
              <li>
                <strong>pom.xml</strong> con las dependencias de Spring Web,
                Spring Data JPA o MongoDB, validaciones y el driver de la base
                elegida.
              </li>
              <li>
                Clase principal de Spring Boot, manejador global de errores
                {graphQlMode ? " para GraphQL" : " para REST"}, configuración de
                la aplicación, README y .gitignore.
              </li>
              <li>
                <strong>docker-compose.yml</strong> cuando la base necesita
                servidor (PostgreSQL, MariaDB, MySQL o MongoDB).
              </li>
            </ul>
          </section>

          <section className="help-dialog__section">
            <h3>API {graphQlMode ? "GraphQL" : "REST"}</h3>
            {graphQlMode ? (
              <ul>
                <li>
                  Un <strong>controller GraphQL</strong> por entidad raíz con
                  queries <code>findAll...</code> / <code>find...ById</code> y
                  mutations <code>create...</code>, <code>update...</code> y{" "}
                  <code>delete...</code>.
                </li>
                <li>
                  Esquema en{" "}
                  <code>src/main/resources/graphql/schema.graphqls</code> con
                  los tipos, inputs, <code>Query</code> y <code>Mutation</code>.
                </li>
                <li>
                  Endpoint GraphQL en <code>/graphql</code>.
                </li>
                <li>
                  <strong>GraphiQL</strong> habilitado en{" "}
                  <code>/graphiql</code>: es una interfaz web para explorar el
                  esquema y probar queries y mutations con autocompletado, sin
                  necesidad de escribir las peticiones HTTP a mano. Es una
                  herramienta de desarrollo, no forma parte de la API, y podés
                  deshabilitarla con{" "}
                  <code>spring.graphql.graphiql.enabled=false</code>.
                </li>
                <li>
                  Los tipos <code>Date</code>, <code>DateTime</code>,{" "}
                  <code>Time</code>, <code>BigDecimal</code> y <code>UUID</code>{" "}
                  se exponen como <strong>scalars GraphQL</strong>.
                </li>
              </ul>
            ) : (
              <ul>
                <li>
                  Un <strong>controller REST</strong> por entidad con endpoints
                  bajo <code>/api/nombreEntidad</code>.
                </li>
                <li>
                  <strong>Swagger UI</strong> en{" "}
                  <code>/swagger-ui.html</code> y el contrato OpenAPI en{" "}
                  <code>/v3/api-docs</code>.
                </li>
                <li>
                  Manejador global de errores para devolver HTTP 400 con el
                  detalle de la validación.
                </li>
              </ul>
            )}
          </section>

          <section className="help-dialog__section">
            <h3>Por cada entidad</h3>
            <ul>
              <li>
                <strong>Domain:</strong> clase JPA o documento MongoDB con su
                id, fechaCreacion, atributos y, en SQL, relaciones. Soporta
                String, Integer, Long, Boolean, Date, DateTime, Time, Decimal,
                Double y UUID. Las entidades embebidas de MongoDB no llevan id
                ni fechaCreacion.
              </li>
              <li>
                <strong>Repository</strong> y <strong>Service</strong> con las
                validaciones configuradas y, en SQL, guardado en cascada de
                entidades relacionadas.
              </li>
              <li>
                <strong>Controller {graphQlMode ? "GraphQL" : "REST"}</strong>{" "}
                {graphQlMode
                  ? "con queries y mutations que delegan en el service de la entidad."
                  : "con endpoints bajo /api/nombreEntidad."}
              </li>
              <li>
                <strong>Tests y builders</strong> para el service de cada
                entidad.
              </li>
            </ul>
          </section>

          <section className="help-dialog__section">
            <h3>Base de datos y esquema</h3>
            <ul>
              <li>
                Configuración de conexión y driver para PostgreSQL, MariaDB,
                MySQL, SQLite, H2 o MongoDB.
              </li>
              <li>
                <strong>schema.sql</strong> con tablas, claves primarias y
                foreign keys para las bases SQL. MongoDB no usa schema: las
                colecciones se crean al insertar.
              </li>
              <li>
                Con H2 se habilita la consola en <code>/h2-console</code> y la
                base se crea automáticamente.
              </li>
              <li>
                Los tipos Date, DateTime, Time, Decimal, Double y UUID se
                traducen al tipo nativo de cada motor SQL y se persisten como
                fecha, decimal, número o UUID en MongoDB.
              </li>
            </ul>
          </section>

          <section className="help-dialog__section">
            <h3>Relaciones y composiciones</h3>
            <ul>
              <li>
                En las bases SQL, las relaciones <strong>CUSTOM</strong> se
                generan como <code>@ManyToOne</code>, las{" "}
                <strong>CUSTOM_LIST</strong> como <code>@OneToMany</code>, las{" "}
                <strong>CUSTOM_ONE_TO_ONE</strong> como <code>@OneToOne</code>{" "}
                con foreign key única y las{" "}
                <strong>CUSTOM_MANY_TO_MANY</strong> como{" "}
                <code>@ManyToMany</code> con tabla intermedia.
              </li>
              <li>
                En MongoDB, las composiciones{" "}
                <strong>COMPOSITION</strong> y{" "}
                <strong>COMPOSITION_LIST</strong> generan documentos embebidos.
                La entidad embebida no tiene colección, repository, service ni
                controller propio.
              </li>
            </ul>
          </section>

          <p className="help-dialog__note">
            Los campos <strong>id</strong> y <strong>fechaCreacion</strong> son
            reservados y se agregan automáticamente en las entidades raíz. En
            SQL los tests usan H2 en memoria; en MongoDB usan Testcontainers y
            requieren Docker. El proyecto es un punto de partida: si usás
            validaciones sobre relaciones o composiciones, puede que necesites
            ajustar los builders o tests generados.
          </p>
        </DialogContent>
        <DialogActions className="help-dialog__actions">
          <Button
            className="help-dialog__accept"
            variant="contained"
            color="success"
            onClick={() => setHelpOpen(false)}
          >
            Entendido
          </Button>
        </DialogActions>
      </Dialog>
    </div>
  );
}

export default HomePage;
