package com.example.projectBase.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
IMPORTS
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ENTITY_DECLARATIONGraphQLController {

    private final ENTITY_DECLARATIONService ENTITYService;

    @QueryMapping
    public List<ENTITY_DECLARATION> findAllENTITY_DECLARATION() {
        return ENTITYService.findAll();
    }

    @QueryMapping
    public ENTITY_DECLARATION findENTITY_DECLARATIONById(@Argument("id") String id) {
        return ENTITYService.findById(ID_EXPRESSION);
    }

    @MutationMapping
    public ENTITY_DECLARATION createENTITY_DECLARATION(@Argument("input") INPUT_TYPE input) {
        CREATE_BODY
    }

    @MutationMapping
    public ENTITY_DECLARATION updateENTITY_DECLARATION(@Argument("input") INPUT_TYPE input) {
        UPDATE_BODY
    }

    @MutationMapping
    public Boolean deleteENTITY_DECLARATION(@Argument("id") String id) {
        ENTITYService.delete(ID_EXPRESSION);
        return true;
    }

}
