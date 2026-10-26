import React, { useState } from "react";
import {
  Card,
  Chip,
  Collapse,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  TextField,
  Typography,
} from "@mui/material";
import DeleteIcon from "@mui/icons-material/Delete";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import "../css/app.css";

const TYPE_LABELS = {
  STRING: "String",
  INTEGER: "Integer",
  LONG: "Long",
  BOOLEAN: "Boolean",
  CUSTOM: "Relación",
  CUSTOM_LIST: "Lista",
};

const AttributeForm = ({
  attribute,
  entities,
  entityUuid,
  onChange,
  onDelete,
}) => {
  const isEmpty = !attribute.name.trim();
  const [expanded, setExpanded] = useState(false);

  const changeName = (name) => {
    onChange({ ...attribute, name });
  };

  const changeType = (type) => {
    onChange({
      ...attribute,
      type,
      customType: type.includes("CUSTOM") ? attribute.customType : "",
      validations: [],
    });
  };

  const changeCustomType = (customType) => {
    onChange({ ...attribute, customType });
  };

  const addValidation = (validation) => {
    if (!attribute.validations.includes(validation)) {
      onChange({
        ...attribute,
        validations: [...attribute.validations, validation],
      });
    }
  };

  const deleteValidation = (validation) => {
    onChange({
      ...attribute,
      validations: attribute.validations.filter((value) => value !== validation),
    });
  };

  const toggleExpanded = () => {
    setExpanded((current) => !current);
  };

  const handleSummaryKeyDown = (event) => {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      toggleExpanded();
    }
  };

  const typeLabel = TYPE_LABELS[attribute.type] || attribute.type;
  const isRelation = attribute.type.includes("CUSTOM");
  const bodyId = `attribute-body-${attribute.uuid}`;

  return (
    <Card
      className={`attribute-card${expanded ? " attribute-card--expanded" : ""}`}
      variant="outlined"
    >
      <div
        className="attribute-summary"
        role="button"
        tabIndex={0}
        aria-expanded={expanded}
        aria-controls={bodyId}
        onClick={toggleExpanded}
        onKeyDown={handleSummaryKeyDown}
      >
        <ExpandMoreIcon
          className={`attribute-summary-chevron${
            expanded ? " attribute-summary-chevron--open" : ""
          }`}
          fontSize="small"
        />
        <div className="attribute-summary-content">
          <span
            className={`attribute-summary-name${
              isEmpty ? " attribute-summary-name--empty" : ""
            }`}
          >
            {isEmpty ? "Atributo sin nombre" : attribute.name}
          </span>
          <span className="attribute-summary-tags">
            <Chip
              className="attribute-summary-chip"
              size="small"
              label={typeLabel}
            />
            {isRelation && (
              <Chip
                className="attribute-summary-chip attribute-summary-chip--relation"
                size="small"
                label={attribute.customType || "sin entidad"}
              />
            )}
            {!!attribute.validations.length && (
              <Chip
                className="attribute-summary-chip attribute-summary-chip--validations"
                size="small"
                label={
                  attribute.validations.length === 1
                    ? attribute.validations[0]
                    : `${attribute.validations.length} validaciones`
                }
              />
            )}
          </span>
        </div>
        <IconButton
          aria-label={`Eliminar atributo ${attribute.name || "sin nombre"}`}
          size="small"
          onClick={(event) => {
            event.stopPropagation();
            onDelete();
          }}
          onKeyDown={(event) => event.stopPropagation()}
        >
          <DeleteIcon fontSize="small" />
        </IconButton>
      </div>

      <Collapse in={expanded} timeout="auto" unmountOnExit>
        <div className="attribute-body" id={bodyId}>
          <TextField
            color="success"
            className="input"
            id={`attribute-name-${attribute.uuid}`}
            label="Nombre del atributo"
            size="small"
            variant="outlined"
            value={attribute.name}
            onChange={({ target: { value } }) => changeName(value)}
          />
          <InputLabel id={`attribute-type-label-${attribute.uuid}`}>
            Tipo del atributo
          </InputLabel>
          <Select
            color="success"
            labelId={`attribute-type-label-${attribute.uuid}`}
            id={`attribute-type-${attribute.uuid}`}
            value={attribute.type}
            label="Tipo"
            sx={{ width: "100%" }}
            size="small"
            onChange={({ target: { value } }) => changeType(value)}
          >
            <MenuItem value={"STRING"}>String (VARCHAR(255))</MenuItem>
            <MenuItem value={"INTEGER"}>Integer (INT)</MenuItem>
            <MenuItem value={"LONG"}>Long (BIGINT)</MenuItem>
            <MenuItem value={"BOOLEAN"}>Boolean (BOOL)</MenuItem>
            <MenuItem value={"CUSTOM"}>CUSTOM (MANY TO ONE)</MenuItem>
            <MenuItem value={"CUSTOM_LIST"}>CUSTOM LIST (ONE TO MANY)</MenuItem>
          </Select>
          {isRelation && (
            <>
              <InputLabel id={`attribute-type-custom-label-${attribute.uuid}`}>
                Tipo CUSTOM del atributo
              </InputLabel>
              <Select
                color="success"
                labelId={`attribute-type-custom-label-${attribute.uuid}`}
                id={`attribute-type-custom-${attribute.uuid}`}
                value={attribute.customType || ""}
                displayEmpty
                label="Tipo CUSTOM"
                size="small"
                sx={{ width: "100%" }}
                onChange={({ target: { value } }) => changeCustomType(value)}
              >
                <MenuItem value={""} disabled>
                  Seleccionar entidad
                </MenuItem>
                {entities
                  .filter(
                    (entity) =>
                      entity.uuid !== entityUuid && entity.entityName.trim(),
                  )
                  .map((entity) => (
                    <MenuItem key={entity.uuid} value={entity.entityName}>
                      {entity.entityName}
                    </MenuItem>
                  ))}
              </Select>
            </>
          )}
          <InputLabel id={`attribute-validations-label-${attribute.uuid}`}>
            Validaciones
          </InputLabel>
          <Select
            color="success"
            labelId={`attribute-validations-label-${attribute.uuid}`}
            id={`attribute-validations-${attribute.uuid}`}
            value=""
            displayEmpty
            label="Validaciones"
            size="small"
            sx={{ width: "100%" }}
            onChange={({ target: { value } }) => value && addValidation(value)}
          >
            <MenuItem value="" disabled>
              Seleccionar validación
            </MenuItem>
            <MenuItem
              value={"NOT_NULL"}
              disabled={attribute.validations.includes("NOT_NULL")}
            >
              Not null
            </MenuItem>
            {attribute.type === "STRING" && (
              <MenuItem
                value={"NOT_BLANK"}
                disabled={attribute.validations.includes("NOT_BLANK")}
              >
                String no vacío
              </MenuItem>
            )}
            {attribute.type === "CUSTOM_LIST" && (
              <MenuItem
                value={"NOT_EMPTY"}
                disabled={attribute.validations.includes("NOT_EMPTY")}
              >
                Array no vacío
              </MenuItem>
            )}
          </Select>
          {!!attribute.validations.length && (
            <Typography variant="body2" sx={{ marginTop: "0.75em" }}>
              Validaciones seleccionadas:
            </Typography>
          )}
          <div className="attribute-validation-chips">
            {attribute.validations.map((validation) => (
              <Chip
                key={validation}
                label={validation}
                size="small"
                onDelete={() => deleteValidation(validation)}
              />
            ))}
          </div>
        </div>
      </Collapse>
    </Card>
  );
};

export default AttributeForm;
