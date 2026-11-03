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
import { useLanguage } from "../i18n/LanguageContext";
import "../css/app.css";

const SQL_RELATION_TYPES = [
  "CUSTOM",
  "CUSTOM_LIST",
  "CUSTOM_ONE_TO_ONE",
  "CUSTOM_MANY_TO_MANY",
];
const MONGO_COMPOSITION_TYPES = ["COMPOSITION", "COMPOSITION_LIST"];

const isRelationType = (type) => SQL_RELATION_TYPES.includes(type);
const isCompositionType = (type) => MONGO_COMPOSITION_TYPES.includes(type);
const isLinkType = (type) => isRelationType(type) || isCompositionType(type);
const isListType = (type) =>
  type === "CUSTOM_LIST" ||
  type === "CUSTOM_MANY_TO_MANY" ||
  type === "COMPOSITION_LIST";
const isNumericType = (type) =>
  ["INTEGER", "LONG", "DECIMAL", "DOUBLE"].includes(type);
const isTemporalType = (type) => ["DATE", "DATETIME", "TIME"].includes(type);

const TYPE_LABELS = {
  STRING: "String",
  INTEGER: "Integer",
  LONG: "Long",
  BOOLEAN: "Boolean",
  DATE: "Date",
  DATETIME: "DateTime",
  TIME: "Time",
  DECIMAL: "Decimal",
  DOUBLE: "Double",
  UUID: "UUID",
  CUSTOM: "N:1",
  CUSTOM_LIST: "1:N",
  CUSTOM_ONE_TO_ONE: "1:1",
  CUSTOM_MANY_TO_MANY: "N:M",
};

