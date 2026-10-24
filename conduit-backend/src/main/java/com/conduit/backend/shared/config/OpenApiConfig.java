package com.conduit.backend.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI conduitOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("RealWorld Conduit API")
                        .description("Conduit API documentation")
                        .version("2.0.0"))
                .components(new Components().addSecuritySchemes("Token", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("Authorization")
                        .description("Format: Token xxxxxx.yyyyyyy.zzzzzz")));
    }
}
