import React, { useRef, useState } from "react";
import {
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Drawer,
  IconButton,
  Tooltip,
  Typography,
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import CachedIcon from "@mui/icons-material/Cached";
import CloseIcon from "@mui/icons-material/Close";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import DownloadIcon from "@mui/icons-material/Download";
import FolderOpenIcon from "@mui/icons-material/FolderOpen";
import UploadFileIcon from "@mui/icons-material/UploadFile";
import "../css/app.css";

const DATABASE_LABELS = {
  POSTGRESQL: "PostgreSQL",
  MARIADB: "MariaDB",
  MYSQL: "MySQL",
  SQLITE: "SQLite",
  H2: "H2",
  MONGODB: "MongoDB",
};

const formatUpdatedAt = (value) => {
  try {
    return new Intl.DateTimeFormat("es-AR", {
      day: "2-digit",
      month: "short",
      hour: "2-digit",
      minute: "2-digit",
    }).format(new Date(value));
  } catch {
    return "";
  }
};

const ProjectsDrawer = ({
  open,
  onClose,
  projects,
  currentProjectId,
  onOpenProject,
  onDeleteProject,
  onNewProject,
  onImport,
  onExport,
}) => {
  const [pendingDelete, setPendingDelete] = useState(null);
  const fileInputRef = useRef(null);

  const confirmDelete = () => {
    if (pendingDelete) {
      onDeleteProject(pendingDelete.id);
    }
    setPendingDelete(null);
  };

  const handleFileChange = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = "";

    if (!file) {
      return;
    }

    try {
      const text = await file.text();
      onImport?.(text);
    } catch {
      onImport?.("");
    }
  };

  return (
    <>
      <Drawer
        anchor="left"
        open={open}
        onClose={onClose}
        PaperProps={{ className: "projects-drawer-paper" }}
      >
        <div className="projects-drawer">
          <div className="projects-drawer__header">
            <div className="projects-drawer__titles">
              <span className="projects-drawer__eyebrow">TUS PROYECTOS</span>
              <Typography className="projects-drawer__title">
                Proyectos guardados
              </Typography>
              <span className="projects-drawer__subtitle">
                Se guardan en este navegador.
              </span>
            </div>
            <IconButton
              className="projects-drawer__close"
              aria-label="Cerrar proyectos guardados"
              onClick={onClose}
            >
              <CloseIcon />
            </IconButton>
          </div>

          <div className="projects-drawer__body">
            {projects.length === 0 ? (
              <div className="projects-drawer__empty">
                <FolderOpenIcon />
                <p>Todavía no hay proyectos guardados</p>
                <span>
                  Usá el botón Guardar del encabezado para guardar el trabajo
                  actual.
                </span>
              </div>
            ) : (
              projects.map((project) => {
                const isCurrent = project.id === currentProjectId;
                const entityCount = project.entities.length;

                return (
                  <div
                    key={project.id}
                    role="button"
                    tabIndex={0}
                    className={`project-item${
                      isCurrent ? " project-item--current" : ""
                    }`}
                    onClick={() => onOpenProject(project)}
                    onKeyDown={(event) => {
                      if (event.target !== event.currentTarget) {
                        return;
                      }
                      if (event.key === "Enter" || event.key === " ") {
                        event.preventDefault();
                        onOpenProject(project);
                      }
                    }}
                  >
                    <div className="project-item__top">
                      <span className="project-item__name">{project.name}</span>
                      {isCurrent && (
                        <span className="project-item__current">
                          En edición
                        </span>
                      )}
                      <Tooltip title="Eliminar proyecto" arrow>
                        <IconButton
                          size="small"
                          className="project-item__delete"
                          aria-label={`Eliminar proyecto ${project.name}`}
                          onClick={(event) => {
                            event.stopPropagation();
                            setPendingDelete(project);
                          }}
                        >
                          <DeleteOutlineIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </div>

                    <span className="project-item__meta">
                      {formatUpdatedAt(project.updatedAt)} · {entityCount}{" "}
                      {entityCount === 1 ? "entidad" : "entidades"}
                    </span>

                    <div className="project-item__tags">
                      <Chip
                        size="small"
                        label={
                          DATABASE_LABELS[project.database] || project.database
                        }
                        className="project-item__tag"
                      />
                      <Chip
                        size="small"
                        label={project.apiType}
                        className="project-item__tag"
                      />
                      {project.cache?.enabled && (
                        <Chip
                          size="small"
                          icon={<CachedIcon />}
                          label="Redis"
                          className="project-item__tag project-item__tag--redis"
                        />
                      )}
                    </div>

                    {entityCount > 0 && (
                      <div className="project-item__entities">
                        {project.entities.slice(0, 5).map((entity) => (
                          <Chip
                            key={entity.uuid}
                            size="small"
                            variant="outlined"
                            label={entity.entityName || "Sin nombre"}
                            className="project-item__entity"
                          />
                        ))}
                        {entityCount > 5 && (
                          <Chip
                            size="small"
                            variant="outlined"
                            label={`+${entityCount - 5}`}
                            className="project-item__entity"
                          />
                        )}
                      </div>
                    )}
                  </div>
                );
              })
            )}
          </div>

          <div className="projects-drawer__footer">
            <Button
              variant="outlined"
              color="success"
              startIcon={<AddIcon />}
              className="projects-drawer__new"
              onClick={onNewProject}
            >
              Nuevo proyecto
            </Button>
            <div className="projects-drawer__actions">
              <Button
                className="projects-drawer__action"
                startIcon={<UploadFileIcon />}
                onClick={() => fileInputRef.current?.click()}
              >
                Importar
              </Button>
              <Button
                className="projects-drawer__action"
                startIcon={<DownloadIcon />}
                onClick={onExport}
                disabled={projects.length === 0}
              >
                Exportar
              </Button>
            </div>
            <input
              ref={fileInputRef}
              type="file"
              accept=".json,application/json"
              aria-label="Importar proyectos desde un archivo JSON"
              className="projects-drawer__file-input"
              onChange={handleFileChange}
            />
            <span className="projects-drawer__footer-note">
              Los proyectos quedan guardados solo en este navegador. Usá
              Importar/Exportar para moverlos entre navegadores.
            </span>
          </div>
        </div>
      </Drawer>

      <Dialog
        open={!!pendingDelete}
        onClose={() => setPendingDelete(null)}
        maxWidth="xs"
        fullWidth
        PaperProps={{
          className: "project-dialog-paper project-dialog-paper--warning",
        }}
      >
        <DialogTitle className="project-dialog__title">
          <span className="project-dialog__eyebrow">ELIMINAR PROYECTO</span>
          <Typography className="project-dialog__heading">
            ¿Eliminar «{pendingDelete?.name}»?
          </Typography>
        </DialogTitle>
        <DialogContent className="project-dialog__content" dividers>
          <p className="project-dialog__intro">
            Se borrará de este navegador. Esta acción no se puede deshacer.
          </p>
        </DialogContent>
        <DialogActions className="project-dialog__actions">
          <Button
            className="project-dialog__cancel"
            onClick={() => setPendingDelete(null)}
          >
            Cancelar
          </Button>
          <Button
            className="project-dialog__confirm project-dialog__confirm--danger"
            variant="contained"
            onClick={confirmDelete}
          >
            Eliminar
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default ProjectsDrawer;
