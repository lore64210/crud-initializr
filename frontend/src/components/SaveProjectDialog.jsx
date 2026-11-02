import React, { useEffect, useState } from "react";
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
  Typography,
} from "@mui/material";
import SaveIcon from "@mui/icons-material/Save";
import "../css/app.css";

const SaveProjectDialog = ({
  open,
  initialName = "",
  description = "Guardá el trabajo actual para poder retomarlo después.",
  onCancel,
  onConfirm,
}) => {
  const [name, setName] = useState(initialName);
  const [error, setError] = useState("");

  useEffect(() => {
    if (open) {
      setName(initialName || "");
      setError("");
    }
  }, [open, initialName]);

  const submit = () => {
    const trimmed = name.trim();
    if (!trimmed) {
      setError("El proyecto necesita un nombre para guardarse");
      return;
    }
    if (trimmed.length > 30) {
      setError("El nombre no puede tener más de 30 caracteres");
      return;
    }
    onConfirm(trimmed);
  };

  return (
    <Dialog
      open={open}
      onClose={onCancel}
      maxWidth="xs"
      fullWidth
      PaperProps={{ className: "project-dialog-paper" }}
    >
      <DialogTitle className="project-dialog__title">
        <span className="project-dialog__eyebrow">GUARDAR PROYECTO</span>
        <Typography className="project-dialog__heading">
          <SaveIcon className="project-dialog__icon" />
          Nombrá tu proyecto
        </Typography>
      </DialogTitle>
      <DialogContent className="project-dialog__content" dividers>
        <p className="project-dialog__intro">{description}</p>
        <TextField
          autoFocus
          fullWidth
          color="success"
          size="small"
          label="Nombre del proyecto"
          value={name}
          error={!!error}
          helperText={error || "También se usa como nombre del ZIP."}
          onChange={({ target: { value } }) => {
            setName(value);
            if (error) {
              setError("");
            }
          }}
          onKeyDown={({ key }) => {
            if (key === "Enter") {
              submit();
            }
          }}
        />
      </DialogContent>
      <DialogActions className="project-dialog__actions">
        <Button className="project-dialog__cancel" onClick={onCancel}>
          Cancelar
        </Button>
        <Button
          className="project-dialog__confirm"
          variant="contained"
          color="success"
          onClick={submit}
        >
          Guardar
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default SaveProjectDialog;
