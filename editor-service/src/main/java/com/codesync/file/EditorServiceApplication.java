package com.codesync.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class EditorServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EditorServiceApplication.class, args);
	}

}
