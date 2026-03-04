package com.smart_ecomernce_api.smart_ecomernce_api.modules.product;

import com.smart_ecomernce_api.smart_ecomernce_api.security.SecurityRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class ProductSecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                .requestMatchers("/v1/products/admin/**").hasRole("ADMIN")
                .requestMatchers("/v1/products/admin").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/v1/products/**").permitAll();
    }
}
