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
import { useLanguage } from "../i18n/LanguageContext";
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
  const { t } = useLanguage();
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
        { value: "COMPOSITION", label: t("relation.type.mongoEmbedded") },
        { value: "COMPOSITION_LIST", label: t("relation.type.mongoList") },
      ]
    : [
        { value: "CUSTOM", label: t("relation.type.manyToOne") },
        { value: "CUSTOM_LIST", label: t("relation.type.oneToMany") },
        { value: "CUSTOM_ONE_TO_ONE", label: t("relation.type.oneToOne") },
        { value: "CUSTOM_MANY_TO_MANY", label: t("relation.type.manyToMany") },
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
      setError(t("relation.error.targetEmbedded"));
      return;
    }

    const trimmed = name.trim();
    const normalized = formatStringToCamelCase(trimmed);

    if (!trimmed) {
      setError(t("relation.error.nameEmpty"));
      return;
    }
    if (!/^[A-Za-z][A-Za-z0-9]*$/.test(normalized)) {
      setError(t("relation.error.nameStartsWithLetter"));
      return;
    }
    if (normalized.length > 30) {
      setError(t("relation.error.nameTooLong"));
      return;
    }
    if (
      otherNames.some(
        (otherName) => normalize(otherName) === normalized.toLowerCase(),
      )
    ) {
      setError(t("relation.error.nameDuplicate", { source: sourceName }));
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
          {mode === "edit"
            ? t("relation.eyebrowEdit")
            : t("relation.eyebrowCreate")}
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
            {t("relation.deleteWarning", { name: initialName, source: sourceName })}
          </Alert>
        ) : (
          <>
            <TextField
              autoFocus
              fullWidth
              size="small"
              color="success"
              label={t("relation.nameLabel")}
              value={name}
              error={!!error}
              helperText={
                error ||
                (mode === "create"
                  ? t("relation.helperCreate")
                  : t("relation.helperEdit"))
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
              {isMongo
                ? t("relation.typeComposition")
                : t("relation.typeRelation")}
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
                {t("relation.targetInvalid", { name: targetName })}
              </Alert>
            )}

            {!targetExists && (
              <Alert severity="warning" variant="outlined" sx={{ mt: 2 }}>
                {t("relation.targetMissing", { name: targetName })}
              </Alert>
            )}

            <Typography className="relation-dialog__hint">
              {mode === "create"
                ? t("relation.hintCreate", { source: sourceName })
                : t("relation.hintEdit")}
            </Typography>
          </>
        )}
      </DialogContent>

      <DialogActions className="relation-dialog__actions">
        {confirmDelete ? (
          <>
            <Button color="inherit" onClick={() => setConfirmDelete(false)}>
              {t("relation.back")}
            </Button>
            <Button color="error" variant="contained" onClick={onDelete}>
              {t("common.delete")}
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
                {t("relation.deleteRelation")}
              </Button>
            )}
            <Button color="inherit" onClick={onClose}>
              {t("common.cancel")}
            </Button>
            <Button
              color="success"
              variant="contained"
              onClick={save}
              disabled={targetInvalid}
            >
              {mode === "edit"
                ? t("common.save")
                : isMongo
                  ? t("relation.createComposition")
                  : t("relation.createRelation")}
            </Button>
          </>
        )}
      </DialogActions>
    </Dialog>
  );
};

export default RelationDialog;
