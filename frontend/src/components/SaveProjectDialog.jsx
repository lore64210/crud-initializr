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
import { useLanguage } from "../i18n/LanguageContext";
import "../css/app.css";

const SaveProjectDialog = ({
  open,
  initialName = "",
  description,
  onCancel,
  onConfirm,
}) => {
  const { t } = useLanguage();
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
      setError(t("saveDialog.errorEmpty"));
      return;
    }
    if (trimmed.length > 30) {
      setError(t("saveDialog.errorTooLong"));
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
        <span className="project-dialog__eyebrow">
          {t("saveDialog.eyebrow")}
        </span>
        <Typography className="project-dialog__heading">
          <SaveIcon className="project-dialog__icon" />
          {t("saveDialog.title")}
        </Typography>
      </DialogTitle>
      <DialogContent className="project-dialog__content" dividers>
        <p className="project-dialog__intro">
          {description || t("saveDialog.descriptionDefault")}
        </p>
        <TextField
          autoFocus
          fullWidth
          color="success"
          size="small"
          label={t("saveDialog.nameLabel")}
          value={name}
          error={!!error}
          helperText={error || t("saveDialog.nameHelper")}
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
          {t("common.cancel")}
        </Button>
        <Button
          className="project-dialog__confirm"
          variant="contained"
          color="success"
          onClick={submit}
        >
          {t("common.save")}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default SaveProjectDialog;
