import React, { useState } from "react";
import EntityForm from "../components/EntityForm";
import service from "../service/service";
import uuid from "react-uuid";
import {
  Snackbar,
  Alert,
  Container,
  Button,
  TextField,
  Typography,
} from "@mui/material";

import "../css/app.css";

const emptyDomainEntity = {
  entityName: "",
  attributes: [],
};

function HomePage() {
  const [errorMessage, setErrorMessage] = useState("");
  const [businessName, setBusinessName] = useState("");
  const [entities, setEntities] = useState([
    { ...emptyDomainEntity, uuid: uuid() },
  ]);

  const addNewDomainEntityForm = () => {
    setEntities([...entities, { ...emptyDomainEntity, uuid: uuid() }]);
  };

  const submit = async () => {
    const error = await service.createProject({ businessName, entities });
    if (error) {
      setErrorMessage(error);
    }
  };

  return (
    <>
      <div className="header-container">
        <div className="header">
          <Typography variant="h4" className="title">
            Spring CRUD Initializr
          </Typography>
          <Button
            color="success"
            variant="contained"
            onClick={addNewDomainEntityForm}
          >
            Agregar nueva entidad de dominio
          </Button>
          <Button variant="contained" onClick={submit} color="success">
            Crear Proyecto
          </Button>
        </div>
        <p>
          Proyecto personal de Lorenzo Lopez para crear aplicaciones de Spring
        </p>
        <Container maxWidth="sm" className="bussines-name-input-container">
          <TextField
            color="success"
            className="input"
            id="project-name"
            label="Nombre del proyecto"
            variant="outlined"
            size="small"
            value={businessName}
            onChange={({ target: { value } }) => setBusinessName(value)}
          />
        </Container>
      </div>
      {!!entities?.length && (
        <div className="entity-forms-container">
          {entities.map((entity) => (
            <EntityForm
              key={entity.uuid}
              entities={entities}
              setEntities={setEntities}
              uuid={entity.uuid}
            />
          ))}
        </div>
      )}

      <Snackbar
        open={!!errorMessage.length}
        autoHideDuration={3000}
        onClose={() => setErrorMessage("")}
      >
        <Alert severity="error" sx={{ width: "100%" }}>
          <div>{errorMessage}</div>
        </Alert>
      </Snackbar>
    </>
  );
}

export default HomePage;
