import React from "react";
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Typography,
} from "@mui/material";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import "../css/app.css";

const OpenProjectDialog = ({
  open,
  project,
  currentName = "",
  onCancel,
  onDiscard,
  onSaveAndContinue,
}) => {
  const isNew = !project;
  const currentUnnamed = !String(currentName || "").trim();

  return (
    <Dialog
      open={open}
      onClose={onCancel}
      maxWidth="sm"
      fullWidth
      PaperProps={{
        className: "project-dialog-paper project-dialog-paper--warning",
      }}
    >
      <DialogTitle className="project-dialog__title">
        <span className="project-dialog__eyebrow">REEMPLAZAR CONFIGURACIÓN</span>
        <Typography className="project-dialog__heading">
          <WarningAmberIcon className="project-dialog__icon project-dialog__icon--warning" />
          {isNew ? "Empezar un proyecto nuevo" : `Abrir «${project.name}»`}
        </Typography>
      </DialogTitle>
      <DialogContent className="project-dialog__content" dividers>
        <p className="project-dialog__intro">
          {isNew
            ? "Se vaciará la configuración actual para empezar de cero: nombre, entidades, base de datos, API y cache."
            : "Se reemplazará la configuración actual con la del proyecto guardado: nombre, entidades, base de datos, API y cache."}
        </p>
        <p className="project-dialog__note">
          Los cambios sin guardar se pierden.{" "}
          {currentUnnamed
            ? "El trabajo actual no tiene nombre: podés descartarlo o escribir uno para guardarlo."
            : "Podés guardar el trabajo actual antes de continuar."}
        </p>
      </DialogContent>
      <DialogActions className="project-dialog__actions">
        <Button className="project-dialog__cancel" onClick={onCancel}>
          Cancelar
        </Button>
        <Button className="project-dialog__discard" onClick={onDiscard}>
          Descartar cambios
        </Button>
        <Button
          className="project-dialog__confirm"
          variant="contained"
          color="success"
          onClick={onSaveAndContinue}
        >
          Guardar y continuar
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default OpenProjectDialog;
