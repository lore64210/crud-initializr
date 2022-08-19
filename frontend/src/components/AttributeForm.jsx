import React, { useEffect, useState } from 'react';
import { Button, Card, Chip, InputLabel, MenuItem, Select, TextField, Typography } from '@mui/material';
import '../css/app.css';

const AttributeForm = ({initialAttribute, setEntity, entity, uuid, entities}) => {

    const [attribute, setAttribute] = useState(initialAttribute);
   
    useEffect(() => {
        setEntity({...entity, attributes: entity.attributes.map(a => a.uuid === uuid ? {...attribute} : a)});
    }, [attribute]);

    useEffect(() => {
        setAttribute({...attribute, validations: [  ]})
    }, [attribute.type]);

    const changeAttributeName = (name) => {
        setAttribute({...attribute, name})
    }
    
    const changeAttributeType = (type) => {
        setAttribute({...attribute, type})
    }

    const changeAttributeCustomType = (customType) => {
        setAttribute({...attribute, customType});
    }

    const addValidation = (value) => {
        if (!attribute.validations.includes(value)) {
            setAttribute({...attribute, validations: [...attribute.validations, value]})
        }
    }

    const deleteValidation = (value) => {
        setAttribute({...attribute, validations: attribute.validations.filter(v => v !== value)})
    }

    const deleteAttribute = () => {
        setEntity({...entity, attributes: entity.attributes.filter(a => a.uuid !== uuid)});
    }

    return (
        <Card variant="outlined" sx={{padding: '1em'}} key={attribute.uuid}>
            <TextField 
                className="input"
                id={`attribute-name-${attribute.uuid}`} 
                label="Nombre del atributo"
                variant="outlined" 
                value={attribute.name}
                onChange={({target: {value}}) => changeAttributeName(value)}
            />
            <InputLabel id={`attribute-type-${attribute.uuid}`}>Tipo del atributo</InputLabel>
            <Select
                labelId={`attribute-type-${attribute.uuid}`}
                id={`attribute-type-${attribute.uuid}`}
                value={attribute.type}
                label="Tipo"
                sx={{width: "100%"}}
                onChange={({target: {value}}) => changeAttributeType(value)}
            >
                <MenuItem value={"STRING"}>String (VARCHAR(255))</MenuItem>
                <MenuItem value={"INTEGER"}>Integer (INT)</MenuItem>
                <MenuItem value={"LONG"}>Long (BIGINT)</MenuItem>
                <MenuItem value={"BOOLEAN"}>Boolean (BOOL)</MenuItem>
                <MenuItem value={"CUSTOM"}>CUSTOM (MANY TO ONE)</MenuItem>
                <MenuItem value={"CUSTOM_LIST"}>CUSTOM LIST (ONE TO MANY)</MenuItem>
            </Select>
            {
                !!attribute.type.includes("CUSTOM") && (
                    <>
                        <InputLabel id={`attribute-type-custom-${attribute.uuid}`}>Tipo CUSTOM del atributo</InputLabel>
                        <Select
                            labelId={`attribute-type-custom-${attribute.uuid}`}
                            id={`attribute-type-custom-${attribute.uuid}`}
                            value={attribute.customType}
                            label="Tipo CUSTOM"
                            sx={{width: '100%'}}
                            onChange={({target: {value}}) => changeAttributeCustomType(value)}
                        >
                            {
                                entities
                                    .filter(e => e.uuid !== entity.uuid)
                                    .map(e => <MenuItem value={e.entityName}>{e.entityName}</MenuItem>)
                            }
                        </Select>
                    </>
                        
                )
            }
            <InputLabel id={`attribute-validations-${attribute.uuid}`}>Validaciones</InputLabel>
            <Select
                labelId={`attribute-validations-${attribute.uuid}`}
                id={`attribute-validations-${attribute.uuid}`}
                value={attribute.validations}
                label="Tipo"
                sx={{width: '100%'}}
                onChange={({target: {value}}) => addValidation(value)}
            >
                <MenuItem value={"NOT_NULL"}>Not null</MenuItem>
                {attribute.type === "STRING" && <MenuItem value={"NOT_BLANK"}>String no vacio</MenuItem>}
                {attribute.type === "CUSTOM_LIST" && <MenuItem value={"NOT_EMPTY"}>Array no vacio</MenuItem>}
            </Select> 
            {!!attribute.validations.length && <Typography>Validaciones seleccionadas:</Typography>}
            {attribute.validations.map(v => (
                <Chip
                    key={v}
                    label={v}
                    onDelete={() => deleteValidation(v)}
                  />
            ))}
            <br></br>
            <Button 
                variant="contained" 
                sx={{fontSize: '.75rem', backgroundColor: 'red', marginTop: '1em', marginBottom: '1em'}} 
                onClick={deleteAttribute}
            >
                Eliminar Atributo
            </Button>
        </Card>
    );
}

export default AttributeForm;