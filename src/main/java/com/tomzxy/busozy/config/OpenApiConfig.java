package com.tomzxy.busozy.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(info = @Info(title = "Busozy API", version = "1.0", description = "Hệ thống đặt vé xe khách online – Busozy", contact = @Contact(name = "Busozy Team", email = "support@busozy.vn")), servers = {
        @Server(url = "http://localhost:8080", description = "Local Dev"),
        @Server(url = "https://api.busozy.vn", description = "Production")
})
@SecurityScheme(name = "BearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", in = SecuritySchemeIn.HEADER)
@Configuration
public class OpenApiConfig {
}
