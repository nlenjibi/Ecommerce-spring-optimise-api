package com.smart_ecomernce_api.smart_ecomernce_api.exception;

import graphql.ErrorClassification;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class GraphQLExceptionResolver extends DataFetcherExceptionResolverAdapter {


    private static final ErrorClassification UNAUTHORIZED = buildClassification("UNAUTHORIZED");
    private static final ErrorClassification FORBIDDEN    = buildClassification("FORBIDDEN");
    private static final ErrorClassification NOT_FOUND    = buildClassification("NOT_FOUND");

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {

        String path = env.getExecutionStepInfo().getPath().toString();

        // ── 401-equivalent ─────────────────────────────────────────────────────
        if (ex instanceof TokenNotFoundException
                || ex instanceof TokenExpiredException
                || ex instanceof InvalidTokenException) {
            log.warn("GraphQL token error on '{}': {}", path, ex.getMessage());
            return error(env, UNAUTHORIZED, "Authentication required: " + ex.getMessage());
        }

        if (ex instanceof BadCredentialsException
                || ex instanceof UsernameNotFoundException) {
            log.warn("GraphQL authentication failure on '{}': {}", path, ex.getMessage());
            return error(env, UNAUTHORIZED, "Invalid credentials.");
        }

        if (ex instanceof AccountExpiredException
                || ex instanceof LockedException
                || ex instanceof DisabledException
                || ex instanceof CredentialsExpiredException) {
            log.warn("GraphQL account status error on '{}' [{}]: {}", path, ex.getClass().getSimpleName(), ex.getMessage());
            return error(env, UNAUTHORIZED, "Account is not in a valid state. Please contact support.");
        }

        // ── 403-equivalent ─────────────────────────────────────────────────────
        if (ex instanceof AccessDeniedException || ex instanceof AuthorizationException) {
            log.warn("GraphQL access denied on '{}': {}", path, ex.getMessage());
            return error(env, FORBIDDEN, "You do not have permission to perform this action.");
        }

        if (ex instanceof AccountLockedException) {
            log.warn("GraphQL locked account on '{}': {}", path, ex.getMessage());
            return error(env, FORBIDDEN, ex.getMessage());
        }

        // ── 404-equivalent ─────────────────────────────────────────────────────
        if (ex instanceof ResourceNotFoundException || ex instanceof CartNotFoundException) {
            log.warn("GraphQL resource not found on '{}': {}", path, ex.getMessage());
            return error(env, NOT_FOUND, ex.getMessage());
        }

        // ── Validation / business rule violations ──────────────────────────────
        if (ex instanceof InvalidDataException || ex instanceof BadRequestException) {
            log.warn("GraphQL validation error on '{}': {}", path, ex.getMessage());
            return error(env, ErrorType.ValidationError, ex.getMessage());
        }

        // ── Conflict / resource state errors ───────────────────────────────────
        if (ex instanceof DuplicateResourceException) {
            log.warn("GraphQL duplicate resource on '{}': {}", path, ex.getMessage());
            return error(env, ErrorType.ExecutionAborted, ex.getMessage());
        }

        if (ex instanceof InsufficientStockException) {
            log.warn("GraphQL insufficient stock on '{}': {}", path, ex.getMessage());
            return error(env, ErrorType.ExecutionAborted, ex.getMessage());
        }

        if (ex instanceof RateLimitExceededException) {
            log.warn("GraphQL rate limit exceeded on '{}': {}", path, ex.getMessage());
            return error(env, ErrorType.ExecutionAborted,
                    "Too many requests. Please slow down and try again.");
        }

        // ── Catch-all ──────────────────────────────────────────────────────────
        // Log full stack trace for unexpected errors — critical for debugging.
        log.error("Unexpected GraphQL error on '{}': {}", path, ex.getMessage(), ex);
        return error(env, ErrorType.DataFetchingException,
                "An unexpected error occurred. Please contact support if this persists.");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private GraphQLError error(DataFetchingEnvironment env,
                               ErrorClassification classification,
                               String message) {
        return GraphqlErrorBuilder.newError()
                .errorType(classification)
                .message(message)
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
    }

    /**
     * Creates a custom {@link ErrorClassification} that appears as the
     * {@code "classification"} field in the GraphQL error extensions block,
     * allowing frontend clients to switch on typed error codes.
     */
    private static ErrorClassification buildClassification(String name) {
        return new ErrorClassification() {
            @Override
            public String toString() { return name; }
        };
    }
}