package com.codesync.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class AuthServiceApplication {

	
	public static void main(String[] args) {
		ConfigurableApplicationContext run = SpringApplication.run(AuthServiceApplication.class, args);

	}

}
