package com.smart_ecomernce_api.smart_ecomernce_api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@Slf4j
public class AspectConfiguration {
    
    public AspectConfiguration() {
        log.info("AOP Configuration initialized - Aspects are active");
    }
}
