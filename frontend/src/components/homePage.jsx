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
    <Container maxWidth="sm" className="container">
      <Typography variant="h4">Crud Initializr</Typography>
      <TextField
        className="input"
        id="project-name"
        label="Nombre del proyecto"
        variant="outlined"
        value={businessName}
        onChange={({ target: { value } }) => setBusinessName(value)}
      />
      {entities.map((entity) => (
        <EntityForm
          key={entity.uuid}
          entities={entities}
          setEntities={setEntities}
          uuid={entity.uuid}
        />
      ))}
      <div className="container button-container">
        <Button variant="contained" onClick={addNewDomainEntityForm}>
          Agregar nueva entidad de dominio
        </Button>
        <Button variant="contained" onClick={submit}>
          Crear Proyecto
        </Button>
      </div>
      <Snackbar
        open={!!errorMessage.length}
        autoHideDuration={3000}
        onClose={() => setErrorMessage("")}
      >
        <Alert severity="error" sx={{ width: "100%" }}>
          <div>{errorMessage}</div>
        </Alert>
      </Snackbar>
    </Container>
  );
}

export default HomePage;
