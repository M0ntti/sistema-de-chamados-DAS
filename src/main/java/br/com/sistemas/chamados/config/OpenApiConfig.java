package br.com.sistemas.chamados.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/** Informacoes exibidas no topo do Swagger UI (http://localhost:8080/swagger-ui.html). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiChamados() {
        return new OpenAPI().info(new Info()
            .title("Sistema de Chamados - API")
            .description("API REST de chamados de suporte. "
                + "Trabalho da disciplina DAS - Design e Arquitetura de Sistemas (UNIVILLE).")
            .version("1.0.0"));
    }
}
