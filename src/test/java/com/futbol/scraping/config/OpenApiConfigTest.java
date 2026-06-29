package com.futbol.scraping.config;

import com.futbol.scraping.annotation.FutbolUnit;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@FutbolUnit
class OpenApiConfigTest {

    private final OpenApiConfig openApiConfig = new OpenApiConfig();

    @Test
    void futbolScrapingOpenAPI_ContainsExpectedMetadata() {
        OpenAPI openAPI = openApiConfig.futbolScrapingOpenAPI();

        assertThat(openAPI).isNotNull();
        Info info = openAPI.getInfo();

        assertThat(info).isNotNull();
        assertThat(info.getTitle()).isEqualTo("Futbol Scraping API");
        assertThat(info.getDescription()).isEqualTo("API para sincronizacion de jugadores, cotizaciones y ordenes.");
        assertThat(info.getVersion()).isEqualTo("v1");

        assertThat(info.getContact()).isNotNull();
        assertThat(info.getContact().getName()).isEqualTo("Futbol Scraping Team");
        assertThat(info.getContact().getEmail()).isEqualTo("soporte@futbol.local");

        assertThat(info.getLicense()).isNotNull();
        assertThat(info.getLicense().getName()).isEqualTo("Apache 2.0");
        assertThat(info.getLicense().getUrl()).isEqualTo("https://www.apache.org/licenses/LICENSE-2.0");
    }
}

