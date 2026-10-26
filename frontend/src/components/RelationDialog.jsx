import React, { useEffect, useState } from "react";
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from "@mui/material";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import { formatStringToCamelCase } from "../service/service";
import "../css/app.css";

const RelationDialog = ({
  open,
  mode,
  database = "POSTGRESQL",
  sourceName,
  targetName,
  targetExists,
  targetEmbedded,
  initialName,
  initialType,
  otherNames,
  suggestName,
  onClose,
  onSave,
  onDelete,
}) => {
  const [name, setName] = useState(initialName);
  const [type, setType] = useState(initialType);
  const [error, setError] = useState("");
  const [touched, setTouched] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);

  useEffect(() => {
    if (!open) return;
    setName(initialName);
    setType(initialType);
    setError("");
    setTouched(false);
    setConfirmDelete(false);
  }, [open, initialName, initialType]);

  const isMongo = database === "MONGODB";
  const targetInvalid = isMongo && !targetEmbedded;
  const relationTypes = isMongo
    ? [
        { value: "COMPOSITION", label: "1:1 · Embebido" },
        { value: "COMPOSITION_LIST", label: "1:N · Lista" },
      ]
    : [
        { value: "CUSTOM", label: "N:1 · Muchos a uno" },
        { value: "CUSTOM_LIST", label: "1:N · Uno a muchos" },
        { value: "CUSTOM_ONE_TO_ONE", label: "1:1 · Uno a uno" },
        { value: "CUSTOM_MANY_TO_MANY", label: "N:M · Muchos a muchos" },
      ];
  const normalize = (value) => formatStringToCamelCase(value).toLowerCase();

  const changeType = (nextType) => {
    if (!nextType || nextType === type) return;
    setType(nextType);
    setError("");
    if (mode === "create" && (!touched || !name.trim())) {
      const suggestion = suggestName?.(nextType);
      if (suggestion) setName(suggestion);
    }
  };

  const save = () => {
    if (targetInvalid) {
      setError("La entidad destino debe ser un documento embebido");
      return;
    }

    const trimmed = name.trim();
    const normalized = formatStringToCamelCase(trimmed);

    if (!trimmed) {
      setError("El nombre no puede estar vacío");
      return;
    }
    if (!/^[A-Za-z][A-Za-z0-9]*$/.test(normalized)) {
      setError("El nombre debe empezar con una letra");
      return;
    }
    if (normalized.length > 30) {
      setError("El nombre no puede tener más de 30 caracteres");
      return;
    }
    if (
      otherNames.some(
        (otherName) => normalize(otherName) === normalized.toLowerCase(),
      )
    ) {
      setError(`Ya existe un atributo con ese nombre en ${sourceName}`);
      return;
    }

    onSave({ name: trimmed, type });
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="xs"
      fullWidth
      sx={{ zIndex: 1600 }}
      ModalProps={{ sx: { zIndex: 1600 } }}
      PaperProps={{ className: "relation-dialog-paper" }}
    >
      <DialogTitle className="relation-dialog__heading">
        <span className="relation-dialog__eyebrow">
          {mode === "edit" ? "EDITAR RELACIÓN" : "NUEVA RELACIÓN"}
        </span>
        <span className="relation-dialog__route">
          <span>{sourceName}</span>
          <span className="relation-dialog__arrow">→</span>
          <span>{targetName}</span>
        </span>
      </DialogTitle>

      <DialogContent className="relation-dialog__content">
        {confirmDelete ? (
          <Alert severity="warning" variant="outlined">
            Se va a eliminar el atributo «{initialName}» de {sourceName}. Esta
            acción no se puede deshacer.
          </Alert>
        ) : (
          <>
            <TextField
              autoFocus
              fullWidth
              size="small"
              color="success"
              label="Nombre del atributo"
              value={name}
              error={!!error}
              helperText={
                error ||
                (mode === "create"
                  ? "Se agrega como atributo de la entidad origen."
                  : "Nombre del atributo en la entidad origen.")
              }
              onChange={({ target: { value } }) => {
                setName(value);
                setTouched(true);
                if (error) setError("");
              }}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  save();
                }
              }}
            />

            <Typography className="relation-dialog__label">
              {isMongo ? "Tipo de composición" : "Tipo de relación"}
            </Typography>
            <ToggleButtonGroup
              exclusive
              fullWidth
              size="small"
              value={type}
              onChange={(_event, value) => changeType(value)}
              className="relation-dialog__toggle"
            >
              {relationTypes.map((relationType) => (
                <ToggleButton
                  key={relationType.value}
                  value={relationType.value}
                >
                  {relationType.label}
                </ToggleButton>
              ))}
            </ToggleButtonGroup>

            {targetInvalid && (
              <Alert severity="warning" variant="outlined" sx={{ mt: 2 }}>
                La entidad «{targetName}» debe estar marcada como documento
                embebido para usarla en una composición.
              </Alert>
            )}

            {!targetExists && (
              <Alert severity="warning" variant="outlined" sx={{ mt: 2 }}>
                La entidad «{targetName}» no existe. La relación va a quedar sin
                destino hasta que crees esa entidad.
              </Alert>
            )}

            <Typography className="relation-dialog__hint">
              {mode === "create"
                ? `También podés ajustar esta relación desde el formulario de ${sourceName}.`
                : "También podés arrastrar el extremo de la relación en el mapa para cambiar la entidad destino."}
            </Typography>
          </>
        )}
      </DialogContent>

      <DialogActions className="relation-dialog__actions">
        {confirmDelete ? (
          <>
            <Button color="inherit" onClick={() => setConfirmDelete(false)}>
              Volver
            </Button>
            <Button color="error" variant="contained" onClick={onDelete}>
              Eliminar
            </Button>
          </>
        ) : (
          <>
            {mode === "edit" && (
              <Button
                color="error"
                startIcon={<DeleteOutlineIcon />}
                onClick={() => setConfirmDelete(true)}
                sx={{ marginRight: "auto" }}
              >
                Eliminar relación
              </Button>
            )}
            <Button color="inherit" onClick={onClose}>
              Cancelar
            </Button>
            <Button
              color="success"
              variant="contained"
              onClick={save}
              disabled={targetInvalid}
            >
              {mode === "edit"
                ? "Guardar"
                : isMongo
                  ? "Crear composición"
                  : "Crear relación"}
            </Button>
          </>
        )}
      </DialogActions>
    </Dialog>
  );
};

export default RelationDialog;
