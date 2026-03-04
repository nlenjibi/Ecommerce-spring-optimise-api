package com.smart_ecomernce_api.smart_ecomernce_api.config;

import com.smart_ecomernce_api.smart_ecomernce_api.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Propagates the already-authenticated {@link UserPrincipal} into the
 * GraphQL execution context so that resolvers can read
 * {@code @ContextValue Long userId} (and related values) without performing
 * any additional JWT parsing or database lookups.
 *
 * <p>Authentication itself is handled entirely by {@code JwtAuthenticationFilter},
 * which runs before this interceptor and populates the {@link SecurityContextHolder}.
 * This interceptor is intentionally kept to a single responsibility: bridging the
 * Spring Security context into the GraphQL context.
 */
@Component
@Slf4j
public class GraphQLContextInterceptor implements WebGraphQlInterceptor {

    @Override
    public @NonNull Mono<WebGraphQlResponse> intercept(@NonNull WebGraphQlRequest request,
                                                       @NonNull Chain chain) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserPrincipal principal) {

            request.configureExecutionInput((executionInput, builder) ->
                    builder.graphQLContext(ctx -> {
                        ctx.put("userId",        principal.getId());
                        ctx.put("userRole",      principal.getRole().name());
                        ctx.put("userPrincipal", principal);
                    }).build()
            );

            log.debug("GraphQL context populated for user {} with role {}",
                    principal.getId(), principal.getRole());
        }

        return chain.next(request);
    }
}