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
  return (
    <Card id={`entity-card-${entity.uuid}`} className="card">
      <div className="form-header">
        <TextField
          color="success"
          size="small"
          className="input name-input"
          id={"entity-name-" + entity.uuid}
          label="Nombre de la entidad de dominio"
          variant="outlined"
          value={entity.entityName}
          onChange={({ target: { value } }) => onChangeEntityName(value)}
        />
        <IconButton
          aria-label={`Eliminar entidad ${entity.entityName}`}
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
            label="Documento embebido"
          />
          <Typography variant="body2" color="text.secondary">
            {entity.embedded
              ? "Se guarda dentro de otra entidad: no genera colección ni API propia."
              : "Entidad raíz: genera colección, repository, service y controller."}
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
            label="Cachear esta entidad"
          />
          <Typography variant="body2" color="text.secondary">
            {entity.cacheEnabled !== false
              ? "Sus endpoints usan Redis para las lecturas y las escrituras invalidan el cache."
              : "Sus endpoints no usan cache: siempre consultan la base de datos."}
          </Typography>
        </div>
      )}

      <Typography sx={{ marginTop: "1em" }}>
        Atributos de la entidad:
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
        Agregar nuevo atributo
      </Button>
    </Card>
  );
};

export default EntityForm;
