import React from "react";
import {
  FormControlLabel,
  Switch,
  TextField,
  Typography,
} from "@mui/material";
import CachedIcon from "@mui/icons-material/Cached";
import { useLanguage } from "../i18n/LanguageContext";
import "../css/app.css";

const CacheSettings = ({ value, onChange, entities = [], onToggleEntity }) => {
  const { t } = useLanguage();
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
            {t("cache.title")}
          </Typography>
          <Typography className="database-selector__subtitle">
            {t("cache.subtitle")}
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
        label={t("cache.enable")}
      />

      {value.enabled && (
        <>
          <div className="cache-settings__fields">
            <TextField
              color="success"
              className="input"
              id="cache-prefix"
              label={t("cache.prefix")}
              size="small"
              variant="outlined"
              value={value.prefix}
              helperText={t("cache.prefixExample")}
              onChange={({ target: { value: prefix } }) => update({ prefix })}
            />
            <TextField
              color="success"
              className="input"
              id="cache-ttl"
              label={t("cache.ttl")}
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
              label={t("cache.cacheLists")}
            />
          </div>

          <div className="cache-settings__entities">
            <div className="cache-settings__entities-header">
              <Typography className="cache-settings__entities-title">
                {t("cache.entitiesTitle")}
              </Typography>
              <Typography className="cache-settings__entities-count">
                {t("cache.entitiesCount", {
                  cached: cachedCount,
                  total: rootEntities.length,
                })}
              </Typography>
            </div>

            {rootEntities.length === 0 ? (
              <Typography className="database-selector__note">
                {t("cache.empty")}
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
                    label={
                      entity.entityName.trim() || t("home.unnamedEntity")
                    }
                  />
                ))}
              </div>
            )}
          </div>

          <Typography className="database-selector__note">
            {t("cache.note")}
          </Typography>
        </>
      )}

      {!value.enabled && (
        <Typography className="database-selector__note">
          {t("cache.disabledNote")}
        </Typography>
      )}
    </div>
  );
};

export default CacheSettings;
