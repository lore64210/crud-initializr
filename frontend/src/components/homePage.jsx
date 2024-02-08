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
  IconButton,
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";

import "../css/app.css";

const emptyDomainEntity = {
  entityName: "",
  attributes: [],
};

const warningMessage =
  "WARNING: Segun las validaciones que se usen en los atributos de tipo CUSTOM y CUSTOM_LIST, los test pueden fallar. Revisar los builders que se usan al crear instancias genericas y reemplazar por lo que haga falta";

function HomePage() {
  const [projectDownloaded, setProjectDownloaded] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [businessName, setBusinessName] = useState("");
  const [entities, setEntities] = useState([
    { ...emptyDomainEntity, uuid: uuid() },
  ]);

  const addNewDomainEntityForm = () => {
    setEntities([{ ...emptyDomainEntity, uuid: uuid() }, ...entities]);
  };

  const submit = async () => {
    const error = await service.createProject({
      businessName,
      entities,
    });
    if (error) {
      setErrorMessage(error);
    } else {
      setProjectDownloaded(true);
    }
  };

  return (
    <div className="main">
      <div className="header-container">
        <div className="header">
          <div className="logo-container">
            <img src="/favicon.ico" alt="spring-logo" className="spring-logo" />
            <Typography variant="h4" className="title">
              Spring CRUD Initializr
            </Typography>
          </div>
        </div>
        <p>
          Proyecto personal de Lorenzo Lopez para crear aplicaciones de
          SpringBoot v3.2.0.0.0.0
        </p>
        <p></p>
      </div>
      <div className="bussines-name-input-container">
        <Container maxWidth="sm">
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
      <div className="entity-forms-container">
        <IconButton
          aria-label="add"
          className="add-entity-button"
          onClick={addNewDomainEntityForm}
        >
          <AddIcon />
        </IconButton>
        {entities?.map((entity) => (
          <EntityForm
            key={entity.uuid}
            entities={entities}
            setEntities={setEntities}
            uuid={entity.uuid}
          />
        ))}
      </div>
      <Button
        className="create-project-button"
        variant="contained"
        onClick={submit}
        color="secondary"
      >
        Crear Proyecto
      </Button>
      <Snackbar
        open={!!errorMessage.length}
        autoHideDuration={3000}
        onClose={() => setErrorMessage("")}
      >
        <Alert severity="error" sx={{ width: "100%" }}>
          <div>{errorMessage}</div>
        </Alert>
      </Snackbar>
      <Snackbar
        open={projectDownloaded}
        autoHideDuration={10000}
        onClose={() => setProjectDownloaded(false)}
      >
        <Alert severity="warning" sx={{ width: "95%" }}>
          <div>{warningMessage}</div>
        </Alert>
      </Snackbar>
    </div>
  );
}

export default HomePage;
