import React, { useCallback, useState } from "react";
import EntityForm from "../components/EntityForm";
import DatabaseSelector from "../components/DatabaseSelector";
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

import "../css/app.css";

const createEmptyAttribute = () => ({
  uuid: uuid(),
  name: "",
  type: "STRING",
  customType: "",
  validations: [],
});

const createEmptyEntity = () => ({
  uuid: uuid(),
  entityName: "",
  attributes: [createEmptyAttribute()],
});

const warningMessage =
  "WARNING: Segun las validaciones que se usen en los atributos de tipo CUSTOM y CUSTOM_LIST, los test pueden fallar. Revisar los builders que se usan al crear instancias genericas y reemplazar por lo que haga falta";

const validateForm = (businessName, entities) => {
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

      if (attribute.type.includes("CUSTOM") && !attribute.customType) {
        return `El atributo "${attribute.name}" necesita un tipo CUSTOM seleccionado`;
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
  const [helpOpen, setHelpOpen] = useState(false);

  const selectedEntity =
    entities.find((entity) => entity.uuid === selectedEntityUuid) ?? null;

  const relationCount = entities.reduce(
    (total, entity) =>
      total +
      entity.attributes.filter(
        (attribute) =>
          attribute.type.includes("CUSTOM") && attribute.customType?.trim(),
      ).length,
    0,
  );

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
            attribute.type.includes("CUSTOM") &&
            attribute.customType === previousName
              ? { ...attribute, customType: entityName }
              : attribute,
          ),
        };
      });
    });
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
            attribute.type.includes("CUSTOM") &&
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
    const validationError = validateForm(businessName, entities);
    if (validationError) {
      setErrorMessage(validationError);
      return;
    }

    setErrorMessage("");
    setLoading(true);
    const result = await service.createProject({ businessName, entities, database });
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
            title="Descarga un ZIP con un proyecto Spring Boot listo para ejecutar, incluyendo tus entidades y relaciones."
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
      <DatabaseSelector value={database} onChange={setDatabase} />
      <div className="workspace">
        <div className="workspace__graph">
          <RelationshipGraphPanel
            entities={entities}
            relationCount={relationCount}
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
                onChangeEntityName={(entityName) =>
                  changeEntityName(selectedEntity.uuid, entityName)
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
        <Alert severity="warning" sx={{ width: "95%" }}>
          <div>{warningMessage}</div>
        </Alert>
      </Snackbar>
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
            del nombre del proyecto, la base de datos elegida y las entidades
            definidas en el mapa.
          </p>

          <section className="help-dialog__section">
            <h3>Proyecto base</h3>
            <ul>
              <li>
                <strong>pom.xml</strong> con las dependencias de Spring Web,
                Spring Data JPA, validaciones y el driver de la base elegida.
              </li>
              <li>
                Clase principal de Spring Boot, manejador global de errores,
                configuración de la aplicación, README y .gitignore.
              </li>
              <li>
                <strong>docker-compose.yml</strong> cuando la base necesita
                servidor (PostgreSQL, MariaDB o MySQL).
              </li>
            </ul>
          </section>

          <section className="help-dialog__section">
            <h3>Por cada entidad</h3>
            <ul>
              <li>
                <strong>Domain:</strong> clase JPA con su tabla, id,
                fechaCreacion, atributos y relaciones.
              </li>
              <li>
                <strong>Repository</strong> y <strong>Service</strong> con las
                validaciones configuradas y guardado en cascada de entidades
                relacionadas.
              </li>
              <li>
                <strong>Controller REST</strong> con endpoints bajo{" "}
                <code>/api/nombreEntidad</code>.
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
                MySQL, SQLite o H2.
              </li>
              <li>
                <strong>schema.sql</strong> con tablas, claves primarias y
                foreign keys, más el esquema equivalente para los tests.
              </li>
              <li>
                Con H2 se habilita la consola en <code>/h2-console</code> y la
                base se crea automáticamente.
              </li>
            </ul>
          </section>

          <section className="help-dialog__section">
            <h3>Relaciones</h3>
            <ul>
              <li>
                Las relaciones <strong>CUSTOM</strong> se generan como{" "}
                <code>@ManyToOne</code> y las <strong>CUSTOM_LIST</strong> como{" "}
                <code>@OneToMany</code>, reflejando las foreign keys en el
                esquema.
              </li>
            </ul>
          </section>

          <p className="help-dialog__note">
            Las columnas <strong>id</strong> y <strong>fechaCreacion</strong>{" "}
            son reservadas y se agregan automáticamente. El proyecto es un punto
            de partida: si usás validaciones sobre tipos CUSTOM o CUSTOM_LIST,
            puede que necesites ajustar los builders o tests generados.
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
