import React from "react";
import { Button, Card, IconButton, TextField, Typography } from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/Delete";
import AttributeForm from "./AttributeForm";
import "../css/app.css";

const EntityForm = ({
  entity,
  entities,
  onChangeEntityName,
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
      <Typography sx={{ marginTop: "1em" }}>
        Atributos de la entidad:
      </Typography>

      {entity.attributes.map((attribute) => (
        <AttributeForm
          key={attribute.uuid}
          attribute={attribute}
          entities={entities}
          entityUuid={entity.uuid}
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
