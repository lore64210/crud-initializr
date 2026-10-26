package com.example.projectBase.controller.advice;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class GraphQLExceptionHandler {

    @GraphQlExceptionHandler
    public GraphQLError handleIllegalArgument(
            IllegalArgumentException exception, GraphqlErrorBuilder<?> errorBuilder) {
        return errorBuilder
                .errorType(ErrorType.BAD_REQUEST)
                .message(exception.getMessage())
                .build();
    }
}
