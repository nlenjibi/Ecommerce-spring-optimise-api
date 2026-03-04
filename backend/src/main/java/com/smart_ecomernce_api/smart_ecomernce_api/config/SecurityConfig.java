package com.smart_ecomernce_api.smart_ecomernce_api.config;

import com.smart_ecomernce_api.smart_ecomernce_api.exception.CustomAccessDeniedHandler;
import com.smart_ecomernce_api.smart_ecomernce_api.exception.CustomAuthenticationEntryPoint;
import com.smart_ecomernce_api.smart_ecomernce_api.security.*;
import com.smart_ecomernce_api.smart_ecomernce_api.services.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Central Spring Security configuration.
 *
 * <h3>Session policy rationale</h3>
 * The API is stateless (JWT-based), so we use {@code IF_REQUIRED} rather than
 * {@code STATELESS} solely because the OAuth2 authorization-code flow requires
 * a short-lived HTTP session to carry the PKCE state / nonce between the
 * redirect to the provider and the callback. Once the success handler fires and
 * the JWT cookies are set, the session is no longer needed.
 *
 * <h3>Cross-origin (Next.js on a different server)</h3>
 * CORS is fully driven by {@code application.properties}. For the cookie-based
 * token scheme to work across origins, the frontend must call the API with
 * {@code credentials: 'include'} (fetch) or {@code withCredentials: true}
 * (axios), and the cookies must carry {@code SameSite=None; Secure=true}
 * (configured in the auth handlers via {@code app.cookie.secure}).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    // ── Core security components ───────────────────────────────────────────────
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PasswordEncoder passwordEncoder;

    // ── Exception handlers ─────────────────────────────────────────────────────
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    // ── OAuth2 handlers ────────────────────────────────────────────────────────
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    // ── Authorization rules ────────────────────────────────────────────────────
    private final List<SecurityRules> securityRules;

    // ── CORS ───────────────────────────────────────────────────────────────────
    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Value("${cors.allowed-methods}")
    private List<String> allowedMethods;

    @Value("${cors.max-age:3600}")
    private Long maxAge;

    // ── Security filter chain ──────────────────────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF disabled: we rely on SameSite cookies + CORS, not CSRF tokens.
                .csrf(AbstractHttpConfigurer::disable)

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )

                // Session is needed only during the OAuth2 authorization-code flow.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation().migrateSession()
                        // Limit concurrent sessions as a defence-in-depth measure.
                        .maximumSessions(5)
                )

                .authorizeHttpRequests(auth -> {
                    if (securityRules != null) {
                        securityRules.forEach(rule -> rule.configure(auth));
                    }
                    auth.anyRequest().authenticated();
                })

                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestRepository(httpSessionOAuth2AuthorizationRequestRepository())
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                )

                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ── Beans ──────────────────────────────────────────────────────────────────

    @Bean
    public HttpSessionOAuth2AuthorizationRequestRepository httpSessionOAuth2AuthorizationRequestRepository() {
        return new HttpSessionOAuth2AuthorizationRequestRepository();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(allowedOrigins);   // Must be explicit, not "*", for credentialed requests
        config.setAllowedMethods(allowedMethods);
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);           // Required so the browser sends cookies cross-origin
        config.setMaxAge(maxAge);

        log.debug("CORS configured — allowed origins: {}", allowedOrigins);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}