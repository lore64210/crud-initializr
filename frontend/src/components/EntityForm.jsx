import React, { useState, useEffect } from "react";
import { Button, Card, IconButton, TextField, Typography } from "@mui/material";
import createUUID from "react-uuid";
import DeleteIcon from "@mui/icons-material/Delete";
import AttributeForm from "./AttributeForm";
import "../css/app.css";

const emptyDomainEntityAttribute = {
  name: "",
  type: "STRING",
  validations: [],
};

const emptyDomainEntity = {
  entityName: "",
  attributes: [],
};

const EntityForm = ({ entities, setEntities, uuid }) => {
  const [entity, setEntity] = useState({
    ...emptyDomainEntity,
    uuid,
    attributes: [{ ...emptyDomainEntityAttribute, uuid: createUUID() }],
  });

  useEffect(() => {
    setEntities((entities) =>
      entities.map((e) => (e.uuid === entity.uuid ? { ...entity } : e)),
    );
  }, [entity]);

  const addNewDomainEntityAttribute = () => {
    setEntity({
      ...entity,
      attributes: [
        ...entity.attributes,
        { ...emptyDomainEntityAttribute, uuid: createUUID() },
      ],
    });
  };

  const deleteEntity = () => {
    setEntities((entities) => entities.filter((e) => e.uuid !== uuid));
  };

  return (
    <Card className="card">
      <div className="form-header">
        <TextField
          color="success"
          size="small"
          className="input name-input"
          id={"entity-name-" + entity.uuid}
          label="Nombre de la entidad de dominio"
          variant="outlined"
          value={entity.entityName}
          onChange={({ target: { value } }) =>
            setEntity({ ...entity, entityName: value })
          }
        />
        <IconButton aria-label="delete" onClick={deleteEntity}>
          <DeleteIcon />
        </IconButton>
      </div>
      <Typography sx={{ marginTop: "1em" }}>
        Atributos de la entidad:
      </Typography>

      {entity?.attributes?.map((attribute) => (
        <AttributeForm
          key={attribute.uuid}
          uuid={attribute.uuid}
          initialAttribute={attribute}
          setEntity={setEntity}
          entity={entity}
          entities={entities}
        />
      ))}
      <Button
        variant="contained"
        color="success"
        onClick={addNewDomainEntityAttribute}
        sx={{ marginTop: "1em", marginBottom: "1em" }}
      >
        Agregar nuevo atributo
      </Button>
    </Card>
  );
};

export default EntityForm;
