package com.smart_ecomernce_api.smart_ecomernce_api.config;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.repository.UserRepository;
import com.smart_ecomernce_api.smart_ecomernce_api.security.JwtTokenProvider;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class GraphQLSecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    private static final Set<String> PUBLIC_OPERATIONS = Set.of(
            "products", "categories", "product", "category",
            "searchProducts", "searchCategories", "activeCategories", "featuredProducts",
            "login", "register", "newProducts", "discountedProducts"
    );

    private static final Set<String> AUTHENTICATED_OPERATIONS = Set.of(
            "userProfile", "myOrders", "myCart", "myWishlist", "orderHistory",
            "addToCart", "removeFromCart", "updateCartItem",
            "addToWishlist", "removeFromWishlist",
            "createOrder", "cancelOrder",
            "createReview", "updateReview", "deleteReview",
            "updateProfile", "changePassword", "mySessions"
    );

    private static final Set<String> ADMIN_OPERATIONS = Set.of(
            "users", "allOrders", "adminDashboard", "performanceMetrics",
            "createUser", "updateUser", "deleteUser",
            "createProduct", "updateProduct", "deleteProduct",
            "createCategory", "updateCategory", "deleteCategory",
            "lockAccount", "unlockAccount", "analytics"
    );

    private static final Set<String> STAFF_OPERATIONS = Set.of(
            "staffDashboard", "manageOrders", "processOrder",
            "manageProducts", "viewAllReviews", "moderateReview"
    );

    public boolean isPublicOperation(String operationName) {
        return PUBLIC_OPERATIONS.contains(operationName);
    }

    public boolean requiresAuthentication(String operationName) {
        return AUTHENTICATED_OPERATIONS.contains(operationName) || 
               ADMIN_OPERATIONS.contains(operationName) ||
               STAFF_OPERATIONS.contains(operationName);
    }

    public boolean requiresAdminRole(String operationName) {
        return ADMIN_OPERATIONS.contains(operationName);
    }

    public boolean requiresStaffRole(String operationName) {
        return STAFF_OPERATIONS.contains(operationName);
    }

    public void authenticateRequest(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtTokenProvider.validateToken(token)) {
            return;
        }

        try {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            String role = jwtTokenProvider.getRoleFromToken(token);

            userRepository.findById(userId).ifPresent(user -> {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("GraphQL authenticated: {} with role: {}", user.getEmail(), role);
            });
        } catch (Exception e) {
            log.warn("GraphQL authentication error: {}", e.getMessage());
        }
    }

    @Bean
    public graphql.execution.DataFetcherExceptionHandler dataFetcherExceptionHandler() {
        return new graphql.execution.DataFetcherExceptionHandler() {
            @Override
            public java.util.concurrent.CompletableFuture<graphql.execution.DataFetcherExceptionHandlerResult> handleException(
                    graphql.execution.DataFetcherExceptionHandlerParameters handlerParameters) {

                Throwable exception = handlerParameters.getException();
                DataFetchingEnvironment environment = handlerParameters.getDataFetchingEnvironment();
                String fieldName = environment.getField().getName();

                log.warn("GraphQL exception on field '{}': {}", fieldName, exception.getMessage());

                String message = exception.getMessage() != null ? exception.getMessage() : "Internal error";
                
                if (exception instanceof org.springframework.security.access.AccessDeniedException) {
                    message = "Access denied. Insufficient permissions.";
                } else if (exception instanceof org.springframework.security.core.AuthenticationException) {
                    message = "Authentication required for this operation";
                }

                return java.util.concurrent.CompletableFuture.completedFuture(
                        graphql.execution.DataFetcherExceptionHandlerResult.newResult()
                                .error(graphql.GraphqlErrorBuilder.newError()
                                        .message(message)
                                        .location(environment.getField().getSourceLocation())
                                        .build())
                                .build());
            }
        };
    }
}
