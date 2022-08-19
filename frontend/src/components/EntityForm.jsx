import React, {useState, useEffect} from 'react';
import { Button, Card, TextField, Typography } from '@mui/material';
import createUUID from 'react-uuid'
import AttributeForm from './AttributeForm';
import '../css/app.css';

const emptyDomainEntityAttribute = {
    name: "",
    type: "STRING",
    validations: [],
};

const emptyDomainEntity = {
    entityName: "",
    attributes: []
};

const EntityForm = ({entities, setEntities, uuid}) => {
    
    const [entity, setEntity] = useState({...emptyDomainEntity, uuid, attributes: [{...emptyDomainEntityAttribute, uuid: createUUID()}]});

    useEffect(() => {
        setEntities(entities => entities.map(e => e.uuid === entity.uuid ? {...entity} : e));
    }, [entity]);

    const addNewDomainEntityAttribute = () => {
        setEntity({...entity, attributes: [...entity.attributes, {...emptyDomainEntityAttribute, uuid: createUUID()}]});
    }

    const deleteEntity = () => {
        setEntities(entities => entities.filter(e => e.uuid !== uuid));
    }

    return (
        <Card className="card">
            <Typography variant="h6">Entidad de dominio: {entity.entityName}</Typography>
            <br></br>
            <TextField 
                className="input"
                id={"entity-name-" + entity.uuid} 
                label="Nombre de la entidad de dominio"
                variant="outlined" 
                value={entity.entityName}
                onChange={({target: {value}}) => setEntity({...entity, entityName: value})}
            />
            <Typography sx={{marginTop: '1em', fontWeight: 'bold'}}>Atributos de la entidad:</Typography>
            
            {
                entity?.attributes?.map(attribute => (
                    <AttributeForm 
                        key={attribute.uuid}
                        uuid={attribute.uuid}
                        initialAttribute={attribute}
                        setEntity={setEntity}
                        entity={entity}
                        entities={entities}
                    />
                ))
            }
            <Button variant="contained" onClick={addNewDomainEntityAttribute} sx={{marginTop: '1em', marginBottom: '1em'}}>Agregar nuevo atributo</Button>
            <br></br>
            <Button variant="contained" sx={{backgroundColor: 'red', marginTop: '1em', marginBottom: '1em'}} onClick={deleteEntity}>Eliminar entidad de dominio</Button>
        </Card>
    )
}

export default EntityForm;