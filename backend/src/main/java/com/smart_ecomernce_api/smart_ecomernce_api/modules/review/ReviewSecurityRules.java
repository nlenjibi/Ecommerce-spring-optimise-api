package com.smart_ecomernce_api.smart_ecomernce_api.modules.review;

import com.smart_ecomernce_api.smart_ecomernce_api.security.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class ReviewSecurityRules implements SecurityRules {
    @Override
    public void configure(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                .requestMatchers("/v1/reviews/admin/**").hasRole("ADMIN")
                .requestMatchers("/v1/reviews/admin").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/v1/reviews").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/reviews/**").permitAll();
    }
}
