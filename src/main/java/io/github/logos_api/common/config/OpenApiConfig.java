package io.github.logos_api.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * `/v3/api-docs`로 내려가는 OpenAPI 스펙의 공통 정보를 정의하는 구성 클래스입니다.
 * <p>
 * 스펙은 Postman이나 코드 생성기, AI 에이전트가 읽어 쓰는 기계용 문서입니다.
 * 사람이 보는 문서는 `/docs` 페이지가 맡으므로 Swagger UI는 두지 않습니다.
 * </p>
 */
@Configuration
public class OpenApiConfig {

    private static final String API_KEY_SCHEME = "ApiKeyAuth";

    /**
     * API 제목과 설명, 운영 주소, 인증 방식을 담은 스펙 정보를 만듭니다.
     *
     * @return OpenAPI 스펙 정의
     */
    @Bean
    public OpenAPI logosOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Logos API")
                        .version("1.0")
                        .description("개역개정 성경 구절을 제공하는 REST API입니다. "
                                + "모든 요청에는 X-API-KEY 헤더가 필요하며, 키는 https://logos-api.com 에서 발급합니다. "
                                + "운영시간은 08:00 ~ 22:00 (KST)입니다.")
                        .license(new License()
                                .name("비상업 · 수정 금지 v1.1")
                                .url("https://github.com/Hasegos/logos-api/blob/master/LICENSE")))
                .servers(List.of(new Server().url("https://logos-api.com").description("운영")))
                .components(new Components().addSecuritySchemes(API_KEY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-KEY")
                                .description("발급받은 UUID")))
                .addSecurityItem(new SecurityRequirement().addList(API_KEY_SCHEME));
    }
}
