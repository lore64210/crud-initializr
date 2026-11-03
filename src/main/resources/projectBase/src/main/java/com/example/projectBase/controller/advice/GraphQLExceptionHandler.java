package com.example.projectBase.controller.advice;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class GraphQLExceptionHandler {

    @GraphQlExceptionHandler
    public GraphQLError handleNotFound(
            NotFoundException exception, GraphqlErrorBuilder<?> errorBuilder) {
        return errorBuilder
                .errorType(ErrorType.NOT_FOUND)
                .message(exception.getMessage())
                .build();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleIllegalArgument(
            IllegalArgumentException exception, GraphqlErrorBuilder<?> errorBuilder) {
        return errorBuilder
                .errorType(ErrorType.BAD_REQUEST)
                .message(exception.getMessage())
                .build();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleDataIntegrity(
            DataIntegrityViolationException exception, GraphqlErrorBuilder<?> errorBuilder) {
        return errorBuilder
                .errorType(ErrorType.BAD_REQUEST)
                .message("No se pudo guardar: viola una restricción de integridad.")
                .build();
    }
}
