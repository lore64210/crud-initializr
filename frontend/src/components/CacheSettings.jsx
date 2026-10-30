import React from "react";
import {
  FormControlLabel,
  Switch,
  TextField,
  Typography,
} from "@mui/material";
import CachedIcon from "@mui/icons-material/Cached";
import "../css/app.css";

const CacheSettings = ({ value, onChange, entities = [], onToggleEntity }) => {
  const update = (patch) => onChange({ ...value, ...patch });
  const rootEntities = entities.filter((entity) => !entity.embedded);
  const cachedCount = rootEntities.filter(
    (entity) => entity.cacheEnabled !== false,
  ).length;

  return (
    <div className="database-selector database-selector--cache">
      <div className="database-selector__header">
        <CachedIcon className="database-selector__icon" />
        <div className="database-selector__titles">
          <Typography className="database-selector__title">
            Cache Redis
          </Typography>
          <Typography className="database-selector__subtitle">
            Opcional. Agrega una capa de cache para las lecturas del proyecto
            generado.
          </Typography>
        </div>
      </div>

      <FormControlLabel
        className="cache-settings__toggle"
        control={
          <Switch
            color="success"
            checked={!!value.enabled}
            onChange={({ target: { checked } }) => update({ enabled: checked })}
          />
        }
        label="Habilitar cache Redis"
      />

      {value.enabled && (
        <>
          <div className="cache-settings__fields">
            <TextField
              color="success"
              className="input"
              id="cache-prefix"
              label="Prefijo de claves"
              size="small"
              variant="outlined"
              value={value.prefix}
              helperText="Ejemplo: crud"
              onChange={({ target: { value: prefix } }) => update({ prefix })}
            />
            <TextField
              color="success"
              className="input"
              id="cache-ttl"
              label="TTL (segundos)"
              size="small"
              type="number"
              variant="outlined"
              value={value.ttlSeconds}
              onChange={({ target: { value: ttlSeconds } }) =>
                update({ ttlSeconds })
              }
            />
            <FormControlLabel
              control={
                <Switch
                  color="success"
                  checked={!!value.cacheFindAll}
                  onChange={({ target: { checked } }) =>
                    update({ cacheFindAll: checked })
                  }
                />
              }
              label="Cachear listados"
            />
          </div>

          <div className="cache-settings__entities">
            <div className="cache-settings__entities-header">
              <Typography className="cache-settings__entities-title">
                Entidades a cachear
              </Typography>
              <Typography className="cache-settings__entities-count">
                {cachedCount} de {rootEntities.length}
              </Typography>
            </div>

            {rootEntities.length === 0 ? (
              <Typography className="database-selector__note">
                Agregá al menos una entidad para poder cachearla.
              </Typography>
            ) : (
              <div className="cache-settings__entity-list">
                {rootEntities.map((entity) => (
                  <FormControlLabel
                    key={entity.uuid}
                    className="cache-settings__entity"
                    control={
                      <Switch
                        size="small"
                        color="success"
                        checked={entity.cacheEnabled !== false}
                        onChange={({ target: { checked } }) =>
                          onToggleEntity?.(entity.uuid, checked)
                        }
                      />
                    }
                    label={entity.entityName.trim() || "Entidad sin nombre"}
                  />
                ))}
              </div>
            )}
          </div>

          <Typography className="database-selector__note">
            findById se cachea siempre en las entidades seleccionadas. Las
            escrituras actualizan o invalidan el cache. Los tests generados usan
            cache deshabilitado.
          </Typography>
        </>
      )}

      {!value.enabled && (
        <Typography className="database-selector__note">
          El proyecto se genera sin Redis. Podés habilitarlo después.
        </Typography>
      )}
    </div>
  );
};

export default CacheSettings;
