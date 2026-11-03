import React from "react";
import {
  Button,
  Card,
  FormControlLabel,
  IconButton,
  Switch,
  TextField,
  Typography,
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/Delete";
import AttributeForm from "./AttributeForm";
import { useLanguage } from "../i18n/LanguageContext";
import "../css/app.css";

const EntityForm = ({
  entity,
  entities,
  database,
  documentMode = false,
  cacheEnabled = false,
  onChangeEntityName,
  onChangeEmbedded,
  onChangeCacheEnabled,
  onChangeAttribute,
  onAddAttribute,
  onDeleteAttribute,
  onDelete,
}) => {
  const { t } = useLanguage();

  return (
    <Card id={`entity-card-${entity.uuid}`} className="card">
      <div className="form-header">
        <TextField
          color="success"
          size="small"
          className="input name-input"
          id={"entity-name-" + entity.uuid}
          label={t("entity.nameLabel")}
          variant="outlined"
          value={entity.entityName}
          onChange={({ target: { value } }) => onChangeEntityName(value)}
        />
        <IconButton
          aria-label={t("entity.deleteAria", { name: entity.entityName })}
          onClick={onDelete}
        >
          <DeleteIcon />
        </IconButton>
      </div>
      {documentMode && (
        <div className="entity-embedded-toggle">
          <FormControlLabel
            control={
              <Switch
                color="success"
                checked={!!entity.embedded}
                onChange={({ target: { checked } }) =>
                  onChangeEmbedded?.(checked)
                }
              />
            }
            label={t("entity.embedded")}
          />
          <Typography variant="body2" color="text.secondary">
            {entity.embedded
              ? t("entity.embeddedTrue")
              : t("entity.embeddedFalse")}
          </Typography>
        </div>
      )}

      {cacheEnabled && !entity.embedded && (
        <div className="entity-cache-toggle">
          <FormControlLabel
            control={
              <Switch
                color="success"
                checked={entity.cacheEnabled !== false}
                onChange={({ target: { checked } }) =>
                  onChangeCacheEnabled?.(checked)
                }
              />
            }
            label={t("entity.cacheLabel")}
          />
          <Typography variant="body2" color="text.secondary">
            {entity.cacheEnabled !== false
              ? t("entity.cacheTrue")
              : t("entity.cacheFalse")}
          </Typography>
        </div>
      )}

      <Typography sx={{ marginTop: "1em" }}>
        {t("entity.attributes")}
      </Typography>

      {entity.attributes.map((attribute) => (
        <AttributeForm
          key={attribute.uuid}
          attribute={attribute}
          entities={entities}
          entityUuid={entity.uuid}
          database={database}
          isEmbedded={!!entity.embedded}
          onChange={onChangeAttribute}
          onDelete={() => onDeleteAttribute(attribute.uuid)}
        />
      ))}
      <Button
        className="add-attribute-button"
        variant="outlined"
        color="success"
        startIcon={<AddIcon />}
        onClick={onAddAttribute}
      >
        {t("entity.addAttribute")}
      </Button>
    </Card>
  );
};

export default EntityForm;
