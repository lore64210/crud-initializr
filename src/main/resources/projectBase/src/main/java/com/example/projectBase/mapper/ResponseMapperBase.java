package com.example.projectBase.mapper;

import com.example.projectBase.domain.ENTITY_CLASS;
import com.example.projectBase.dto.ENTITY_CLASSResponse;
IMPORTS

public final class ENTITY_CLASSResponseMapper {

    private ENTITY_CLASSResponseMapper() {
    }

    public static ENTITY_CLASSResponse toResponse(ENTITY_CLASS entity) {
        ENTITY_CLASSResponse response = new ENTITY_CLASSResponse();
        response.setId(entity.getId());
        response.setFechaCreacion(entity.getFechaCreacion());
        MAPPING
        return response;
    }

}
