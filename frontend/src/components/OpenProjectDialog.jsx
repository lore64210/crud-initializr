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
import { useLanguage } from "../i18n/LanguageContext";
import "../css/app.css";

const OpenProjectDialog = ({
  open,
  project,
  currentName = "",
  onCancel,
  onDiscard,
  onSaveAndContinue,
}) => {
  const { t } = useLanguage();
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
        <span className="project-dialog__eyebrow">
          {t("openDialog.eyebrow")}
        </span>
        <Typography className="project-dialog__heading">
          <WarningAmberIcon className="project-dialog__icon project-dialog__icon--warning" />
          {isNew
            ? t("openDialog.newTitle")
            : t("openDialog.openTitle", { name: project.name })}
        </Typography>
      </DialogTitle>
      <DialogContent className="project-dialog__content" dividers>
        <p className="project-dialog__intro">
          {isNew ? t("openDialog.newIntro") : t("openDialog.openIntro")}
        </p>
        <p className="project-dialog__note">
          {t("openDialog.note")}{" "}
          {currentUnnamed
            ? t("openDialog.noteUnnamed")
            : t("openDialog.noteNamed")}
        </p>
      </DialogContent>
      <DialogActions className="project-dialog__actions">
        <Button className="project-dialog__cancel" onClick={onCancel}>
          {t("common.cancel")}
        </Button>
        <Button className="project-dialog__discard" onClick={onDiscard}>
          {t("openDialog.discard")}
        </Button>
        <Button
          className="project-dialog__confirm"
          variant="contained"
          color="success"
          onClick={onSaveAndContinue}
        >
          {t("openDialog.saveAndContinue")}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default OpenProjectDialog;
