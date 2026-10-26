package com.example.projectBase.config;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

@Configuration
public class GraphQLScalarConfig {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_TIME;

    @Bean
    public RuntimeWiringConfigurer graphQlScalars() {
        return wiringBuilder -> wiringBuilder
                .scalar(stringScalar("Date", "Fecha ISO-8601 (yyyy-MM-dd)", LocalDate.class,
                        value -> LocalDate.parse(value, DATE_FORMAT),
                        value -> value.format(DATE_FORMAT)))
                .scalar(stringScalar("DateTime", "Fecha y hora ISO-8601", LocalDateTime.class,
                        value -> LocalDateTime.parse(value, DATE_TIME_FORMAT),
                        value -> value.format(DATE_TIME_FORMAT)))
                .scalar(stringScalar("Time", "Hora ISO-8601 (HH:mm:ss)", LocalTime.class,
                        value -> LocalTime.parse(value, TIME_FORMAT),
                        value -> value.format(TIME_FORMAT)))
                .scalar(stringScalar("UUID", "Identificador UUID", UUID.class,
                        UUID::fromString,
                        UUID::toString))
                .scalar(bigDecimalScalar());
    }

    private static <T> GraphQLScalarType stringScalar(
            String name,
            String description,
            Class<T> type,
            Function<String, T> parser,
            Function<T, String> formatter) {
        return GraphQLScalarType.newScalar()
                .name(name)
                .description(description)
                .coercing(new Coercing<T, String>() {
                    @Override
                    public String serialize(Object input, GraphQLContext context, Locale locale) {
                        if (type.isInstance(input)) {
                            return formatter.apply(type.cast(input));
                        }
                        throw new CoercingSerializeException("Expected " + type.getSimpleName()
                                + " for " + name + " but was " + input.getClass().getSimpleName());
                    }

                    @Override
                    public T parseValue(Object input, GraphQLContext context, Locale locale) {
                        if (input instanceof String value) {
                            try {
                                return parser.apply(value);
                            } catch (RuntimeException exception) {
                                throw new CoercingParseValueException(
                                        "Invalid " + name + ": " + value, exception);
                            }
                        }
                        throw new CoercingParseValueException("Expected String for " + name);
                    }

                    @Override
                    public T parseLiteral(
                            Value<?> input,
                            CoercedVariables variables,
                            GraphQLContext context,
                            Locale locale) {
                        if (input instanceof StringValue value && value.getValue() != null) {
                            String rawValue = value.getValue();
                            try {
                                return parser.apply(rawValue);
                            } catch (RuntimeException exception) {
                                throw new CoercingParseLiteralException(
                                        "Invalid " + name + ": " + rawValue, exception);
                            }
                        }
                        throw new CoercingParseLiteralException("Expected StringValue for " + name);
                    }
                })
                .build();
    }

    private static GraphQLScalarType bigDecimalScalar() {
        return GraphQLScalarType.newScalar()
                .name("BigDecimal")
                .description("Número decimal de precisión arbitraria")
                .coercing(new Coercing<BigDecimal, String>() {
                    @Override
                    public String serialize(Object input, GraphQLContext context, Locale locale) {
                        if (input instanceof BigDecimal value) {
                            return value.toPlainString();
                        }
                        if (input instanceof Number number) {
                            return new BigDecimal(number.toString()).toPlainString();
                        }
                        throw new CoercingSerializeException("Expected BigDecimal but was "
                                + input.getClass().getSimpleName());
                    }

                    @Override
                    public BigDecimal parseValue(Object input, GraphQLContext context, Locale locale) {
                        if (input instanceof BigDecimal value) {
                            return value;
                        }
                        if (input instanceof Number number) {
                            return new BigDecimal(number.toString());
                        }
                        if (input instanceof String value) {
                            return parseBigDecimalValue(value);
                        }
                        throw new CoercingParseValueException(
                                "Expected number or String for BigDecimal");
                    }

                    @Override
                    public BigDecimal parseLiteral(
                            Value<?> input,
                            CoercedVariables variables,
                            GraphQLContext context,
                            Locale locale) {
                        if (input instanceof StringValue value && value.getValue() != null) {
                            return parseBigDecimalLiteral(value.getValue());
                        }
                        if (input instanceof IntValue value) {
                            return new BigDecimal(value.getValue());
                        }
                        if (input instanceof FloatValue value) {
                            return value.getValue();
                        }
                        throw new CoercingParseLiteralException(
                                "Expected number or StringValue for BigDecimal");
                    }
                })
                .build();
    }

    private static BigDecimal parseBigDecimalValue(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new CoercingParseValueException("Invalid BigDecimal: " + value, exception);
        }
    }

    private static BigDecimal parseBigDecimalLiteral(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new CoercingParseLiteralException("Invalid BigDecimal: " + value, exception);
        }
    }
}
