package com.smart_ecomernce_api.smart_ecomernce_api.controller;

import com.smart_ecomernce_api.smart_ecomernce_api.security.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class HomeSecurityRules implements SecurityRules {

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // Allow root path (home/health check)
                .requestMatchers(HttpMethod.GET, "/", "/api", "/api/health").permitAll()
                // Allow static resources if you have any
                .requestMatchers(HttpMethod.GET, "/index.html", "/favicon.ico", "/static/**").permitAll();
    }
}