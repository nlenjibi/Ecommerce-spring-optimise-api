package com.smart_ecomernce_api.smart_ecomernce_api.config;

import graphql.language.StringValue;
import graphql.scalars.ExtendedScalars;
import graphql.schema.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * GraphQL Scalar Type Configuration
 * Defines custom scalar types for the GraphQL API including DateTime and file Upload scalars.
 */
@Configuration
public class GraphQLScalarConfiguration {

    private static final DateTimeFormatter ISO_OFFSET_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter ISO_LOCAL_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Configures runtime wiring for custom GraphQL scalars
     */
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(dateTimeScalar())
                .scalar(ExtendedScalars.GraphQLBigDecimal)
                .scalar(ExtendedScalars.UUID)
                .scalar(ExtendedScalars.GraphQLLong)
                .scalar(uploadScalar());
    }

    /**
     * Custom DateTime scalar that handles LocalDateTime serialization/deserialization
     *
     * Serialization: LocalDateTime → ISO OffsetDateTime string (UTC)
     * Deserialization: ISO string → LocalDateTime
     */
    @Bean
    public GraphQLScalarType dateTimeScalar() {
        return GraphQLScalarType.newScalar()
                .name("DateTime")
                .description("DateTime scalar for Java LocalDateTime with ISO 8601 format support")
                .coercing(new DateTimeCoercing())
                .build();
    }

    /**
     * Custom Upload scalar for handling file uploads in GraphQL mutations
     */
    @Bean
    public GraphQLScalarType uploadScalar() {
        return GraphQLScalarType.newScalar()
                .name("Upload")
                .description("Custom scalar for multipart file uploads")
                .coercing(new UploadCoercing())
                .build();
    }

    /**
     * Coercing implementation for DateTime scalar type
     */
    private static class DateTimeCoercing implements Coercing<LocalDateTime, String> {

        @Override
        public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
            if (dataFetcherResult == null) {
                return null;
            }

            if (dataFetcherResult instanceof LocalDateTime localDateTime) {
                return localDateTime.atOffset(ZoneOffset.UTC)
                        .format(ISO_OFFSET_FORMATTER);
            }

            if (dataFetcherResult instanceof String) {
                return (String) dataFetcherResult;
            }

            throw new CoercingSerializeException(
                    String.format("Cannot serialize '%s' as DateTime. Expected LocalDateTime or String.",
                            dataFetcherResult.getClass().getSimpleName()));
        }

        @Override
        public LocalDateTime parseValue(Object input) throws CoercingParseValueException {
            if (input == null) {
                return null;
            }

            if (!(input instanceof String)) {
                throw new CoercingParseValueException(
                        String.format("Cannot parse value as DateTime. Expected String but received: %s",
                                input.getClass().getSimpleName()));
            }

            return parseDateTime((String) input);
        }

        @Override
        public LocalDateTime parseLiteral(Object input) throws CoercingParseLiteralException {
            if (input == null) {
                return null;
            }

            if (!(input instanceof StringValue)) {
                throw new CoercingParseLiteralException(
                        String.format("Cannot parse literal as DateTime. Expected StringValue but received: %s",
                                input.getClass().getSimpleName()));
            }

            String value = ((StringValue) input).getValue();
            try {
                return parseDateTime(value);
            } catch (CoercingParseValueException e) {
                throw new CoercingParseLiteralException(e.getMessage(), e);
            }
        }

        /**
         * Parses a date-time string, attempting multiple formats
         */
        private LocalDateTime parseDateTime(String dateString) throws CoercingParseValueException {
            try {
                // Try parsing as OffsetDateTime first (ISO format with offset)
                OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateString, ISO_OFFSET_FORMATTER);
                return offsetDateTime.toLocalDateTime();
            } catch (DateTimeParseException e1) {
                try {
                    // Fallback: try parsing as LocalDateTime (ISO format without offset)
                    return LocalDateTime.parse(dateString, ISO_LOCAL_FORMATTER);
                } catch (DateTimeParseException e2) {
                    throw new CoercingParseValueException(
                            String.format("Cannot parse '%s' as DateTime. Expected ISO 8601 format " +
                                    "(e.g., '2024-01-29T10:15:30Z' or '2024-01-29T10:15:30')", dateString));
                }
            }
        }
    }

    /**
     * Coercing implementation for Upload scalar type
     * Passes through the input/output without transformation for file handling
     */
    private static class UploadCoercing implements Coercing<Object, Object> {

        @Override
        public Object serialize(Object dataFetcherResult) throws CoercingSerializeException {
            return dataFetcherResult;
        }

        @Override
        public Object parseValue(Object input) throws CoercingParseValueException {
            return input;
        }

        @Override
        public Object parseLiteral(Object input) throws CoercingParseLiteralException {
            return input;
        }
    }
}