const AttributeForm = ({
  attribute,
  entities,
  entityUuid,
  database = "POSTGRESQL",
  isEmbedded = false,
  onChange,
  onDelete,
}) => {
  const { t } = useLanguage();
  const isEmpty = !attribute.name.trim();
  const [expanded, setExpanded] = useState(false);

  const isMongo = database === "MONGODB";
  const isLink = isLinkType(attribute.type);
  const relationInvalidForDatabase =
    (isMongo && isRelationType(attribute.type)) ||
    (!isMongo && isCompositionType(attribute.type));
  const linkInvalidForEmbedded = isEmbedded && isLink;

  const changeName = (name) => {
    onChange({ ...attribute, name });
  };

  const changeType = (type) => {
    onChange({
      ...attribute,
      type,
      customType: isLinkType(type) ? attribute.customType : "",
      validations: [],
      minValue: "",
      maxValue: "",
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
      validations: attribute.validations.filter(
        (value) => value !== validation,
      ),
      minValue: validation === "MIN" ? "" : attribute.minValue,
      maxValue: validation === "MAX" ? "" : attribute.maxValue,
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

  const validationLabel = (validation) =>
    t(`validationLabel.${validation}`);

  const typeLabel =
    attribute.type === "COMPOSITION"
      ? t("attribute.type.composition")
      : attribute.type === "COMPOSITION_LIST"
        ? t("attribute.type.compositionList")
        : TYPE_LABELS[attribute.type] || attribute.type;

  const bodyId = `attribute-body-${attribute.uuid}`;
  const selectedTarget = entities.find(
    (entity) => entity.entityName === attribute.customType,
  );
  const compositionTargetInvalid =
    isMongo &&
    isCompositionType(attribute.type) &&
    !!attribute.customType &&
    !selectedTarget?.embedded;

  const typeOption = (value, label, disabled, reason) => (
    <MenuItem value={value} disabled={disabled}>
      {label}
      {disabled && reason ? ` · ${reason}` : ""}
    </MenuItem>
  );

  const validationOption = (value) => (
    <MenuItem value={value} disabled={attribute.validations.includes(value)}>
      {validationLabel(value)}
    </MenuItem>
  );

  const unavailableReason = () =>
    isMongo
      ? t("attribute.reason.mongoUnavailable")
      : t("attribute.reason.embeddedUnavailable");

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
            {isEmpty ? t("attribute.unnamed") : attribute.name}
          </span>
          <span className="attribute-summary-tags">
            <Chip
              className="attribute-summary-chip"
              size="small"
              label={typeLabel}
            />
            {isLink && (
              <Chip
                className="attribute-summary-chip attribute-summary-chip--relation"
                size="small"
                label={attribute.customType || t("attribute.noTarget")}
              />
            )}
            {!!attribute.validations.length && (
              <Chip
                className="attribute-summary-chip attribute-summary-chip--validations"
                size="small"
                label={
                  attribute.validations.length === 1
                    ? validationLabel(attribute.validations[0])
                    : t("attribute.validationsCount", {
                        count: attribute.validations.length,
                      })
                }
              />
            )}
          </span>
        </div>
        <IconButton
          aria-label={t("attribute.deleteAria", {
            name: attribute.name || t("attribute.unnamed"),
          })}
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
            label={t("attribute.nameLabel")}
            size="small"
            variant="outlined"
            value={attribute.name}
            onChange={({ target: { value } }) => changeName(value)}
          />
          <InputLabel id={`attribute-type-label-${attribute.uuid}`}>
            {t("attribute.typeLabel")}
          </InputLabel>
          <Select
            color="success"
            labelId={`attribute-type-label-${attribute.uuid}`}
            id={`attribute-type-${attribute.uuid}`}
            value={attribute.type}
            label={t("attribute.type")}
            sx={{ width: "100%" }}
            size="small"
            onChange={({ target: { value } }) => changeType(value)}
          >
            <MenuItem value={"STRING"}>String (VARCHAR(255))</MenuItem>
            <MenuItem value={"INTEGER"}>Integer (INT)</MenuItem>
            <MenuItem value={"LONG"}>Long (BIGINT)</MenuItem>
            <MenuItem value={"BOOLEAN"}>Boolean (BOOL)</MenuItem>
            <MenuItem value={"DATE"}>Date (LocalDate / DATE)</MenuItem>
            <MenuItem value={"DATETIME"}>
              DateTime (LocalDateTime / TIMESTAMP)
            </MenuItem>
            <MenuItem value={"TIME"}>Time (LocalTime / TIME)</MenuItem>
            <MenuItem value={"DECIMAL"}>
              Decimal (BigDecimal / DECIMAL)
            </MenuItem>
            <MenuItem value={"DOUBLE"}>Double (DOUBLE)</MenuItem>
            <MenuItem value={"UUID"}>UUID</MenuItem>
            {typeOption(
              "CUSTOM",
              t("attribute.typeOption.relationManyToOne"),
              isMongo || isEmbedded,
              unavailableReason(),
            )}
            {typeOption(
              "CUSTOM_LIST",
              t("attribute.typeOption.relationOneToMany"),
              isMongo || isEmbedded,
              unavailableReason(),
            )}
            {typeOption(
              "CUSTOM_ONE_TO_ONE",
              t("attribute.typeOption.relationOneToOne"),
              isMongo || isEmbedded,
              unavailableReason(),
            )}
            {typeOption(
              "CUSTOM_MANY_TO_MANY",
              t("attribute.typeOption.relationManyToMany"),
              isMongo || isEmbedded,
              unavailableReason(),
            )}
            {typeOption(
              "COMPOSITION",
              t("attribute.typeOption.composition"),
              !isMongo || isEmbedded,
              !isMongo
                ? t("attribute.reason.mongoOnly")
                : t("attribute.reason.embeddedUnavailable"),
            )}
            {typeOption(
              "COMPOSITION_LIST",
              t("attribute.typeOption.compositionList"),
              !isMongo || isEmbedded,
              !isMongo
                ? t("attribute.reason.mongoOnly")
                : t("attribute.reason.embeddedUnavailable"),
            )}
          </Select>

          {isEmbedded && (
            <Typography
              variant="body2"
              sx={{ color: "text.secondary", fontSize: "0.75rem" }}
            >
              {t("attribute.embeddedSimple")}
            </Typography>
          )}

          {linkInvalidForEmbedded && (
            <Typography
              variant="body2"
              sx={{ color: "warning.main", fontSize: "0.75rem" }}
            >
              {t("attribute.embeddedNoLinks")}
            </Typography>
          )}

          {!linkInvalidForEmbedded && relationInvalidForDatabase && (
            <Typography
              variant="body2"
              sx={{ color: "warning.main", fontSize: "0.75rem" }}
            >
              {isMongo
                ? t("attribute.mongoNoRelations")
                : t("attribute.sqlNoCompositions")}
            </Typography>
          )}

          {compositionTargetInvalid && (
            <Typography
              variant="body2"
              sx={{ color: "warning.main", fontSize: "0.75rem" }}
            >
              {t("attribute.targetMustBeEmbedded")}
            </Typography>
          )}

          {isLink && (
            <>
              <InputLabel id={`attribute-type-custom-label-${attribute.uuid}`}>
                {t("attribute.targetLabel")}
              </InputLabel>
              <Select
                color="success"
                labelId={`attribute-type-custom-label-${attribute.uuid}`}
                id={`attribute-type-custom-${attribute.uuid}`}
                value={attribute.customType || ""}
                displayEmpty
                label={t("attribute.targetLabel")}
                size="small"
                sx={{ width: "100%" }}
                onChange={({ target: { value } }) => changeCustomType(value)}
              >
                <MenuItem value={""} disabled>
                  {t("attribute.selectEntity")}
                </MenuItem>
                {entities
                  .filter(
                    (entity) =>
                      entity.uuid !== entityUuid && entity.entityName.trim(),
                  )
                  .map((entity) => {
                    const unavailableForComposition =
                      isMongo && !entity.embedded;
                    return (
                      <MenuItem
                        key={entity.uuid}
                        value={entity.entityName}
                        disabled={unavailableForComposition}
                      >
                        {entity.entityName}
                        {unavailableForComposition
                          ? t("attribute.markAsEmbedded")
                          : ""}
                      </MenuItem>
                    );
                  })}
              </Select>
            </>
          )}

          <InputLabel id={`attribute-validations-label-${attribute.uuid}`}>
            {t("attribute.validations")}
          </InputLabel>
          <Select
            color="success"
            labelId={`attribute-validations-label-${attribute.uuid}`}
            id={`attribute-validations-${attribute.uuid}`}
            value=""
            displayEmpty
            label={t("attribute.validations")}
            size="small"
            sx={{ width: "100%" }}
            onChange={({ target: { value } }) => value && addValidation(value)}
          >
            <MenuItem value="" disabled>
              {t("attribute.selectValidation")}
            </MenuItem>
            {validationOption("NOT_NULL")}
            {attribute.type === "STRING" && validationOption("NOT_BLANK")}
            {isListType(attribute.type) && validationOption("NOT_EMPTY")}
            {isNumericType(attribute.type) && (
              <>
                {validationOption("POSITIVE")}
                {validationOption("NEGATIVE")}
                {validationOption("POSITIVE_OR_ZERO")}
                {validationOption("NEGATIVE_OR_ZERO")}
                {validationOption("MIN")}
                {validationOption("MAX")}
              </>
            )}
            {isTemporalType(attribute.type) && (
              <>
                {validationOption("PAST")}
                {validationOption("PAST_OR_PRESENT")}
                {validationOption("FUTURE")}
                {validationOption("FUTURE_OR_PRESENT")}
              </>
            )}
          </Select>

          {isNumericType(attribute.type) &&
            attribute.validations.includes("MIN") && (
              <TextField
                color="success"
                className="input"
                label={t("attribute.min")}
                type="number"
                size="small"
                variant="outlined"
                inputProps={{ step: "any" }}
                value={attribute.minValue ?? ""}
                onChange={({ target: { value } }) =>
                  onChange({ ...attribute, minValue: value })
                }
              />
            )}

          {isNumericType(attribute.type) &&
            attribute.validations.includes("MAX") && (
              <TextField
                color="success"
                className="input"
                label={t("attribute.max")}
                type="number"
                size="small"
                variant="outlined"
                inputProps={{ step: "any" }}
                value={attribute.maxValue ?? ""}
                onChange={({ target: { value } }) =>
                  onChange({ ...attribute, maxValue: value })
                }
              />
            )}
          {!!attribute.validations.length && (
            <Typography variant="body2" sx={{ marginTop: "0.75em" }}>
              {t("attribute.selectedValidations")}
            </Typography>
          )}
          <div className="attribute-validation-chips">
            {attribute.validations.map((validation) => (
              <Chip
                key={validation}
                label={
                  validation === "MIN" && attribute.minValue !== ""
                    ? t("attribute.minChip", { value: attribute.minValue })
                    : validation === "MAX" && attribute.maxValue !== ""
                      ? t("attribute.maxChip", { value: attribute.maxValue })
                      : validationLabel(validation)
                }
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
