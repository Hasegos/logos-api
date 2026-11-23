package io.github.logos_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Logos API 애플리케이션의 시작 클래스입니다.
 */
@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
public class LogosApiApplication {

	/**
	 * 애플리케이션을 시작합니다.
	 *
	 * @param args 실행 인자
	 */
	public static void main(String[] args) {
		SpringApplication.run(LogosApiApplication.class, args);
	}
}