package com.smart_ecomernce_api.smart_ecomernce_api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class GraphiQLStartupLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GraphiQLStartupLogger.class);

    @Value("${server.port:9190}")
    private int serverPort;

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    @Override
    public void run(ApplicationArguments args) {
        String baseUrl = "http://localhost:" + serverPort + contextPath;

        log.info("GraphQL Server Started Successfully!");
        log.info("GraphQL Endpoint: {}/graphql", baseUrl);
        log.info("GraphiQL UI: {}/graphiql", baseUrl);
        log.info("Swagger UI: {}/swagger-ui.html", baseUrl);
    }
}