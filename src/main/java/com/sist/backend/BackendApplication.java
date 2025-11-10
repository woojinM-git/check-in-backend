package com.sist.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling // @Scheduled 어노테이션을 사용하기 위해 필요
@EnableAsync // @Async 어노테이션을 사용하기 위해 필요
public class BackendApplication {

	public static void main(String[] args) {
		// JVM 타임존을 한국 시간으로 설정
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
		
		Dotenv dotenv = Dotenv.load();
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });
		
		SpringApplication.run(BackendApplication.class, args);
	}

}
