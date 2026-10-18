package com.scanin.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "ScanIn - İşçi Giriş-Çıxış (QR Attendance) API",
                version = "1.0",
                description = "QR kod vasitəsilə işçilərin işə gəliş-gediş saatlarını, gecikmələrini və ofis Wi-Fi IP yoxlamasını idarə edən REST API.",
                contact = @Contact(name = "Samir Həşimov", email = "support@scanin.az")
        ),
        // Bütün endpoint-lərdə default olaraq Bearer Token tələb olunduğunu göstərir:
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        description = "Login və ya Activate etdikdən sonra aldığınız JWT Token-i bura yapışdırın ('Bearer ' sözünü yazmağa ehtiyac yoxdur, özü əlavə edir).",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}