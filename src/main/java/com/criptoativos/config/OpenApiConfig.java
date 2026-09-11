package com.criptoativos.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Paste the accessToken returned by POST /api/v1/auth/login.")
public class OpenApiConfig {

    @Bean
    OpenAPI criptoAtivosOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("CriptoAtivos API")
                                .version("v1")
                                .description(
                                        """
                                        Crypto portfolio management: accounts, two-factor authentication, \
                                        wallets, live market prices, and trading against a finite exchange \
                                        inventory.

                                        Most endpoints require a bearer token. Register, log in, then use \
                                        the Authorize button above.\
                                        """)
                                .contact(
                                        new Contact()
                                                .name("CriptoAtivos")
                                                .url(
                                                        "https://github.com/alvarogalhardo/CriptoAtivos"))
                                .license(
                                        new License()
                                                .name("MIT")
                                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
